package org.opencds.cqf.cql.evaluator.fhir.api;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface ParametersParameterComponentAdapter {

    public String getId(IBaseBackboneElement ppc);

    public void setId(IBaseBackboneElement ppc, String id);

    public String getName(IBaseBackboneElement ppc);

    public void setName(IBaseBackboneElement ppc, String name);

    public List<IBaseBackboneElement> getPart(IBaseBackboneElement ppc);

    public void setPart(IBaseBackboneElement ppc, List<IBaseBackboneElement> parametersParameterComponents);

    public IBaseBackboneElement addPart(IBaseBackboneElement ppc); 
    
    public boolean hasPart(IBaseBackboneElement ppc);

    public boolean hasResource(IBaseBackboneElement ppc);

    public IBaseResource getResource(IBaseBackboneElement ppc);

    public void setResource(IBaseBackboneElement ppc, IBaseResource resource);

    public boolean hasValue(IBaseBackboneElement ppc);

    public boolean hasPrimitiveValue(IBaseBackboneElement ppc);

    public void setValue(IBaseBackboneElement ppc, IBaseDatatype value);

    public IBaseDatatype getValue(IBaseBackboneElement ppc);
}