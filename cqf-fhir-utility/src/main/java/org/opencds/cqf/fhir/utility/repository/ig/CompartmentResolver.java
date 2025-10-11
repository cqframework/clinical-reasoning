package org.opencds.cqf.fhir.utility.repository.ig;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeSearchParam;
import java.util.Objects;
import java.util.Optional;
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
class CompartmentResolver {

    private final FhirContext fhirContext;
    private final IgConventions conventions;

    CompartmentResolver(FhirContext fhirContext, IgConventions conventions) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext cannot be null");
        this.conventions = Objects.requireNonNull(conventions, "conventions cannot be null");
    }

    Optional<CompartmentAssignment> resolve(IBaseResource resource) {
        Objects.requireNonNull(resource, "resource cannot be null");

        var compartmentMode = conventions.compartmentMode();
        if (compartmentMode == CompartmentMode.NONE) {
            return Optional.empty();
        }

        var resourceType = resource.fhirType();
        if (resourceType == null || resourceType.isBlank()) {
            return Optional.empty();
        }

        var resourceCategory = ResourceCategory.forType(resourceType);
        if (!compartmentMode.resourceBelongsToCompartment(fhirContext, resourceType)) {
            if (resourceCategory == ResourceCategory.DATA
                    && conventions.categoryLayout() == IgConventions.CategoryLayout.DEFINITIONAL_AND_DATA) {
                return Optional.of(CompartmentAssignment.shared());
            }

            return Optional.empty();
        }

        if (compartmentMode.type().equalsIgnoreCase(resourceType)) {
            var idElement = resource.getIdElement();
            if (idElement != null && idElement.hasIdPart()) {
                return Optional.of(CompartmentAssignment.of(compartmentMode.type(), idElement.getIdPart()));
            }
        }

        var params = compartmentMode.compartmentSearchParams(fhirContext, resourceType);
        var assignment = resolveFromParameters(resource, params);
        if (assignment.isPresent()) {
            return assignment;
        }

        if (resourceCategory == ResourceCategory.DATA
                && conventions.categoryLayout() == IgConventions.CategoryLayout.DEFINITIONAL_AND_DATA) {
            return Optional.of(CompartmentAssignment.shared());
        }

        return Optional.empty();
    }

    private Optional<CompartmentAssignment> resolveFromParameters(
            IBaseResource resource, Set<RuntimeSearchParam> searchParams) {
        if (searchParams == null || searchParams.isEmpty()) {
            return Optional.empty();
        }

        var terser = fhirContext.newTerser();
        var compartmentType = conventions.compartmentMode().type();

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
                        return Optional.of(CompartmentAssignment.of(compartmentType, reference.getIdPart()));
                    }
                }
            }
        }

        return Optional.empty();
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

            var ensured = Ids.ensureIdType(asString, conventions.compartmentMode().type());
            return Ids.newId(fhirContext, ensured);
        }

        if (value instanceof IBaseReference reference) {
            var element = reference.getReferenceElement();
            if (element == null || !element.hasIdPart()) {
                return null;
            }

            if (!element.hasResourceType()) {
                return Ids.newId(fhirContext, conventions.compartmentMode().type(), element.getIdPart());
            }

            return element;
        }

        return null;
    }
}
