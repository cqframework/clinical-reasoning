package org.opencds.cqf.cql.evaluator.builder.library;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.cql2elm.InMemoryLibrarySourceProvider;

public class CqlFileLibrarySourceProviderFactory implements TypedLibrarySourceProviderFactory {
   
    @Override
    public String getType() {
        return Constants.HL7_CQL_FILES;
    }

    @Override
    public LibrarySourceProvider create(String url, List<String> headers) {
        List<String> libraries = this.getLibrariesFromPath(url);
        return new InMemoryLibrarySourceProvider(libraries);
    }
    
    protected List<String> getLibrariesFromPath(String path) {
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
    
}
