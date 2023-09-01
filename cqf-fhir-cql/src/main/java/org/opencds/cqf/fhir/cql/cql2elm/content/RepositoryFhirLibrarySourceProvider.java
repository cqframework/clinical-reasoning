package org.opencds.cqf.fhir.cql.cql2elm.content;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.opencds.cqf.fhir.utility.search.Searches;

public class RepositoryFhirLibrarySourceProvider extends BaseFhirLibrarySourceProvider {

    private Repository repository;
    private FhirContext fhirContext;
    private LibraryVersionSelector libraryVersionSelector;

    public RepositoryFhirLibrarySourceProvider(
            Repository repository, AdapterFactory adapterFactory, LibraryVersionSelector libraryVersionSelector) {
        super(adapterFactory);
        this.repository = requireNonNull(repository, "repository can not be null");
        this.fhirContext = repository.fhirContext();
        this.libraryVersionSelector = requireNonNull(libraryVersionSelector, "libraryVersionSelector can not be null");
    }

    protected Repository getRepository() {
        return this.repository;
    }

    protected FhirContext getFhirContext() {
        return this.fhirContext;
    }

    @Override
    protected IBaseResource getLibrary(VersionedIdentifier libraryIdentifier) {
        List<? extends IBaseResource> resources = BundleUtil.toListOfResources(
                this.fhirContext,
                Repositories.searchRepositoryWithPaging(
                        fhirContext,
                        repository,
                        this.fhirContext.getResourceDefinition("Library").getImplementingClass(),
                        Searches.byNameAndVersion(libraryIdentifier.getId(), libraryIdentifier.getVersion()),
                        null));

        if (resources == null || resources.isEmpty()) {
            return null;
        }

        Collection<IBaseResource> libraries =
                resources.stream().map(x -> (IBaseResource) x).collect(Collectors.toList());

        if (libraries == null || libraries.isEmpty()) {
            return null;
        }

        return this.libraryVersionSelector.select(libraryIdentifier, libraries);
    }
}
