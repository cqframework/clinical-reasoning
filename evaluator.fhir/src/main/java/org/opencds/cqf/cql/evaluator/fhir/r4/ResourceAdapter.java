package org.opencds.cqf.cql.evaluator.fhir.r4;

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Resource;

import org.hl7.fhir.exceptions.FHIRException;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirVersionEnum;

public class ResourceAdapter implements org.opencds.cqf.cql.evaluator.fhir.api.ResourceAdapter {

    protected Resource castResource(IBaseResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource can not be null");
        }

        if (!resource.getStructureFhirVersionEnum().equals(FhirVersionEnum.DSTU3)) {
            throw new IllegalArgumentException("resource is incorrect fhir version for this adapter");
        }

        return (Resource) resource;
    }

    protected Base castBase(IBase base) {
        if (base == null) {
            throw new IllegalArgumentException("base can not be null");
        }

        if (!(base instanceof Base)) {
            throw new IllegalArgumentException("base is incorrect fhir version for this adapter");
        }

        return (Base) base;
    }

    @Override
    public IBase setProperty(IBaseResource resource, String name, IBase value) throws FHIRException {
        return castResource(resource).setProperty(name, castBase(value));
    }

    @Override
    public IBase addChild(IBaseResource resource, String name) throws FHIRException {
        return castResource(resource).addChild(name);
    }

    @Override
    public IBase getSingleProperty(IBaseResource resource, String name) throws FHIRException {
        IBase[] values = this.getProperty(resource, name, true);

        if (values == null || values.length == 0) {
            return null;
        }

        if (values.length > 1) {
            throw new IllegalArgumentException(String.format("more than one value found for property: %s", name));
        }

        return values[0];
    }

    @Override
    public IBase[] getProperty(IBaseResource resource, String name) throws FHIRException {
        return this.getProperty(resource, name, true);
    }

    @Override
    public IBase[] getProperty(IBaseResource resource, String name, boolean checkValid) throws FHIRException {
        return castResource(resource).getProperty(name.hashCode(), name, checkValid);
    }

    @Override
    public IBase makeProperty(IBaseResource resource, String name) throws FHIRException {
        return castResource(resource).makeProperty(name.hashCode(), name);
    }

    @Override
    public String[] getTypesForProperty(IBaseResource resource, String name) throws FHIRException {
        return castResource(resource).getTypesForProperty(name.hashCode(), name);
    }

    @Override
    public IBaseResource copy(IBaseResource resource) {
        return castResource(resource).copy();
    }

    @Override
    public void copyValues(IBaseResource resource, IBaseResource dst) {
        castResource(resource).copyValues(castResource(dst));
    }

    @Override
    public boolean equalsDeep(IBase resource, IBase other) {
        return castBase(resource).equalsDeep(castBase(other));
    }

    @Override
    public boolean equalsShallow(IBase resource, IBase other) {
        return castBase(resource).equalsShallow(castBase(other));
    }
}