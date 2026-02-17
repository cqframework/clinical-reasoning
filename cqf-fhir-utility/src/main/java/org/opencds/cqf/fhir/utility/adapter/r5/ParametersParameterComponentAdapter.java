package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.Tuple;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;

class ParametersParameterComponentAdapter extends BaseAdapter implements IParametersParameterComponentAdapter {

    private final Parameters.ParametersParameterComponent parametersParameterComponent;

    protected Parameters.ParametersParameterComponent getParametersParameterComponent() {
        return this.parametersParameterComponent;
    }

    public ParametersParameterComponentAdapter(IBase parametersParameterComponent) {
        super(FhirVersionEnum.R5, parametersParameterComponent);
        if (!parametersParameterComponent.fhirType().equals("Parameters.parameter")) {
            throw new IllegalArgumentException(
                    "element passed as parametersParameterComponent argument is not a ParametersParameterComponent Element");
        }

        this.parametersParameterComponent = (ParametersParameterComponent) parametersParameterComponent;
    }

    @Override
    public IBaseBackboneElement get() {
        return this.parametersParameterComponent;
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
    public List<IParametersParameterComponentAdapter> getPart() {
        return this.getParametersParameterComponent().getPart().stream()
                .map(adapterFactory::createParametersParameter)
                .toList();
    }

    @Override
    public List<IBase> getPartValues(String name) {
        return this.getParametersParameterComponent().getPart().stream()
                .filter(p -> p.getName().equals(name))
                .map(p -> p.hasResource() ? p.getResource() : p.getValue())
                .filter(Objects::nonNull)
                .map(IBase.class::cast)
                .toList();
    }

    @Override
    public void setPart(List<IBaseBackboneElement> parametersParameterComponents) {
        this.getParametersParameterComponent()
                .setPart(
                        parametersParameterComponents == null
                                ? null
                                : parametersParameterComponents.stream()
                                        .map(x -> (ParametersParameterComponent) x)
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
    public boolean hasPart(String name) {
        return this.getParametersParameterComponent().hasPart(name);
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
        return hasValue() && getValue() instanceof IPrimitiveType<?>;
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
    public String getPrimitiveValue() {
        return hasPrimitiveValue()
                ? this.getParametersParameterComponent().getValue().primitiveValue()
                : null;
    }

    @Override
    public IBase newTupleWithParts() {
        var tuple = new Tuple();
        getPart().forEach(p -> tuple.addProperty(p.getName(), List.of((Base) p.getPartValue(p))));
        return tuple;
    }
}
