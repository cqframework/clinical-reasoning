package org.opencds.cqf.fhir.utility.iterable;

import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.bundle.BundleEntryParts;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;

public class BundleResourceIterator<B extends IBaseBundle> implements Iterator<IBaseResource> {

    protected Repository repository;
    protected B bundle;
    protected int index = 0;
    protected List<BundleEntryParts> parts;

    public BundleResourceIterator(Repository repository, B bundle) {
        this.repository = repository;
        this.bundle = bundle;
        this.parts = BundleUtil.toListOfEntries(repository.fhirContext(), bundle);
    }

    @Override
    public boolean hasNext() {
        return parts.size() > index;
    }

    @Override
    public IBaseResource next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        var next = parts.get(index);
        index++;

        if (index >= parts.size()) {
            getNextBundle();
        }

        return next.getResource();
    }

    protected void getNextBundle() {
        var nextLink = BundleUtil.getLinkUrlOfType(this.repository.fhirContext(), bundle, IBaseBundle.LINK_NEXT);
        if (nextLink == null) {
            return;
        }

        // Reset the iterator
        index = 0;

        @SuppressWarnings("unchecked")
        var clazz = (Class<B>) bundle.getClass();
        this.bundle = this.repository.link(clazz, nextLink);
        if (bundle == null) {
            this.parts = Collections.emptyList();
        } else {
            this.parts = BundleUtil.toListOfEntries(this.repository.fhirContext(), bundle);
        }
    }
}
