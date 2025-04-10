package org.opencds.cqf.fhir.utility.iterable;

import ca.uhn.fhir.repository.Repository;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.bundle.BundleEntryParts;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.hl7.fhir.instance.model.api.IBaseBundle;

public class BundleIterator<B extends IBaseBundle> implements Iterator<BundleEntryParts> {

    protected final Repository repository;
    protected B bundle;
    protected int index = 0;
    protected List<BundleEntryParts> parts;

    public BundleIterator(Repository repository, B bundle) {
        this.repository = repository;
        this.bundle = bundle;
        this.parts = BundleUtil.toListOfEntries(repository.fhirContext(), bundle);
    }

    @Override
    public boolean hasNext() {
        return parts.size() > index;
    }

    @Override
    public BundleEntryParts next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        var next = parts.get(index);
        index++;

        if (index >= parts.size()) {
            getNextBundle();
        }

        return next;
    }

    protected void getNextBundle() {
        // Reset internal counter
        index = 0;

        var nextLink = BundleUtil.getLinkUrlOfType(this.repository.fhirContext(), bundle, IBaseBundle.LINK_NEXT);

        // No next Bundle, no parts.
        if (nextLink == null) {
            this.parts = Collections.emptyList();
            return;
        }

        @SuppressWarnings("unchecked")
        var clazz = (Class<B>) bundle.getClass();

        this.bundle = this.repository.link(clazz, nextLink);

        // No Bundle returned, no parts
        if (bundle == null) {
            this.parts = Collections.emptyList();
            return;
        }

        this.parts = BundleUtil.toListOfEntries(repository.fhirContext(), bundle);

        // Bundle returned an empty set, try the next one.
        if (this.parts.isEmpty()) {
            getNextBundle();
        }
    }
}
