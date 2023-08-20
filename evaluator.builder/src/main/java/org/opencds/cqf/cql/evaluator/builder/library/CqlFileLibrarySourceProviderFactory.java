package org.opencds.cqf.cql.evaluator.builder.library;

import static org.opencds.cqf.fhir.utility.UriUtil.isUri;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
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
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.InMemoryLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.fhir.Constants;

@Named
public class CqlFileLibrarySourceProviderFactory implements TypedLibrarySourceProviderFactory {

  @Inject
  public CqlFileLibrarySourceProviderFactory() {}

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
    URI uri;
    try {
      if (!isUri(path)) {
        File file = new File(path);
        uri = file.toURI();
      } else {
        uri = new URI(path);
      }
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(String.format("error attempting to bundle path: %s", path),
          e);
    }

    Collection<File> files;
    if (uri.getScheme() != null && uri.getScheme().startsWith("jar")) {
      files = this.listJar(uri, path);
    } else {
      files = this.listDirectory(uri.getPath());
    }


    return this.readFiles(files);
  }


  private Collection<File> listJar(URI uri, String path) {
    try (var fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap())) {
      Path jarPath = fileSystem.getPath(path);
      try (Stream<Path> walk = Files.walk(jarPath, FileVisitOption.FOLLOW_LINKS)) {
        return walk.map(Path::toFile).filter(File::isFile)
            .filter(x -> x.getName().endsWith("json") || x.getName().endsWith("xml"))
            .collect(Collectors.toList());
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(
          String.format("error attempting to list jar: %s", uri.toString()), e);
    }
  }

  private Collection<File> listDirectory(String path) {
    File resourceDirectory = new File(path);
    if (!resourceDirectory.getAbsoluteFile().exists()) {
      throw new IllegalArgumentException(
          String.format("The specified path to resource files does not exist: %s", path));
    }

    if (resourceDirectory.getAbsoluteFile().isDirectory()) {
      return FileUtils.listFiles(resourceDirectory, new String[] {"cql"}, true);
    } else if (path.toLowerCase().endsWith("cql")) {
      return Collections.singletonList(resourceDirectory);
    } else {
      throw new IllegalArgumentException(String
          .format("path was not a directory or a recognized CQL file format (.cql) : %s", path));
    }
  }

  List<String> readFiles(Collection<File> files) {
    return files.stream().map(File::toPath).filter(Files::isRegularFile).map(t -> {
      try {
        return Files.readAllBytes(t);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }).filter(x -> x != null).map(x -> new String(x, StandardCharsets.UTF_8))
        .collect(Collectors.toList());
  }

}
