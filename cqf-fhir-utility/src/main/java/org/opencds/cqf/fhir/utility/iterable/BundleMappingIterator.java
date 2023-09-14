package org.opencds.cqf.fhir.utility.iterable;

import ca.uhn.fhir.util.bundle.BundleEntryParts;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.fhir.api.Repository;

public class BundleMappingIterator<B extends IBaseBundle, T> implements Iterator<T> {

    private final Function<BundleEntryParts, T> mapper;
    private final BundleIterator<B> inner;

    public BundleMappingIterator(Repository repository, B bundle, Function<BundleEntryParts, T> mapper) {
        this.inner = new BundleIterator<>(repository, bundle);
        this.mapper = mapper;
    }

    @Override
    public boolean hasNext() {
        return inner.hasNext();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        var next = inner.next();
        return this.mapper.apply(next);
    }
}
