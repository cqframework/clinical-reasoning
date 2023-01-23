package org.opencds.cqf.cql.evaluator.questionnaireresponse.r5;

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.JsonObject;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.questionnaireresponse.BaseQuestionnaireResponseProcessor;

import java.util.*;
import java.util.stream.Collectors;

public class QuestionnaireResponseProcessor extends BaseQuestionnaireResponseProcessor<QuestionnaireResponse> {

    public QuestionnaireResponseProcessor(FhirContext fhirContext, FhirDal fhirDal) {
        super(fhirContext, fhirDal);
    }

    protected IBaseBundle createResourceBundle(QuestionnaireResponse questionnaireResponse, List<IBaseResource> resources) {
        var newBundle = new Bundle();
        var bundleId = new Identifier();
        bundleId.setValue("QuestionnaireResponse/" + questionnaireResponse.getIdElement().getIdPart());
        newBundle.setType(Bundle.BundleType.TRANSACTION);
        newBundle.setIdentifier(bundleId);
        resources.forEach(resource -> {
            var request = new Bundle.BundleEntryRequestComponent();
            request.setMethod(Bundle.HTTPVerb.PUT);
            request.setUrl(resource.fhirType() + "/" + resource.getIdElement().getIdPart());

            var entry = new Bundle.BundleEntryComponent();
            entry.setResource((Resource) resource);
            entry.setRequest(request);
            newBundle.addEntry(entry);
        });

        return newBundle;
    }

    public List<IBaseResource> processItems(QuestionnaireResponse questionnaireResponse) {
        var questionnaireCanonical = questionnaireResponse.getQuestionnaire();
        if (questionnaireCanonical == null || questionnaireCanonical.isEmpty()) {
            throw new IllegalArgumentException("The QuestionnaireResponse must have the source Questionnaire specified to do extraction");
        }

        var resources = new ArrayList<IBaseResource>();
        var subject = questionnaireResponse.getSubject();
        var itemExtractionContext = questionnaireResponse.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT);
        if (itemExtractionContext != null) {
            processDefinitionItem(itemExtractionContext, "root", questionnaireResponse.getItem(), questionnaireResponse, resources, subject);
        } else {
            var questionnaireCodeMap = getQuestionnaireCodeMap(questionnaireCanonical);
            questionnaireResponse.getItem().forEach(item -> {
                if (item.hasItem()) {
                    processGroupItem(item, questionnaireResponse, questionnaireCodeMap, resources, subject);
                } else {
                    processItem(item, questionnaireResponse, questionnaireCodeMap, resources, subject);
                }
            });
        }

        return resources;
    }

    private void processGroupItem(
            QuestionnaireResponseItemComponent item, QuestionnaireResponse questionnaireResponse,
            Map<String, List<Coding>> questionnaireCodeMap, List<IBaseResource> resources, Reference subject) {
        var subjectItems = item.getItem().stream().filter(child -> child.hasExtension(Constants.SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT)).collect(Collectors.toList());
        var groupSubject = subjectItems.size() != 0 ? subjectItems.get(0).getAnswer().get(0).getValueReference() : subject.copy();
        var itemExtractionContext = item.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT);
        if (itemExtractionContext != null) {
            processDefinitionItem(itemExtractionContext, item.getLinkId(), item.getItem(), questionnaireResponse, resources, groupSubject);
        } else {
            item.getItem().forEach(childItem -> {
                if (!childItem.hasExtension(Constants.SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT)) {
                    if (childItem.hasItem()) {
                        processGroupItem(childItem, questionnaireResponse, questionnaireCodeMap, resources, groupSubject);
                    } else {
                        processItem(childItem, questionnaireResponse, questionnaireCodeMap, resources, groupSubject);
                    }
                }
            });
        }
    }

    private Enumerations.FHIRTypes getFhirType(Extension extension) {
        return Enumerations.FHIRTypes.fromCode(((CodeType) extension.getValue()).getCode());
    }

    private String getSubjectProperty(StructureDefinition resourceDefinition) {
        if (resourceDefinition.getSnapshot().getElement().stream().anyMatch(e -> e.getPath().equals("subject"))) {
            return "subject";
        } else if (resourceDefinition.getSnapshot().getElement().stream().anyMatch(e -> e.getPath().equals("patient"))) {
            return "patient";
        } else {
            return "";
        }
    }

    private void processDefinitionItem(Extension itemExtractionContext, String linkId,
            List<QuestionnaireResponseItemComponent> items, QuestionnaireResponse questionnaireResponse,
            List<IBaseResource> resources, Reference subject) {
        // Definition-based extraction - http://build.fhir.org/ig/HL7/sdc/extraction.html#definition-based-extraction
        var resourceType = getFhirType(itemExtractionContext);
        var resourceDefinition = (StructureDefinition) this.fhirContext.getResourceDefinition(resourceType.toCode()).toProfile("");
        var resourceBuilder = new JsonObject();
        resourceBuilder.addProperty("id", "qr" + questionnaireResponse.getIdElement().getIdPart() + "." + linkId);
        resourceBuilder.addProperty("resourceType", resourceType.toCode());
        var subjectProperty = getSubjectProperty(resourceDefinition);
        if (!subjectProperty.isEmpty()) {
            resourceBuilder.addProperty(subjectProperty, convertToJson(subject));
        }
        items.forEach(childItem -> {
            if (childItem.hasDefinition()) {
                var definition = childItem.getDefinition().split("#");
                var path = definition[1];
                var profileDefinition = (StructureDefinition) fhirDal.read(new IdType("StructureDefinition", resourceType.toCode()));
                // var profile = definition[0];
//                var searchResults = fhirDal.read(new IdType("StructureDefinition", resourceType.toCode()));
//                var profileDefinition = searchResults.iterator().hasNext() ? (StructureDefinition) searchResults.iterator().next() : resourceDefinition;
                var pathElements = path.split("\\.");
                var elementCount = pathElements.length;
                var answerValue = transformAnswerValue(childItem.getAnswerFirstRep(), profileDefinition);
                if (answerValue != null) {
                    // First element is always the resource type, so it can be ignored
                    if (elementCount == 2) {
                        resourceBuilder.addProperty(pathElements[1], answerValue);
                    } else if (elementCount > 2) {
                        // Nested properties may already exist
                        var existingProperty = (JsonObject) resourceBuilder.get(pathElements[1]);
                        if (existingProperty != null) {
                            existingProperty.getAsJsonObject().addProperty(pathElements[2], answerValue);
                        } else {
                            existingProperty = new JsonObject();
                            existingProperty.addProperty(pathElements[2], answerValue);
                        }
                    }
                }
            }
        });

        resources.add(parser.parseResource(resourceBuilder.toString()));
    }

    private String transformAnswerValue(QuestionnaireResponseItemAnswerComponent itemAnswer, StructureDefinition resourceDefinition) {
        if (!itemAnswer.hasValue()) { return null; }

        var answerValue = itemAnswer.getValue();

        String retVal = convertToJson(answerValue);

        if (resourceDefinition != null) {
            retVal = retVal;
        }

        return retVal;
    }

//    private Resource createResource(String resourceType) {
//        switch (resourceType) {
//            case "Patient": return new Patient();
//            case "Organization": return new Organization();
//            default: return null;
//        }
//    }

    private void processItem(
            QuestionnaireResponseItemComponent item, QuestionnaireResponse questionnaireResponse,
            Map<String, List<Coding>> questionnaireCodeMap, List<IBaseResource> resources, Reference subject) {
        if (item.hasAnswer()) {
            item.getAnswer().forEach(answer -> {
                if (answer.hasItem()) {
                    answer.getItem().forEach(answerItem -> {
                        processItem(answerItem, questionnaireResponse, questionnaireCodeMap, resources, subject);
                    });
                } else {
                    if (questionnaireCodeMap.get(item.getLinkId()).size() > 0) {
                        resources.add(createObservationFromItemAnswer(answer, item.getLinkId(), questionnaireResponse, subject, questionnaireCodeMap));
                    }
                }
            });
        }

//        if (item.hasItem()) {
//            item.getItem().forEach(itemItem -> {
//                processItem(itemItem, questionnaireResponse, questionnaireCodeMap, resources, subject);
//            });
//        }
    }

    private Observation createObservationFromItemAnswer(
            QuestionnaireResponseItemAnswerComponent answer, String linkId, QuestionnaireResponse questionnaireResponse,
            Reference subject, Map<String, List<Coding>> questionnaireCodeMap) {
        // Observation-based extraction - http://build.fhir.org/ig/HL7/sdc/extraction.html#observation-based-extraction
        var obs = new Observation();
        obs.setId("qr" + questionnaireResponse.getIdElement().getIdPart() + "." + linkId);
        obs.setBasedOn(questionnaireResponse.getBasedOn());
        obs.setPartOf(questionnaireResponse.getPartOf());
        obs.setStatus(Enumerations.ObservationStatus.FINAL);

        var qrCategoryCoding = new Coding();
        qrCategoryCoding.setCode("survey");
        qrCategoryCoding.setSystem("http://hl7.org/fhir/observation-category");
        obs.setCategory(Collections.singletonList(new CodeableConcept().addCoding(qrCategoryCoding)));

        obs.setCode(new CodeableConcept().setCoding(questionnaireCodeMap.get(linkId)));
        obs.setSubject(subject);
        // obs.setFocus();
        obs.setEncounter(questionnaireResponse.getEncounter());
        obs.setEffective(new DateTimeType(questionnaireResponse.getAuthored()));
        obs.setIssued(questionnaireResponse.getAuthored());
        obs.setPerformer(Collections.singletonList(questionnaireResponse.getAuthor()));

        switch (answer.getValue().fhirType()) {
            case "string":
                obs.setValue(new StringType(answer.getValueStringType().getValue()));
                break;
            case "Coding":
                obs.setValue(new CodeableConcept().addCoding(answer.getValueCoding()));
                break;
            case "boolean":
                obs.setValue(new BooleanType(answer.getValueBooleanType().booleanValue()));
                break;
        }
        var questionnaireResponseReference = new Reference();
        questionnaireResponseReference.setReference("QuestionnaireResponse/" + questionnaireResponse.getIdElement().getIdPart());
        obs.setDerivedFrom(Collections.singletonList(questionnaireResponseReference));

        var linkIdExtension = new Extension();
        linkIdExtension.setUrl("http://hl7.org/fhir/uv/sdc/StructureDefinition/derivedFromLinkId");
        var innerLinkIdExtension = new Extension();
        innerLinkIdExtension.setUrl("text");
        innerLinkIdExtension.setValue(new StringType(linkId));
        linkIdExtension.setExtension(Collections.singletonList(innerLinkIdExtension));
        obs.addExtension(linkIdExtension);

        return obs;
    }

//    private Bundle sendObservationBundle(Bundle observationsBundle) throws IllegalArgumentException {
//        String url = mySdcProperties.getExtract().getEndpoint();
//        if (null == url || url.length() < 1) {
//            throw new IllegalArgumentException(
//                    "Unable to transmit observation bundle.  No observation.endpoint defined in sdc properties.");
//        }
//        String user = mySdcProperties.getExtract().getUsername();
//        String password = mySdcProperties.getExtract().getPassword();
//
//        IGenericClient client = Clients.forUrl(fhirContext, url);
//        Clients.registerBasicAuth(client, user, password);
//        return client.transaction().withBundle(observationsBundle).execute();
//    }

    private Map<String, List<Coding>> getQuestionnaireCodeMap(String questionnaireUrl) {
//        String url = mySdcProperties.getExtract().getEndpoint();
//        if (null == url || url.length() < 1) {
//            throw new IllegalArgumentException("Unable to GET Questionnaire.  No observation.endpoint defined in sdc properties.");
//        }
//        String user = mySdcProperties.getExtract().getUsername();
//        String password = mySdcProperties.getExtract().getPassword();
//
//        IGenericClient client = Clients.forUrl(fhirContext, url);
//        Clients.registerBasicAuth(client, user, password);
//
//        Questionnaire questionnaire = client.read().resource(Questionnaire.class).withUrl(questionnaireUrl).execute();
        var questionnaire = (Questionnaire) this.fhirDal.searchByUrl("Questionnaire", questionnaireUrl).iterator().next();

        if (questionnaire == null) {
            throw new IllegalArgumentException("Unable to find resource by URL " + questionnaireUrl);
        }

        return createCodeMap(questionnaire);
    }

    // this is based on "if a questionnaire.item has items then this item is a
    // header and will not have a specific code to be used with an answer"
    private Map<String, List<Coding>> createCodeMap(Questionnaire questionnaire) {
        var questionnaireCodeMap = new HashMap<String, List<Coding>>();
        questionnaire.getItem().forEach(item -> { processQuestionnaireItems(item, questionnaireCodeMap); });

        return questionnaireCodeMap;
    }

    private void processQuestionnaireItems(Questionnaire.QuestionnaireItemComponent item, Map<String, List<Coding>> questionnaireCodeMap) {
        if (item.hasItem()) {
            item.getItem().forEach(qItem -> { processQuestionnaireItems(qItem, questionnaireCodeMap); });
        } else {
            questionnaireCodeMap.put(item.getLinkId(), item.getCode());
        }
    }
}
