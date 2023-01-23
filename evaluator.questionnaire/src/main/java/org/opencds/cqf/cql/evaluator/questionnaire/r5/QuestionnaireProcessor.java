package org.opencds.cqf.cql.evaluator.questionnaire.r5;

import ca.uhn.fhir.context.FhirContext;
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

        processItems(questionnaire.getItem(), libraryUrl.getValue());

        return questionnaire;
    }

    public void processItems(List<QuestionnaireItemComponent> items, String defaultLibrary) {
        items.forEach(item -> {
            if (item.hasItem()) {
                processItems(item.getItem(), defaultLibrary);
            } else {
                if (item.hasExtension(Constants.CQF_EXPRESSION)) {
                    // evaluate expression and set the result as the initialAnswer on the item
                    var expression = (Expression) item.getExtensionByUrl(Constants.CQF_EXPRESSION).getValue();
                    var libraryUrl = expression.hasReference() ? expression.getReference() : defaultLibrary;
                    var result = getExpressionResult(expression.getExpression(), expression.getLanguage(), libraryUrl, this.parameters);
                    item.addInitial(new Questionnaire.QuestionnaireItemInitialComponent().setValue((DataType)result));
                }
            }
        });
    }

    @Override
    public IBaseResource populate(Questionnaire questionnaire, String patientId, IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpopint, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
        var populatedQuestionnaire = prePopulate(questionnaire, patientId, parameters, bundle, dataEndpopint, contentEndpoint, terminologyEndpoint);
        var response = new QuestionnaireResponse();
        response.setId(populatedQuestionnaire.getId() + "-response");
        response.setQuestionnaire(populatedQuestionnaire.getUrl());
        response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
        response.setSubject(new Reference(new IdType("Patient", patientId)));
        var responseItems = new ArrayList<QuestionnaireResponse.QuestionnaireResponseItemComponent>();
        processResponseItems(populatedQuestionnaire.getItem(), responseItems);
        response.setItem(responseItems);

        return response;
    }

    public void processResponseItems(List<QuestionnaireItemComponent> items, List<QuestionnaireResponseItemComponent> responseItems) {
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
    public Questionnaire generateQuestionnaire() {
        var retVal = new Questionnaire();

        return retVal;
    }
}
