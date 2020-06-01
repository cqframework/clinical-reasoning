package org.opencds.cqf.cql.evaluator.factory.implementation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opencds.cqf.cql.evaluator.Helpers;
import org.opencds.cqf.cql.evaluator.factory.ClientFactory;
import org.opencds.cqf.cql.evaluator.factory.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.loader.TranslatingLibraryLoader;
import org.opencds.cqf.cql.evaluator.manager.CacheAwareModelManager;
import org.opencds.cqf.cql.evaluator.provider.Dstu3FhirServerLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.provider.InMemoryLibrarySourceProvider;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslator.Options;
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.execution.LibraryLoader;

import ca.uhn.fhir.rest.client.api.IGenericClient;

// This is intended to be general-purpose factory but there are specific cases where we probably want to change the behavior.
// For example, the global model cache will slow down (slightly) processing in the CLI mode, while it will speed up requests
// overall in service mode due to not needing to reload models on every request.
public class DefaultLibraryLoaderFactory implements LibraryLoaderFactory {

    private static final Map<VersionedIdentifier, Model> globalModelCache = new HashMap<>();

    public LibraryLoader create(List<String> libraries, EnumSet<CqlTranslator.Options> translatorOptions) {
        if (libraries == null || libraries.isEmpty()) {
            return null;
        }

        ModelManager modelManager = new CacheAwareModelManager(globalModelCache);
        LibraryManager libraryManager = createLibraryManager(modelManager, libraries, translatorOptions);
        LibraryLoader libraryLoader = new TranslatingLibraryLoader(libraryManager);

        return libraryLoader;
    }

    public LibraryLoader create(String libraryPath, EnumSet<Options> translatorOptions) {
        return this.create(libraryPath, translatorOptions, null);
    }

    public LibraryLoader create(String libraryUrl, EnumSet<CqlTranslator.Options> translatorOptions, ClientFactory clientFactory) {
        if (libraryUrl == null) {
            return null;
        }

        ModelManager modelManager = new CacheAwareModelManager(globalModelCache);

        LibraryManager libraryManager;
        if (Helpers.isFileUri(libraryUrl)) {
            List<String> libraries = this.getLibrariesFromPath(libraryUrl);
            libraryManager = this.createLibraryManager(modelManager, libraries, translatorOptions);
        } else {
            if (clientFactory == null) {
                throw new IllegalArgumentException(String.format("Needed to access remote url %s and clientFactory was null"));
            }
            libraryManager = this.createLibraryManager(modelManager, libraryUrl, translatorOptions, clientFactory);
        }

        return new TranslatingLibraryLoader(libraryManager);
    }

    private LibraryManager createLibraryManager(ModelManager modelManager, List<String> libraries,
            EnumSet<CqlTranslator.Options> translatorOptions) {
        LibraryManager libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().registerProvider(new InMemoryLibrarySourceProvider(libraries));
        libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
        return libraryManager;
    }

    private LibraryManager createLibraryManager(ModelManager modelManager, String libraryUrl,
            EnumSet<CqlTranslator.Options> translatorOptions, ClientFactory clientFactory) {
        LibraryManager libraryManager = new LibraryManager(modelManager);

        IGenericClient client = clientFactory.create(libraryUrl);

        libraryManager.getLibrarySourceLoader().registerProvider(new Dstu3FhirServerLibrarySourceProvider(client));
        libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
        return libraryManager;
    }

    List<String> getLibrariesFromPath(String path) {
        Path directoryPath = Paths.get(path);
        File directory = new File(directoryPath.toAbsolutePath().toString());
        File[] files = directory.listFiles((d, name) -> name.endsWith(".cql"));

        return Arrays.asList(files).stream().map(x -> x.toPath()).filter(Files::isRegularFile).map(t -> {
            try {
                return Files.readAllBytes(t);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(x -> x != null).map(x -> new String(x, StandardCharsets.UTF_8)).collect(Collectors.toList());
    }
}