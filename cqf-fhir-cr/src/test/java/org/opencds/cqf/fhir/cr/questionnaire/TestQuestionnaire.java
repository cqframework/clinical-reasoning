package org.opencds.cqf.fhir.cr.questionnaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.json.JSONException;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.common.IDataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.helpers.DataRequirementsLibrary;
import org.opencds.cqf.fhir.cr.helpers.GeneratedPackage;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.generate.IGenerateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.IPopulateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.VersionUtilities;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemAnswerComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import org.skyscreamer.jsonassert.JSONAssert;

@SuppressWarnings("UnstableApiUsage")
public class TestQuestionnaire {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/shared";

    public static Given given() {
        return new Given();
    }

    public static class Given {
        private IRepository repository;
        private EvaluationSettings evaluationSettings;
        private IGenerateProcessor generateProcessor;
        private IPackageProcessor packageProcessor;
        private IDataRequirementsProcessor dataRequirementsProcessor;
        private IPopulateProcessor populateProcessor;

        public Given repository(IRepository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = new IgRepository(
                    fhirContext, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            return this;
        }

        public Given evaluationSettings(EvaluationSettings evaluationSettings) {
            this.evaluationSettings = evaluationSettings;
            return this;
        }

        public Given generateProcessor(IGenerateProcessor generateProcessor) {
            this.generateProcessor = generateProcessor;
            return this;
        }

        public Given packageProcessor(IPackageProcessor packageProcessor) {
            this.packageProcessor = packageProcessor;
            return this;
        }

        public Given dataRequirementsProcessor(IDataRequirementsProcessor dataRequirementsProcessor) {
            this.dataRequirementsProcessor = dataRequirementsProcessor;
            return this;
        }

        public Given populateProcessor(IPopulateProcessor populateProcessor) {
            this.populateProcessor = populateProcessor;
            return this;
        }

        public QuestionnaireProcessor buildProcessor(IRepository repository) {
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
            return new QuestionnaireProcessor(
                    repository,
                    evaluationSettings,
                    generateProcessor,
                    packageProcessor,
                    dataRequirementsProcessor,
                    populateProcessor);
        }

        public When when() {
            return new When(repository, buildProcessor(repository));
        }
    }

    public static class When {
        private final IRepository repository;
        private final QuestionnaireProcessor processor;
        private IPrimitiveType<String> questionnaireUrl;
        private IIdType questionnaireId;
        private IBaseResource questionnaire;
        private String subjectId;
        private List<IBaseBackboneElement> context;
        private IBaseExtension<?, ?> launchContext;
        private boolean useServerData;
        private IBaseBundle data;
        private IBaseParameters parameters;
        private Boolean isPut;
        private IIdType profileId;

        When(IRepository repository, QuestionnaireProcessor processor) {
            this.repository = repository;
            this.processor = processor;
            useServerData = true;
        }

        private FhirContext fhirContext() {
            return repository.fhirContext();
        }

        private PopulateRequest buildRequest() {
            return new PopulateRequest(
                    processor.resolveQuestionnaire(Eithers.for3(questionnaireUrl, questionnaireId, questionnaire)),
                    Ids.newId(fhirContext(), "Patient", subjectId),
                    context,
                    launchContext,
                    parameters,
                    data,
                    new LibraryEngine(repository, processor.evaluationSettings),
                    processor.modelResolver);
        }

        public When questionnaireUrl(IPrimitiveType<String> url) {
            questionnaireUrl = url;
            return this;
        }

        public When questionnaireId(IIdType id) {
            questionnaireId = id;
            return this;
        }

        public When questionnaire(IBaseResource resource) {
            questionnaire = resource;
            return this;
        }

        public When subjectId(String id) {
            subjectId = id;
            return this;
        }

        public When context(List<IBaseBackboneElement> context) {
            this.context = context;
            return this;
        }

        public When launchContext(IBaseExtension<?, ?> extension) {
            launchContext = extension;
            return this;
        }

        public When useServerData(boolean value) {
            useServerData = value;
            return this;
        }

        public When additionalData(IBaseBundle data) {
            this.data = data;
            return this;
        }

        public When parameters(IBaseParameters params) {
            parameters = params;
            return this;
        }

        public When isPut(Boolean value) {
            isPut = value;
            return this;
        }

        public When profileId(IIdType id) {
            profileId = id;
            return this;
        }

        public IBaseResource runPopulate() {
            return processor.populate(
                    Eithers.for3(questionnaireUrl, questionnaireId, questionnaire),
                    subjectId,
                    context,
                    launchContext,
                    parameters,
                    data,
                    useServerData,
                    (IBaseResource) null,
                    null,
                    null);
        }

        public GeneratedQuestionnaireResponse thenPopulate(Boolean buildRequest) {
            if (buildRequest) {
                var populateRequest = buildRequest();
                return new GeneratedQuestionnaireResponse(
                        repository, populateRequest, processor.populate(populateRequest));
            } else {
                return new GeneratedQuestionnaireResponse(repository, null, runPopulate());
            }
        }

        public GeneratedPackage thenPackage() {
            var param = Eithers.for3(questionnaireUrl, questionnaireId, questionnaire);
            return new GeneratedPackage(
                    isPut == null
                            ? processor.packageQuestionnaire(param)
                            : processor.packageQuestionnaire(param, isPut),
                    fhirContext());
        }

        public GeneratedQuestionnaire thenGenerate() {
            var request = new GenerateRequest(
                    processor.resolveStructureDefinition(Eithers.for3(null, profileId, null)),
                    false,
                    true,
                    new LibraryEngine(repository, processor.evaluationSettings),
                    processor.modelResolver);
            return new GeneratedQuestionnaire(repository, request, processor.generateQuestionnaire(request, null));
        }

        public DataRequirementsLibrary thenDataRequirements() {
            var param = Eithers.for3(questionnaireUrl, questionnaireId, questionnaire);
            return new DataRequirementsLibrary(processor.dataRequirements(param, parameters));
        }
    }

    public static class GeneratedQuestionnaire {
        public IQuestionnaireAdapter questionnaire;
        IRepository repository;
        IAdapterFactory adapterFactory;
        IParser jsonParser;
        GenerateRequest request;
        List<IBase> items;
        IIdType expectedQuestionnaireId;

        private void populateItems(List<? extends IItemComponentAdapter> itemList) {
            itemList.forEach(item -> {
                items.add(item.get());
                if (item.hasItem()) {
                    populateItems(item.getItem());
                }
            });
        }

        public GeneratedQuestionnaire(IRepository repository, GenerateRequest request, IBaseResource questionnaire) {
            this.repository = repository;
            this.request = request;
            adapterFactory = IAdapterFactory.forFhirContext(this.repository.fhirContext());
            this.questionnaire = adapterFactory.createQuestionnaire(questionnaire);
            jsonParser = this.repository.fhirContext().newJsonParser().setPrettyPrint(true);
            items = new ArrayList<>();
            if (request != null) {
                populateItems(this.questionnaire.getItem());
                expectedQuestionnaireId = Ids.newId(
                        questionnaire.getClass(),
                        request.getQuestionnaire().getIdElement().getIdPart());
            }
        }

        public void isEqualsToExpected(Class<? extends IBaseResource> resourceType) {
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(repository.read(resourceType, expectedQuestionnaireId)),
                        jsonParser.encodeResourceToString(questionnaire.get()),
                        true);
            } catch (JSONException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public GeneratedQuestionnaire hasItems(int expectedItemCount) {
            assertEquals(expectedItemCount, items.size());

            return this;
        }

        public GeneratedQuestionnaire itemHasInitial(String theLinkId) {
            var matchingItems = items.stream()
                    .map(i -> IAdapterFactory.createAdapterForBase(
                            repository.fhirContext().getVersion().getVersion(), i))
                    .map(IQuestionnaireItemComponentAdapter.class::cast)
                    .filter(i -> i.getLinkId().equals(theLinkId))
                    .toList();
            for (var item : matchingItems) {
                assertTrue(item.hasInitial());
                // assertFalse(request.resolvePathList(item, "initial").isEmpty());
            }

            return this;
        }

        public GeneratedQuestionnaire hasErrors() {
            assertTrue(questionnaire.hasExtension(Constants.CQF_MESSAGES));
            assertTrue(questionnaire.hasContained());
            assertTrue(questionnaire.getContained().stream()
                    .anyMatch(r -> r.fhirType().equals("OperationOutcome")));

            return this;
        }
    }

    public static class GeneratedQuestionnaireResponse {
        IRepository repository;
        IAdapterFactory adapterFactory;
        IParser jsonParser;
        PopulateRequest request;
        IQuestionnaireResponseAdapter questionnaireResponse;
        Map<String, IQuestionnaireResponseItemComponentAdapter> items;
        IIdType expectedId;

        private void populateItems(List<? extends IItemComponentAdapter> itemList) {
            itemList.stream()
                    .map(IQuestionnaireResponseItemComponentAdapter.class::cast)
                    .forEach(item -> {
                        items.put(item.getLinkId(), item);
                        if (item.hasItem()) {
                            populateItems(item.getItem());
                        }
                        if (item.hasAnswer()) {
                            item.getAnswer().forEach(answer -> {
                                if (answer.hasItem()) {
                                    populateItems(answer.getItem());
                                }
                            });
                        }
                    });
        }

        public GeneratedQuestionnaireResponse(
                IRepository repository, PopulateRequest request, IBaseResource questionnaireResponse) {
            this.repository = repository;
            adapterFactory = IAdapterFactory.forFhirContext(this.repository.fhirContext());
            this.request = request;
            this.questionnaireResponse = adapterFactory.createQuestionnaireResponse(questionnaireResponse);
            jsonParser = this.repository.fhirContext().newJsonParser().setPrettyPrint(true);
            items = new HashMap<>();
            if (request != null) {
                populateItems(this.questionnaireResponse.getItem());
                expectedId = Ids.newId(
                        questionnaireResponse.getClass(),
                        "%s-%s"
                                .formatted(
                                        request.getQuestionnaire()
                                                .getIdElement()
                                                .getIdPart(),
                                        request.getSubjectId().getIdPart()));
            }
        }

        public GeneratedQuestionnaireResponse hasItems(int expectedItemCount) {
            assertEquals(expectedItemCount, items.size());

            return this;
        }

        public GeneratedQuestionnaireResponse itemHasAnswer(String linkId) {
            assertTrue(items.get(linkId).hasAnswer());
            return this;
        }

        public GeneratedQuestionnaireResponse itemHasNoAnswer(String linkId) {
            assertFalse(items.get(linkId).hasAnswer());
            return this;
        }

        public GeneratedQuestionnaireResponse itemHasAnswerValue(String linkId, String value) {
            return itemHasAnswerValue(
                    linkId,
                    VersionUtilities.stringTypeForVersion(
                            repository.fhirContext().getVersion().getVersion(), value));
        }

        public GeneratedQuestionnaireResponse itemHasAnswerValue(String linkId, IBase value) {
            var answers = items.get(linkId).getAnswer().stream()
                    .map(IQuestionnaireResponseItemAnswerComponentAdapter::getValue)
                    .toList();
            assertNotNull(answers);
            assertFalse(answers.isEmpty());
            if (value instanceof IPrimitiveType) {
                assertTrue(
                        answers.stream().anyMatch(a -> a.toString().equals(value.toString())),
                        "expected answer to contain value: " + value);
            } else {
                assertTrue(answers.stream().anyMatch(value.getClass()::isInstance));
            }
            return this;
        }

        public GeneratedQuestionnaireResponse itemHasAuthorExt(String linkId) {
            assertNotNull(items.get(linkId).getExtensionByUrl(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR));
            return this;
        }

        public GeneratedQuestionnaireResponse itemHasNoAuthorExt(String linkId) {
            assertNull(items.get(linkId).getExtensionsByUrl(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR));
            return this;
        }

        public GeneratedQuestionnaireResponse hasErrors() {
            assertTrue(questionnaireResponse.hasExtension(Constants.CQF_MESSAGES));
            assertTrue(questionnaireResponse.hasContained());
            assertTrue(questionnaireResponse.getContained().stream()
                    .anyMatch(r -> r.fhirType().equals("OperationOutcome")));

            return this;
        }

        public GeneratedQuestionnaireResponse hasNoErrors() {
            assertFalse(questionnaireResponse.hasExtension(Constants.CQF_MESSAGES));
            assertTrue(questionnaireResponse.getContained().stream()
                    .noneMatch(r -> r.fhirType().equals("OperationOutcome")));

            return this;
        }
    }
}
