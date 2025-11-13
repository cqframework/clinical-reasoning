package org.opencds.cqf.fhir.test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
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
     * @return the on-disk path for the class resources
     */
    public static String getResourcePath(Class<?> clazz) {
       return Path.of(getResourceUri(clazz)).toString();
    }

    /**
     * This method returns the real, physical URI of the resource directory for the given class.
     * @param clazz the class to use to find the resource directory for
     * @return the URI
     */
    public static URI getResourceUri(Class<?> clazz) {
        try {
            return getResourcePath(clazz, "");
        }
        catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * This method returns the real, on-disk path of the resource directory for the given class.
     * @param clazz the class to use to find the resource directory for
     * @param sourcePackage the source package to use as the root
     * @return the on-disk path for the class resources of the sourcePackage
     */
    public static URI getResourcePath(Class<?> clazz, String sourcePackage) throws URISyntaxException {
        var clazzPackage = clazz.getPackageName();
        var targetPackage = sourcePackage == null || sourcePackage.isEmpty() ?
            "/" + clazzPackage.replace(".", "/") :
            sourcePackage;
        var resourceUrl = clazz.getResource(targetPackage);
        if (resourceUrl == null) {
            var msg = "Unable to determine resource url for class %s and sourcePackage %s"
                .formatted(clazz.getSimpleName(), sourcePackage);
            throw new IllegalArgumentException(msg);
        }
        var uri = resourceUrl.toURI();
        // Handle some gradle-specific shenanigans, which is that under dev gradle splits
        // the classes and resources into separate directories: build/classes and build/resources
        var trace = String.join(
            ",",
            Arrays.stream(
                Thread.currentThread().getStackTrace())
                .map(StackTraceElement::toString).toList());
        boolean runningInGradle =
            trace.contains("org.gradle") ||
                System.getProperty("java.class.path", "").contains("gradle");

        if (runningInGradle && uri.getPath().contains("build/classes/java")) {
            var newPath = uri.getPath()
                .replace("build/classes/java", "build/resources");
            uri = new URI(
                uri.getScheme(),
                uri.getUserInfo(),
                uri.getHost(),
                uri.getPort(),
                newPath,
                uri.getQuery(),
                uri.getFragment()
            );
        }

        return uri;
    }

    /**
     * This method copies a resource directory from a jar into a target directory.
     * This is useful for testing, as it allows us to use a export jar resources.
     * <p>
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
        var clazz = getCallerClass(2);

        copyFromJar(clazz, sourcePackage, target);
    }

    /**
     * This method copies a resource directory from a jar into a target directory.
     * This is useful for testing, as it allows us to use a export jar resources.
     * <p>
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

        var uri = getResourcePath(clazz, sourcePackage);
        try (var pathReference = PathReference.getPath(uri)) {
            final Path jarPath = pathReference.path();
            Files.walkFileTree(jarPath, new SimpleFileVisitor<>() {

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

    record PathReference(Path path, FileSystem fs) implements AutoCloseable {

        public void close() throws IOException {
                if (this.fs != null) {
                    this.fs.close();
                }
            }

            public static PathReference getPath(final URI resPath) throws IOException {
                try {
                    // first try getting a path via existing file systems
                    return new PathReference(Path.of(resPath), null);
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
