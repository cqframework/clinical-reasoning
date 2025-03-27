package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
        return this.getParameters().getParameter().stream()
                .map(adapterFactory::createParametersParameter)
                .toList();
    }

    @Override
    public void setParameter(List<IBaseBackboneElement> parametersParameterComponents) {
        this.getParameters()
                .setParameter(parametersParameterComponents.stream()
                        .map(x -> (ParametersParameterComponent) x)
                        .collect(Collectors.toList()));
    }

    @Override
    public void addParameter(String name, String value) {
        getParameters().addParameter().setName(name).setValue(new StringType(value));
    }

    @Override
    public void addParameter(String name, IBase value) {
        if (value instanceof Type) {
            getParameters().addParameter(addParameter().setName(name).setValue((Type) value));
        } else {
            throw new IllegalArgumentException("element passed as value argument is not a valid type");
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

    @SuppressWarnings("unchecked")
    @Override
    public List<Type> getParameterValues(String name) {
        return this.getParameters().getParameter().stream()
                .filter(p -> p.getName().equals(name))
                .map(ParametersParameterComponent::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasParameter(String name) {
        return getParameters().getParameter().stream().anyMatch(p -> p.getName().equals(name));
    }

    @Override
    public IParametersParameterComponentAdapter getParameter(String name) {
        return getParameters().getParameter().stream()
                .filter(p -> p.getName().equals(name))
                .map(adapterFactory::createParametersParameter)
                .findFirst()
                .orElse(null);
    }
}
