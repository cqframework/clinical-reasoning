package org.opencds.cqf.fhir.cql.cql2elm.content;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.iterable.BundleIterable;
import org.opencds.cqf.fhir.utility.search.Searches;

public class RepositoryFhirLibrarySourceProvider extends BaseFhirLibrarySourceProvider {

    private final IRepository repository;
    private final FhirContext fhirContext;
    private final LibraryVersionSelector libraryVersionSelector;

    public RepositoryFhirLibrarySourceProvider(
            IRepository repository, IAdapterFactory adapterFactory, LibraryVersionSelector libraryVersionSelector) {
        super(adapterFactory);
        this.repository = requireNonNull(repository, "repository can not be null");
        this.fhirContext = repository.fhirContext();
        this.libraryVersionSelector = requireNonNull(libraryVersionSelector, "libraryVersionSelector can not be null");
    }

    protected IRepository getRepository() {
        return this.repository;
    }

    protected FhirContext getFhirContext() {
        return this.fhirContext;
    }

    @Override
    protected IBaseResource getLibrary(VersionedIdentifier libraryIdentifier) {

        @SuppressWarnings("unchecked")
        var bt = (Class<IBaseBundle>)
                this.fhirContext.getResourceDefinition("Bundle").getImplementingClass();
        var lt = this.fhirContext.getResourceDefinition("Library").getImplementingClass();

        // HACK: Retry to handle transient failures
        // This should be baked into the IRepository implementation

        var retries = 5;

        BundleIterable<IBaseBundle> iter = null;

        for (int i = 0; i < retries; i++) {
            try {
                var libs = repository.search(
                        bt, lt, Searches.byNameAndVersion(libraryIdentifier.getId(), libraryIdentifier.getVersion()));
                iter = new BundleIterable<>(repository, libs);
                if (iter != null && iter.iterator().hasNext()) {
                    break;
                }

                if (i == retries - 1) {
                    break;
                }

                Thread.sleep(20 * (i + 1));
            } catch (Exception e) {
                if (i == retries - 1) {
                    break;
                }

                try {
                    Thread.sleep(20 * (i + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }

        if (iter == null || !iter.iterator().hasNext()) {
            return null;
        }

        var libraries = new ArrayList<IBaseResource>();
        iter.iterator().forEachRemaining(x -> libraries.add(x.getResource()));

        return this.libraryVersionSelector.select(libraryIdentifier, libraries);
    }
}
