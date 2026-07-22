package org.opencds.cqf.fhir.utility.npm.r4;

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
 * This is for R4 only.
 */
public class R4NpmPackageLoaderInMemory extends BaseNpmPackageLoaderInMemory implements NpmPackageLoader {

    public static R4NpmPackageLoaderInMemory fromNpmPackageAbsolutePath(List<Path> tgzPaths) {
        return fromNpmPackageAbsolutePath(null, tgzPaths);
    }

    public static R4NpmPackageLoaderInMemory fromNpmPackageAbsolutePath(
            NpmNamespaceManager npmNamespaceManager, List<Path> tgzPaths) {
        final Set<NpmPackage> npmPackages = buildNpmPackagesFromAbsolutePath(tgzPaths);

        return new R4NpmPackageLoaderInMemory(npmPackages, npmNamespaceManager);
    }

    public static R4NpmPackageLoaderInMemory fromNpmPackageClasspath(Class<?> clazz, Path... tgzPaths) {
        return fromNpmPackageClasspath(null, clazz, tgzPaths);
    }

    public static R4NpmPackageLoaderInMemory fromNpmPackageClasspath(
            @Nullable NpmNamespaceManager npmNamespaceManager, Class<?> clazz, Path... tgzPaths) {
        return fromNpmPackageClasspath(npmNamespaceManager, clazz, Arrays.asList(tgzPaths));
    }

    public static R4NpmPackageLoaderInMemory fromNpmPackageClasspath(Class<?> clazz, List<Path> tgzPaths) {
        return fromNpmPackageClasspath(null, clazz, tgzPaths);
    }

    public static R4NpmPackageLoaderInMemory fromNpmPackageClasspath(
            @Nullable NpmNamespaceManager npmNamespaceManager, Class<?> clazz, List<Path> tgzPaths) {
        final Set<NpmPackage> npmPackages = buildNpmPackageFromClasspath(clazz, tgzPaths);

        return new R4NpmPackageLoaderInMemory(npmPackages, npmNamespaceManager);
    }

    public R4NpmPackageLoaderInMemory(Set<NpmPackage> npmPackages, @Nullable NpmNamespaceManager npmNamespaceManager) {
        super(npmPackages, npmNamespaceManager);
    }

    @Override
    public FhirContext getFhirContext() {
        return FhirContext.forR4Cached();
    }
}
