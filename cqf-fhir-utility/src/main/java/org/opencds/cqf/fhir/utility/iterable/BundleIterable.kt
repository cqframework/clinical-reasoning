package org.opencds.cqf.fhir.utility.iterable

import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.util.bundle.BundleEntryParts
import java.util.Spliterator
import java.util.Spliterators
import java.util.stream.Stream
import java.util.stream.StreamSupport
import org.hl7.fhir.instance.model.api.IBaseBundle

class BundleIterable<B : IBaseBundle>(private val repository: IRepository, private val bundle: B?) :
    Iterable<BundleEntryParts?> {
    override fun iterator(): Iterator<BundleEntryParts?> {
        return BundleIterator(repository, bundle)
    }

    fun toStream(): Stream<BundleEntryParts?> {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize<BundleEntryParts?>(iterator(), Spliterator.ORDERED),
            false,
        )
    }
}
