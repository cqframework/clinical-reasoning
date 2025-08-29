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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IMeasureAdapter;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simplistic implementation of {@link NpmPackageLoader} that loads NpmPackages from the classpath
 * and stores {@link NpmResourceHolder}s in a Map. This class is recommended for testing
 * and NOT for production.
 * <p/
 * Optionally uses a custom {@link NpmNamespaceManager} but can also resolve all NamespaceInfos
 * by extracting them from all loaded packages at construction time.
 */
public class NpmPackageLoaderInMemory implements NpmPackageLoader {

    private static final Logger logger = LoggerFactory.getLogger(NpmPackageLoaderInMemory.class);

    private static final Pattern PATTERN_PIPE = Pattern.compile("\\|");
    public static final String FAILED_TO_LOAD_RESOURCE_TEMPLATE = "Failed to load resource: %s";

    private final Map<UrlAndVersion, NpmResourceHolder> measureUrlToResourceInfo = new HashMap<>();
    private final Map<UrlAndVersion, NpmPackage> libraryUrlToPackage = new HashMap<>();
    private final NpmNamespaceManager npmNamespaceManager;

    public static NpmPackageLoaderInMemory fromNpmPackageAbsolutePath(List<Path> tgzPaths) {
        return fromNpmPackageAbsolutePath(null, tgzPaths);
    }

    public static NpmPackageLoaderInMemory fromNpmPackageAbsolutePath(
            NpmNamespaceManager npmNamespaceManager, List<Path> tgzPaths) {
        final List<NpmPackage> npmPackages = buildNpmPackagesFromAbsolutePath(tgzPaths);

        return new NpmPackageLoaderInMemory(npmPackages, npmNamespaceManager);
    }

    public static NpmPackageLoaderInMemory fromNpmPackageClasspath(Class<?> clazz, Path... tgzPaths) {
        return fromNpmPackageClasspath(null, clazz, tgzPaths);
    }

    public static NpmPackageLoaderInMemory fromNpmPackageClasspath(
            @Nullable NpmNamespaceManager npmNamespaceManager, Class<?> clazz, Path... tgzPaths) {
        return fromNpmPackageClasspath(npmNamespaceManager, clazz, Arrays.asList(tgzPaths));
    }

    public static NpmPackageLoaderInMemory fromNpmPackageClasspath(Class<?> clazz, List<Path> tgzPaths) {
        return fromNpmPackageClasspath(null, clazz, tgzPaths);
    }

    public static NpmPackageLoaderInMemory fromNpmPackageClasspath(
            @Nullable NpmNamespaceManager npmNamespaceManager, Class<?> clazz, List<Path> tgzPaths) {
        final List<NpmPackage> npmPackages = buildNpmPackageFromClasspath(clazz, tgzPaths);

        return new NpmPackageLoaderInMemory(npmPackages, npmNamespaceManager);
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
    public NpmResourceHolder loadNpmResources(IPrimitiveType<String> measureUrl) {
        return measureUrlToResourceInfo.entrySet().stream()
                .filter(entry -> doUrlAndVersionMatch(measureUrl, entry))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(NpmResourceHolder.EMPTY);
    }

    @Override
    public Optional<ILibraryAdapter> loadLibraryByUrl(String url) {
        for (NpmPackage npmPackage : libraryUrlToPackage.values()) {
            final FhirContext fhirContext = getFhirContext(npmPackage);
            try (InputStream libraryInputStream = npmPackage.loadByCanonical(url)) {
                if (libraryInputStream != null) {
                    final IResourceAdapter resourceAdapter = IAdapterFactory.createAdapterForResource(
                            fhirContext.newJsonParser().parseResource(libraryInputStream));
                    if (resourceAdapter instanceof ILibraryAdapter libraryAdapter) {
                        return Optional.of(libraryAdapter);
                    }
                }
            } catch (IOException exception) {
                throw new InternalErrorException(exception);
            }
        }
        return Optional.empty();
    }

    @Override
    public NpmNamespaceManager getNamespaceManager() {
        return npmNamespaceManager;
    }

    @Nonnull
    private static List<NpmPackage> buildNpmPackagesFromAbsolutePath(List<Path> tgzPaths) {
        return tgzPaths.stream()
                .map(NpmPackageLoaderInMemory::getNpmPackageFromAbsolutePaths)
                .toList();
    }

    @Nonnull
    private static List<NpmPackage> buildNpmPackageFromClasspath(Class<?> clazz, List<Path> tgzPaths) {
        return tgzPaths.stream()
                .map(path -> getNpmPackageFromClasspath(clazz, path))
                .toList();
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

    private NpmPackageLoaderInMemory(List<NpmPackage> npmPackages, @Nullable NpmNamespaceManager npmNamespaceManager) {

        if (npmNamespaceManager == null) {
            var namespaceInfos = npmPackages.stream()
                    .map(npmPackage -> new NamespaceInfo(npmPackage.name(), npmPackage.canonical()))
                    .toList();

            this.npmNamespaceManager = new NpmNamespaceManagerFromList(namespaceInfos);
        } else {
            this.npmNamespaceManager = npmNamespaceManager;
        }

        npmPackages.forEach(this::setup);
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

        final List<IResourceAdapter> resources = findResources(packageFolder, fhirContext);

        final Optional<IMeasureAdapter> optMeasure = findMeasure(resources);
        final List<ILibraryAdapter> libraries = findLibraries(resources);

        storeResources(npmPackage, optMeasure.orElse(null), libraries);
    }

    private List<IResourceAdapter> findResources(NpmPackage.NpmPackageFolder packageFolder, FhirContext fhirContext)
            throws IOException {

        final Map<String, List<String>> types = packageFolder.getTypes();
        final List<IResourceAdapter> resources = new ArrayList<>();

        for (Map.Entry<String, List<String>> typeToFiles : types.entrySet()) {
            for (String nextFile : typeToFiles.getValue()) {
                final String fileContents = new String(packageFolder.fetchFile(nextFile), StandardCharsets.UTF_8);

                if (nextFile.toLowerCase().endsWith(".json")) {
                    final IResourceAdapter resourceAdapter = IAdapterFactory.createAdapterForResource(
                            fhirContext.newJsonParser().parseResource(fileContents));

                    resources.add(resourceAdapter);
                }
            }
        }

        return resources;
    }

    private Optional<IMeasureAdapter> findMeasure(List<IResourceAdapter> resources) {
        return resources.stream()
                .filter(IMeasureAdapter.class::isInstance)
                .map(IMeasureAdapter.class::cast)
                .findFirst();
    }

    private List<ILibraryAdapter> findLibraries(List<IResourceAdapter> resources) {
        return resources.stream()
                .filter(ILibraryAdapter.class::isInstance)
                .map(ILibraryAdapter.class::cast)
                .toList();
    }

    private void storeResources(
            NpmPackage npmPackage, @Nullable IMeasureAdapter measure, List<ILibraryAdapter> libraries) {
        if (measure != null) {
            measureUrlToResourceInfo.put(
                    UrlAndVersion.fromCanonicalAndVersion(measure.getUrl(), measure.getVersion()),
                    new NpmResourceHolder(measure, findMatchingLibrary(measure, libraries), List.of(npmPackage)));
        }

        for (ILibraryAdapter library : libraries) {
            libraryUrlToPackage.put(
                    UrlAndVersion.fromCanonicalAndVersion(library.getUrl(), library.getVersion()), npmPackage);
        }
    }

    private ILibraryAdapter findMatchingLibrary(IMeasureAdapter measure, List<ILibraryAdapter> libraries) {
        return libraries.stream()
                .filter(library -> measure.getLibrary().stream()
                        .anyMatch(measureLibraryUrl -> doMeasureUrlAndLibraryMatch(measureLibraryUrl, library)))
                .findFirst()
                .orElse(null);
    }

    private static boolean doUrlAndVersionMatch(
            IPrimitiveType<String> measureUrl, Map.Entry<UrlAndVersion, NpmResourceHolder> entry) {

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
