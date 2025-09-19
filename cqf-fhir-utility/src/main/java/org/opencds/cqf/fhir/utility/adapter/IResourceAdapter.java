package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IResourceAdapter extends IAdapter<IBaseResource> {

    IBaseResource get();

    IBase setProperty(String name, IBase value) throws FHIRException;

    IBase addChild(String name) throws FHIRException;

    IBase getSingleProperty(String name) throws FHIRException;

    IBase[] getProperty(String name) throws FHIRException;

    IBase[] getProperty(String name, boolean checkValid) throws FHIRException;

    IBase makeProperty(String name) throws FHIRException;

    String[] getTypesForProperty(String name) throws FHIRException;

    IBaseResource copy();

    void copyValues(IBaseResource destination);

    boolean equalsDeep(IBase other);

    boolean equalsShallow(IBase other);

    default <R extends IBaseResource> List<R> getContained() {
        return getContained(get());
    }

    default boolean hasContained() {
        return hasContained(get());
    }

    @SuppressWarnings("unchecked")
    default <R extends IBaseResource> List<R> getContained(IBaseResource base) {
        return resolvePathList(base, "contained", IBaseResource.class).stream()
                .map(r -> (R) r)
                .collect(Collectors.toList());
    }

    default Boolean hasContained(IBaseResource base) {
        return !getContained(base).isEmpty();
    }
}
