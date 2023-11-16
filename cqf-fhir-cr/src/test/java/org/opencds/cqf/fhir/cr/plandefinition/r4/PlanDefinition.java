package org.opencds.cqf.fhir.cr.plandefinition.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.json.JSONException;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.utility.repository.IGFileStructureRepository;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.skyscreamer.jsonassert.JSONAssert;

public class PlanDefinition {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/plandefinition/r4";

    private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);
    private static final EvaluationSettings evaluationSettings = EvaluationSettings.getDefault();

    public PlanDefinition() {
        evaluationSettings
                .getRetrieveSettings()
                .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

        evaluationSettings
                .getTerminologySettings()
                .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
    }

    private static InputStream open(String asset) {
        return PlanDefinition.class.getResourceAsStream(asset);
    }

    public static String load(InputStream asset) throws IOException {
        return new String(asset.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static String load(String asset) throws IOException {
        return load(open(asset));
    }

    public static IBaseResource parse(String asset) {
        return jsonParser.parseResource(open(asset));
    }

    public static PlanDefinitionProcessor buildProcessor(Repository repository) {
        return new PlanDefinitionProcessor(repository, evaluationSettings);
    }

    /** Fluent interface starts here **/
    public static class Assert {
        public static Apply that(String planDefinitionID, String patientID, String encounterID, String practitionerID) {
            return new Apply(planDefinitionID, patientID, encounterID, practitionerID);
        }
    }

    public static class Apply {
        private String planDefinitionID;

        private String patientID;
        private String encounterID;
        private String practitionerID;

        private Repository repository;
        private Repository dataRepository;
        private Repository contentRepository;
        private Repository terminologyRepository;
        private Bundle additionalData;
        private IdType additionalDataId;
        private Parameters parameters;
        private IdType expectedBundleId;
        private IdType expectedCarePlanId;

        public Apply(String planDefinitionID, String patientID, String encounterID, String practitionerID) {
            this.planDefinitionID = planDefinitionID;
            this.patientID = patientID;
            this.encounterID = encounterID;
            this.practitionerID = practitionerID;
        }

        public Apply withData(String dataAssetName) {
            dataRepository = new InMemoryFhirRepository(fhirContext, (Bundle) parse(dataAssetName));

            return this;
        }

        public Apply withContent(String dataAssetName) {
            contentRepository = new InMemoryFhirRepository(fhirContext, (Bundle) parse(dataAssetName));

            return this;
        }

        public Apply withTerminology(String dataAssetName) {
            terminologyRepository = new InMemoryFhirRepository(fhirContext, (Bundle) parse(dataAssetName));

            return this;
        }

        private void loadAdditionalData(IBaseResource resource) {
            additionalData = resource.getIdElement().getResourceType().equals(FHIRAllTypes.BUNDLE.toCode())
                    ? (Bundle) resource
                    : new Bundle()
                            .setType(BundleType.COLLECTION)
                            .addEntry(new BundleEntryComponent().setResource((Resource) resource));
        }

        public Apply withAdditionalData(String dataAssetName) {
            var data = parse(dataAssetName);
            loadAdditionalData(data);

            return this;
        }

        public Apply withAdditionalDataId(IdType id) {
            additionalDataId = id;

            return this;
        }

        public Apply withParameters(Parameters params) {
            parameters = params;

            return this;
        }

        public Apply withRepository(Repository repository) {
            this.repository = repository;

            return this;
        }

        public Apply withExpectedBundleId(IdType id) {
            expectedBundleId = id;

            return this;
        }

        public Apply withExpectedCarePlanId(IdType id) {
            expectedCarePlanId = id;

            return this;
        }

        private void buildRepository() {
            if (repository != null) {
                return;
            }
            var local = new IGFileStructureRepository(
                    fhirContext,
                    this.getClass()
                                    .getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation()
                                    .getPath()
                            + CLASS_PATH,
                    IGLayoutMode.TYPE_PREFIX,
                    EncodingEnum.JSON);
            if (dataRepository == null && contentRepository == null && terminologyRepository == null) {
                repository = local;
                return;
            }

            if (dataRepository == null) {
                dataRepository = local;
            }
            if (contentRepository == null) {
                contentRepository = local;
            }
            if (terminologyRepository == null) {
                terminologyRepository = local;
            }

            repository = Repositories.proxy(dataRepository, contentRepository, terminologyRepository);
        }

        public GeneratedBundle applyR5() {
            buildRepository();
            var libraryEngine = new LibraryEngine(this.repository, evaluationSettings);
            Bundle expectedBundle = null;
            if (expectedBundleId != null) {
                try {
                    expectedBundle = repository.read(Bundle.class, expectedBundleId);
                } catch (Exception e) {
                }
            }
            if (additionalDataId != null) {
                var resource = repository.read(
                        fhirContext
                                .getResourceDefinition(additionalDataId.getResourceType())
                                .newInstance()
                                .getClass(),
                        additionalDataId);
                loadAdditionalData(resource);
            }
            return new GeneratedBundle(
                    (Bundle) buildProcessor(repository)
                            .applyR5(
                                    new IdType("PlanDefinition", planDefinitionID),
                                    null,
                                    null,
                                    patientID,
                                    encounterID,
                                    practitionerID,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    parameters,
                                    null,
                                    additionalData,
                                    null,
                                    libraryEngine),
                    expectedBundle);
        }

        public GeneratedCarePlan apply() {
            buildRepository();
            var libraryEngine = new LibraryEngine(this.repository, evaluationSettings);
            CarePlan expectedCarePlan = null;
            if (expectedCarePlanId != null) {
                try {
                    expectedCarePlan = repository.read(CarePlan.class, expectedCarePlanId);
                } catch (Exception e) {
                }
            }
            if (additionalDataId != null) {
                var resource = repository.read(
                        fhirContext
                                .getResourceDefinition(additionalDataId.getResourceType())
                                .newInstance()
                                .getClass(),
                        additionalDataId);
                loadAdditionalData(resource);
            }
            return new GeneratedCarePlan(
                    (CarePlan) buildProcessor(repository)
                            .apply(
                                    new IdType("PlanDefinition", planDefinitionID),
                                    null,
                                    null,
                                    patientID,
                                    encounterID,
                                    practitionerID,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    parameters,
                                    null,
                                    additionalData,
                                    null,
                                    libraryEngine),
                    expectedCarePlan);
        }

        public GeneratedPackage packagePlanDefinition() {
            buildRepository();
            return new GeneratedPackage(
                    (Bundle) buildProcessor(repository)
                            .packagePlanDefinition(new IdType("PlanDefinition", planDefinitionID), null, null, true),
                    null);
        }
    }

    static class GeneratedBundle {
        Bundle generatedBundle;
        Bundle expectedBundle;

        public GeneratedBundle(Bundle generatedBundle, Bundle expectedBundle) {
            this.generatedBundle = generatedBundle;
            this.expectedBundle = expectedBundle;
        }

        public void isEqualsTo(String expectedBundleAssetName) {
            try {
                JSONAssert.assertEquals(
                        load(expectedBundleAssetName), jsonParser.encodeResourceToString(generatedBundle), true);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public void isEqualsToExpected() {
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(expectedBundle),
                        jsonParser.encodeResourceToString(generatedBundle),
                        true);
            } catch (JSONException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public void hasEntry(int count) {
            assertEquals(count, generatedBundle.getEntry().size());
        }

        public void hasCommunicationRequestPayload() {
            assertTrue(generatedBundle.getEntry().stream()
                    .filter(e -> e.getResource().getResourceType().equals(ResourceType.CommunicationRequest))
                    .map(e -> (CommunicationRequest) e.getResource())
                    .allMatch(c -> c.hasPayload()));
        }

        public void hasQuestionnaireOperationOutcome() {
            assertTrue(generatedBundle.getEntry().stream()
                    .map(e -> e.getResource())
                    .anyMatch(r -> r.getResourceType().equals(ResourceType.Questionnaire)
                            && ((Questionnaire) r).getContained().stream().anyMatch(c -> c.getResourceType()
                                    .equals(ResourceType.OperationOutcome))));
        }
    }

    static class GeneratedCarePlan {
        CarePlan generatedCarePlan;
        CarePlan expectedCarePlan;

        public GeneratedCarePlan(CarePlan generatedCarePlan, CarePlan expectedCarePlan) {
            this.generatedCarePlan = generatedCarePlan;
            this.expectedCarePlan = expectedCarePlan;
        }

        public void isEqualsTo(String expectedCarePlanAssetName) {
            try {
                JSONAssert.assertEquals(
                        load(expectedCarePlanAssetName), jsonParser.encodeResourceToString(generatedCarePlan), true);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public void isEqualsToExpected() {
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(expectedCarePlan),
                        jsonParser.encodeResourceToString(generatedCarePlan),
                        true);
            } catch (JSONException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public void hasContained(int count) {
            assertEquals(count, generatedCarePlan.getContained().size());
        }

        public void hasOperationOutcome() {
            assertTrue(generatedCarePlan.getContained().stream()
                    .anyMatch(r -> r.getResourceType().equals(ResourceType.OperationOutcome)));
        }
    }

    static class GeneratedPackage {
        Bundle generatedBundle;
        Bundle expectedBundle;

        public GeneratedPackage(Bundle generatedBundle, Bundle expectedBundle) {
            this.generatedBundle = generatedBundle;
            this.expectedBundle = expectedBundle;
        }

        public void hasEntry(int count) {
            assertEquals(count, generatedBundle.getEntry().size());
        }
    }
}
