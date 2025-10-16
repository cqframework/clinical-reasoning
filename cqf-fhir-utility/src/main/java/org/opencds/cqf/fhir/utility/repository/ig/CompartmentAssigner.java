package org.opencds.cqf.fhir.utility.repository.ig;

import static java.util.Objects.requireNonNull;

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
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CategoryLayout;

/**
 * Determines the compartment assignment for a resource based on repository conventions
 * and resource references.
 */
class CompartmentAssigner {

    private final FhirContext fhirContext;
    private final CompartmentMode compartmentMode;
    private final CategoryLayout categoryLayout;

    CompartmentAssigner(FhirContext fhirContext, CompartmentMode compartmentMode, CategoryLayout categoryLayout) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext cannot be null");
        this.compartmentMode = Objects.requireNonNull(compartmentMode, "compartmentMode cannot be null");
        this.categoryLayout = Objects.requireNonNull(categoryLayout, "categoryLayout cannot be null");
    }

    /**
     * Determines the compartment assignment for the supplied resource based on repository conventions.
     */
    CompartmentAssignment assign(IBaseResource resource) {
        requireNonNull(resource, "resource cannot be null");
        requireNonNull(resource.getIdElement().getIdPart(), "resource id cannot be null");

        if (compartmentMode == CompartmentMode.NONE) {
            return CompartmentAssignment.NONE;
        }

        var resourceType = resource.fhirType();
        var resourceCategory = ResourceCategory.forType(resourceType);
        if (resourceCategory != ResourceCategory.DATA) {
            return CompartmentAssignment.NONE;
        }

        // If the resource is of the compartment type, assign it to its own compartment.
        if (compartmentMode.type().equalsIgnoreCase(resourceType)) {
            return CompartmentAssignment.of(
                    compartmentMode.type(), resource.getIdElement().getIdPart());
        }

        var params = compartmentMode.compartmentSearchParams(fhirContext, resourceType);
        var assignment = resolveFromParameters(resource, params);
        if (assignment.isPresent()) {
            return assignment.get();
        }

        return categoryLayout == IgConventions.CategoryLayout.DEFINITIONAL_AND_DATA
                ? CompartmentAssignment.SHARED
                : CompartmentAssignment.NONE;
    }

    private Optional<CompartmentAssignment> resolveFromParameters(
            IBaseResource resource, Set<RuntimeSearchParam> searchParams) {

        if (searchParams == null || searchParams.isEmpty()) {
            return Optional.empty();
        }

        var terser = fhirContext.newTerser();
        var compartmentType = compartmentMode.type();

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
