package org.opencds.cqf.fhir.utility.iterable

import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.util.bundle.BundleEntryParts
import java.util.function.Function
import org.hl7.fhir.instance.model.api.IBaseBundle

class BundleMappingIterator<B : IBaseBundle, T>(
    repository: IRepository,
    bundle: B?,
    private val mapper: Function<BundleEntryParts?, T?>,
) : Iterator<T?> {
    private val inner = BundleIterator(repository, bundle)

    override fun hasNext(): Boolean {
        return inner.hasNext()
    }

    override fun next(): T? {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        val next = inner.next()
        return this.mapper.apply(next)
    }
}
