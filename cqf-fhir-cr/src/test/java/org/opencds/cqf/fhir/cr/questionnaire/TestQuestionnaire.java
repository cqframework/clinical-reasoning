package org.opencds.cqf.fhir.cr.questionnaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.json.JSONException;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.helpers.GeneratedPackage;
import org.opencds.cqf.fhir.cr.questionnaire.generate.IGenerateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.IPopulateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import org.skyscreamer.jsonassert.JSONAssert;

public class TestQuestionnaire {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/questionnaire";

    public static Given given() {
        return new Given();
    }

    public static class Given {
        private Repository repository;
        private EvaluationSettings evaluationSettings;
        private IGenerateProcessor generateProcessor;
        private IPackageProcessor packageProcessor;
        private IPopulateProcessor populateProcessor;

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

        public Given generateProcessor(IGenerateProcessor generateProcessor) {
            this.generateProcessor = generateProcessor;
            return this;
        }

        public Given packageProcessor(IPackageProcessor packageProcessor) {
            this.packageProcessor = packageProcessor;
            return this;
        }

        public Given populateProcessor(IPopulateProcessor populateProcessor) {
            this.populateProcessor = populateProcessor;
            return this;
        }

        public QuestionnaireProcessor buildProcessor(Repository repository) {
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
                    repository, evaluationSettings, generateProcessor, packageProcessor, populateProcessor);
        }

        public When when() {
            return new When(repository, buildProcessor(repository));
        }
    }

    public static class When {
        private final Repository repository;
        private final QuestionnaireProcessor processor;
        private IPrimitiveType<String> questionnaireUrl;
        private IIdType questionnaireId;
        private IBaseResource questionnaire;
        private String subjectId;
        private IBaseBundle bundle;
        private IBaseParameters parameters;
        private Boolean isPut;

        When(Repository repository, QuestionnaireProcessor processor) {
            this.repository = repository;
            this.processor = processor;
        }

        private FhirContext fhirContext() {
            return repository.fhirContext();
        }

        private PopulateRequest buildRequest(String operationName) {
            return new PopulateRequest(
                    operationName,
                    processor.resolveQuestionnaire(Eithers.for3(questionnaireUrl, questionnaireId, questionnaire)),
                    Ids.newId(fhirContext(), "Patient", subjectId),
                    parameters,
                    bundle,
                    true,
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

        public When additionalData(IBaseBundle bundle) {
            this.bundle = bundle;
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

        // public GeneratedQuestionnaire thenPrepopulate(Boolean buildRequest) {
        //     if (buildRequest) {
        //         var populateRequest = buildRequest("prepopulate");
        //         return new GeneratedQuestionnaire(repository, populateRequest,
        // processor.prePopulate(populateRequest));
        //     } else {
        //         return new GeneratedQuestionnaire(
        //                 repository,
        //                 null,
        //                 processor.prePopulate(
        //                         Eithers.for3(questionnaireUrl, questionnaireId, questionnaire),
        //                         subjectId,
        //                         parameters,
        //                         bundle,
        //                         true,
        //                         (IBaseResource) null,
        //                         null,
        //                         null));
        //     }
        // }

        public IBaseResource runPopulate() {
            return processor.populate(
                    Eithers.for3(questionnaireUrl, questionnaireId, questionnaire),
                    subjectId,
                    parameters,
                    bundle,
                    true,
                    (IBaseResource) null,
                    null,
                    null);
        }

        public GeneratedQuestionnaireResponse thenPopulate(Boolean buildRequest) {
            if (buildRequest) {
                var populateRequest = buildRequest("populate");
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
    }

    public static class GeneratedQuestionnaire {
        Repository repository;
        IParser jsonParser;
        PopulateRequest request;
        IBaseResource questionnaire;
        List<IBaseBackboneElement> items;
        IIdType expectedQuestionnaireId;

        private void populateItems(List<IBaseBackboneElement> itemList) {
            for (var item : itemList) {
                items.add(item);
                var childItems = request.getItems(item);
                if (!childItems.isEmpty()) {
                    populateItems(childItems);
                }
            }
        }

        public GeneratedQuestionnaire(Repository repository, PopulateRequest request, IBaseResource questionnaire) {
            this.repository = repository;
            this.request = request;
            this.questionnaire = questionnaire;
            jsonParser = this.repository.fhirContext().newJsonParser().setPrettyPrint(true);
            items = new ArrayList<>();
            if (request != null) {
                populateItems(request.getItems(questionnaire));
                expectedQuestionnaireId = Ids.newId(
                        questionnaire.getClass(),
                        String.format(
                                "%s-%s",
                                request.getQuestionnaire().getIdElement().getIdPart(),
                                request.getSubjectId().getIdPart()));
            }
        }

        public void isEqualsToExpected(Class<? extends IBaseResource> resourceType) {
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(repository.read(resourceType, expectedQuestionnaireId)),
                        jsonParser.encodeResourceToString(questionnaire),
                        true);
            } catch (JSONException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public GeneratedQuestionnaire hasItems(int expectedItemCount) {
            assertEquals(items.size(), expectedItemCount);

            return this;
        }

        public GeneratedQuestionnaire itemHasInitial(String theLinkId) {
            var matchingItems = items.stream()
                    .filter(i -> request.getItemLinkId(i).equals(theLinkId))
                    .collect(Collectors.toList());
            for (var item : matchingItems) {
                assertFalse(request.resolvePathList(item, "initial").isEmpty());
            }

            return this;
        }

        public GeneratedQuestionnaire hasErrors() {
            assertTrue(request.hasExtension(questionnaire, Constants.EXT_CRMI_MESSAGES));
            assertTrue(request.hasContained(questionnaire));
            assertTrue(request.getContained(questionnaire).stream()
                    .anyMatch(r -> r.fhirType().equals("OperationOutcome")));

            return this;
        }
    }

    public static class GeneratedQuestionnaireResponse {
        Repository repository;
        IParser jsonParser;
        PopulateRequest request;
        IBaseResource questionnaireResponse;
        List<IBaseBackboneElement> items;
        IIdType expectedId;

        private void populateItems(List<IBaseBackboneElement> itemList) {
            for (var item : itemList) {
                items.add(item);
                var childItems = request.getItems(item);
                if (!childItems.isEmpty()) {
                    populateItems(childItems);
                }
            }
        }

        public GeneratedQuestionnaireResponse(
                Repository repository, PopulateRequest request, IBaseResource questionnaireResponse) {
            this.repository = repository;
            this.request = request;
            this.questionnaireResponse = questionnaireResponse;
            jsonParser = this.repository.fhirContext().newJsonParser().setPrettyPrint(true);
            items = new ArrayList<>();
            if (request != null) {
                populateItems(request.getItems(questionnaireResponse));
                expectedId = Ids.newId(
                        questionnaireResponse.getClass(),
                        String.format(
                                "%s-%s",
                                request.getQuestionnaire().getIdElement().getIdPart(),
                                request.getSubjectId().getIdPart()));
            }
        }

        public void isEqualsToExpected(Class<? extends IBaseResource> resourceType) {
            try {
                JSONAssert.assertEquals(
                        jsonParser.encodeResourceToString(repository.read(resourceType, expectedId)),
                        jsonParser.encodeResourceToString(questionnaireResponse),
                        true);
            } catch (JSONException e) {
                e.printStackTrace();
                fail("Unable to compare Jsons: " + e.getMessage());
            }
        }

        public GeneratedQuestionnaireResponse hasItems(int expectedItemCount) {
            assertEquals(expectedItemCount, items.size());

            return this;
        }

        public GeneratedQuestionnaireResponse itemHasAnswer(String theLinkId) {
            var matchingItems = items.stream()
                    .filter(i -> request.getItemLinkId(i).equals(theLinkId))
                    .collect(Collectors.toList());
            for (var item : matchingItems) {
                assertFalse(request.resolvePathList(item, "answer").isEmpty());
            }

            return this;
        }

        public GeneratedQuestionnaireResponse itemHasAuthorExt(String theLinkId) {
            var matchingItems = items.stream()
                    .filter(i -> request.getItemLinkId(i).equals(theLinkId))
                    .collect(Collectors.toList());
            for (var item : matchingItems) {
                assertNotNull(request.getExtensionByUrl(item, Constants.QUESTIONNAIRE_RESPONSE_AUTHOR));
            }

            return this;
        }

        public GeneratedQuestionnaireResponse hasErrors() {
            assertTrue(request.hasExtension(questionnaireResponse, Constants.EXT_CRMI_MESSAGES));
            assertTrue(request.hasContained(questionnaireResponse));
            assertTrue(request.getContained(questionnaireResponse).stream()
                    .anyMatch(r -> r.fhirType().equals("OperationOutcome")));

            return this;
        }
    }
}
