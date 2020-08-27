package org.opencds.cqf.cql.evaluator.fhir.api;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface ParametersAdapter {

    public String getId(IBaseResource parameters);

    public void setId(IBaseResource parameters, String id);

    public List<IBaseBackboneElement> getParameter(IBaseResource parameters);

    public void setParameter(IBaseResource parameters, List<IBaseBackboneElement> parametersParameterComponents);

    public IBaseBackboneElement addParameter(IBaseResource parameters);
    
}