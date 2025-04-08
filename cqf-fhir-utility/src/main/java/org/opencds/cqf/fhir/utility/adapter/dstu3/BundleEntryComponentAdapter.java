package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IBundleEntryComponentAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class BundleEntryComponentAdapter implements IBundleEntryComponentAdapter {
    private final BundleEntryComponent entry;
    private final FhirContext fhirContext = FhirContext.forDstu3Cached();
    private final ModelResolver modelResolver =
            FhirModelResolverCache.resolverForVersion(fhirContext.getVersion().getVersion());

    public BundleEntryComponentAdapter(IBaseBackboneElement entry) {
        if (!(entry instanceof Bundle.BundleEntryComponent entryInner)) {
            throw new IllegalArgumentException(
                    "object passed as dataRequirement argument is not a DataRequirement data type");
        }
        this.entry = entryInner;
    }

    public BundleEntryComponentAdapter(BundleEntryComponent entry) {
        this.entry = entry;
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
