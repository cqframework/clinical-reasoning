package org.opencds.cqf.fhir.utility.npm;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// LUKETODO:  redo java
/**
 * Simplistic implementation of {@link NpmPackageLoader} that loads NpmPackages from the classpath
 * and stores {@link NpmResourceHolder}s in a Map. This class is recommended for testing
 * and NOT for production.
 * <p/
 * Optionally uses a custom {@link NpmNamespaceManager} but can also resolve all NamespaceInfos
 * by extracting them from all loaded packages at construction time.
 */
public class R4NpmPackageLoaderInMemory implements NpmPackageLoader {

    private static final Logger logger = LoggerFactory.getLogger(R4NpmPackageLoaderInMemory.class);

    private static final Pattern PATTERN_PIPE = Pattern.compile("\\|");
    public static final String FAILED_TO_LOAD_RESOURCE_TEMPLATE = "Failed to load resource: %s";

    private final Set<NpmPackage> npmPackages;
    //    private final Map<UrlAndVersion, IBaseResource> resourceUrlToResource = new HashMap<>();
    //    private final Map<UrlAndVersion, NpmPackage> libraryUrlToPackage = new HashMap<>();
    private final NpmNamespaceManager npmNamespaceManager;

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

    record UrlAndVersion(String url, @Nullable String version) {

        static UrlAndVersion fromCanonical(String canonical) {
            final String[] parts = PATTERN_PIPE.split(canonical);
            if (parts.length > 2) {
                throw new IllegalArgumentException("Invalid canonical URL: " + canonical);
            }
            if (parts.length == 1) {
                return new UrlAndVersion(parts[0], null);
            }
            return new UrlAndVersion(parts[0], parts[1]);
        }

        static UrlAndVersion fromCanonicalAndVersion(String canonical, @Nullable String version) {
            if (version == null) {
                return new UrlAndVersion(canonical, null);
            }

            return new UrlAndVersion(canonical, version);
        }

        @Override
        @Nonnull
        public String toString() {
            return url + "|" + version;
        }
    }

    @Override
    public Optional<? extends IBaseResource> loadNpmResource(IPrimitiveType<String> resourceUrl) {
        return npmPackages.stream()
                .filter(npmPackage -> doesPackageMatch(resourceUrl, npmPackage))
                .map(npmPackage -> getResource(npmPackage, resourceUrl))
                .findFirst();
    }

    private IBaseResource getResource(NpmPackage npmPackage, IPrimitiveType<String> resourceUrl) {
        try {
            return tryGetResource(npmPackage, resourceUrl);
        } catch (IOException exception) {
            throw new RuntimeException(FAILED_TO_LOAD_RESOURCE_TEMPLATE.formatted(resourceUrl.getValue()), exception);
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
            throw new RuntimeException(FAILED_TO_LOAD_RESOURCE_TEMPLATE, exception);
        }
    }

    @Override
    public FhirContext getFhirContext() {
        return FhirContext.forR4Cached();
    }

    //    @Override
    //    public Optional<ILibraryAdapter> loadLibraryByUrl(String url) {
    //        for (NpmPackage npmPackage : libraryUrlToPackage.values()) {
    //            final FhirContext fhirContext = getFhirContext(npmPackage);
    //            try (InputStream libraryInputStream = npmPackage.loadByCanonical(url)) {
    //                if (libraryInputStream != null) {
    //                    final IResourceAdapter resourceAdapter = IAdapterFactory.createAdapterForResource(
    //                            fhirContext.newJsonParser().parseResource(libraryInputStream));
    //                    if (resourceAdapter instanceof ILibraryAdapter libraryAdapter) {
    //                        return Optional.of(libraryAdapter);
    //                    }
    //                }
    //            } catch (IOException exception) {
    //                throw new InternalErrorException(exception);
    //            }
    //        }
    //        return Optional.empty();
    //    }

    @Override
    public NpmNamespaceManager getNamespaceManager() {
        return npmNamespaceManager;
    }

    @Nonnull
    private static Set<NpmPackage> buildNpmPackagesFromAbsolutePath(List<Path> tgzPaths) {
        return tgzPaths.stream()
                .map(R4NpmPackageLoaderInMemory::getNpmPackageFromAbsolutePaths)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Nonnull
    private static Set<NpmPackage> buildNpmPackageFromClasspath(Class<?> clazz, List<Path> tgzPaths) {
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

    private R4NpmPackageLoaderInMemory(Set<NpmPackage> npmPackages, @Nullable NpmNamespaceManager npmNamespaceManager) {

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

    private void setup(NpmPackage npmPackage) {
        try {
            trySetup(npmPackage);
        } catch (Exception e) {
            throw new InternalErrorException("Failed to setup NpmPackage:  " + npmPackage.name(), e);
        }
    }

    private void trySetup(NpmPackage npmPackage) throws IOException {
        final FhirContext fhirContext = getFhirContext(npmPackage);

        final Optional<NpmPackage.NpmPackageFolder> optPackageFolder = npmPackage.getFolders().entrySet().stream()
                .filter(entry -> "package".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();

        if (optPackageFolder.isPresent()) {
            setupNpmPackageInfo(npmPackage, optPackageFolder.get(), fhirContext);
        }
    }

    private void setupNpmPackageInfo(
            NpmPackage npmPackage, NpmPackage.NpmPackageFolder packageFolder, FhirContext fhirContext)
            throws IOException {

        // LUKETODO:  store all resources

        final List<IBaseResource> resources = findResources(packageFolder, fhirContext);

        //        final Optional<Measure> optMeasure = findMeasure(resources);
        //        final List<Library> libraries = findLibraries(resources);
        //
        //        storeResources(npmPackage, optMeasure.orElse(null), libraries);
    }

    private List<IBaseResource> findResources(NpmPackage.NpmPackageFolder packageFolder, FhirContext fhirContext)
            throws IOException {

        final Map<String, List<String>> types = packageFolder.getTypes();
        final List<IBaseResource> resources = new ArrayList<>();

        for (Map.Entry<String, List<String>> typeToFiles : types.entrySet()) {
            for (String nextFile : typeToFiles.getValue()) {
                final String fileContents = new String(packageFolder.fetchFile(nextFile), StandardCharsets.UTF_8);

                if (nextFile.toLowerCase().endsWith(".json")) {
                    final IBaseResource resource = fhirContext.newJsonParser().parseResource(fileContents);

                    resources.add(resource);
                }
            }
        }

        return resources;
    }

    //    private Optional<Measure> findMeasure(List<IResourceAdapter> resources) {
    //        return resources.stream()
    //                .filter(Measure.class::isInstance)
    //                .map(IMeasureAdapter.class::cast)
    //                .findFirst();
    //    }

    private List<ILibraryAdapter> findLibraries(List<IResourceAdapter> resources) {
        return resources.stream()
                .filter(ILibraryAdapter.class::isInstance)
                .map(ILibraryAdapter.class::cast)
                .toList();
    }

    //    private void storeResources(
    //            NpmPackage npmPackage, @Nullable IMeasureAdapter measure, List<ILibraryAdapter> libraries) {
    //        if (measure != null) {
    //            resourceUrlToResource.put(
    //                    UrlAndVersion.fromCanonicalAndVersion(measure.getUrl(), measure.getVersion()), measure.get());
    //        }
    //
    //        for (ILibraryAdapter library : libraries) {
    //            libraryUrlToPackage.put(
    //                    UrlAndVersion.fromCanonicalAndVersion(library.getUrl(), library.getVersion()), npmPackage);
    //        }
    //    }

    private static boolean doUrlAndVersionMatch(
            IPrimitiveType<String> measureUrl, Map.Entry<UrlAndVersion, IBaseResource> entry) {

        if (entry.getKey().equals(UrlAndVersion.fromCanonical(measureUrl.getValueAsString()))) {
            return true;
        }

        return entry.getKey().url.equals(measureUrl.getValueAsString());
    }

    private static boolean doMeasureUrlAndLibraryMatch(String measureLibraryUrl, ILibraryAdapter library) {
        final String[] split = PATTERN_PIPE.split(measureLibraryUrl);

        if (split.length == 1) {
            return library.getUrl().equals(measureLibraryUrl);
        }

        if (split.length == 2) {
            return library.getUrl().equals(split[0]) && library.getVersion().equals(split[1]);
        }

        throw new InternalErrorException("bad measureUrl: " + measureLibraryUrl);
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
