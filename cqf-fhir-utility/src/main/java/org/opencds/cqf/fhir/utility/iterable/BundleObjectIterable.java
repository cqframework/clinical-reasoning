package org.opencds.cqf.fhir.utility.iterable;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.fhir.api.Repository;

public class BundleObjectIterable<B extends IBaseBundle> implements Iterable<Object> {

    Repository repository;
    B bundle;

    public BundleObjectIterable(Repository repository, B bundle) {
        this.repository = repository;
        this.bundle = bundle;
    }

    @Override
    public Iterator<Object> iterator() {
        return new BundleObjectIterator<>(repository, bundle);
    }

    public Stream<Object> toStream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }
}
