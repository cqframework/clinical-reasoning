package org.opencds.cqf.fhir.utility.npm;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.utilities.npm.NpmPackage;

/**
 * Simplistic implementation of {@link R4NpmPackageLoader} that loads NpmPackages from the classpath
 * and stores {@link R4NpmResourceInfoForCql}s in a Map. This class is recommended for testing
 * and NOT for production.
 */
public class R4NpmPackageLoaderInMemory implements R4NpmPackageLoader {

    private final Map<String, R4NpmResourceInfoForCql> urlToResourceInfo = new HashMap<>();

    public static R4NpmPackageLoaderInMemory fromNpmPackageTgzPath(Class<?> clazz, Path... tgzPaths) {
        final List<NpmPackage> npmPackages = buildNpmPackage(clazz, tgzPaths);

        return new R4NpmPackageLoaderInMemory(npmPackages);
    }

    public static R4NpmPackageLoaderInMemory fromNpmPackages(NpmPackage... npmPackage) {
        return new R4NpmPackageLoaderInMemory(Arrays.asList(npmPackage));
    }

    @Override
    public R4NpmResourceInfoForCql loadNpmResources(CanonicalType measureUrl) {
        return urlToResourceInfo.computeIfAbsent(measureUrl.asStringValue(), input -> R4NpmResourceInfoForCql.EMPTY);
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

    private R4NpmPackageLoaderInMemory(List<NpmPackage> npmPackages) {
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
        final FhirVersionEnum fhirVersion = FhirVersionEnum.forVersionString(npmPackage.fhirVersion());
        final FhirContext fhirContext = FhirContext.forCached(fhirVersion);

        final Optional<NpmPackage.NpmPackageFolder> optPackageFolder = npmPackage.getFolders().entrySet().stream()
                .filter(entry -> "package".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();

        if (optPackageFolder.isPresent()) {
            setupNpmPackageInfo(npmPackage, optPackageFolder.get(), fhirContext);
        }
    }

    private void setupNpmPackageInfo(NpmPackage npmPackage,
            NpmPackage.NpmPackageFolder packageFolder, FhirContext fhirContext) throws IOException {
        final Map<String, List<String>> types = packageFolder.getTypes();

        Measure measure = null;
        Library library = null;

        for (Map.Entry<String, List<String>> typeToFiles : types.entrySet()) {
            for (String nextFile : typeToFiles.getValue()) {
                final byte[] fileBytes = packageFolder.fetchFile(nextFile);
                final String fileContents = new String(fileBytes, StandardCharsets.UTF_8);

                if (nextFile.toLowerCase().endsWith(".json")) {
                    final IBaseResource resource =
                            fhirContext.newJsonParser().parseResource(fileContents);

                    if (resource instanceof Library libraryToUse) {
                        library = libraryToUse;
                    }

                    if (resource instanceof Measure measureToUse) {
                        measure = measureToUse;
                    }
                }
            }
        }

        if (measure != null) {
            urlToResourceInfo.put(
                    measure.getUrl(), new R4NpmResourceInfoForCql(measure, library, List.of(
                    npmPackage)));
        }
    }
}
