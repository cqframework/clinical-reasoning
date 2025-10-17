package org.opencds.cqf.fhir.utility.repository.ig;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.model.api.IQueryParameterType;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Ids;

/**
 * Determines the compartment assignment for a resource based on repository conventions
 * and resource references.
 */
class CompartmentAssigner {

    private static final List<String> COMPARTMENT_REFERENCE_PRIORITY =
            List.of("subject", "patient", "beneficiary", "member", "individual", "encounter", "episodeofcare");

    private final FhirContext fhirContext;
    private final CompartmentMode compartmentMode;

    CompartmentAssigner(FhirContext fhirContext, CompartmentMode compartmentMode) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext cannot be null");
        this.compartmentMode = Objects.requireNonNull(compartmentMode, "compartmentMode cannot be null");
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
    CompartmentAssignment assign(IBaseResource resource) {
        requireNonNull(resource, "resource cannot be null");
        requireNonNull(resource.getIdElement().getIdPart(), "resource id cannot be null");

        if (compartmentMode == CompartmentMode.NONE) {
            return CompartmentAssignment.none();
        }

        var resourceType = resource.fhirType();
        var resourceCategory = ResourceCategory.forType(resourceType);
        if (resourceCategory != ResourceCategory.DATA) {
            return CompartmentAssignment.shared();
        }

        if (!this.compartmentMode.resourceBelongsToCompartment(fhirContext, resourceType)) {
            return CompartmentAssignment.shared();
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
    CompartmentAssignment assign(String resourceType, IIdType id) {
        requireNonNull(resourceType, "resourceType cannot be null");
        requireNonNull(id, "id cannot be null");

        if (compartmentMode == CompartmentMode.NONE) {
            return CompartmentAssignment.none();
        }

        var resourceCategory = ResourceCategory.forType(resourceType);
        if (resourceCategory != ResourceCategory.DATA) {
            return CompartmentAssignment.shared();
        }

        if (!this.compartmentMode.resourceBelongsToCompartment(fhirContext, resourceType)) {
            return CompartmentAssignment.shared();
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
            return CompartmentAssignment.shared();
        }

        var terser = fhirContext.newTerser();
        var compartmentType = compartmentMode.type();

        var candidates = new ArrayList<ReferenceCandidate>();
        var orderedParams = orderCompartmentParams(searchParams);
        var sequence = 0;

        for (var param : orderedParams) {
            for (var originalPath : param.getPathsSplit()) {
                var path = sanitizeSearchPath(originalPath);
                if (path == null || path.isBlank()) {
                    continue;
                }

                var values = terser.getValues(resource, path);
                for (var value : values) {
                    var reference = extractReference(value);
                    if (reference == null || !reference.hasIdPart()) {
                        continue;
                    }

                    var referencedType = reference.getResourceType();
                    if (referencedType == null || referencedType.isBlank()) {
                        referencedType = compartmentType;
                    }

                    if (compartmentType.equalsIgnoreCase(referencedType)) {
                        candidates.add(new ReferenceCandidate(
                                priorityIndex(param.getName()), sequence++, reference.getIdPart()));
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return CompartmentAssignment.shared();
        }

        candidates.sort(
                Comparator.comparingInt(ReferenceCandidate::priority).thenComparingInt(ReferenceCandidate::sequence));

        return CompartmentAssignment.of(compartmentType, candidates.get(0).id());
    }

    /**
     * Determines the compartment assignments based on search parameters. If there are multiple possible
     * compartment assignments, only the first is returned.
     *
     * Possile outcomes:
     * - NONE: No compartment assignment (repository is not compartmentalized)
     * - SHARED: Resource is shared across all compartments
     * - Specific compartment: Resource is assigned to a specific compartment (e.g. Patient/123)
     * - UNKNOWN: Resource belongs to the current compartment type, but the specific compartment cannot be determined
     * @return The compartment assignment for the search
     */
    public CompartmentAssignment assign(
            String resourceType, Multimap<String, List<IQueryParameterType>> searchParameters) {
        requireNonNull(resourceType, "resourceType cannot be null");
        if (compartmentMode == CompartmentMode.NONE) {
            return CompartmentAssignment.none();
        }

        var resourceCategory = ResourceCategory.forType(resourceType);
        if (resourceCategory != ResourceCategory.DATA) {
            return CompartmentAssignment.shared();
        }

        if (!this.compartmentMode.resourceBelongsToCompartment(fhirContext, resourceType)) {
            return CompartmentAssignment.shared();
        }

        if (compartmentMode.type().equalsIgnoreCase(resourceType)) {
            var idString = searchParameters.get("_id").stream()
                    .flatMap(List::stream)
                    .findFirst()
                    .map(x -> x.getValueAsQueryToken(fhirContext))
                    .orElse(null);

            var id = Ids.newId(fhirContext, idString);

            if (id.hasIdPart()) {
                return CompartmentAssignment.of(compartmentMode.type(), id.getIdPart());
            } else {
                return CompartmentAssignment.unknown(compartmentMode.type());
            }
        }

        var membershipParams =
                orderCompartmentParams(compartmentMode.compartmentSearchParams(fhirContext, resourceType));

        for (var param : membershipParams) {
            var values = collectQueryParameters(searchParameters, param.getName());
            if (values.isEmpty()) {
                continue;
            }

            var assignment = resolveFromQuery(values);
            if (assignment == null) {
                continue;
            }

            if (assignment.hasCompartmentId()) {
                return assignment;
            }
        }

        return CompartmentAssignment.unknown(compartmentMode.type());
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

            var ensured = Ids.ensureIdType(asString, compartmentMode.type());
            return Ids.newId(fhirContext, ensured);
        }

        if (value instanceof IBaseReference reference) {
            var element = reference.getReferenceElement();
            if (element == null || !element.hasIdPart()) {
                return null;
            }

            if (!element.hasResourceType()) {
                return Ids.newId(fhirContext, compartmentMode.type(), element.getIdPart());
            }

            return element;
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
        for (var key : searchParameters.keySet()) {
            if (key == null) {
                continue;
            }

            var lowerKey = key.toLowerCase();
            if (!lowerKey.equals(normalized) && !lowerKey.startsWith(normalized + ":")) {
                continue;
            }

            for (var group : searchParameters.get(key)) {
                if (group == null) {
                    continue;
                }
                matches.addAll(group);
            }
        }

        return matches;
    }

    private CompartmentAssignment resolveFromQuery(List<IQueryParameterType> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        var compartmentType = compartmentMode.type();
        var sawValue = false;
        for (var queryValue : values) {
            if (queryValue == null) {
                continue;
            }

            sawValue = true;
            var token = queryValue.getValueAsQueryToken(fhirContext);
            if (token == null || token.isBlank()) {
                continue;
            }

            var ensured = Ids.ensureIdType(token, compartmentType);
            var id = Ids.newId(fhirContext, ensured);
            if (!id.hasIdPart()) {
                continue;
            }

            if (id.hasResourceType() && !compartmentType.equalsIgnoreCase(id.getResourceType())) {
                continue;
            }

            return CompartmentAssignment.of(compartmentType, id.getIdPart());
        }

        return sawValue ? CompartmentAssignment.unknown(compartmentType) : null;
    }

    private record ReferenceCandidate(int priority, int sequence, String id) {}
}
