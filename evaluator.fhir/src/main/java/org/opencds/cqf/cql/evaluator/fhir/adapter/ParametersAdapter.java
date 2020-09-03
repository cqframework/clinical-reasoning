package org.opencds.cqf.cql.evaluator.fhir.adapter;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface ParametersAdapter extends ResourceAdapter {

    public IBaseResource get();

    public List<IBaseBackboneElement> getParameter();

    public void setParameter(List<IBaseBackboneElement> parametersParameterComponents);

    public IBaseBackboneElement addParameter();
    
}