package org.opencds.cqf.fhir.cql.engine.utility;

import java.util.Iterator;
import java.util.stream.Stream;

public class StreamIterable<T> implements Iterable<T> {

    private final Stream<T> stream;

    public StreamIterable(Stream<T> stream) {
        this.stream = stream;
    }

    @Override
    public Iterator<T> iterator() {
        return stream.iterator();
    }
}
