package org.opencds.cqf.fhir.benchmark.plandefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
import static org.opencds.cqf.fhir.utility.BundleHelper.addEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResource;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResources;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;
import static org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResource;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;
import static org.opencds.cqf.fhir.utility.SearchHelper.readRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.json.JSONException;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.benchmark.TestOperationProvider;
import org.opencds.cqf.fhir.benchmark.helpers.DataRequirementsLibrary;
import org.opencds.cqf.fhir.benchmark.helpers.GeneratedPackage;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.plandefinition.PlanDefinitionProcessor;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import org.skyscreamer.jsonassert.JSONAssert;

@SuppressWarnings({"squid:S5960"})
public class TestPlanDefinition {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestPlanDefinition.class);

    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/shared";
    private static final String PLAN_DEFINITION = "PlanDefinition";
    private static final String QUESTIONNAIRE = "Questionnaire";
    private static final String CONTAINED = "contained";
    private static final String OPERATION_OUTCOME = "OperationOutcome";
    private static final String UNABLE_TO_COMPARE_JSONS = "Unable to compare Jsons: ";

    private TestPlanDefinition() {
        // private constructor
    }

    private static InputStream open(String asset) {
        var path = Path.of("%s/%s/%s".formatted(getResourcePath(TestPlanDefinition.class), CLASS_PATH, asset));
        var file = path.toFile();
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static String load(InputStream asset) throws IOException {
        return new String(asset.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static String load(String asset) throws IOException {
        if (asset == null) {
            return null;
        }
        final InputStream inputStream = open(asset);
        if (inputStream == null) {
            return null;
        }
        return load(inputStream);
    }

    public static Given given() {
        return new Given();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class Given {
        private IRepository repository;
        private EvaluationSettings evaluationSettings;

        public Given repository(IRepository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = new IgRepository(
                    fhirContext,
                    Path.of("%s/%s/%s".formatted(getResourcePath(this.getClass()), CLASS_PATH, repositoryPath)));
            return this;
        }

        public Given evaluationSettings(EvaluationSettings evaluationSettings) {
            this.evaluationSettings = evaluationSettings;
            return this;
        }

        public PlanDefinitionProcessor buildProcessor(IRepository repository) {
            if (repository instanceof IgRepository igRepository) {
                igRepository.setOperationProvider(TestOperationProvider.newProvider(repository.fhirContext()));
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
            var crSettings = CrSettings.getDefault().withEvaluationSettings(evaluationSettings);
            return new PlanDefinitionProcessor(repository, crSettings);
        }

        public When when() {
            return new When(repository, buildProcessor(repository));
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class When {
        private final IRepository repository;
        private final PlanDefinitionProcessor processor;
        private final IParser jsonParser;

        private String planDefinitionId;

        private String subjectId;
        private String encounterId;
        private String practitionerId;
        private String organizationId;
        private boolean useServerData;
        private IRepository dataRepository;
        private IRepository contentRepository;
        private IRepository terminologyRepository;
        private IBaseBundle additionalData;
        private IIdType additionalDataId;
        private List<? extends IBaseBackboneElement> prefetchData;
        private IBaseParameters parameters;
        private boolean isPackagePut;

        public When(IRepository repository, PlanDefinitionProcessor processor) {
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

        public When useServerData(boolean value) {
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
                    : addEntry(newBundle(fhirVersion), newEntryWithResource(resource));
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

        public When prefetchData(List<? extends IBaseBackboneElement> data) {
            prefetchData = data;
            return this;
        }

        public When prefetchData(String name, String dataAssetName) {
            var data = jsonParser.parseResource(open(dataAssetName));
            if (!(data instanceof IBaseBundle)) {
                throw new IllegalArgumentException("prefetch data asset must be a Bundle");
            }
            prefetchData = List.of((IBaseBackboneElement) newPart(
                    repository.fhirContext(),
                    "prefetchData",
                    newStringPart(repository.fhirContext(), "key", name),
                    newPart(repository.fhirContext(), "data", data)));
            return this;
        }

        public When parameters(IBaseParameters params) {
            parameters = params;
            return this;
        }

        public When isPut(boolean value) {
            isPackagePut = value;
            return this;
        }

        public IBaseBundle applyR5() {
            if (additionalDataId != null) {
                loadAdditionalData(readRepository(repository, additionalDataId));
            }
            var param = (IParametersAdapter) IAdapterFactory.createAdapterForResource(processor.applyR5(
                    Eithers.forMiddle3(Ids.newId(repository.fhirContext(), PLAN_DEFINITION, planDefinitionId)),
                    List.of(subjectId),
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
                    prefetchData,
                    dataRepository,
                    contentRepository,
                    terminologyRepository));
            return (IBaseBundle) param.getParameter().get(0).getResource();
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
                            Eithers.forMiddle3(Ids.newId(repository.fhirContext(), PLAN_DEFINITION, planDefinitionId)),
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
                            prefetchData,
                            dataRepository,
                            contentRepository,
                            terminologyRepository));
        }

        public GeneratedPackage thenPackage() {
            if (isPackagePut) {
                return new GeneratedPackage(
                        processor.packagePlanDefinition(Eithers.forMiddle3(
                                Ids.newId(repository.fhirContext(), PLAN_DEFINITION, planDefinitionId))),
                        repository.fhirContext());
            } else {
                return new GeneratedPackage(
                        processor.packagePlanDefinition(
                                Eithers.forMiddle3(
                                        Ids.newId(repository.fhirContext(), PLAN_DEFINITION, planDefinitionId)),
                                isPackagePut),
                        repository.fhirContext());
            }
        }

        public DataRequirementsLibrary thenDataRequirements() {
            return new DataRequirementsLibrary(processor.dataRequirements(
                    Eithers.forMiddle3(Ids.newId(repository.fhirContext(), PLAN_DEFINITION, planDefinitionId)),
                    parameters));
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class GeneratedBundle {
        final IRepository repository;
        final IBaseBundle generatedBundleInner;
        final IParser jsonParser;
        final ModelResolver modelResolver;
        IBaseResource questionnaire;
        IBaseResource questionnaireResponse;
        Map<String, IBaseBackboneElement> items;

        public GeneratedBundle(IRepository repository, IBaseBundle generatedBundleInner) {
            this.repository = repository;
            this.generatedBundleInner = generatedBundleInner;
            jsonParser = this.repository.fhirContext().newJsonParser().setPrettyPrint(true);
            modelResolver = FhirModelResolverCache.resolverForVersion(
                    this.repository.fhirContext().getVersion().getVersion());
            questionnaireResponse = getEntryResources(this.generatedBundleInner).stream()
                    .filter(r -> r.fhirType().equals("QuestionnaireResponse"))
                    .findFirst()
                    .orElse(null);
            questionnaire = getEntryResources(this.generatedBundleInner).stream()
                    .filter(r -> r.fhirType().equals(QUESTIONNAIRE))
                    .findFirst()
                    .orElse(null);
            if (questionnaireResponse != null) {
                items = new HashMap<>();
                populateItems(getItems(questionnaireResponse));
            }
        }

        @SuppressWarnings("unchecked")
        private List<IBaseBackboneElement> getItems(IBase base) {
            var pathResult = modelResolver.resolvePath(base, "item");
            return pathResult instanceof List ? (List<IBaseBackboneElement>) pathResult : new ArrayList<>();
        }

        private void populateItems(List<IBaseBackboneElement> itemList) {
            for (var item : itemList) {
                @SuppressWarnings("unchecked")
                var linkIdPath = (IPrimitiveType<String>) modelResolver.resolvePath(item, "linkId");
                var linkId = linkIdPath == null ? null : linkIdPath.getValue();
                items.put(linkId, item);
                var childItems = getItems(item);
                if (!childItems.isEmpty()) {
                    populateItems(childItems);
                }
            }
        }

        public GeneratedBundle isEqualsTo(String expectedBundleAssetName) {
            try {
                JSONAssert.assertEquals(
                        load(expectedBundleAssetName), jsonParser.encodeResourceToString(generatedBundleInner), true);
            } catch (JSONException | IOException e) {
                logger.error(UNABLE_TO_COMPARE_JSONS + e.getMessage(), e);
                fail(UNABLE_TO_COMPARE_JSONS + e.getMessage());
            }
            return this;
        }

        public GeneratedBundle isEqualsTo(IIdType expectedBundleId) {
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(readRepository(repository, expectedBundleId)),
                        jsonParser.encodeResourceToString(generatedBundleInner),
                        true);
            } catch (JSONException e) {
                logger.error(UNABLE_TO_COMPARE_JSONS + e.getMessage(), e);
                fail(UNABLE_TO_COMPARE_JSONS + e.getMessage());
            }
            return this;
        }

        public GeneratedBundle hasEntry(int count) {
            assertEquals(count, getEntry(generatedBundleInner).size());
            return this;
        }

        public GeneratedBundle hasCommunicationRequestPayload() {
            var communications = getEntryResources(generatedBundleInner).stream()
                    .filter(r -> r.fhirType().equals("CommunicationRequest"))
                    .toList();
            assertFalse(communications.isEmpty());
            assertTrue(communications.stream().allMatch(c -> modelResolver.resolvePath(c, "payload") != null));
            return this;
        }

        public GeneratedBundle hasQuestionnaire() {
            assertNotNull(questionnaire);
            return this;
        }

        public GeneratedBundle hasQuestionnaireResponse() {
            assertNotNull(questionnaireResponse);
            return this;
        }

        @SuppressWarnings({"unchecked", "squid:S2259"})
        public GeneratedBundle hasQuestionnaireResponseItemValue(String linkId, String value) {
            var answerPath = modelResolver.resolvePath(items.get(linkId), "answer");
            var answers = answerPath instanceof List<?> l
                    ? l.stream()
                            .map(a -> (IPrimitiveType<String>) modelResolver.resolvePath(a, "value"))
                            .toList()
                    : null;
            assertNotNull(answers);
            assertTrue(
                    answers.stream().anyMatch(a -> a.getValue().equals(value)),
                    "expected answer to contain value: " + value);
            return this;
        }

        @SuppressWarnings("unchecked")
        public GeneratedBundle hasQuestionnaireOperationOutcome() {
            assertTrue(getEntryResources(generatedBundleInner).stream()
                    .anyMatch(r -> r.fhirType().equals(QUESTIONNAIRE)
                            && ((List<IBaseResource>) modelResolver.resolvePath(r, CONTAINED))
                                    .stream().anyMatch(c -> c.fhirType().equals(OPERATION_OUTCOME))));
            return this;
        }

        @SuppressWarnings("unchecked")
        public GeneratedBundle entryHasOperationOutcome(int entry) {
            var resource = getEntryResource(
                    generatedBundleInner.getStructureFhirVersionEnum(),
                    getEntry(generatedBundleInner).get(entry));
            assertTrue(((List<IBaseResource>) modelResolver.resolvePath(resource, CONTAINED))
                    .stream().anyMatch(c -> c.fhirType().equals(OPERATION_OUTCOME)));
            return this;
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static class GeneratedCarePlan {
        final IRepository repository;
        final IBaseResource generatedCarePlanInner;
        final IParser jsonParser;
        final ModelResolver modelResolver;

        public GeneratedCarePlan(IRepository repository, IBaseResource generatedCarePlanInner) {
            this.repository = repository;
            this.generatedCarePlanInner = generatedCarePlanInner;
            jsonParser = this.repository.fhirContext().newJsonParser().setPrettyPrint(true);
            modelResolver = FhirModelResolverCache.resolverForVersion(
                    this.repository.fhirContext().getVersion().getVersion());
        }

        public GeneratedCarePlan isEqualsTo(String expectedCarePlanAssetName) {
            try {
                JSONAssert.assertEquals(
                        load(expectedCarePlanAssetName),
                        jsonParser.encodeResourceToString(generatedCarePlanInner),
                        true);
            } catch (JSONException | IOException e) {
                logger.error(UNABLE_TO_COMPARE_JSONS + e.getMessage(), e);
                fail(UNABLE_TO_COMPARE_JSONS + e.getMessage());
            }
            return this;
        }

        public GeneratedCarePlan isEqualsTo(IIdType expectedCarePlanId) {
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(readRepository(repository, expectedCarePlanId)),
                        jsonParser.encodeResourceToString(generatedCarePlanInner),
                        true);
            } catch (JSONException e) {
                logger.error(UNABLE_TO_COMPARE_JSONS + e.getMessage(), e);
                fail(UNABLE_TO_COMPARE_JSONS + e.getMessage());
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        public GeneratedCarePlan hasContained(int count) {
            assertEquals(
                    count, ((List<IBaseResource>) modelResolver.resolvePath(generatedCarePlanInner, CONTAINED)).size());
            return this;
        }

        @SuppressWarnings("unchecked")
        public GeneratedCarePlan hasOperationOutcome() {
            assertTrue(((List<IBaseResource>) modelResolver.resolvePath(generatedCarePlanInner, CONTAINED))
                    .stream().anyMatch(r -> r.fhirType().equals(OPERATION_OUTCOME)));
            return this;
        }

        @SuppressWarnings("unchecked")
        public GeneratedCarePlan hasQuestionnaire() {
            assertTrue(((List<IBaseResource>) modelResolver.resolvePath(generatedCarePlanInner, CONTAINED))
                    .stream().anyMatch(r -> r.fhirType().equals(QUESTIONNAIRE)));
            return this;
        }

        @SuppressWarnings("unchecked")
        public GeneratedCarePlan hasCommunicationRequestPayload() {
            var communications = ((List<IBaseResource>) modelResolver.resolvePath(generatedCarePlanInner, CONTAINED))
                    .stream()
                            .filter(r -> r.fhirType().equals("CommunicationRequest"))
                            .toList();
            assertFalse(communications.isEmpty());
            assertTrue(communications.stream().allMatch(c -> modelResolver.resolvePath(c, "payload") != null));
            return this;
        }
    }
}
