package org.opencds.cqf.fhir.utility.iterable

import java.util.stream.Stream

class StreamIterable<T>(private val stream: Stream<T?>) : Iterable<T?> {
    override fun iterator(): Iterator<T?> {
        return stream.iterator()
    }
}
