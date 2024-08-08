package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

class ParametersParameterComponentAdapter
        implements org.opencds.cqf.fhir.utility.adapter.ParametersParameterComponentAdapter {

    private final Parameters.ParametersParameterComponent parametersParametersComponent;
    private final FhirContext fhirContext;
    private final ModelResolver modelResolver;

    public ParametersParameterComponentAdapter(IBaseBackboneElement parametersParametersComponent) {
        if (parametersParametersComponent == null) {
            throw new IllegalArgumentException("parametersParametersComponent can not be null");
        }

        if (!parametersParametersComponent.fhirType().equals("Parameters.parameter")) {
            throw new IllegalArgumentException(
                    "element passed as parametersParametersComponent argument is not a ParametersParameterComponent Element");
        }

        this.parametersParametersComponent = (ParametersParameterComponent) parametersParametersComponent;
        fhirContext = FhirContext.forCached(FhirVersionEnum.R5);
        modelResolver = FhirModelResolverCache.resolverForVersion(
                fhirContext.getVersion().getVersion());
    }

    @Override
    public IBaseBackboneElement get() {
        return this.parametersParametersComponent;
    }

    @Override
    public String getName() {
        return this.parametersParametersComponent.getName();
    }

    @Override
    public void setName(String name) {
        this.parametersParametersComponent.setName(name);
    }

    @Override
    public List<IBaseBackboneElement> getPart() {
        return this.parametersParametersComponent.getPart().stream().collect(Collectors.toList());
    }

    @Override
    public void setPart(List<IBaseBackboneElement> parametersParameterComponents) {
        this.parametersParametersComponent.setPart(parametersParameterComponents.stream()
                .map(x -> (ParametersParameterComponent) x)
                .collect(Collectors.toList()));
    }

    @Override
    public IBaseBackboneElement addPart() {
        return this.parametersParametersComponent.addPart();
    }

    @Override
    public boolean hasPart() {
        return this.parametersParametersComponent.hasPart();
    }

    @Override
    public boolean hasResource() {
        return this.parametersParametersComponent.hasResource();
    }

    @Override
    public IBaseResource getResource() {
        return this.parametersParametersComponent.getResource();
    }

    @Override
    public void setResource(IBaseResource resource) {
        this.parametersParametersComponent.setResource((Resource) resource);
    }

    @Override
    public boolean hasValue() {
        return this.parametersParametersComponent.hasValue();
    }

    @Override
    public boolean hasPrimitiveValue() {
        return this.parametersParametersComponent.hasPrimitiveValue();
    }

    @Override
    public void setValue(IBaseDatatype value) {
        this.parametersParametersComponent.setValue((Type) value);
    }

    @Override
    public IBaseDatatype getValue() {
        return this.parametersParametersComponent.getValue();
    }

    @Override
    public FhirContext fhirContext() {
        return fhirContext;
    }

    @Override
    public ModelResolver getModelResolver() {
        return modelResolver;
    }
}
