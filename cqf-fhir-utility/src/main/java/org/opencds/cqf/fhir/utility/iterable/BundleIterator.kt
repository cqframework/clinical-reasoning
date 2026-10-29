package org.opencds.cqf.fhir.utility.iterable

import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.util.BundleUtil
import ca.uhn.fhir.util.bundle.BundleEntryParts
import org.hl7.fhir.instance.model.api.IBaseBundle

class BundleIterator<B : IBaseBundle>(
    protected val repository: IRepository,
    protected var bundle: B?,
) : Iterator<BundleEntryParts?> {
    protected var index: Int = 0
    protected var parts: MutableList<BundleEntryParts?> =
        BundleUtil.toListOfEntries(repository.fhirContext(), bundle)

    override fun hasNext(): Boolean {
        return parts.size > index
    }

    override fun next(): BundleEntryParts? {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        val next = parts.get(index)
        index++

        if (index >= parts.size) {
            this.nextBundle
        }

        return next
    }

    protected val nextBundle: Unit
        get() {
            // Reset internal counter
            index = 0

            val nextLink =
                BundleUtil.getLinkUrlOfType(
                    this.repository.fhirContext(),
                    bundle,
                    IBaseBundle.LINK_NEXT,
                )

            // No next Bundle, no parts.
            if (nextLink == null) {
                this.parts = mutableListOf<BundleEntryParts?>()
                return
            }

            val clazz = bundle!!.javaClass

            this.bundle = this.repository.link(clazz, nextLink)

            // No Bundle returned, no parts
            if (bundle == null) {
                this.parts = mutableListOf<BundleEntryParts?>()
                return
            }

            this.parts = BundleUtil.toListOfEntries(repository.fhirContext(), bundle)

            // Bundle returned an empty set, try the next one.
            if (this.parts.isEmpty()) {
                this.nextBundle
            }
        }
}
