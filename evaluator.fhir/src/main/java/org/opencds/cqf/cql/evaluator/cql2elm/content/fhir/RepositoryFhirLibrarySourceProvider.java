package org.opencds.cqf.cql.evaluator.cql2elm.content.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.fhir.api.Repository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class RepositoryFhirLibrarySourceProvider extends BaseFhirLibrarySourceProvider {

    private Repository repository;
    private FhirContext fhirContext;
    private LibraryVersionSelector libraryVersionSelector;

    public RepositoryFhirLibrarySourceProvider(FhirContext fhirContext, Repository repository, AdapterFactory adapterFactory, LibraryVersionSelector libraryVersionSelector) {
        super(adapterFactory);
        this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null");
        this.repository = requireNonNull(repository, "repository can not be null");
        this.libraryVersionSelector = requireNonNull(libraryVersionSelector, "libraryVersionSelector can not be null");

        //todo: need to match version
//        if (!this.repository..getStructureFhirVersionEnum().equals(fhirContext.getVersion().getVersion())) {
//            throw new IllegalArgumentException("the FHIR versions of bundle and fhirContext must match");
//        }
    }

    protected Repository getRepository() {
        return this.repository;
    }

    protected FhirContext getFhirContext() {
        return this.fhirContext;
    }

    @Override
    protected IBaseResource getLibrary(VersionedIdentifier libraryIdentifier) {
        List<? extends IBaseResource> resources = BundleUtil.toListOfResources(this.fhirContext, repository.search(IBaseBundle.class,
                this.fhirContext.getResourceDefinition("Library").getImplementingClass(),
                null, null));
        if (resources == null || resources.isEmpty()) {
            return null;
        }

        Collection<IBaseResource> libraries = resources.stream().map(x -> (IBaseResource) x).collect(Collectors.toList());

        if (libraries == null || libraries.isEmpty()) {
            return null;
        }

        return this.libraryVersionSelector.select(libraryIdentifier, libraries);
    }
}
