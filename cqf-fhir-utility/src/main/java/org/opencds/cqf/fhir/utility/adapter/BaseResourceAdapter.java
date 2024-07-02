package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public abstract class BaseResourceAdapter implements ResourceAdapter {
    protected final FhirContext fhirContext;
    protected final BaseRuntimeElementDefinition<?> elementDefinition;
    protected final IBaseResource resource;
    protected final ModelResolver modelResolver;

    public BaseResourceAdapter(IBaseResource resource) {
        this.resource = resource;
        fhirContext = FhirContext.forCached(resource.getStructureFhirVersionEnum());
        elementDefinition = fhirContext.getElementDefinition(this.resource.getClass());
        modelResolver = FhirModelResolverCache.resolverForVersion(
                fhirContext.getVersion().getVersion());
    }

    public ModelResolver getModelResolver() {
        return modelResolver;
    }

    public IBaseResource get() {
        return resource;
    }
}
