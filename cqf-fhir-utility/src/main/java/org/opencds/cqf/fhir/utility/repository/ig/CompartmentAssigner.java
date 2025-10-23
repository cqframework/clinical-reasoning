package org.opencds.cqf.fhir.utility.repository.ig;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.model.api.IQueryParameterType;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CompartmentIsolation;

/**
 * Determines the compartment assignment for a resource based on repository conventions
 * and resource references.
 */
class CompartmentAssigner {

    private static final List<String> COMPARTMENT_REFERENCE_PRIORITY =
            List.of("subject", "patient", "beneficiary", "member", "individual", "encounter", "episodeofcare");

    private final FhirContext fhirContext;
    private final CompartmentMode compartmentMode;
    private final CompartmentIsolation compartmentIsolation;

    CompartmentAssigner(
            FhirContext fhirContext, CompartmentMode compartmentMode, CompartmentIsolation compartmentIsolation) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext cannot be null");
        this.compartmentMode = Objects.requireNonNull(compartmentMode, "compartmentMode cannot be null");
        this.compartmentIsolation = Objects.requireNonNull(compartmentIsolation, "compartmentIsolation cannot be null");
    }

    CompartmentAssignment fromHeaders(Map<String, String> headers) {
        if (this.compartmentMode == CompartmentMode.NONE) {
            return CompartmentAssignment.none();
        }

        if (headers == null) {
            return CompartmentAssignment.none();
        }

        var headerValue = headers.get(IgRepository.FHIR_COMPARTMENT_HEADER);
        if (headerValue == null || headerValue.isBlank()) {
            return CompartmentAssignment.none();
        }

        var segments = headerValue.split("/");
        if (segments.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid compartment header value. Expected format: <CompartmentType>/<CompartmentId>");
        }

        var compartmentType = segments[0].trim();
        var compartmentId = segments[1].trim();
        return CompartmentAssignment.of(compartmentType, compartmentId);
    }

    /**
     * Determines the compartment assignment for the supplied resource based on repository conventions.
     *
     * Possible outcomes:
     * - NONE: No compartment assignment (repository is not compartmentalized)
     * - SHARED: Resource is shared across all compartments
     * - Specific compartment: Resource is assigned to a specific compartment (e.g. Patient/123)
     * - UNKNOWN: Resource belongs to the current compartment type, but the specific compartment cannot be determined
     * @return The compartment assignment for the resource
     */
    CompartmentAssignment fromResource(IBaseResource resource) {
        requireNonNull(resource, "resource cannot be null");
        requireNonNull(resource.getIdElement().getIdPart(), "resource id cannot be null");

        var resourceType = resource.fhirType();
        var assignment = typeBasedAssignment(resourceType);
        if (!assignment.isUnknown()) {
            return assignment;
        }
        // If the resource is of the compartment type, assign it to its own compartment.
        if (compartmentMode.type().equalsIgnoreCase(resourceType)) {
            return CompartmentAssignment.of(
                    compartmentMode.type(), resource.getIdElement().getIdPart());
        }

        return resolveFrom(resource);
    }

    /**
     * Determines the compartment assignment for the supplied resource type and id
     *
     * Possible outcomes:
     * - NONE: No compartment assignment (repository is not compartmentalized)
     * - SHARED: Resource is shared across all compartments
     * - Specific compartment: Resource is assigned to a specific compartment (e.g. Patient/123)
     * - UNKNOWN: Resource belongs to the current compartment type, but the specific compartment cannot be determined
     * @return The compartment assignment for the resource
     */
    CompartmentAssignment fromId(String resourceType, IIdType id) {
        requireNonNull(resourceType, "resourceType cannot be null");
        requireNonNull(id, "id cannot be null");

        var assignment = typeBasedAssignment(resourceType);
        if (!assignment.isUnknown()) {
            return assignment;
        }

        // If the resource is of the compartment type, assign it to its own compartment.
        if (compartmentMode.type().equalsIgnoreCase(resourceType)) {
            return CompartmentAssignment.of(compartmentMode.type(), id.getIdPart());
        }

        return CompartmentAssignment.unknown(compartmentMode.type());
    }

    private CompartmentAssignment resolveFrom(IBaseResource resource) {
        var resourceType = resource.fhirType();
        var searchParams = compartmentMode.compartmentSearchParams(fhirContext, resourceType);
        if (searchParams.isEmpty()) {
            return CompartmentAssignment.unknown(this.compartmentMode.type());
        }

        var terser = fhirContext.newTerser();
        var compartmentType = compartmentMode.type();

        var orderedParams = orderCompartmentParams(searchParams);

        // Here we're looking at all the attributes on a FHIR resource that
        // potentially provide compartment membership, by matching those up
        // to search paramters that provide compartment membership. We're
        // checking all those paths on the research in priority order
        // (i.e. the "patient" search parameter is more specific than "subject"
        // so search attributes that "patient" indexes on first) looking for
        // references to our current compartment type. The first one we find
        // is the compartment assignment.
        for (var param : orderedParams) {
            for (var originalPath : param.getPathsSplit()) {
                var path = sanitizeSearchPath(originalPath);
                if (path == null || path.isBlank()) {
                    continue;
                }

                var values = terser.getValues(resource, path);
                var ids = referencedIds(values).stream()
                        .filter(i -> compartmentType.equals(i.getResourceType()))
                        .toList();

                if (!ids.isEmpty()) {
                    var id = ids.get(0);
                    return CompartmentAssignment.of(compartmentType, id.getIdPart());
                }
            }
        }

        return CompartmentAssignment.shared();
    }

    private List<IIdType> referencedIds(List<IBase> values) {
        var ids = new ArrayList<IIdType>();
        for (var value : values) {
            var reference = extractReference(value);
            if (reference != null && reference.hasResourceType() && reference.hasIdPart()) {
                ids.add(reference);
            }
        }
        return ids;
    }

    /**
     * Determines the compartment assignments based on search parameters. If there are multiple possible
     * compartment assignments, only the first is returned.
     *
     * Possible outcomes:
     * - NONE: No compartment assignment (repository is not compartmentalized)
     * - SHARED: Resource is shared across all compartments
     * - Specific compartment: Resource is assigned to a specific compartment (e.g. Patient/123)
     * - UNKNOWN: Resource belongs to the current compartment type, but the specific compartment cannot be determined
     * @return The compartment assignment for the search
     */
    public CompartmentAssignment fromSearchParameters(
            String resourceType, Multimap<String, List<IQueryParameterType>> searchParameters) {
        requireNonNull(resourceType, "resourceType cannot be null");

        var assignment = typeBasedAssignment(resourceType);
        if (!assignment.isUnknown()) {
            return assignment;
        }

        // Try to assign based on id
        // TODO: This can fail when there are multiple ids being searched.
        // (e.g. _id=123,456,789)
        // A future version of this class might return mutliple potential
        // Assignments, each of which can be searched.
        if (compartmentMode.type().equalsIgnoreCase(resourceType)) {
            var idString = searchParameters.get("_id").stream()
                    .flatMap(List::stream)
                    .findFirst()
                    .map(x -> x.getValueAsQueryToken(fhirContext))
                    .orElse(null);

            var ensured = Ids.ensureIdType(idString, resourceType);
            var id = Ids.newId(fhirContext, ensured);
            return CompartmentAssignment.of(compartmentMode.type(), id.getIdPart());
        }

        var membershipParams =
                orderCompartmentParams(compartmentMode.compartmentSearchParams(fhirContext, resourceType));

        for (var param : membershipParams) {
            var values = collectQueryParameters(searchParameters, param.getName());
            if (values.isEmpty()) {
                continue;
            }

            // Certain search parameters that target only one type can be abbreviated
            // if they only target one type:
            // Observation?patient=123
            //
            // Others are ambiguous if unspecified:
            // Observation?subject=ABC (Bad! A Patient or Device?)
            // Observation?subject=Patient/123 (Good! We can work with this)
            var target = param.hasTargets() && param.getTargets().size() == 1
                    ? param.getTargets().iterator().next()
                    : null;
            assignment = resolveFromQuery(target, values);

            // We found and assignment, bail out.
            if (!assignment.isUnknown()) {
                return assignment;
            }
        }

        // Did our best, coudln't determine the assignment.
        return CompartmentAssignment.unknown(compartmentMode.type());
    }

    private CompartmentAssignment typeBasedAssignment(String resourceType) {
        if (compartmentMode == CompartmentMode.NONE) {
            return CompartmentAssignment.none();
        }

        var resourceCategory = ResourceCategory.forType(resourceType);
        if (resourceCategory != ResourceCategory.DATA) {
            return compartmentIsolation == CompartmentIsolation.FULL
                    ? CompartmentAssignment.unknown(this.compartmentMode.type())
                    : CompartmentAssignment.shared();
        }

        if (!this.compartmentMode.resourceBelongsToCompartment(fhirContext, resourceType)) {
            return compartmentIsolation == CompartmentIsolation.FULL
                    ? CompartmentAssignment.unknown(this.compartmentMode.type())
                    : CompartmentAssignment.shared();
        }

        return CompartmentAssignment.unknown(this.compartmentMode.type());
    }

    private String sanitizeSearchPath(String path) {
        if (path == null) {
            return null;
        }

        var sanitized = path;
        var whereIndex = sanitized.indexOf(".where(");
        if (whereIndex >= 0) {
            sanitized = sanitized.substring(0, whereIndex);
        }

        // The terser expects paths relative to the resource and does not understand resolve()
        return sanitized.replace(".resolve()", "");
    }

    private IIdType extractReference(Object value) {
        if (value instanceof IIdType idType) {
            return idType;
        }

        if (value instanceof IPrimitiveType<?> primitive) {
            var asString = primitive.getValueAsString();
            if (asString == null || asString.isBlank()) {
                return null;
            }

            return Ids.newId(fhirContext, asString);
        }

        if (value instanceof IBaseReference reference) {
            return reference.getReferenceElement();
        }

        return null;
    }

    private List<RuntimeSearchParam> orderCompartmentParams(Set<RuntimeSearchParam> params) {
        return params.stream()
                .sorted(Comparator.comparingInt((RuntimeSearchParam param) -> priorityIndex(param.getName()))
                        .thenComparing(RuntimeSearchParam::getName))
                .toList();
    }

    private int priorityIndex(String name) {
        if (name == null) {
            return COMPARTMENT_REFERENCE_PRIORITY.size();
        }
        var normalized = name.toLowerCase();
        var index = COMPARTMENT_REFERENCE_PRIORITY.indexOf(normalized);
        return index >= 0 ? index : COMPARTMENT_REFERENCE_PRIORITY.size();
    }

    private List<IQueryParameterType> collectQueryParameters(
            Multimap<String, List<IQueryParameterType>> searchParameters, String expectedName) {
        var matches = new ArrayList<IQueryParameterType>();
        if (searchParameters == null || expectedName == null) {
            return matches;
        }

        var normalized = expectedName.toLowerCase();
        var searchParamKeys = searchParameters.keySet().stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .toList();

        for (var key : searchParamKeys) {
            if (!key.startsWith(normalized)) {
                continue;
            }

            var searchGroups = searchParameters.get(key);
            for (var group : searchGroups) {
                matches.addAll(group);
            }
        }

        return matches;
    }

    private CompartmentAssignment resolveFromQuery(String targetIfUnspecified, List<IQueryParameterType> values) {
        requireNonNull(values, "values cannot be null");

        var compartmentType = compartmentMode.type();
        for (var queryValue : values) {
            var token = queryValue.getValueAsQueryToken(fhirContext);
            var id = idFromToken(targetIfUnspecified, token);
            if (id != null && compartmentType.equalsIgnoreCase(id.getResourceType())) {
                return CompartmentAssignment.of(compartmentType, id.getIdPart());
            }
        }

        return CompartmentAssignment.unknown(this.compartmentMode.type());
    }

    private IIdType idFromToken(String target, String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        // If no target is specified, assume the token is of the target compartment type
        if (target == null || target.isBlank()) {
            return Ids.newId(fhirContext, token);
        } else {
            var ensured = Ids.ensureIdType(token, target);
            return Ids.newId(fhirContext, ensured);
        }
    }
}
