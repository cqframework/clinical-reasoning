package org.opencds.cqf.fhir.cql.npm;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.utilities.npm.NpmPackage;

// LUKETODO:  outer util class?
class R4NpmPackageLoaderForTests implements R4NpmPackageLoader {

    private final Map<String, R4NpmResourceInfoForCql> urlToResourceInfo = new HashMap<>();

    public R4NpmPackageLoaderForTests(NpmPackage npmPackage) {
        setup(npmPackage);
    }

    private void setup(NpmPackage npmPackage) {
        try {
            trySetup(npmPackage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup NpmPackage:  " + npmPackage.name(), e);
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
            final NpmPackage.NpmPackageFolder packageFolder = optPackageFolder.get();
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
                        measure.getUrl(), new R4NpmResourceInfoForCql(measure, library, List.of(npmPackage)));
            }
        }
    }

    @Override
    public R4NpmResourceInfoForCql loadNpmResources(CanonicalType measureUrl) {
        return urlToResourceInfo.computeIfAbsent(measureUrl.asStringValue(), input -> R4NpmResourceInfoForCql.EMPTY);
    }
}
