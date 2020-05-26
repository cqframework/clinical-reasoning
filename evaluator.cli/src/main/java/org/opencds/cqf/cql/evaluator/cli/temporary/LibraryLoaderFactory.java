package org.opencds.cqf.cql.evaluator.cli.temporary;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;
import org.opencds.cqf.cql.evaluator.translation.provider.InMemoryLibrarySourceProvider;

// WARNING: This class is just a temporary stand-in until the builder is complete.
// We should replace this at the earliest opportunity
// DON'T FIX IT, DON'T EXTEND IT. KILL IT!
public class LibraryLoaderFactory {
    public LibraryLoader create(List<String> libraries) {

        ModelManager modelManager = new ModelManager();

        LibraryManager libraryManager = createLibraryManager(modelManager, libraries);
        LibraryLoader libraryLoader = new TranslatingLibraryLoader(libraryManager, CqlTranslatorOptions.defaultOptions());

        return libraryLoader;
    }

    public LibraryLoader create(String libraryPath) {
        // TODO: At some point we might want to add support for a remote library URI
        List<String> libraries = this.getLibrariesFromPath(libraryPath);

        return this.create(libraries);
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
        })
        .filter(x -> x != null)
        .map(x -> new String(x, StandardCharsets.UTF_8))
        .collect(Collectors.toList());
    }

    private LibraryManager createLibraryManager(ModelManager modelManager, List<String> libraries) {
        LibraryManager libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().registerProvider(new InMemoryLibrarySourceProvider(libraries));
        libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
        return libraryManager;
    }
}