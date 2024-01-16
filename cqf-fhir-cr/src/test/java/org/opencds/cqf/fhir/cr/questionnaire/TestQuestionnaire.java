package org.opencds.cqf.fhir.cr.questionnaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
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
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.test.TestRepositoryFactory;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;
import org.skyscreamer.jsonassert.JSONAssert;

public class TestQuestionnaire {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/questionnaire";

    public static Given given() {
        return new Given();
    }

    public static class Given {
        private Repository repository;

        public Given repository(Repository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = TestRepositoryFactory.createRepository(
                    fhirContext, this.getClass(), CLASS_PATH + "/" + repositoryPath, IGLayoutMode.TYPE_PREFIX);
            return this;
        }

        public static QuestionnaireProcessor buildProcessor(Repository repository) {
            return new QuestionnaireProcessor(repository);
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

        public GeneratedQuestionnaire thenPrepopulate(Boolean buildRequest) {
            if (buildRequest) {
                var populateRequest = buildRequest("prepopulate");
                return new GeneratedQuestionnaire(repository, populateRequest, processor.prePopulate(populateRequest));
            } else {
                return new GeneratedQuestionnaire(
                        repository,
                        null,
                        processor.prePopulate(
                                Eithers.for3(questionnaireUrl, questionnaireId, questionnaire),
                                subjectId,
                                parameters,
                                bundle,
                                null,
                                null,
                                null));
            }
        }

        public GeneratedQuestionnaireResponse thenPopulate(Boolean buildRequest) {
            if (buildRequest) {
                var populateRequest = buildRequest("populate");
                return new GeneratedQuestionnaireResponse(
                        repository, populateRequest, processor.populate(populateRequest));
            } else {
                return new GeneratedQuestionnaireResponse(
                        repository,
                        null,
                        processor.populate(
                                Eithers.for3(questionnaireUrl, questionnaireId, questionnaire),
                                subjectId,
                                parameters,
                                bundle,
                                null,
                                null,
                                null));
            }
        }

        public IBaseBundle thenPackage() {
            var param = Eithers.for3(questionnaireUrl, questionnaireId, questionnaire);
            return isPut == null ? processor.packageQuestionnaire(param) : processor.packageQuestionnaire(param, isPut);
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

        public GeneratedQuestionnaireResponse hasErrors() {
            assertTrue(request.hasExtension(questionnaireResponse, Constants.EXT_CRMI_MESSAGES));
            assertTrue(request.hasContained(questionnaireResponse));
            assertTrue(request.getContained(questionnaireResponse).stream()
                    .anyMatch(r -> r.fhirType().equals("OperationOutcome")));

            return this;
        }
    }
}
