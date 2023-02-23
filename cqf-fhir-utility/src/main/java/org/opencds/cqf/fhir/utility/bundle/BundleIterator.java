package org.opencds.cqf.fhir.utility.bundle;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.fhir.api.Repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.bundle.BundleEntryParts;

public class BundleIterator<B extends IBaseBundle> implements Iterator<BundleEntryParts> {

    protected Repository repository;
    protected B bundle;
    protected int index = 0;
    protected List<BundleEntryParts> parts;
    protected FhirContext fhirContext;
    protected Class<B> bundleType;

    public BundleIterator(FhirContext fhirContext, Repository repository, Class<B> bundleType,
            B bundle) {
        this.fhirContext = fhirContext;
        this.repository = repository;
        this.bundle = bundle;
        this.bundleType = bundleType;
        this.parts = BundleUtil.toListOfEntries(fhirContext, bundle);
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
        var nextLink = BundleUtil.getLinkUrlOfType(fhirContext, bundle, IBaseBundle.LINK_NEXT);
        if (nextLink == null) {
            return;
        }

        index = 0;
        this.bundle = this.repository.link(bundleType, nextLink);
        if (bundle == null) {
            this.parts = Collections.emptyList();
        } else {
            this.parts = BundleUtil.toListOfEntries(fhirContext, bundle);
        }
    }
}
