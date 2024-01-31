package org.opencds.cqf.fhir.utility;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

public class Resources {

    private Resources() {}

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
}
