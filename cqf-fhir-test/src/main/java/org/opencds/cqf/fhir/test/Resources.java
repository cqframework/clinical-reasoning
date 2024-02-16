package org.opencds.cqf.fhir.test;

import java.io.File;
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
 * This class implements a number of utility methods for working with resource files.
 */
public class Resources {

    private Resources() {
        // intentionally empty
    }

    /**
     * This method returns the real, on-disk path of the resource directory for the given class.
     * @param clazz the class to use to find the resource directory for
     * @return
     */
    public static String getResourcePath(Class<?> clazz) {
        return new File(clazz.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .getPath())
                .getAbsolutePath();
    }

    /**
     * This method copies a resource directory from a jar into a target directory.
     * This is useful for testing, as it allows us to use a export jar resources.
     *
     * This method defaults to using the caller class to locate the resource package.
     *
     * @param sourcePackage the source package to copy
     * @param target the target directory to copy to
     * @throws URISyntaxException
     * @throws IOException
     * @throws ClassNotFoundException if caller class cannot be determined
     */
    public static void copyFromJar(final String sourcePackage, Path target)
            throws URISyntaxException, IOException, ClassNotFoundException {
        copyFromJar(getCallerClass(2), sourcePackage, target);
    }

    /**
     * This method copies a resource directory from a jar into a target directory.
     * This is useful for testing, as it allows us to use a export jar resources.
     *
     * This method allows the caller to specify the class to use.
     *
     * @param clazz the class to use for locating the resource package
     * @param sourcePackage the source package to copy
     * @param target the target directory to copy to
     * @throws URISyntaxException
     * @throws IOException
     */
    public static void copyFromJar(final Class<?> clazz, final String sourcePackage, final Path target)
            throws URISyntaxException, IOException {
        URI resource = clazz.getResource(sourcePackage).toURI();
        try (var pathReference = PathReference.getPath(resource)) {
            final Path jarPath = pathReference.getPath();
            Files.walkFileTree(jarPath, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    var currentTarget = target.resolve(jarPath.relativize(dir).toString());
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
        }
    }

    private static Class<?> getCallerClass(int level) throws ClassNotFoundException {
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        String rawFQN = stElements[level + 1].toString().split("\\(")[0];
        return Class.forName(rawFQN.substring(0, rawFQN.lastIndexOf('.')));
    }

    static class PathReference implements AutoCloseable {

        private final Path path;
        private final FileSystem fs;

        public PathReference(Path path, FileSystem fs) {
            this.path = path;
            this.fs = fs;
        }

        public void close() throws IOException {
            if (this.fs != null) {
                this.fs.close();
            }
        }

        public Path getPath() {
            return this.path;
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
