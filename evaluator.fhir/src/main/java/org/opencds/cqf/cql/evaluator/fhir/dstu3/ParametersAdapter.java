package org.opencds.cqf.cql.evaluator.fhir.dstu3;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirVersionEnum;

public class ParametersAdapter implements org.opencds.cqf.cql.evaluator.fhir.api.ParametersAdapter {

    public ParametersAdapter(IBaseResource parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("parameters can not be null");
        }

        if (!parameters.fhirType().equals("Parameters")) {
            throw new IllegalArgumentException("resource passed as parameters argument is not a Parameters resource");
        }

        if (!parameters.getStructureFhirVersionEnum().equals(FhirVersionEnum.DSTU3)) {
            throw new IllegalArgumentException("parameters is incorrect fhir version for this adapter");
        }

        this.parameters = (Parameters)parameters;
    }


    private Parameters parameters;

    protected Parameters getParameters() {
        return this.parameters;
    }

    @Override 
    public IBaseResource get() {
        return this.parameters;
    }

    @Override
    public List<IBaseBackboneElement> getParameter() {
        return this.getParameters().getParameter().stream().collect(Collectors.toList());
    }

    @Override
    public void setParameter(List<IBaseBackboneElement> parametersParameterComponents) {
        this.getParameters()
            .setParameter(parametersParameterComponents.stream().map(x -> (ParametersParameterComponent)x).collect(Collectors.toList()));
    }

    @Override
    public IBaseBackboneElement addParameter() {
        return this.getParameters().addParameter();
    }


}