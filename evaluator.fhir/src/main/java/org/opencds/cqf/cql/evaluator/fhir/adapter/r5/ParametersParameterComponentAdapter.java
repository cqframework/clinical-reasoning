package org.opencds.cqf.cql.evaluator.fhir.adapter.r5;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;

class ParametersParameterComponentAdapter
        implements org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersParameterComponentAdapter {

    private Parameters.ParametersParameterComponent parametersParametersComponent;

    protected Parameters.ParametersParameterComponent getParametersParameterComponent() {
        return this.parametersParametersComponent;
    }

    public ParametersParameterComponentAdapter(IBaseBackboneElement parametersParametersComponent){
        if (parametersParametersComponent == null) {
            throw new IllegalArgumentException("parametersParametersComponent can not be null");
        }

        if (!parametersParametersComponent.fhirType().equals("Parameters.parameter")) {
            throw new IllegalArgumentException("element passed as parametersParametersComponent argument is not a ParametersParameterComponent Element");
        }

        this.parametersParametersComponent = (ParametersParameterComponent) parametersParametersComponent;

    }

    @Override
    public IBaseBackboneElement get() {
        return this.parametersParametersComponent;
    }

    @Override
    public String getName() {
        return this.getParametersParameterComponent().getName();
    }

    @Override
    public void setName(String name) {
        this.getParametersParameterComponent().setName(name);
    }

    @Override
    public List<IBaseBackboneElement> getPart() {
        return this.getParametersParameterComponent().getPart().stream().collect(Collectors.toList());
    }

    @Override
    public void setPart(List<IBaseBackboneElement> parametersParameterComponents) {
        this.getParametersParameterComponent().setPart(parametersParameterComponents.stream().map(x -> (ParametersParameterComponent) x)
                .collect(Collectors.toList()));
    }

    @Override
    public IBaseBackboneElement addPart() {
        return this.getParametersParameterComponent().addPart();
    }

    @Override
    public boolean hasPart() {
        return this.getParametersParameterComponent().hasPart();
    }

    @Override
    public boolean hasResource() {
        return this.getParametersParameterComponent().hasResource();
    }

    @Override
    public IBaseResource getResource() {
        return this.getParametersParameterComponent().getResource();
    }

    @Override
    public void setResource(IBaseResource resource) {
        this.getParametersParameterComponent().setResource((Resource) resource);
    }

    @Override
    public boolean hasValue() {
        return this.getParametersParameterComponent().hasValue();
    }

    @Override
    public boolean hasPrimitiveValue() {
        return this.getParametersParameterComponent().hasPrimitiveValue();
    }

    @Override
    public void setValue(IBaseDatatype value) {
        this.getParametersParameterComponent().setValue((DataType) value);
    }

    @Override
    public IBaseDatatype getValue() {
        return this.getParametersParameterComponent().getValue();
    }

    @Override
    public Boolean hasExtension() {
        return this.parametersParametersComponent.hasExtension();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IBaseExtension<?, ?>> getExtension() {
        return (List<IBaseExtension<?, ?>>)(List<?>)this.parametersParametersComponent.getExtension();
    }
}