package org.opencds.cqf.cql.evaluator.builder.library;

import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isUri;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.cql2elm.content.InMemoryLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class CqlFileLibraryContentProviderFactory implements TypedLibraryContentProviderFactory {
    private Logger logger = LoggerFactory.getLogger(CqlFileLibraryContentProviderFactory.class);

    @Inject
    CqlFileLibraryContentProviderFactory(){}

    @Override
    public String getType() {
        return Constants.HL7_CQL_FILES;
    }

    @Override
    public LibraryContentProvider create(String url, List<String> headers) {
        List<String> libraries = this.getLibrariesFromPath(url);
        return new InMemoryLibraryContentProvider(libraries);
    }

    protected List<String> getLibrariesFromPath(String path) {
        URI uri;
        try{
            if (!isUri(path)) {
                File file = new File(path);
                uri = file.toURI();
            }
            else {
                uri = new URI(path);
            }
        }
        catch(Exception e) {
            logger.error(String.format("error attempting to bundle path: %s", path), e);
            throw new RuntimeException(e);
        }

        Collection<File> files;
        if (uri.getScheme() != null && uri.getScheme().startsWith("jar")) {
            files = this.listJar(uri, path);
        }
        else {
            files = this.listDirectory(uri.getPath());
        }

            
        return this.readFiles(files);
    }


    private Collection<File> listJar(URI uri, String path) {
        try {
            FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
            Path jarPath = fileSystem.getPath(path);
            try(Stream<Path> walk = Files.walk(jarPath, FileVisitOption.FOLLOW_LINKS)) {
                return walk.map(x -> x.toFile()).filter(x -> x.isFile()).filter(
                    x -> x.getName().endsWith("json") || x.getName().endsWith("xml")).collect(Collectors.toList());
            }
        }
        catch (Exception e) {
            logger.error(String.format("error attempting to list jar: %s", uri.toString()));
            throw new RuntimeException(e);
        }
    }

    private Collection<File> listDirectory(String path) {
        File resourceDirectory = new File(path);
        if (!resourceDirectory.getAbsoluteFile().exists()) {
            throw new IllegalArgumentException(String.format("The specified path to resource files does not exist: %s", path));
        }

        if (resourceDirectory.getAbsoluteFile().isDirectory()) {
            return FileUtils.listFiles(resourceDirectory, new String[] { "cql" }, true);
        }
        else if (path.toLowerCase().endsWith("cql")) {
            return Collections.singletonList(resourceDirectory);
        }
        else {
            throw new IllegalArgumentException(String.format("path was not a directory or a recognized CQL file format (.cql) : %s", path));
        }
    }

    List<String> readFiles(Collection<File> files) {
        return files.stream().map(x -> x.toPath()).filter(Files::isRegularFile).map(t -> {
            try {
                return Files.readAllBytes(t);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(x -> x != null).map(x -> new String(x, StandardCharsets.UTF_8)).collect(Collectors.toList());
    }

}
