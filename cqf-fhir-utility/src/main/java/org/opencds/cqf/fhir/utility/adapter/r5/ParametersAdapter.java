package org.opencds.cqf.fhir.utility.adapter.r5;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r5.model.Resource;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;

class ParametersAdapter extends ResourceAdapter implements IParametersAdapter {

    public ParametersAdapter(IBaseResource parameters) {
        super(parameters);

        if (!parameters.fhirType().equals("Parameters")) {
            throw new IllegalArgumentException("resource passed as parameters argument is not a Parameters resource");
        }

        this.parameters = (Parameters) parameters;
    }

    private final Parameters parameters;

    protected Parameters getParameters() {
        return this.parameters;
    }

    @Override
    public boolean hasParameter() {
        return parameters.hasParameter();
    }

    @Override
    public List<IParametersParameterComponentAdapter> getParameter() {
        return getParameters().getParameter().stream()
                .map(adapterFactory::createParametersParameter)
                .toList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DataType> getParameterValues(String name) {
        return this.getParameters().getParameterValues(name);
    }

    @Override
    public boolean hasParameter(String name) {
        return parameters.hasParameter(name);
    }

    @Override
    public IParametersParameterComponentAdapter getParameter(String name) {
        var param = getParameters().getParameter(name);
        return param == null ? null : adapterFactory.createParametersParameter(param);
    }

    @Override
    public void setParameter(List<IBaseBackboneElement> parametersParameterComponents) {
        this.getParameters()
                .setParameter(
                        parametersParameterComponents == null
                                ? null
                                : parametersParameterComponents.stream()
                                        .map(x -> (ParametersParameterComponent) x)
                                        .toList());
    }

    @Override
    public void addParameter(String name, String value) {
        getParameters().addParameter(name, value);
    }

    @Override
    public void setParameter(String name, int value) {
        if (hasParameter(name)) {
            getParameter(name).setValue(new IntegerType(value));
        } else {
            getParameters().addParameter(name, value);
        }
    }

    @Override
    public void addParameter(String name, IBase value) {
        if (value instanceof DataType type) {
            getParameters().addParameter(name, type);
        } else {
            throw new IllegalArgumentException("element passed as value argument is not a valid data type");
        }
    }

    @Override
    public void addParameter(String name, IBaseResource resource) {
        if (resource instanceof Resource resource1) {
            getParameters().addParameter().setName(name).setResource(resource1);
        } else {
            throw new IllegalArgumentException("element passed as value argument is not a valid data type");
        }
    }

    @Override
    public void addParameter(IBase parameter) {
        if (parameter instanceof ParametersParameterComponent component) {
            getParameters().addParameter(component);
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
