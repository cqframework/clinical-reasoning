package org.opencds.cqf.cql.evaluator.questionnaire.r5;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.questionnaire.BaseQuestionnaireProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuestionnaireProcessor extends BaseQuestionnaireProcessor<Questionnaire> {
    public QuestionnaireProcessor(
            FhirContext fhirContext, FhirDal fhirDal, LibraryProcessor libraryProcessor,
            ExpressionEvaluator expressionEvaluator) {
        super(fhirContext, fhirDal, libraryProcessor, expressionEvaluator);
    }

    @Override
    public Object resolveParameterValue(IBase value) {
        if (value == null) return null;
        return ((Parameters.ParametersParameterComponent) value).getValue();
    }

    @Override
    public IBaseResource getSubject() { return this.fhirDal.read(new IdType("Patient", this.patientId)); }

    @Override
    public Questionnaire prePopulate(Questionnaire questionnaire, String patientId, IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpopint, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
        if (questionnaire == null) {
            throw new IllegalArgumentException("No questionnaire passed in");
        }
        this.patientId = patientId;
        this.parameters = parameters;
        this.bundle = bundle;
        this.dataEndpoint = dataEndpopint;
        this.contentEndpoint = contentEndpoint;
        this.terminologyEndpoint = terminologyEndpoint;

        var libraryUrl = (CanonicalType) questionnaire.getExtensionByUrl(Constants.CQF_LIBRARY).getValue();
        var oc = new OperationOutcome();
        oc.setId("prepopulate-outcome-" + questionnaire.getIdPart());

        processItems(questionnaire.getItem(), libraryUrl.getValue(), oc);

        if (oc.getIssue().size() > 0) {
            questionnaire.addContained(oc);
            questionnaire.addExtension(Constants.EXT_PREPOPULATE_OPERATION_OUTCOME, new Reference("#" + oc.getIdPart()));
        }

        return questionnaire;
    }

    protected void processItems(List<QuestionnaireItemComponent> items, String defaultLibrary, OperationOutcome oc) {
        items.forEach(item -> {
            if (item.hasItem()) {
                processItems(item.getItem(), defaultLibrary, oc);
            } else {
                if (item.hasExtension(Constants.CQF_EXPRESSION)) {
                    // evaluate expression and set the result as the initialAnswer on the item
                    var expression = (Expression) item.getExtensionByUrl(Constants.CQF_EXPRESSION).getValue();
                    var libraryUrl = expression.hasReference() ? expression.getReference() : defaultLibrary;
                    try {
                    var result = getExpressionResult(expression.getExpression(), expression.getLanguage(), libraryUrl, this.parameters);
                    item.addInitial(new Questionnaire.QuestionnaireItemInitialComponent().setValue((DataType)result));
                    } catch (Exception ex) {
                        var message = String.format("Error encountered evaluating expression (%s) for item (%s): %s",
                                expression.getExpression(), item.getLinkId(), ex.getMessage());
                        logger.error(message);
                        oc.addIssue()
                                .setCode(OperationOutcome.IssueType.EXCEPTION)
                                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                                .setDiagnostics(message);
                    }
                }
            }
        });
    }

    @Override
    public IBaseResource populate(Questionnaire questionnaire, String patientId, IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpopint, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
        var populatedQuestionnaire = prePopulate(questionnaire, patientId, parameters, bundle, dataEndpopint, contentEndpoint, terminologyEndpoint);
        var response = new QuestionnaireResponse();
        response.setId(populatedQuestionnaire.getIdPart() + "-response");
        if (questionnaire.hasExtension(Constants.EXT_PREPOPULATE_OPERATION_OUTCOME)) {
            var ocExt = questionnaire.getExtensionByUrl(Constants.EXT_PREPOPULATE_OPERATION_OUTCOME);
            var ocId = ((Reference) ocExt.getValue()).getReference().replaceFirst("#", "");
            var ocList = questionnaire.getContained().stream().filter(resource -> resource.getIdPart().equals(ocId)).collect(Collectors.toList());
            var oc = ocList == null || ocList.size() == 0 ? null : ocList.get(0);
            if (oc != null) {
                oc.setId("populate-outcome-" + populatedQuestionnaire.getIdPart());
                response.addContained(oc);
                response.addExtension(Constants.EXT_POPULATE_OPERATION_OUTCOME, new Reference("#" + oc.getIdPart()));
            }
        }
        response.setQuestionnaire(populatedQuestionnaire.getUrl());
        response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
        response.setSubject(new Reference(new IdType("Patient", patientId)));
        var responseItems = new ArrayList<QuestionnaireResponse.QuestionnaireResponseItemComponent>();
        processResponseItems(populatedQuestionnaire.getItem(), responseItems);
        response.setItem(responseItems);

        return response;
    }

    protected void processResponseItems(List<QuestionnaireItemComponent> items, List<QuestionnaireResponseItemComponent> responseItems) {
        items.forEach(item -> {
            var responseItem = new QuestionnaireResponse.QuestionnaireResponseItemComponent(item.getLinkId());
            responseItem.setDefinition(item.getDefinition());
            responseItem.setTextElement(item.getTextElement());
            if (item.hasItem()) {
                var nestedResponseItems = new ArrayList<QuestionnaireResponse.QuestionnaireResponseItemComponent>();
                processResponseItems(item.getItem(), nestedResponseItems);
                responseItem.setItem(nestedResponseItems);
            } else if (item.hasInitial()) {
                item.getInitial().forEach(answer -> {
                    responseItem.addAnswer(new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().setValue(answer.getValue()));
                });
            }
            responseItems.add(responseItem);
        });
    }

    @Override
    public Questionnaire generateQuestionnaire(String theId, String patientId, IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpoint, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
        this.patientId = patientId;
        this.parameters = parameters;
        this.bundle = bundle;
        this.dataEndpoint = dataEndpoint;
        this.contentEndpoint = contentEndpoint;
        this.terminologyEndpoint = terminologyEndpoint;

        var questionnaire = new Questionnaire();
        questionnaire.setId(new IdType("Questionnaire", theId));

        return questionnaire;
    }

    public Questionnaire.QuestionnaireItemComponent generateItem(DataRequirement actionInput, Integer itemCount) {
        throw new NotImplementedException();
    }
}
