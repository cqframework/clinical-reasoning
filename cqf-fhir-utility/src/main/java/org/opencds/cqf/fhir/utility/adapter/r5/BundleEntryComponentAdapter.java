package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IBundleEntryComponentAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class BundleEntryComponentAdapter implements IBundleEntryComponentAdapter {
    private final BundleEntryComponent entry;
    private final FhirContext fhirContext = FhirContext.forR5Cached();
    private final ModelResolver modelResolver =
            FhirModelResolverCache.resolverForVersion(fhirContext.getVersion().getVersion());

    public BundleEntryComponentAdapter(IBaseBackboneElement entry) {
        if (!(entry instanceof Bundle.BundleEntryComponent entryInner)) {
            throw new IllegalArgumentException(
                    "object passed as dataRequirement argument is not a DataRequirement data type");
        }
        this.entry = entryInner;
    }

    @Override
    public IBaseBackboneElement get() {
        return entry;
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
