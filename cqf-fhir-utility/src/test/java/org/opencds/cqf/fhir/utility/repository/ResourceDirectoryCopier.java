package org.opencds.cqf.fhir.utility.repository;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;

/**
 * This class just copies a directory from a jar file into some target directory. Its only purpose
 * is to facilitate testing by allowing us to use a temporary directory for testing.
 */
public class ResourceDirectoryCopier {

    public static void copyFromJar(final Class<?> clazz, final String sourceDirectory, final Path target)
            throws URISyntaxException, IOException {
        URI resource = clazz.getResource(sourceDirectory).toURI();
        PathReference pr = null;
        try {
            var pathReference = PathReference.getPath(resource);
            final Path jarPath = pathReference.getPath();
            Files.walkFileTree(jarPath, new SimpleFileVisitor<Path>() {

                private Path currentTarget;

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    currentTarget = target.resolve(jarPath.relativize(dir).toString());
                    Files.createDirectories(currentTarget);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.copy(
                            file,
                            target.resolve(jarPath.relativize(file).toString()),
                            StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Unable to copy directory", e);
        } finally {
            if (pr != null) {
                pr.close();
            }
        }
    }

    static class PathReference {

        private final Path path;
        private final FileSystem fs;

        public PathReference(Path path, FileSystem fs) {
            this.path = path;
            this.fs = fs;
        }

        public void close() throws IOException {
            if (this.fs != null) this.fs.close();
        }

        public Path getPath() {
            return this.path;
        }

        public FileSystem getFileSystem() {
            return this.fs;
        }

        public static PathReference getPath(final URI resPath) throws IOException {
            try {
                // first try getting a path via existing file systems
                return new PathReference(Paths.get(resPath), null);
            } catch (final FileSystemNotFoundException e) {
                /*
                 * not directly on file system, so then it's somewhere else (e.g.: JAR)
                 */
                final Map<String, ?> env = Collections.emptyMap();
                final FileSystem fs = FileSystems.newFileSystem(resPath, env);
                return new PathReference(fs.provider().getPath(resPath), fs);
            }
        }
    }
}
