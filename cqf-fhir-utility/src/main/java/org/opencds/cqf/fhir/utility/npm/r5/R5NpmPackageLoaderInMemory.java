package org.opencds.cqf.fhir.utility.npm.r5;

import ca.uhn.fhir.context.FhirContext;
import jakarta.annotation.Nullable;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.opencds.cqf.fhir.utility.npm.BaseNpmPackageLoaderInMemory;
import org.opencds.cqf.fhir.utility.npm.NpmNamespaceManager;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

// LUKETODO:  redo java

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

    public R5NpmPackageLoaderInMemory(
            Set<NpmPackage> npmPackages, @Nullable NpmNamespaceManager npmNamespaceManager) {
        super(npmPackages, npmNamespaceManager);
    }

    @Override
    public FhirContext getFhirContext() {
        return FhirContext.forR5Cached();
    }
}
