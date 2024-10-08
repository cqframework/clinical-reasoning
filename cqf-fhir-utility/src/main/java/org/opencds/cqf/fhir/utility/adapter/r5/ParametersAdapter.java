package org.opencds.cqf.fhir.utility.adapter.r5;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r5.model.Resource;

class ParametersAdapter extends ResourceAdapter implements org.opencds.cqf.fhir.utility.adapter.ParametersAdapter {

    public ParametersAdapter(IBaseResource parameters) {
        super(parameters);

        if (!parameters.fhirType().equals("Parameters")) {
            throw new IllegalArgumentException("resource passed as parameters argument is not a Parameters resource");
        }

        this.parameters = (Parameters) parameters;
    }

    private Parameters parameters;

    protected Parameters getParameters() {
        return this.parameters;
    }

    @Override
    public List<ParametersParameterComponent> getParameter() {
        return this.getParameters().getParameter().stream().collect(Collectors.toList());
    }

    @Override
    public List<DataType> getParameterValues(String name) {
        return this.getParameters().getParameterValues(name);
    }

    @Override
    public ParametersParameterComponent getParameter(String name) {
        return this.getParameters().getParameter(name);
    }

    @Override
    public void setParameter(List<IBaseBackboneElement> parametersParameterComponents) {
        this.getParameters()
                .setParameter(parametersParameterComponents.stream()
                        .map(x -> (ParametersParameterComponent) x)
                        .collect(Collectors.toList()));
    }

    @Override
    public void addParameter(String name, IBase value) {
        if (value instanceof DataType) {
            getParameters().addParameter(name, (DataType) value);
        } else {
            throw new IllegalArgumentException("element passed as value argument is not a valid data type");
        }
    }

    @Override
    public void addParameter(String name, IBaseResource resource) {
        if (resource instanceof Resource) {
            getParameters().addParameter().setName(name).setResource((Resource) resource);
        } else {
            throw new IllegalArgumentException("element passed as value argument is not a valid data type");
        }
    }

    @Override
    public void addParameter(IBase parameter) {
        if (parameter instanceof ParametersParameterComponent) {
            getParameters().addParameter((ParametersParameterComponent) parameter);
        } else {
            throw new IllegalArgumentException(
                    "element passed as parameter argument is not a valid parameter component");
        }
    }

    @Override
    public ParametersParameterComponent addParameter() {
        return this.getParameters().addParameter();
    }
}
