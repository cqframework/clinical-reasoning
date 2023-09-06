package org.opencds.cqf.fhir.utility.iterable;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;

public class BundleResourceIterable<B extends IBaseBundle> implements Iterable<IBaseResource> {

    Repository repository;
    B bundle;

    public BundleResourceIterable(Repository repository, B bundle) {
        this.repository = repository;
        this.bundle = bundle;
    }

    @Override
    public Iterator<IBaseResource> iterator() {
        return new BundleResourceIterator<>(repository, bundle);
    }

    public Stream<IBaseResource> toStream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }
}
