package org.opencds.cqf.cql.evaluator.fhir.adapter;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface ParametersParameterComponentAdapter extends Adapter<IBaseBackboneElement> {

    public IBaseBackboneElement get();

    public String getName();

    public void setName(String name);

    public List<IBaseBackboneElement> getPart();

    public void setPart(List<IBaseBackboneElement> parametersParameterComponents);

    public IBaseBackboneElement addPart(); 
    
    public boolean hasPart();

    public boolean hasResource();

    public IBaseResource getResource();

    public void setResource(IBaseResource resource);

    public boolean hasValue();

    public boolean hasPrimitiveValue();

    public void setValue(IBaseDatatype value);

    public IBaseDatatype getValue();

    public Boolean hasExtension();

    public List<IBaseExtension<?,?>> getExtension();
}