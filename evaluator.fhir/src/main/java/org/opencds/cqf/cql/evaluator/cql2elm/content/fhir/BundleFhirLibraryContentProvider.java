package org.opencds.cqf.cql.evaluator.cql2elm.content.fhir;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;

/**
 * This class implements the cql-translator LibraryContentProvider API, using a Bundle
 * containing Library Resources as a source for the CQL content.
 */
public class BundleFhirLibraryContentProvider extends BaseFhirLibraryContentProvider {

    private IBaseBundle bundle;
    private FhirContext fhirContext;
    private LibraryVersionSelector libraryVersionSelector;

    public BundleFhirLibraryContentProvider(FhirContext fhirContext, IBaseBundle bundle, AdapterFactory adapterFactory, LibraryVersionSelector libraryVersionSelector) {
        super(adapterFactory);
        this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null");
        this.bundle = requireNonNull(bundle, "bundle can not be null");
        this.libraryVersionSelector = requireNonNull(libraryVersionSelector, "libraryVersionSelector can not be null");

        if (!this.bundle.getStructureFhirVersionEnum().equals(fhirContext.getVersion().getVersion())) {
            throw new IllegalArgumentException("the FHIR versions of bundle and fhirContext must match");
        }
    }

    protected IBaseBundle getBundle() {
        return this.bundle;
    }

    protected FhirContext getFhirContext() {
        return this.fhirContext;
    }

    @Override
    protected IBaseResource getLibrary(VersionedIdentifier libraryIdentifier) {
        List<? extends IBaseResource> resources = BundleUtil.toListOfResourcesOfType(this.getFhirContext(),
                this.getBundle(), this.getFhirContext().getResourceDefinition("Library").getImplementingClass());
        if (resources == null || resources.isEmpty()) {
            return null;
        }

        Collection<IBaseResource> libraries = resources.stream().map(x -> (IBaseResource)x).collect(Collectors.toList());
        
        if (libraries == null || libraries.isEmpty()) {
            return null;
        }
        
        return this.libraryVersionSelector.select(libraryIdentifier, libraries);
    }
}