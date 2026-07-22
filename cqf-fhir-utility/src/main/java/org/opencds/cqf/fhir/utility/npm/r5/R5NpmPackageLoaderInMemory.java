package org.opencds.cqf.fhir.utility.npm.r5;

import ca.uhn.fhir.context.FhirContext;
import jakarta.annotation.Nullable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.opencds.cqf.fhir.utility.npm.BaseNpmPackageLoaderInMemory;
import org.opencds.cqf.fhir.utility.npm.NpmNamespaceManager;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;

/**
 * Simplistic implementation of {@link NpmPackageLoader} that loads NpmPackages from the classpath
 * and stores {@link NpmPackage}s in a Set. This class is recommended for testing
 * and NOT for production.
 * <p/>
 * Optionally uses a custom {@link NpmNamespaceManager} but can also resolve all NamespaceInfos
 * by extracting them from all loaded packages at construction time.
 * <p/>
 * This is for R5 only.
 */
public class R5NpmPackageLoaderInMemory extends BaseNpmPackageLoaderInMemory implements NpmPackageLoader {

    public static R5NpmPackageLoaderInMemory fromNpmPackageAbsolutePath(List<Path> tgzPaths) {
        return fromNpmPackageAbsolutePath(null, tgzPaths);
    }

    public static R5NpmPackageLoaderInMemory fromNpmPackageAbsolutePath(
            NpmNamespaceManager npmNamespaceManager, List<Path> tgzPaths) {
        final Set<NpmPackage> npmPackages = buildNpmPackagesFromAbsolutePath(tgzPaths);

        return new R5NpmPackageLoaderInMemory(npmPackages, npmNamespaceManager);
    }

    public static R5NpmPackageLoaderInMemory fromNpmPackageClasspath(Class<?> clazz, Path... tgzPaths) {
        return fromNpmPackageClasspath(null, clazz, tgzPaths);
    }

    public static R5NpmPackageLoaderInMemory fromNpmPackageClasspath(
            @Nullable NpmNamespaceManager npmNamespaceManager, Class<?> clazz, Path... tgzPaths) {
        return fromNpmPackageClasspath(npmNamespaceManager, clazz, Arrays.asList(tgzPaths));
    }

    public static R5NpmPackageLoaderInMemory fromNpmPackageClasspath(Class<?> clazz, List<Path> tgzPaths) {
        return fromNpmPackageClasspath(null, clazz, tgzPaths);
    }

    public static R5NpmPackageLoaderInMemory fromNpmPackageClasspath(
            @Nullable NpmNamespaceManager npmNamespaceManager, Class<?> clazz, List<Path> tgzPaths) {
        final Set<NpmPackage> npmPackages = buildNpmPackageFromClasspath(clazz, tgzPaths);

        return new R5NpmPackageLoaderInMemory(npmPackages, npmNamespaceManager);
    }

    public R5NpmPackageLoaderInMemory(Set<NpmPackage> npmPackages, @Nullable NpmNamespaceManager npmNamespaceManager) {
        super(npmPackages, npmNamespaceManager);
    }

    @Override
    public FhirContext getFhirContext() {
        return FhirContext.forR5Cached();
    }
}
