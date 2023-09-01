package org.opencds.cqf.fhir.utility.iterable;

import ca.uhn.fhir.util.bundle.BundleEntryParts;
import java.util.Iterator;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.fhir.api.Repository;

public class BundleIterable<B extends IBaseBundle> implements Iterable<BundleEntryParts> {

    Repository repository;
    B bundle;
    Class<B> bundleType;

    public BundleIterable(Repository repository, Class<B> bundleType, B bundle) {
        this.repository = repository;
        this.bundle = bundle;
        this.bundleType = bundleType;
    }

    @Override
    public Iterator<BundleEntryParts> iterator() {
        return new BundleIterator<>(repository, bundleType, bundle);
    }
}
