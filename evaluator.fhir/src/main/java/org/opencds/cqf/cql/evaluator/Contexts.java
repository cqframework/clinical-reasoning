package org.opencds.cqf.cql.evaluator;

import ca.uhn.fhir.context.FhirContext;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.cql2elm.quick.FhirLibrarySourceProvider;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.cql.model.ModelIdentifier;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.RepositoryFhirLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.model.CacheAwareModelManager;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.execution.CacheAwareLibraryLoaderDecorator;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatorOptionAwareLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.terminology.RepositoryTerminologyProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.fhir.api.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Contexts {

    public static Context forRepository(FhirContext fhirContext, Library library,
            Repository repository) {
        Context context = new Context(library);
        TerminologyProvider terminologyProvider =
                new RepositoryTerminologyProvider(fhirContext, repository);
        LibrarySourceProvider librarySourceProvider =
                buildLibrarySource(fhirContext, terminologyProvider, repository);
        LibraryLoader libraryLoader = buildLibraryLoader(librarySourceProvider);

        context.registerLibraryLoader(libraryLoader);
        context.registerTerminologyProvider(terminologyProvider);
        // context.registerDataProvider("", dataProvider);

        return context;
    }


    private static Map<ModelIdentifier, Model> globalModelCache = new ConcurrentHashMap<>();

    private static Map<org.cqframework.cql.elm.execution.VersionedIdentifier, org.cqframework.cql.elm.execution.Library> libraryCache;

    private static CqlOptions cqlOptions = CqlOptions.defaultOptions();

    private static LibrarySourceProvider buildLibrarySource(FhirContext fhirContext,
            TerminologyProvider terminologyProvider, Repository repository) {
        AdapterFactory adapterFactory = getAdapterFactory(fhirContext);
        return new RepositoryFhirLibrarySourceProvider(fhirContext, repository, adapterFactory,
                new LibraryVersionSelector(adapterFactory));

    }

    // TODO: This is duplicate logic from the evaluator builder
    // TODO: Add NPM library source loader support
    private static LibraryLoader buildLibraryLoader(LibrarySourceProvider librarySourceProvider) {
        List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
        librarySourceProviders.add(librarySourceProvider);
        if (cqlOptions.useEmbeddedLibraries()) {
            librarySourceProviders.add(new FhirLibrarySourceProvider());
        }

        TranslatorOptionAwareLibraryLoader libraryLoader =
                new TranslatingLibraryLoader(new CacheAwareModelManager(globalModelCache),
                        librarySourceProviders, cqlOptions.getCqlTranslatorOptions(), null);

        if (libraryCache != null) {
            libraryLoader = new CacheAwareLibraryLoaderDecorator(libraryLoader, libraryCache);
        }

        return libraryLoader;
    }

    private static AdapterFactory getAdapterFactory(FhirContext fhirContext) {
        switch (fhirContext.getVersion().getVersion()) {
            case DSTU3:
                return new org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory();
            case R4:
                return new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory();
            case R5:
                return new org.opencds.cqf.cql.evaluator.fhir.adapter.r5.AdapterFactory();
            default:
                throw new IllegalArgumentException(
                        String.format("unsupported FHIR version: %s", fhirContext));
        }

    }


}
