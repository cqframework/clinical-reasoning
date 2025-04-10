package org.opencds.cqf.fhir.utility.iterable;

import ca.uhn.fhir.repository.Repository;
import ca.uhn.fhir.util.bundle.BundleEntryParts;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.hl7.fhir.instance.model.api.IBaseBundle;

/**
 * This class allows you to iterate a Bundle (including fetching the next links
 * from the Repository) and provide a mapping function
 * for each Bundle entry.
 *
 */
public class BundleMappingIterable<B extends IBaseBundle, T> implements Iterable<T> {

    private final Repository repository;
    private final B bundle;
    private final Function<BundleEntryParts, T> mapper;

    public BundleMappingIterable(Repository repository, B bundle, Function<BundleEntryParts, T> mapper) {
        this.repository = repository;
        this.bundle = bundle;
        this.mapper = mapper;
    }

    @Override
    public Iterator<T> iterator() {
        return new BundleMappingIterator<>(repository, bundle, mapper);
    }

    public Stream<T> toStream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }
}
