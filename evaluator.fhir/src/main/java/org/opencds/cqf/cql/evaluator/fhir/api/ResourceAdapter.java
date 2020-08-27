package org.opencds.cqf.cql.evaluator.fhir.api;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface ResourceAdapter {

    public IBase setProperty(IBaseResource resource, String name, IBase value) throws FHIRException;

    public IBase addChild(IBaseResource resource, String name) throws FHIRException;

    public IBase getSingleProperty(IBaseResource resource, String name) throws FHIRException;

    public IBase[] getProperty(IBaseResource resource, String name) throws FHIRException;

	public IBase[] getProperty(IBaseResource resource, String name, boolean checkValid) throws FHIRException;

	public IBase makeProperty(IBaseResource resource, String name) throws FHIRException;

    public String[] getTypesForProperty(IBaseResource resource, String name) throws FHIRException;

    public IBaseResource copy(IBaseResource resource);

    public void copyValues(IBaseResource resource, IBaseResource dst);

    public boolean equalsDeep(IBase resource, IBase other);

    public boolean equalsShallow(IBase resource, IBase other);
}