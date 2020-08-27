package org.opencds.cqf.cql.evaluator.fhir.r4;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirVersionEnum;

public class ParametersAdapter implements org.opencds.cqf.cql.evaluator.fhir.api.ParametersAdapter {

    protected Parameters castParameters(IBaseResource parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("parameters can not be null");
        }

        if (!parameters.fhirType().equals("Parameters")) {
            throw new IllegalArgumentException("resource passed as parameters argument is not a Parameters resource");
        }

        if (!parameters.getStructureFhirVersionEnum().equals(FhirVersionEnum.R4)) {
            throw new IllegalArgumentException("parameters is incorrect fhir version for this adapter");
        }

        return (Parameters)parameters;
    }

    @Override
    public String getId(IBaseResource parameters) {
        return parameters.getIdElement().getValue();
    }

    @Override
    public void setId(IBaseResource parameters, String id) {
        parameters.setId(id);
    }

    @Override
    public List<IBaseBackboneElement> getParameter(IBaseResource parameters) {
        return castParameters(parameters).getParameter().stream().map(x -> (IBaseBackboneElement)x).collect(Collectors.toList());
    }

    @Override
    public void setParameter(IBaseResource parameters, List<IBaseBackboneElement> parametersParameterComponents) {
        castParameters(parameters)
            .setParameter(parametersParameterComponents.stream().map(x -> (ParametersParameterComponent)x).collect(Collectors.toList()));
    }

    @Override
    public IBaseBackboneElement addParameter(IBaseResource parameters) {
        return castParameters(parameters).addParameter();
    }


}