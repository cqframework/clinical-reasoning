package org.opencds.cqf.fhir.cr.plandefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
import static org.opencds.cqf.fhir.utility.BundleHelper.addEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResources;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;
import static org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResource;
import static org.opencds.cqf.fhir.utility.SearchHelper.readRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.json.JSONException;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.TestOperationProvider;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import org.skyscreamer.jsonassert.JSONAssert;

public class PlanDefinition {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/plandefinition";

    private static InputStream open(String asset) {
        return PlanDefinition.class.getResourceAsStream(asset);
    }

    public static String load(InputStream asset) throws IOException {
        return new String(asset.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static String load(String asset) throws IOException {
        return load(open(asset));
    }

    public static Given given() {
        return new Given();
    }

    public static class Given {
        private Repository repository;
        private EvaluationSettings evaluationSettings;

        public Given repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = new IgRepository(
                    fhirContext, Paths.get(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            return this;
        }

        public Given evaluationSettings(EvaluationSettings evaluationSettings) {
            this.evaluationSettings = evaluationSettings;
            return this;
        }

        public PlanDefinitionProcessor buildProcessor(Repository repository) {
            if (repository instanceof IgRepository) {
                ((IgRepository) repository)
                        .setOperationProvider(TestOperationProvider.newProvider(repository.fhirContext()));
            }
            if (evaluationSettings == null) {
                evaluationSettings = EvaluationSettings.getDefault();
                evaluationSettings
                        .getRetrieveSettings()
                        .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                        .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

                evaluationSettings
                        .getTerminologySettings()
                        .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
            }
            return new PlanDefinitionProcessor(repository, evaluationSettings);
        }

        public When when() {
            return new When(repository, buildProcessor(repository));
        }
    }

    public static class When {
        private final Repository repository;
        private final PlanDefinitionProcessor processor;
        private final IParser jsonParser;

        private String planDefinitionId;

        private String subjectId;
        private String encounterId;
        private String practitionerId;
        private String organizationId;
        private Boolean useServerData;
        private Repository dataRepository;
        private Repository contentRepository;
        private Repository terminologyRepository;
        private IBaseBundle additionalData;
        private IIdType additionalDataId;
        private IBaseParameters parameters;
        private Boolean isPackagePut;

        public When(Repository repository, PlanDefinitionProcessor processor) {
            this.repository = repository;
            this.processor = processor;
            jsonParser = repository.fhirContext().newJsonParser();
        }

        public When planDefinitionId(String id) {
            planDefinitionId = id;
            return this;
        }

        public When subjectId(String id) {
            subjectId = id;
            return this;
        }

        public When encounterId(String id) {
            encounterId = id;
            return this;
        }

        public When practitionerId(String id) {
            practitionerId = id;
            return this;
        }

        public When organizationId(String id) {
            organizationId = id;
            return this;
        }

        public When useServerData(Boolean value) {
            useServerData = value;
            return this;
        }

        public When data(String dataAssetName) {
            dataRepository = new InMemoryFhirRepository(
                    repository.fhirContext(), (IBaseBundle) jsonParser.parseResource(open(dataAssetName)));
            return this;
        }

        public When content(String dataAssetName) {
            contentRepository = new InMemoryFhirRepository(
                    repository.fhirContext(), (IBaseBundle) jsonParser.parseResource(open(dataAssetName)));
            return this;
        }

        public When terminology(String dataAssetName) {
            terminologyRepository = new InMemoryFhirRepository(
                    repository.fhirContext(), (IBaseBundle) jsonParser.parseResource(open(dataAssetName)));
            return this;
        }

        private void loadAdditionalData(IBaseResource resource) {
            var fhirVersion = repository.fhirContext().getVersion().getVersion();
            additionalData = resource.getIdElement().getResourceType().equals("Bundle")
                    ? (IBaseBundle) resource
                    : addEntry(newBundle(fhirVersion), newEntryWithResource(fhirVersion, resource));
        }

        public When additionalData(String dataAssetName) {
            var data = jsonParser.parseResource(open(dataAssetName));
            loadAdditionalData(data);
            return this;
        }

        public When additionalDataId(IIdType id) {
            additionalDataId = id;
            return this;
        }

        public When parameters(IBaseParameters params) {
            parameters = params;
            return this;
        }

        public When isPut(Boolean value) {
            isPackagePut = value;
            return this;
        }

        public IBaseBundle applyR5() {
            if (additionalDataId != null) {
                loadAdditionalData(readRepository(repository, additionalDataId));
            }
            return processor.applyR5(
                    Eithers.forMiddle3(Ids.newId(repository.fhirContext(), "PlanDefinition", planDefinitionId)),
                    subjectId,
                    encounterId,
                    practitionerId,
                    organizationId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    parameters,
                    useServerData,
                    additionalData,
                    null,
                    dataRepository,
                    contentRepository,
                    terminologyRepository);
        }

        public GeneratedBundle thenApplyR5() {
            return new GeneratedBundle(repository, applyR5());
        }

        public GeneratedCarePlan thenApply() {
            if (additionalDataId != null) {
                loadAdditionalData(readRepository(repository, additionalDataId));
            }
            return new GeneratedCarePlan(
                    repository,
                    processor.apply(
                            Eithers.forMiddle3(Ids.newId(repository.fhirContext(), "PlanDefinition", planDefinitionId)),
                            subjectId,
                            encounterId,
                            practitionerId,
                            organizationId,
                            null,
                            null,
                            null,
                            null,
                            null,
                            parameters,
                            useServerData,
                            additionalData,
                            null,
                            dataRepository,
                            contentRepository,
                            terminologyRepository));
        }

        public GeneratedPackage thenPackage() {
            if (isPackagePut == null) {
                return new GeneratedPackage(processor.packagePlanDefinition(
                        Eithers.forMiddle3(Ids.newId(repository.fhirContext(), "PlanDefinition", planDefinitionId))));
            } else {
                return new GeneratedPackage(processor.packagePlanDefinition(
                        Eithers.forMiddle3(Ids.newId(repository.fhirContext(), "PlanDefinition", planDefinitionId)),
                        isPackagePut));
            }
        }
    }

    public static class GeneratedBundle {
        final Repository repository;
        final IBaseBundle generatedBundle;
        final IParser jsonParser;
        final ModelResolver modelResolver;

        public GeneratedBundle(Repository repository, IBaseBundle generatedBundle) {
            this.repository = repository;
            this.generatedBundle = generatedBundle;
            jsonParser = this.repository.fhirContext().newJsonParser().setPrettyPrint(true);
            modelResolver = FhirModelResolverCache.resolverForVersion(
                    this.repository.fhirContext().getVersion().getVersion());
        }

        public GeneratedBundle isEqualsTo(String expectedBundleAssetName) {
            try {
                JSONAssert.assertEquals(
                        load(expectedBundleAssetName), jsonParser.encodeResourceToString(generatedBundle), true);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
            return this;
        }

        public GeneratedBundle isEqualsTo(IIdType expectedBundleId) {
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(readRepository(repository, expectedBundleId)),
                        jsonParser.encodeResourceToString(generatedBundle),
                        true);
            } catch (JSONException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
            return this;
        }

        public GeneratedBundle hasEntry(int count) {
            assertEquals(count, getEntry(generatedBundle).size());
            return this;
        }

        public GeneratedBundle hasCommunicationRequestPayload() {
            assertTrue(getEntryResources(generatedBundle).stream()
                    .filter(r -> r.fhirType().equals("CommunicationRequest"))
                    .allMatch(c -> modelResolver.resolvePath(c, "payload") != null));
            return this;
        }

        public GeneratedBundle hasQuestionnaire() {
            assertTrue(getEntryResources(generatedBundle).stream()
                    .anyMatch(r -> r.fhirType().equals("Questionnaire")));
            return this;
        }

        @SuppressWarnings("unchecked")
        public GeneratedBundle hasQuestionnaireOperationOutcome() {
            assertTrue(getEntryResources(generatedBundle).stream()
                    .anyMatch(r -> r.fhirType().equals("Questionnaire")
                            && ((List<IBaseResource>) modelResolver.resolvePath(r, "contained"))
                                    .stream().anyMatch(c -> c.fhirType().equals("OperationOutcome"))));
            return this;
        }
    }

    public static class GeneratedCarePlan {
        final Repository repository;
        final IBaseResource generatedCarePlan;
        final IParser jsonParser;
        final ModelResolver modelResolver;

        public GeneratedCarePlan(Repository repository, IBaseResource generatedCarePlan) {
            this.repository = repository;
            this.generatedCarePlan = generatedCarePlan;
            jsonParser = this.repository.fhirContext().newJsonParser().setPrettyPrint(true);
            modelResolver = FhirModelResolverCache.resolverForVersion(
                    this.repository.fhirContext().getVersion().getVersion());
        }

        public GeneratedCarePlan isEqualsTo(String expectedCarePlanAssetName) {
            try {
                JSONAssert.assertEquals(
                        load(expectedCarePlanAssetName), jsonParser.encodeResourceToString(generatedCarePlan), true);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
            return this;
        }

        public GeneratedCarePlan isEqualsTo(IIdType expectedCarePlanId) {
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(readRepository(repository, expectedCarePlanId)),
                        jsonParser.encodeResourceToString(generatedCarePlan),
                        true);
            } catch (JSONException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        public GeneratedCarePlan hasContained(int count) {
            assertEquals(
                    count, ((List<IBaseResource>) modelResolver.resolvePath(generatedCarePlan, "contained")).size());
            return this;
        }

        @SuppressWarnings("unchecked")
        public GeneratedCarePlan hasOperationOutcome() {
            assertTrue(((List<IBaseResource>) modelResolver.resolvePath(generatedCarePlan, "contained"))
                    .stream().anyMatch(r -> r.fhirType().equals("OperationOutcome")));
            return this;
        }

        @SuppressWarnings("unchecked")
        public GeneratedCarePlan hasQuestionnaire() {
            assertTrue(((List<IBaseResource>) modelResolver.resolvePath(generatedCarePlan, "contained"))
                    .stream().anyMatch(r -> r.fhirType().equals("Questionnaire")));
            return this;
        }
    }

    public static class GeneratedPackage {
        IBaseBundle generatedBundle;

        public GeneratedPackage(IBaseBundle generatedBundle) {
            this.generatedBundle = generatedBundle;
        }

        public GeneratedPackage hasEntry(int count) {
            assertEquals(count, getEntry(generatedBundle).size());
            return this;
        }
    }
}
