package org.opencds.cqf.fhir.utility.iterable

import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.util.bundle.BundleEntryParts
import java.util.*
import java.util.function.Function
import java.util.stream.Stream
import java.util.stream.StreamSupport
import org.hl7.fhir.instance.model.api.IBaseBundle

/**
 * This class allows you to iterate a Bundle (including fetching the next links from the Repository)
 * and provide a mapping function for each Bundle entry.
 */
class BundleMappingIterable<B : IBaseBundle, T>(
    private val repository: IRepository?,
    private val bundle: B?,
    private val mapper: Function<BundleEntryParts?, T?>?,
) : Iterable<T?> {
    override fun iterator(): Iterator<T?> {
        return BundleMappingIterator(repository!!, bundle, mapper!!)
    }

    fun toStream(): Stream<T?> {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED),
            false,
        )
    }
}
