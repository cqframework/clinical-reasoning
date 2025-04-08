package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.List;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.adapter.IBundleAdapter;

public class BundleAdapter extends ResourceAdapter implements IBundleAdapter {

    private final Bundle bundle;

    public BundleAdapter(IBaseResource resource) {
        super(resource);

        if (!(resource instanceof Bundle bundleInner)) {
            throw new IllegalArgumentException(
                    "object passed as dataRequirement argument is not a DataRequirement data type");
        }
        this.bundle = bundleInner;
    }

    @Override
    public List<BundleEntryComponentAdapter> getEntry() {
        return bundle.getEntry().stream().map(BundleEntryComponentAdapter::new).toList();
    }
}
