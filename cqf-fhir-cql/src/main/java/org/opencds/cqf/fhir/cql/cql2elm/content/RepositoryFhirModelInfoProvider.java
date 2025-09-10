package org.opencds.cqf.fhir.cql.cql2elm.content;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.iterable.BundleIterable;
import org.opencds.cqf.fhir.utility.search.Searches;

@SuppressWarnings("UnstableApiUsage")
public class RepositoryFhirModelInfoProvider extends BaseFhirModelInfoProvider {

    private final IRepository repository;
    private final FhirContext fhirContext;
    private final LibraryVersionSelector libraryVersionSelector;

    public RepositoryFhirModelInfoProvider(
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
    protected IBaseResource getLibrary(ModelIdentifier modelIdentifier) {
        VersionedIdentifier libraryIdentifier = toLibraryIdentifier(modelIdentifier);

        // TODO: Support lookup by URL...

        @SuppressWarnings("unchecked")
        var bt = (Class<IBaseBundle>)
                this.fhirContext.getResourceDefinition("Bundle").getImplementingClass();
        var lt = this.fhirContext.getResourceDefinition("Library").getImplementingClass();

        var libs = repository.search(
                bt, lt, Searches.byNameAndVersion(libraryIdentifier.getId(), libraryIdentifier.getVersion()));

        var iter = new BundleIterable<>(repository, libs).iterator();

        if (!iter.hasNext()) {
            return null;
        }

        var libraries = new ArrayList<IBaseResource>();
        iter.forEachRemaining(x -> libraries.add(x.getResource()));

        return this.libraryVersionSelector.select(libraryIdentifier, libraries);
    }

    protected VersionedIdentifier toLibraryIdentifier(ModelIdentifier modelIdentifier) {
        return new VersionedIdentifier()
                .withSystem(modelIdentifier.getSystem())
                .withId(modelIdentifier.getId())
                .withVersion(modelIdentifier.getVersion());
    }
}
