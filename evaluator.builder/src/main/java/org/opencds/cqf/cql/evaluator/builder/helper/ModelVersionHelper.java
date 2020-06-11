package org.opencds.cqf.cql.evaluator.builder.helper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseBundle;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class ModelVersionHelper {

    public static void setModelVersionFromLibraryPath(Map<String, Pair<String, String>> models, String libraryUri) {
        List<String> libraries = getLibrariesFromPath(libraryUri);
        libraries.forEach(libraryContent -> getModelVersionPairsFromContent(models, libraryContent));
    }

    public static void setModelVersionFromLibraryString(Map<String, Pair<String, String>> models,
            String libraryContent) {
        getModelVersionPairsFromContent(models, libraryContent);
    }

    public static void setModelVersionFromLibraryPaths(Map<String, Pair<String, String>> models,
            List<String> libraryUris) {
        libraryUris.forEach(libraryUri -> {
            List<String> libraries = getLibrariesFromPath(libraryUri);
            libraries.forEach(libraryContent -> getModelVersionPairsFromContent(models, libraryContent));
        });
    }

    public static void setModelVersionFromLibraryStrings(Map<String, Pair<String, String>> models,
            List<String> libraries) {
        libraries.forEach(libraryContent -> getModelVersionPairsFromContent(models, libraryContent));
    }

    public static void setModelVersionFromBundle(Map<String, Pair<String, String>> models, IBaseBundle bundle) {
        FhirVersionEnum versionEnum = bundle.getStructureFhirVersionEnum();
        models.put("http://hl7.org/fhir", Pair.of(versionEnum.getFhirVersionString(), null));
    }

    public static void setModelVersionFromServer(Map<String, Pair<String, String>> models, IGenericClient client) {
        FhirVersionEnum versionEnum = client.getFhirContext().getVersion().getVersion();
        models.put("http://hl7.org/fhir", Pair.of(versionEnum.getFhirVersionString(), null));
    }

    // Should I be translating these?
    private static void getModelVersionPairsFromContent(Map<String, Pair<String, String>> models,
            String libraryContent) {
        int usingStatementIndex = libraryContent.indexOf("using");
        int index = 0;

        if (usingStatementIndex >= 0) {
            String[] includedDefsAndBelow = libraryContent.substring(usingStatementIndex).split("\\n");
            
            for (String string : includedDefsAndBelow) {
                if (string.contains("\\n")) {
                    includedDefsAndBelow = string.split("\\\\n");
                }
            }

            while (includedDefsAndBelow[index].startsWith("using")) {
                String alias = includedDefsAndBelow[index].replace("using ", "").split(" version ")[0];
                String version = includedDefsAndBelow[index].replace("using ", "").split(" version ")[1]
                        .replaceAll("\'", "").split(" called")[0];
                // if (version.contains("\\\\n")) {
                //     version = version.split("\\\\n")[0];
                // }
                Pair<String, String> modelUrlPair = expandAliasToUri(Pair.of(alias, null));
                models.putIfAbsent(modelUrlPair.getLeft(), Pair.of(version, null));
                index++;
            }
        }

    }

    private static Pair<String, String> expandAliasToUri(Pair<String, String> modelUrl) {
        final Map<String, String> aliasMap = new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;

            {
                put("FHIR", "http://hl7.org/fhir");
                put("QUICK", "http://hl7.org/fhir");
                put("QDM", "urn:healthit-gov:qdm:v5_4");
            }
        };

        if (modelUrl == null) {
            return null;
        }

        if (aliasMap.containsKey(modelUrl.getLeft())) {
            return Pair.of(aliasMap.get(modelUrl.getLeft()), modelUrl.getRight());
        }

        return modelUrl;
    }

    public static List<String> getLibrariesFromPath(String path) {
        Path filePath = Paths.get(path);
        File file = new File(filePath.toAbsolutePath().toString());
        if (file.isDirectory()) {
            File[] files = file.listFiles((d, name) -> name.endsWith(".cql"));

            return Arrays.asList(files).stream().map(x -> x.toPath()).filter(Files::isRegularFile).map(t -> {
                try {
                    return Files.readAllBytes(t);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }).filter(x -> x != null).map(x -> new String(x, StandardCharsets.UTF_8)).collect(Collectors.toList());
        } else {
            List<File> files = new ArrayList<File>();
            files.add(file);
            return files.stream().map(x -> x.toPath()).filter(Files::isRegularFile).map(t -> {
                try {
                    return Files.readAllBytes(t);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }).filter(x -> x != null).map(x -> new String(x, StandardCharsets.UTF_8)).collect(Collectors.toList());
        }
    }

    /**
     * Returns the {@link FhirVersionEnum} which corresponds to a specific version
     * of FHIR. Partial version strings (e.g. "3.0") are acceptable.
     * @param theVersionString The String representation of a FHIR Model Version
     * @return Returns null if no version exists matching the given string
     */
    public static FhirVersionEnum forVersionString(String theVersionString) {
        for (FhirVersionEnum next : FhirVersionEnum.values()) {
            if (next.getFhirVersionString().regionMatches(0, (theVersionString), 0, 4)) {
                return next;
            }
        }
        return null;
    }
}