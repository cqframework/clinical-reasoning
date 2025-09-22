package org.opencds.cqf.fhir.cql.cql2elm.content;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.io.IOException;
import java.util.ArrayList;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.hl7.elm_modelinfo.r1.serializing.ModelInfoReaderFactory;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.iterable.BundleIterable;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class RepositoryFhirModelInfoProvider extends BaseFhirModelInfoProvider {

    private static Logger logger = LoggerFactory.getLogger(RepositoryFhirModelInfoProvider.class);

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
    public ModelInfo load(ModelIdentifier modelIdentifier) {
        var is = getModelInfoContent(modelIdentifier, ModelInfoContentType.XML);
        if (is == null) {
            logger.error("Unable to locate model info content for {}", modelIdentifier.getId());
            return null;
        }

        var xmlReader = ModelInfoReaderFactory.getReader(ModelInfoContentType.XML.mimeType());
        try {
            return xmlReader.read(is);
        } catch (IOException e) {
            logger.error(
                    "Error encountered while loading model info for {}: {}", modelIdentifier.getId(), e.getMessage());
            return null;
        }
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
