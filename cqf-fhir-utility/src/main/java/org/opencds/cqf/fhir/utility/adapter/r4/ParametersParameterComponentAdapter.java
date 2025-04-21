package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

class ParametersParameterComponentAdapter implements IParametersParameterComponentAdapter {

    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private final Parameters.ParametersParameterComponent parametersParameterComponent;
    private final ModelResolver modelResolver;
    private final IAdapterFactory adapterFactory;

    protected Parameters.ParametersParameterComponent getParametersParameterComponent() {
        return this.parametersParameterComponent;
    }

    public ParametersParameterComponentAdapter(IBaseBackboneElement parametersParameterComponent) {
        if (parametersParameterComponent == null) {
            throw new IllegalArgumentException("parametersParameterComponent can not be null");
        }

        if (!parametersParameterComponent.fhirType().equals("Parameters.parameter")) {
            throw new IllegalArgumentException(
                    "element passed as parametersParameterComponent argument is not a ParametersParameterComponent Element");
        }

        this.parametersParameterComponent = (ParametersParameterComponent) parametersParameterComponent;
        modelResolver = FhirModelResolverCache.resolverForVersion(
                fhirContext.getVersion().getVersion());
        adapterFactory = IAdapterFactory.forFhirContext(fhirContext);
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
    public List<IBaseDatatype> getPartValues(String name) {
        return this.getParametersParameterComponent().getPart().stream()
                .filter(p -> p.getName().equals(name))
                .map(ParametersParameterComponent::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public void setPart(List<IBaseBackboneElement> parametersParameterComponents) {
        this.getParametersParameterComponent()
                .setPart(parametersParameterComponents.stream()
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
        this.getParametersParameterComponent().setValue((Type) value);
    }

    @Override
    public IBaseDatatype getValue() {
        return this.getParametersParameterComponent().getValue();
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
