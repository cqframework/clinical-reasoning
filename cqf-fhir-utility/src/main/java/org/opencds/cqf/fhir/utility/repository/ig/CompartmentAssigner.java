package org.opencds.cqf.fhir.utility.repository.ig;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Objects;
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

    private final FhirContext fhirContext;
    private final CompartmentMode compartmentMode;

    CompartmentAssigner(FhirContext fhirContext, CompartmentMode compartmentMode) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext cannot be null");
        this.compartmentMode = Objects.requireNonNull(compartmentMode, "compartmentMode cannot be null");
    }

    /**
     * Determines the compartment assignment for the supplied resource based on repository conventions.
     *
     * Possile outcomes:
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
     * Possile outcomes:
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

        // TODO: This currently only supports the first matching reference.
        // CQL picks only one particular compartment if multiple are found,
        // based on which relationship is likely to be the "primary" one.
        // For example, a Coverage may reference a Patient as both the
        // beneficiary and the policyholder, but only the beneficiary is used.
        // See:
        // https://github.com/cqframework/clinical_quality_language/blob/master/Src/java/engine-fhir/src/main/kotlin/org/opencds/cqf/cql/engine/fhir/model/R4FhirModelResolver.kt#L408-L414
        for (var param : searchParams) {
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

                    var referencedType =
                            reference.getResourceType() != null ? reference.getResourceType() : compartmentType;

                    if (compartmentType.equalsIgnoreCase(referencedType)) {
                        return CompartmentAssignment.of(compartmentType, reference.getIdPart());
                    }
                }
            }
        }

        return CompartmentAssignment.shared();
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

        // TODO: Match up the compartment search parameters with the passed in search parameters
        // and use those to determine the compartment assignment.
        return CompartmentAssignment.shared();
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
}
