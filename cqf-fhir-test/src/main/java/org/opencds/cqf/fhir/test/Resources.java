package org.opencds.cqf.fhir.test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

/**
 * This class implements a number of utility methods for working with resource files.
 */
public class Resources {

    private Resources() {
        // intentionally empty
    }

    /**
     * This method returns the real, on-disk path of the test resources root for the given class.
     * It searches the classpath for a resources output directory, which is separate from
     * the compiled classes directory in Gradle's standard layout.
     *
     * @param clazz the class to use to find the resource directory for
     * @return the absolute path to the classpath root containing resources for this class
     */
    public static String getResourcePath(Class<?> clazz) {
        String codeSourcePath = new File(clazz.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .getPath())
                .getAbsolutePath();

        // Walk up the package hierarchy to find the resources classpath root.
        // The classloader may return multiple entries (classes dirs, resources dirs,
        // dependency jars). We specifically look for a root path containing "resources"
        // to distinguish it from compiled classes directories.
        try {
            String[] parts = clazz.getPackageName().split("\\.");
            for (int depth = parts.length; depth > 0; depth--) {
                String probe = String.join("/", java.util.Arrays.copyOf(parts, depth));
                Enumeration<URL> urls = clazz.getClassLoader().getResources(probe);
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    if ("file".equals(url.getProtocol())) {
                        String fullPath = new File(url.toURI()).getAbsolutePath();
                        String root = fullPath.substring(0, fullPath.length() - probe.length() - 1);
                        // Prefer the resources directory over any classes directory
                        if (root.contains("resources")) {
                            return root;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fall through to code source
        }

        return codeSourcePath;
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
        URI resource = resolveResourceDir(clazz, sourcePackage);
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

    /**
     * Resolves a resource directory URI, preferring the resources classpath entry over
     * the classes entry. This handles Gradle's split output directories where class files
     * and resource files live in separate directory trees.
     */
    private static URI resolveResourceDir(Class<?> clazz, String sourcePackage) throws URISyntaxException, IOException {
        URL standard = clazz.getResource(sourcePackage);
        if (standard == null) {
            throw new IllegalArgumentException("Resource not found: " + sourcePackage);
        }

        URI standardUri = standard.toURI();

        // If not a file URI or already in a resources directory, use as-is
        if (!"file".equals(standardUri.getScheme()) || standardUri.getPath().contains("/resources/")) {
            return standardUri;
        }

        // Compute the full resource path to search for via the classloader
        String resourcePath;
        if (sourcePackage.startsWith("/")) {
            resourcePath = sourcePackage.substring(1);
        } else if (sourcePackage.isEmpty()) {
            resourcePath = clazz.getPackageName().replace('.', '/');
        } else {
            resourcePath = clazz.getPackageName().replace('.', '/') + "/" + sourcePackage;
        }

        // Search all classpath entries for this path, preferring the resources directory
        Enumeration<URL> urls = clazz.getClassLoader().getResources(resourcePath.isEmpty() ? "" : resourcePath);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            if ("file".equals(url.getProtocol()) && url.getPath().contains("/resources/")) {
                return url.toURI();
            }
        }

        // Fall back to standard resolution
        return standardUri;
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
