package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;

class ParametersParameterComponentAdapter extends BaseAdapter implements IParametersParameterComponentAdapter {

    private final Parameters.ParametersParameterComponent parametersParameterComponent;

    protected ParametersParameterComponent getParametersParameterComponent() {
        return parametersParameterComponent;
    }

    public ParametersParameterComponentAdapter(IBase parametersParameterComponent) {
        super(FhirVersionEnum.DSTU3, parametersParameterComponent);
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
        return this.parametersParameterComponent.getName();
    }

    @Override
    public void setName(String name) {
        this.parametersParameterComponent.setName(name);
    }

    @Override
    public List<IParametersParameterComponentAdapter> getPart() {
        return this.getParametersParameterComponent().getPart().stream()
                .map(adapterFactory::createParametersParameter)
                .toList();
    }

    @Override
    public List<IBase> getPartValues(String name) {
        return this.parametersParameterComponent.getPart().stream()
                .filter(p -> p.getName().equals(name))
                .map(p -> p.hasResource() ? p.getResource() : p.getValue())
                .filter(Objects::nonNull)
                .map(IBase.class::cast)
                .toList();
    }

    @Override
    public void setPart(List<IBaseBackboneElement> parametersParameterComponents) {
        this.parametersParameterComponent.setPart(
                parametersParameterComponents == null
                        ? null
                        : parametersParameterComponents.stream()
                                .map(x -> (ParametersParameterComponent) x)
                                .collect(Collectors.toList()));
    }

    @Override
    public IBaseBackboneElement addPart() {
        return this.parametersParameterComponent.addPart();
    }

    @Override
    public boolean hasPart() {
        return this.parametersParameterComponent.hasPart();
    }

    @Override
    public boolean hasPart(String name) {
        for (var part : getPart()) {
            if (name.equals(part.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasResource() {
        return this.parametersParameterComponent.hasResource();
    }

    @Override
    public IBaseResource getResource() {
        return this.parametersParameterComponent.getResource();
    }

    @Override
    public void setResource(IBaseResource resource) {
        this.parametersParameterComponent.setResource((Resource) resource);
    }

    @Override
    public boolean hasValue() {
        return this.parametersParameterComponent.hasValue();
    }

    @Override
    public boolean hasPrimitiveValue() {
        return hasValue() && getValue() instanceof IPrimitiveType<?>;
    }

    @Override
    public void setValue(IBaseDatatype value) {
        this.parametersParameterComponent.setValue((Type) value);
    }

    @Override
    public IBaseDatatype getValue() {
        return this.parametersParameterComponent.getValue();
    }

    @Override
    public String getPrimitiveValue() {
        return hasPrimitiveValue()
                ? this.parametersParameterComponent.getValue().primitiveValue()
                : null;
    }

    @Override
    public IBase newTupleWithParts() {
        return null;
    }
}
