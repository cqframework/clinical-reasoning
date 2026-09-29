package org.opencds.cqf.fhir.utility.adapter.r4;

import static java.util.Optional.ofNullable;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Optional;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.utility.adapter.BaseResourceAdapter;

class ResourceAdapter extends BaseResourceAdapter {

    ResourceAdapter(IBaseResource resource) {
        super(resource);
        if (!resource.getStructureFhirVersionEnum().equals(FhirVersionEnum.R4)) {
            throw new IllegalArgumentException("resource is incorrect fhir version for this adapter");
        }
    }

    protected Resource getResource() {
        return (Resource) resource;
    }

    protected boolean isDomainResource() {
        return getDomainResource().isPresent();
    }

    protected Optional<DomainResource> getDomainResource() {
        return ofNullable(resource instanceof DomainResource domainResource ? domainResource : null);
    }

    @Override
    public IBase setProperty(String name, IBase value) throws FHIRException {
        return getResource().setProperty(name, (Base) value);
    }

    @Override
    public IBase addChild(String name) throws FHIRException {
        return getResource().addChild(name);
    }

    @Override
    public IBase getSingleProperty(String name) throws FHIRException {
        IBase[] values = getProperty(name, true);

        if (values == null || values.length == 0) {
            return null;
        }

        if (values.length > 1) {
            throw new IllegalArgumentException("more than one value found for property: %s".formatted(name));
        }

        return values[0];
    }

    @Override
    public IBase[] getProperty(String name) throws FHIRException {
        return getProperty(name, true);
    }

    @Override
    public IBase[] getProperty(String name, boolean checkValid) throws FHIRException {
        return getResource().getProperty(name.hashCode(), name, checkValid);
    }

    @Override
    public IBase makeProperty(String name) throws FHIRException {
        return getResource().makeProperty(name.hashCode(), name);
    }

    @Override
    public String[] getTypesForProperty(String name) throws FHIRException {
        return getResource().getTypesForProperty(name.hashCode(), name);
    }

    @Override
    public IBaseResource copy() {
        return getResource().copy();
    }

    @Override
    public void copyValues(IBaseResource dst) {
        getResource().copyValues((Resource) dst);
    }

    @Override
    public boolean equalsDeep(IBase other) {
        return getResource().equalsDeep((Base) other);
    }

    @Override
    public boolean equalsShallow(IBase other) {
        return getResource().equalsShallow((Base) other);
    }
}
