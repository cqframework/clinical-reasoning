package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public abstract class BaseAdapter implements IAdapter<IBase> {
    protected final FhirContext fhirContext;
    protected final BaseRuntimeElementDefinition<?> baseElementDefinition;
    protected final IBase element;
    protected final ModelResolver modelResolver;
    protected final IAdapterFactory adapterFactory;

    protected BaseAdapter(FhirVersionEnum fhirVersion, IBase element) {
        if (element == null) {
            throw new IllegalArgumentException("element can not be null");
        }
        this.element = element;
        fhirContext = FhirContext.forCached(fhirVersion);
        adapterFactory = IAdapterFactory.forFhirContext(fhirContext);
        baseElementDefinition = fhirContext.getElementDefinition(this.element.getClass());
        modelResolver = FhirModelResolverCache.resolverForVersion(
                fhirContext.getVersion().getVersion());
    }

    public FhirContext fhirContext() {
        return fhirContext;
    }

    public ModelResolver getModelResolver() {
        return modelResolver;
    }

    public IBase get() {
        return element;
    }

    public IAdapterFactory getAdapterFactory() {
        return adapterFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends IBaseExtension<?, ?>> E addExtension() {
        if (get() instanceof IBaseHasExtensions baseHasExtensions) {
            return (E) baseHasExtensions.addExtension();
        }
        return null;
    }
}
