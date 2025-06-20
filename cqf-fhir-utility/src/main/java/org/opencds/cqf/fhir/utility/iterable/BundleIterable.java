package org.opencds.cqf.fhir.utility.iterable;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.util.bundle.BundleEntryParts;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.hl7.fhir.instance.model.api.IBaseBundle;

public class BundleIterable<B extends IBaseBundle> implements Iterable<BundleEntryParts> {

    private final IRepository repository;
    private final B bundle;

    public BundleIterable(IRepository repository, B bundle) {
        this.repository = repository;
        this.bundle = bundle;
    }

    @Override
    public Iterator<BundleEntryParts> iterator() {
        return new BundleIterator<>(repository, bundle);
    }

    public Stream<BundleEntryParts> toStream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }
}
