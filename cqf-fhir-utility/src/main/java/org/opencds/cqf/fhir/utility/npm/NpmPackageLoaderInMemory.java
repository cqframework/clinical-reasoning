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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IMeasureAdapter;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;

// LUKETODO:  feature request to improve JpaPackageCache :  error or warning if we get more than one
// LUKETODO:  document duplicate Measure across NPM problem
// LUKETODO:  not a problem with packages we build... but clients could do the wrong thing
// LUKETODO:  how does support find out?  query the database?
/**
 * Simplistic implementation of {@link NpmPackageLoader} that loads NpmPackages from the classpath
 * and stores {@link NpmResourceInfoForCql}s in a Map. This class is recommended for testing
 * and NOT for production.
 * <p/
 * Does not use an {@link NpmNamespaceManager} and instead resolves all NamespaceInfos by extracting
 * them from all loaded packages at construction time.
 */
public class NpmPackageLoaderInMemory implements NpmPackageLoader {

    private final Map<UrlAndVersion, NpmResourceInfoForCql> measureUrlToResourceInfo = new HashMap<>();
    private final Map<UrlAndVersion, NpmPackage> libraryUrlToPackage = new HashMap<>();
    private final List<NamespaceInfo> namespaceInfos;

    public static NpmPackageLoaderInMemory fromNpmPackageTgzPath(Class<?> clazz, Path... tgzPaths) {
        final List<NpmPackage> npmPackages = buildNpmPackage(clazz, tgzPaths);

        return new NpmPackageLoaderInMemory(npmPackages);
    }

    record UrlAndVersion(String url, @Nullable String version) {

        static UrlAndVersion fromCanonical(String canonical) {
            final String[] parts = canonical.split("\\|");
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
        public String toString() {
            return url + "|" + version;
        }
    }

    @Override
    public NpmResourceInfoForCql loadNpmResources(IPrimitiveType<String> measureUrl) {
        return measureUrlToResourceInfo.entrySet().stream()
                .filter(entry -> doesUrlAndVersionMatch(measureUrl, entry))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(NpmResourceInfoForCql.EMPTY);
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
    public List<NamespaceInfo> getAllNamespaceInfos() {
        return namespaceInfos;
    }

    @Nonnull
    private static List<NpmPackage> buildNpmPackage(Class<?> clazz, Path... tgzPaths) {
        return Arrays.stream(tgzPaths).map(path -> getNpmPackage(clazz, path)).toList();
    }

    @Nonnull
    private static NpmPackage getNpmPackage(Class<?> clazz, Path tgzPath) {
        try (final InputStream simpleAlphaStream = clazz.getResourceAsStream(tgzPath.toString())) {
            if (simpleAlphaStream == null) {
                throw new InvalidRequestException("Failed to load resource: %s".formatted(tgzPath));
            }

            return NpmPackage.fromPackage(simpleAlphaStream);
        } catch (IOException e) {
            throw new InvalidRequestException("Failed to load resource: %s".formatted(tgzPath), e);
        }
    }

    private NpmPackageLoaderInMemory(List<NpmPackage> npmPackages) {

        namespaceInfos = npmPackages.stream()
                .map(npmPackage -> new NamespaceInfo(npmPackage.name(), npmPackage.canonical()))
                .toList();

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

        final Map<String, List<String>> types = packageFolder.getTypes();

        IMeasureAdapter measure = null;
        ILibraryAdapter library = null;

        for (Map.Entry<String, List<String>> typeToFiles : types.entrySet()) {
            for (String nextFile : typeToFiles.getValue()) {
                final String fileContents = new String(packageFolder.fetchFile(nextFile), StandardCharsets.UTF_8);

                if (nextFile.toLowerCase().endsWith(".json")) {
                    final IResourceAdapter resourceAdapter = IAdapterFactory.createAdapterForResource(
                            fhirContext.newJsonParser().parseResource(fileContents));

                    if (resourceAdapter instanceof ILibraryAdapter libraryAdapter) {
                        library = libraryAdapter;
                    }

                    if (resourceAdapter instanceof IMeasureAdapter measureAdapter) {
                        measure = measureAdapter;
                    }
                }
            }
        }

        storeResources(npmPackage, measure, library);
    }

    private void storeResources(NpmPackage npmPackage, IMeasureAdapter measure, ILibraryAdapter library) {
        if (measure != null) {
            measureUrlToResourceInfo.put(
                    UrlAndVersion.fromCanonicalAndVersion(measure.getUrl(), measure.getVersion()),
                    new NpmResourceInfoForCql(measure, library, List.of(npmPackage)));
        }

        if (library != null) {
            libraryUrlToPackage.put(
                    UrlAndVersion.fromCanonicalAndVersion(library.getUrl(), library.getVersion()), npmPackage);
        }
    }

    private static boolean doesUrlAndVersionMatch(
            IPrimitiveType<String> measureUrl, Map.Entry<UrlAndVersion, NpmResourceInfoForCql> entry) {

        if (entry.getKey().equals(UrlAndVersion.fromCanonical(measureUrl.getValueAsString()))) {
            return true;
        }

        return entry.getKey().url.equals(measureUrl.getValueAsString());
    }

    private FhirContext getFhirContext(NpmPackage npmPackage) {
        return FhirContext.forCached(FhirVersionEnum.forVersionString(npmPackage.fhirVersion()));
    }
}
