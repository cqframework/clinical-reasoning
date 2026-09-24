package org.opencds.cqf.fhir.utility.npm;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.utilities.npm.NpmPackage;

public abstract class BaseNpmPackageLoaderInMemory implements NpmPackageLoader {

    public static final String FAILED_TO_LOAD_RESOURCE_TEMPLATE = "Failed to load resource: %s";

    private final Set<NpmPackage> npmPackages;
    private final NpmNamespaceManager npmNamespaceManager;

    @Override
    public Optional<IBaseResource> loadNpmResource(IPrimitiveType<String> resourceUrl) {
        return npmPackages.stream()
                .filter(npmPackage -> doesPackageMatch(resourceUrl, npmPackage))
                .map(npmPackage -> getResource(npmPackage, resourceUrl))
                .findFirst();
    }

    private IBaseResource getResource(NpmPackage npmPackage, IPrimitiveType<String> resourceUrl) {
        try {
            return tryGetResource(npmPackage, resourceUrl);
        } catch (IOException exception) {
            throw new InternalErrorException(
                    FAILED_TO_LOAD_RESOURCE_TEMPLATE.formatted(resourceUrl.getValue()), exception);
        }
    }

    private IBaseResource tryGetResource(NpmPackage npmPackage, IPrimitiveType<String> resourceUrl) throws IOException {

        final FhirContext fhirContext = getFhirContext(npmPackage);
        final String resourceUrlString = resourceUrl.getValue();

        final String[] split = resourceUrlString.split("\\|");

        try (InputStream libraryInputStream = npmPackage.loadByCanonical(split[0])) {
            return fhirContext.newJsonParser().parseResource(libraryInputStream);
        }
    }

    private boolean doesPackageMatch(IPrimitiveType<String> resourceUrl, NpmPackage npmPackage) {
        try {
            return npmPackage.hasCanonical(resourceUrl.getValue());
        } catch (IOException exception) {
            throw new InternalErrorException(FAILED_TO_LOAD_RESOURCE_TEMPLATE, exception);
        }
    }

    @Override
    public NpmNamespaceManager getNamespaceManager() {
        return npmNamespaceManager;
    }

    @Nonnull
    protected static Set<NpmPackage> buildNpmPackagesFromAbsolutePath(List<Path> tgzPaths) {
        return tgzPaths.stream()
                .map(BaseNpmPackageLoaderInMemory::getNpmPackageFromAbsolutePaths)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    protected static Set<NpmPackage> buildNpmPackageFromClasspath(Class<?> clazz, List<Path> tgzPaths) {
        return tgzPaths.stream()
                .map(path -> getNpmPackageFromClasspath(clazz, path))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    private static NpmPackage getNpmPackageFromAbsolutePaths(Path tgzPath) {
        try (final InputStream npmStream = Files.newInputStream(tgzPath)) {
            return NpmPackage.fromPackage(npmStream);
        } catch (IOException exception) {
            throw new InvalidRequestException(FAILED_TO_LOAD_RESOURCE_TEMPLATE.formatted(tgzPath), exception);
        }
    }

    @Nonnull
    private static NpmPackage getNpmPackageFromClasspath(Class<?> clazz, Path tgzClasspathPath) {
        try (final InputStream simpleAlphaStream = clazz.getResourceAsStream(tgzClasspathPath.toString())) {
            if (simpleAlphaStream == null) {
                throw new InvalidRequestException(FAILED_TO_LOAD_RESOURCE_TEMPLATE.formatted(tgzClasspathPath));
            }

            return NpmPackage.fromPackage(simpleAlphaStream);
        } catch (IOException exception) {
            throw new InvalidRequestException(FAILED_TO_LOAD_RESOURCE_TEMPLATE.formatted(tgzClasspathPath), exception);
        }
    }

    protected BaseNpmPackageLoaderInMemory(
            Set<NpmPackage> npmPackages, @Nullable NpmNamespaceManager npmNamespaceManager) {

        if (npmNamespaceManager == null) {
            var namespaceInfos = npmPackages.stream()
                    .map(npmPackage -> new NamespaceInfo(npmPackage.name(), npmPackage.canonical()))
                    .toList();

            this.npmNamespaceManager = new NpmNamespaceManagerFromList(namespaceInfos);
        } else {
            this.npmNamespaceManager = npmNamespaceManager;
        }

        this.npmPackages = npmPackages;
    }

    private FhirContext getFhirContext(NpmPackage npmPackage) {
        return FhirContext.forCached(FhirVersionEnum.forVersionString(npmPackage.fhirVersion()));
    }

    /**
     * Meant to test various scenarios involving missing of faulty NamespaceInfo data.
     */
    public static class NpmNamespaceManagerFromList implements NpmNamespaceManager {

        private final List<NamespaceInfo> namespaceInfos;

        public NpmNamespaceManagerFromList(List<NamespaceInfo> namespaceInfos) {
            this.namespaceInfos = List.copyOf(namespaceInfos);
        }

        @Override
        public List<NamespaceInfo> getAllNamespaceInfos() {
            return namespaceInfos;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NpmNamespaceManagerFromList that = (NpmNamespaceManagerFromList) o;
            return Objects.equals(namespaceInfos, that.namespaceInfos);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(namespaceInfos);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", NpmNamespaceManagerFromList.class.getSimpleName() + "[", "]")
                    .add("namespaceInfos=" + namespaceInfos)
                    .toString();
        }
    }
}
