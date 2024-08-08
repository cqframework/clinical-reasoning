package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface ResourceAdapter extends Adapter<IBaseResource> {

    public IBaseResource get();

    public IBase setProperty(String name, IBase value) throws FHIRException;

    public IBase addChild(String name) throws FHIRException;

    public IBase getSingleProperty(String name) throws FHIRException;

    public IBase[] getProperty(String name) throws FHIRException;

    public IBase[] getProperty(String name, boolean checkValid) throws FHIRException;

    public IBase makeProperty(String name) throws FHIRException;

    public String[] getTypesForProperty(String name) throws FHIRException;

    public IBaseResource copy();

    public void copyValues(IBaseResource destination);

    public boolean equalsDeep(IBase other);

    public boolean equalsShallow(IBase other);

    public default List<? extends IBaseResource> getContained() {
        return getContained(get());
    }

    public default boolean hasContained() {
        return hasContained(get());
    }

    public default List<? extends IBaseResource> getContained(IBaseResource base) {
        return resolvePathList(base, "contained", IBaseResource.class);
    }

    public default Boolean hasContained(IBaseResource base) {
        return !getContained(base).isEmpty();
    }
}
