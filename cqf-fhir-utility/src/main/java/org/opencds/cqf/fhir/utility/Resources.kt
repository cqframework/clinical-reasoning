package org.opencds.cqf.fhir.utility;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

public class Resources {

    private Resources() {}

    public static <T extends IBase> Optional<T> castOrThrow(IBase obj, Class<T> type, String errorMessage) {
        if (obj == null) return Optional.empty();
        if (type.isInstance(obj)) {
            return Optional.of(type.cast(obj));
        }
        throw new IllegalArgumentException(errorMessage);
    }

    public static <T extends IBaseResource, I extends IIdType> T newResource(Class<T> resourceClass, String idPart) {
        checkNotNull(resourceClass);
        checkNotNull(idPart);
        checkArgument(!idPart.contains("/"), "idPart must be a simple id. Do not include resourceType or history");
        T resource = newResource(resourceClass);

        @SuppressWarnings("unchecked")
        I id = (I) Ids.newId(resourceClass, idPart);
        resource.setId(id);
        return resource;
    }

    public static <T extends IBaseResource> T newResource(Class<T> resourceClass) {
        checkNotNull(resourceClass);
        T resource = null;
        try {
            resource = resourceClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "resourceClass must be a type with an empty default constructor to use this function");
        }

        return resource;
    }

    public static <T extends IBaseBackboneElement> T newBackboneElement(Class<T> backboneElementClass) {
        checkNotNull(backboneElementClass);
        T backboneElement = null;
        try {
            backboneElement = backboneElementClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "backboneElementClass must be a type with an empty default constructor to use this function");
        }

        return backboneElement;
    }

    public static IBase newBaseForVersion(String type, FhirVersionEnum fhirVersion) {
        return Resources.newBase(Resources.getClassForTypeAndVersion(type, fhirVersion));
    }

    public static <T extends IBase> T newBase(Class<T> baseClass) {
        T base = null;
        try {
            base = baseClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "bClass must be a type with an empty default constructor to use this function");
        }

        return base;
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBase> Class<T> getClassForTypeAndVersion(String type, FhirVersionEnum fhirVersion) {
        try {
            return (Class<T>) Class.forName(
                    "org.hl7.fhir.%s.model.%s".formatted(fhirVersion.toString().toLowerCase(), type));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Deep clone a resource with the FhirTerser
     *
     * @param <T> the resource type
     * @param resource the resource to clone
     * @return the cloned resource
     */
    public static <T extends IBaseResource> T clone(T resource) {
        checkNotNull(resource);
        var terser =
                FhirContext.forCached(resource.getStructureFhirVersionEnum()).newTerser();
        return terser.clone(resource);
    }

    /**
     * Convert a resource to a prettified JSON string
     *
     * @param resource the resource to convert
     * @return the JSON string
     */
    public static String stringify(IBaseResource resource) {
        checkNotNull(resource);
        return FhirContext.forCached(resource.getStructureFhirVersionEnum())
                .newJsonParser()
                .setPrettyPrint(true)
                .encodeResourceToString(resource);
    }
}
