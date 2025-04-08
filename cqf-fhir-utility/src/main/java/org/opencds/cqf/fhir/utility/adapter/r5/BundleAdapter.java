package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r5.model.Bundle;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IBundleAdapter;

public class BundleAdapter extends ResourceAdapter implements IBundleAdapter {

    private final Bundle bundle;

    public BundleAdapter(IBaseBundle bundle) {
        super(bundle);
        if (!(bundle instanceof Bundle bundleInner)) {
            throw new IllegalArgumentException(
                    "object passed as dataRequirement argument is not a DataRequirement data type");
        }
        this.bundle = bundleInner;
    }

    @Override
    public IBaseBundle get() {
        return bundle;
    }

    @Override
    public FhirContext fhirContext() {
        return null;
    }

    @Override
    public ModelResolver getModelResolver() {
        return null;
    }

    @Override
    public List<BundleEntryComponentAdapter> getEntry() {
        return bundle.getEntry().stream().map(BundleEntryComponentAdapter::new).toList();
    }
}
