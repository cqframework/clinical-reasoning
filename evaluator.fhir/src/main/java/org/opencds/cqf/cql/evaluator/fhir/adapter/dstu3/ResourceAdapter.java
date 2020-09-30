package org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3;

import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.Resource;

import org.hl7.fhir.exceptions.FHIRException;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirVersionEnum;

class ResourceAdapter implements org.opencds.cqf.cql.evaluator.fhir.adapter.ResourceAdapter {

    public ResourceAdapter(IBaseResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource can not be null");
        }

        if (!resource.getStructureFhirVersionEnum().equals(FhirVersionEnum.DSTU3)) {
            throw new IllegalArgumentException("resource is incorrect fhir version for this adapter");
        }

        this.resource = (Resource) resource;
    }

    private Resource resource;

    protected Resource getResource() {
        return this.resource;
    }

    public IBaseResource get() {
        return this.resource;
    }

    @Override
    public IBase setProperty(String name, IBase value) throws FHIRException {
        return this.getResource().setProperty(name, (Base) value);
    }

    @Override
    public IBase addChild(String name) throws FHIRException {
        return this.getResource().addChild(name);
    }

    @Override
    public IBase getSingleProperty(String name) throws FHIRException {
        IBase[] values = this.getProperty(name, true);

        if (values == null || values.length == 0) {
            return null;
        }

        if (values.length > 1) {
            throw new IllegalArgumentException(String.format("more than one value found for property: %s", name));
        }

        return values[0];
    }

    @Override
    public IBase[] getProperty(String name) throws FHIRException {
        return this.getProperty(name, true);
    }

    @Override
    public IBase[] getProperty(String name, boolean checkValid) throws FHIRException {
        return this.getResource().getProperty(name.hashCode(), name, checkValid);
    }

    @Override
    public IBase makeProperty(String name) throws FHIRException {
        return this.getResource().makeProperty(name.hashCode(), name);
    }

    @Override
    public String[] getTypesForProperty(String name) throws FHIRException {
        return this.getResource().getTypesForProperty(name.hashCode(), name);
    }

    @Override
    public IBaseResource copy() {
        return this.getResource().copy();
    }

    @Override
    public void copyValues(IBaseResource dst) {
        this.getResource().copyValues((Resource) dst);
    }

    @Override
    public boolean equalsDeep(IBase other) {
        return this.getResource().equalsDeep((Base) other);
    }

    @Override
    public boolean equalsShallow(IBase other) {
        return this.getResource().equalsShallow((Base) other);
    }
}