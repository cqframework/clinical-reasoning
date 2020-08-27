package org.opencds.cqf.cql.evaluator.cql2elm;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;

/**
 * This class implements the cql-translator LibrarySourceProvider API, using a Bundle
 * containing Library Resources as a source for the CQL content.
 */
public class BundleLibrarySourceProvider
        extends VersionComparingLibrarySourceProvider {

    private IBaseBundle bundle;
    private FhirContext fhirContext;

    public BundleLibrarySourceProvider(FhirContext fhirContext, IBaseBundle bundle) 
    {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null");
        this.bundle = Objects.requireNonNull(bundle, "bundle can not be null");

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

        return this.select(libraries, libraryIdentifier);
    }
}