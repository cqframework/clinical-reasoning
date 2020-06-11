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
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;

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
     * @param theVersionString
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

    public static void main(String[] args) {
        Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
        String cql = "library RuleFilters version '1.0.0'\r\n\r\nusing FHIR version '4.0.0'\r\n\r\ninclude FHIRHelpers version '4.0.0'\r\n\r\ncodesystem \"UsageContext\": 'http://terminology.hl7.org/CodeSystem/usage-context-type'\r\nvalueset \"Indeterminate or Equivocal Lab Result Value\": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1035'\r\nvalueset \"Negative or Undetected Lab Result Value\": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1034'\r\n\r\ncode \"focus\": 'focus' from UsageContext\r\n//code \"Chlamydia\": 'Chlamydia'\r\n\r\nparameter \"Triggering Encounter\" Encounter\r\n\r\ncontext Patient\r\n\r\ndefine \"Chlamydia ValueSets\": [Patient]\r\n  //[ValueSet] ValueSet\r\n    /* where exists (\r\n      ValueSet.useContext UseContext\r\n        where UseContext.code ~ \"focus\"\r\n          and not  IsNull(\r\n            UseContext.value Value\r\n              where Value.text = 'Chlamydia'\r\n          )\r\n    ) */\r\n\r\n/* define function ExpandValueSetCodes(value List<ValueSet>):\r\n  value Value\r\n    return Value.expansion.contains.code\r\n\r\ndefine function ExpandValueSetSystems(value List<ValueSet>):\r\n  value Value\r\n    return Value.expansion.contains.system\r\n\r\ndefine \"Flattened ValueSet Expansion Codes\":\r\n  flatten( ExpandValueSetCodes(\"Chlamydia ValueSets\") )\r\n\r\ndefine \"Flattened ValueSet Expansion Systems\":\r\n  flatten( ExpandValueSetSystems(\"Chlamydia ValueSets\") )\r\n\r\ndefine function ObservationWithSystemAndCodeInChlamydiaValueSets(observation Observation):\r\n    not IsNull(\r\n      observation.code  OConcept\r\n        where exists (\r\n          OConcept.coding OCoding\r\n            where ( OCoding.code in \"Flattened ValueSet Expansion Codes\" )\r\n               and OCoding.system in \"Flattened ValueSet Expansion Systems\"\r\n        )\r\n    )\r\n\r\ndefine function ObservationWithCodeInChlamydiaValueSets(observation Observation):\r\n  not IsNull(\r\n    observation.code  OConcept\r\n      where exists (\r\n        OConcept.coding OCoding where OCoding.code in flatten( ExpandValueSetCodes(\"Chlamydia ValueSets\") )\r\n      )\r\n  )\r\n\r\ndefine function ChlamydiaObservations(observation Observation):\r\n  if exists (observation.code.coding.system)\r\n  then ObservationWithSystemAndCodeInChlamydiaValueSets(observation)\r\n  else ObservationWithCodeInChlamydiaValueSets(observation)\r\n\r\ndefine \"Chlamydia Test Results\":\r\n  [Observation] O\r\n    where ChlamydiaObservations(O)\r\n      and O.status in { 'preliminary', 'final', 'amended', 'corrected' }\r\n\r\ndefine \"Encounter Location References\":\r\n  \"Triggering Encounter\" TriggeringEncounter\r\n    return TriggeringEncounter.location.location\r\n\r\ndefine \"Locations Matching Encounter Location References\":\r\n  [Location] Location\r\n    where exists (\r\n        \"Encounter Location References\" LocationReference\r\n          where Location.id ~ LocationReference.reference\r\n      )\r\n\r\ndefine \"Encounter Location Addresses And Patient Addresses\":\r\n  \"Locations Matching Encounter Location References\".address\r\n    union Patient.address\r\n\r\ndefine \"Address Elements Relevant to Jurisdiction Qualification\":\r\n  \"Encounter Location Addresses And Patient Addresses\" Address\r\n    return Tuple { state: Address.state,  postalCode: Address.postalCode }\r\n\r\ndefine \"Jurisdiction Codes\":\r\n  { 'UT', 'GA', 'CA', '84054', '84118', '30302' }\r\n\r\ndefine \"Address is in Jurisdiction Codes\":\r\n  exists (\r\n    \"Address Elements Relevant to Jurisdiction Qualification\" AddressElement\r\n      where AddressElement.state in \"Jurisdiction Codes\"\r\n        or AddressElement.postalCode in \"Jurisdiction Codes\"\r\n  )\r\n\r\ndefine \"Indeterminate Chlamydia Test Results\":\r\n  \"Chlamydia Test Results\" O\r\n    let organization: [Organization]\r\n    where (\r\n      (\r\n        exists (\r\n            O.interpretation interpretationConcept\r\n              where interpretationConcept as CodeableConcept in \"Indeterminate or Equivocal Lab Result Value\"\r\n        )\r\n          or O.value as CodeableConcept in \"Indeterminate or Equivocal Lab Result Value\"\r\n      )\r\n        and \"Address is in Jurisdiction Codes\"\r\n    )\r\n\r\ndefine \"Negative Chlamydia Test Results\":\r\n  \"Chlamydia Test Results\" O\r\n    let organization: [Organization]\r\n    where (\r\n      (\r\n        exists (\r\n            O.interpretation interpretationConcept\r\n              where interpretationConcept as CodeableConcept in \"Negative or Undetected Lab Result Value\"\r\n        )\r\n          or O.value as CodeableConcept in \"Negative or Undetected Lab Result Value\"\r\n      )\r\n        and \"Address is in Jurisdiction Codes\"\r\n    ) */\r\n\r\ndefine \"IsReportable\": exists (\"Chlamydia ValueSets\")\r\n  /* exists \"Indeterminate Chlamydia Test Results\"\r\n    or exists \"Negative Chlamydia Test Results\" */\r\n";
        setModelVersionFromLibraryString(models, cql);
        System.out.println("hurray");
    }
}