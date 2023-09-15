package org.opencds.cqf.fhir.cr.questionnaireresponse.r4;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.questionnaireresponse.BaseQuestionnaireResponseProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.search.Searches;

public class QuestionnaireResponseProcessor extends BaseQuestionnaireResponseProcessor<QuestionnaireResponse> {

    public QuestionnaireResponseProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public QuestionnaireResponseProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        super(repository, evaluationSettings);
    }

    @Override
    public QuestionnaireResponse resolveQuestionnaireResponse(IIdType id, IBaseResource questionnaireResponse) {
        var baseQuestionnaireResponse = questionnaireResponse;
        if (baseQuestionnaireResponse == null && id != null) {
            baseQuestionnaireResponse = this.repository.read(QuestionnaireResponse.class, id);
        }

        return castOrThrow(
                baseQuestionnaireResponse,
                QuestionnaireResponse.class,
                "The QuestionnaireResponse passed to repository was not a valid instance of QuestionnaireResponse.class")
                .orElse(null);
    }

    @Override
    protected IBaseBundle createResourceBundle(
            QuestionnaireResponse questionnaireResponse, List<IBaseResource> resources) {
        var newBundle = new Bundle();
        newBundle.setId(new IdType(FHIRAllTypes.BUNDLE.toCode(), getExtractId(questionnaireResponse)));
        newBundle.setType(Bundle.BundleType.TRANSACTION);
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

    @Override
    protected void setup(QuestionnaireResponse questionnaireResponse) {
        patientId = questionnaireResponse.getSubject().getId();
        libraryUrl = questionnaireResponse.hasExtension(Constants.CQF_LIBRARY)
                ? ((CanonicalType) questionnaireResponse
                        .getExtensionByUrl(Constants.CQF_LIBRARY)
                        .getValue())
                        .getValue()
                : null;
    }

    @Override
    public List<IBaseResource> processItems(QuestionnaireResponse questionnaireResponse) {
        var resources = new ArrayList<IBaseResource>();
        var subject = questionnaireResponse.getSubject();
        Questionnaire questionnaire = null;
        var questionnaireCanonical = questionnaireResponse.getQuestionnaire();
        if (questionnaireCanonical != null && !questionnaireCanonical.isEmpty()) {
            var results = this.repository.search(
                    Bundle.class, Questionnaire.class, Searches.byCanonical(questionnaireCanonical));
            questionnaire = results.hasEntry()
                    ? (Questionnaire) results.getEntryFirstRep().getResource()
                    : null;
        }

        if (questionnaireResponse.hasExtension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)) {
            questionnaireResponse
                    .getItem()
                    .forEach(item -> processDefinitionItem(item, questionnaireResponse, resources, subject));
        } else {
            var questionnaireCodeMap = createCodeMap(questionnaire);
            questionnaireResponse.getItem().forEach(item -> {
                if (item.hasItem()) {
                    processGroupItem(item, questionnaireResponse, questionnaireCodeMap, resources, subject);
                } else if (item.hasDefinition()) {
                    processDefinitionItem(item, questionnaireResponse, resources, subject);
                } else {
                    processItem(item, questionnaireResponse, questionnaireCodeMap, resources, subject);
                }
            });
        }

        return resources;
    }

    private void processGroupItem(
            QuestionnaireResponseItemComponent item,
            QuestionnaireResponse questionnaireResponse,
            Map<String, List<Coding>> questionnaireCodeMap,
            List<IBaseResource> resources,
            Reference subject) {
        var subjectItems = item.getItem().stream()
                .filter(child -> child.hasExtension(Constants.SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT))
                .collect(Collectors.toList());
        var groupSubject = !subjectItems.isEmpty() ? subjectItems.get(0).getAnswer().get(0).getValueReference()
                : subject.copy();
        if (item.hasDefinition()) {
            processDefinitionItem(item, questionnaireResponse, resources, groupSubject);
        } else {
            item.getItem().forEach(childItem -> {
                if (!childItem.hasExtension(Constants.SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT)) {
                    if (childItem.hasItem()) {
                        processGroupItem(
                                childItem, questionnaireResponse, questionnaireCodeMap, resources, groupSubject);
                    } else {
                        processItem(childItem, questionnaireResponse, questionnaireCodeMap, resources, groupSubject);
                    }
                }
            });
        }
    }

    private Property getSubjectProperty(Resource resource) {
        var property = resource.getNamedProperty("subject");
        if (property == null) {
            property = resource.getNamedProperty("patient");
        }

        return property;
    }

    private Property getAuthorProperty(Resource resource) {
        var property = resource.getNamedProperty("recorder");
        if (property == null && resource.fhirType().equals(FHIRAllTypes.OBSERVATION.toCode())) {
            property = resource.getNamedProperty("performer");
        }

        return property;
    }

    private List<Property> getDateProperties(Resource resource) {
        List<Property> results = new ArrayList<>();
        results.add(resource.getNamedProperty("onset"));
        results.add(resource.getNamedProperty("issued"));
        results.add(resource.getNamedProperty("effective"));
        results.add(resource.getNamedProperty("recordDate"));

        return results.stream().filter(p -> p != null).collect(Collectors.toList());
    }

    private void resolveMeta(DomainResource resource, String definition) {
        var meta = new Meta();
        // Consider setting source and lastUpdated here?
        if (definition != null && !definition.isEmpty()) {
            meta.addProfile(definition.split("#")[0]);
            resource.setMeta(meta);
        }
    }

    private List<IBase> getExpressionResult(Expression expression, String itemLinkId) {
        if (expression == null || expression.getExpression().isEmpty()) {
            return null;
        }
        try {
            return libraryEngine.resolveExpression(
                    patientId, new CqfExpression(expression, libraryUrl, null), parameters, bundle);
        } catch (Exception ex) {
            var message = String.format(
                    "Error encountered evaluating expression (%s) for item (%s): %s",
                    expression.getExpression(), itemLinkId, ex.getMessage());
            logger.error(message);
        }

        return null;
    }

    private String getDefinitionType(String definition) {
        if (!definition.contains("#")) {
            throw new IllegalArgumentException(
                    String.format("Unable to determine resource type from item definition: %s", definition));
        }
        return definition.split("#")[1];
    }

    private void processDefinitionItem(
            QuestionnaireResponseItemComponent item,
            QuestionnaireResponse questionnaireResponse,
            List<IBaseResource> resources,
            Reference subject) {
        // Definition-based extraction -
        // http://build.fhir.org/ig/HL7/sdc/extraction.html#definition-based-extraction
        var contextExtension = Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT;
        var itemExtractionContext = item.hasExtension(contextExtension)
                ? item.getExtensionByUrl(contextExtension)
                : questionnaireResponse.getExtensionByUrl(contextExtension);
        if (itemExtractionContext != null) {
            var contextExpression = (Expression) itemExtractionContext.getValue();
            var context = getExpressionResult(contextExpression, item.getLinkId());
            if (context != null && !context.isEmpty()) {
                // TODO: edit context instead of creating new resources
            }
        }

        var resourceType = getDefinitionType(item.getDefinition());
        var resource = (Resource) newValue(resourceType);
        resource.setId(new IdType(resourceType, getExtractId(questionnaireResponse) + "-" + item.getLinkId()));
        resolveMeta((DomainResource) resource, item.getDefinition());
        var subjectProperty = getSubjectProperty(resource);
        if (subjectProperty != null) {
            resource.setProperty(subjectProperty.getName(), subject);
        }
        var authorProperty = getAuthorProperty(resource);
        if (authorProperty != null && questionnaireResponse.hasAuthor()) {
            resource.setProperty(authorProperty.getName(), questionnaireResponse.getAuthor());
        }
        var dateProperties = getDateProperties(resource);
        if (dateProperties != null && !dateProperties.isEmpty() && questionnaireResponse.hasAuthored()) {
            dateProperties.forEach(p -> {
                try {
                    var propertyDef = fhirContext().getElementDefinition(
                            p.getTypeCode().contains("|") ? p.getTypeCode().split("\\|")[0] : p.getTypeCode());
                    if (propertyDef != null) {
                        modelResolver.setValue(resource, p.getName(), propertyDef.getImplementingClass()
                                .getConstructor(Date.class).newInstance(questionnaireResponse.getAuthored()));
                    }
                } catch (Exception ex) {
                    var message = String.format(
                            "Error encountered attempting to set property (%s) on resource type (%s): %s",
                            p.getName(), resource.fhirType(), ex.getMessage());
                    logger.error(message);
                }
            });
        }
        item.getItem().forEach(childItem -> {
            if (childItem.hasDefinition()) {
                var definition = childItem.getDefinition().split("#");
                var path = definition[1];
                // First element is always the resource type, so it can be ignored
                path = path.replace(resourceType + ".", "");
                var answerValue = childItem.getAnswerFirstRep().getValue();
                if (answerValue != null) {
                    modelResolver.setValue(resource, path, answerValue);
                }
            }
        });

        resources.add(resource);
    }

    private Base newValue(String type) {
        try {
            return (Base) Class.forName("org.hl7.fhir.r4.model." + type)
                    .getConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void processItem(
            QuestionnaireResponseItemComponent item,
            QuestionnaireResponse questionnaireResponse,
            Map<String, List<Coding>> questionnaireCodeMap,
            List<IBaseResource> resources,
            Reference subject) {
        if (questionnaireCodeMap == null || questionnaireCodeMap.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unable to retrieve Questionnaire code map for Observation based extraction");
        }
        if (item.hasAnswer()) {
            item.getAnswer().forEach(answer -> {
                if (answer.hasItem()) {
                    answer.getItem()
                            .forEach(answerItem -> processItem(
                                    answerItem, questionnaireResponse, questionnaireCodeMap, resources, subject));
                } else {
                    if (questionnaireCodeMap.get(item.getLinkId()) != null
                            && !questionnaireCodeMap.get(item.getLinkId()).isEmpty()) {
                        resources.add(createObservationFromItemAnswer(
                                answer, item.getLinkId(), questionnaireResponse, subject, questionnaireCodeMap));
                    }
                }
            });
        }
    }

    private Observation createObservationFromItemAnswer(
            QuestionnaireResponseItemAnswerComponent answer,
            String linkId,
            QuestionnaireResponse questionnaireResponse,
            Reference subject,
            Map<String, List<Coding>> questionnaireCodeMap) {
        // Observation-based extraction -
        // http://build.fhir.org/ig/HL7/sdc/extraction.html#observation-based-extraction
        var obs = new Observation();
        obs.setId(getExtractId(questionnaireResponse) + "." + linkId);
        obs.setBasedOn(questionnaireResponse.getBasedOn());
        obs.setPartOf(questionnaireResponse.getPartOf());
        obs.setStatus(Observation.ObservationStatus.FINAL);

        var qrCategoryCoding = new Coding();
        qrCategoryCoding.setCode("survey");
        qrCategoryCoding.setSystem("http://hl7.org/fhir/observation-category");
        obs.setCategory(Collections.singletonList(new CodeableConcept().addCoding(qrCategoryCoding)));

        obs.setCode(new CodeableConcept().setCoding(questionnaireCodeMap.get(linkId)));
        obs.setSubject(subject);
        // obs.setFocus();
        obs.setEncounter(questionnaireResponse.getEncounter());
        var authoredDate = new DateTimeType((questionnaireResponse.hasAuthored()
                ? questionnaireResponse.getAuthored().toInstant()
                : Instant.now())
                .toString());
        obs.setEffective(authoredDate);
        obs.setIssuedElement(new InstantType(authoredDate));
        obs.setPerformer(Collections.singletonList(questionnaireResponse.getAuthor()));

        switch (answer.getValue().fhirType()) {
            case "Coding":
                obs.setValue(new CodeableConcept().addCoding(answer.getValueCoding()));
                break;
            case "date":
                obs.setValue(new DateTimeType(((DateType) answer.getValue()).getValue()));
                break;
            default:
                obs.setValue(answer.getValue());
        }
        obs.setDerivedFrom(Collections.singletonList(new Reference(questionnaireResponse)));

        var linkIdExtension = new Extension();
        linkIdExtension.setUrl("http://hl7.org/fhir/uv/sdc/StructureDefinition/derivedFromLinkId");
        var innerLinkIdExtension = new Extension();
        innerLinkIdExtension.setUrl("text");
        innerLinkIdExtension.setValue(new StringType(linkId));
        linkIdExtension.setExtension(Collections.singletonList(innerLinkIdExtension));
        obs.addExtension(linkIdExtension);

        return obs;
    }

    // private Bundle sendObservationBundle(Bundle observationsBundle) throws
    // IllegalArgumentException {
    // String url = mySdcProperties.getExtract().getEndpoint();
    // if (null == url || url.length() < 1) {
    // throw new IllegalArgumentException(
    // "Unable to transmit observation bundle. No observation.endpoint defined in
    // sdc properties.");
    // }
    // String user = mySdcProperties.getExtract().getUsername();
    // String password = mySdcProperties.getExtract().getPassword();
    //
    // IGenericClient client = Clients.forUrl(fhirContext, url);
    // Clients.registerBasicAuth(client, user, password);
    // return client.transaction().withBundle(observationsBundle).execute();
    // }

    // this is based on "if a questionnaire.item has items then this item is a
    // header and will not have a specific code to be used with an answer"
    private Map<String, List<Coding>> createCodeMap(Questionnaire questionnaire) {
        if (questionnaire == null) {
            return null;
        }
        var questionnaireCodeMap = new HashMap<String, List<Coding>>();
        questionnaire.getItem().forEach(item -> processQuestionnaireItems(item, questionnaireCodeMap));

        return questionnaireCodeMap;
    }

    private void processQuestionnaireItems(
            Questionnaire.QuestionnaireItemComponent item, Map<String, List<Coding>> questionnaireCodeMap) {
        if (item.hasItem()) {
            item.getItem().forEach(qItem -> processQuestionnaireItems(qItem, questionnaireCodeMap));
        } else {
            questionnaireCodeMap.put(item.getLinkId(), item.getCode());
        }
    }
}
