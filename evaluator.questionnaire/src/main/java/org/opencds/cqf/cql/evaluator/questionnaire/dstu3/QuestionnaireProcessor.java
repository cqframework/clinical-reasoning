package org.opencds.cqf.cql.evaluator.questionnaire.dstu3;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
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

        var libraryExtensions = questionnaire.getExtensionsByUrl(Constants.CQF_LIBRARY);
        if (libraryExtensions == null || libraryExtensions.size() == 0) {
            throw new IllegalArgumentException("No default library found for evaluation");
        }

        var libraryUrl = ((UriType) libraryExtensions.get(0).getValue()).getValue();

        processItems(questionnaire.getItem(), libraryUrl);

        return questionnaire;
    }

    public void processItems(List<QuestionnaireItemComponent> items, String defaultLibrary) {
        items.forEach(item -> {
            if (item.hasItem()) {
                processItems(item.getItem(), defaultLibrary);
            } else {
                if (item.hasExtension(Constants.CQF_EXPRESSION)) {
                    // evaluate expression and set the result as the initialAnswer on the item
                    var expression = item.getExtensionsByUrl(Constants.CQF_EXPRESSION).get(0).getValue().toString();
                    var language = item.getExtensionsByUrl(Constants.CQF_EXPRESSION_LANGUAGE).get(0).getValue().toString();
                    var result = getExpressionResult(expression, language, defaultLibrary, this.parameters);
                    item.setInitial((Type) result);
                }
            }
        });
    }

    @Override
    public IBaseResource populate(Questionnaire questionnaire, String patientId, IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpopint, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
        var populatedQuestionnaire = prePopulate(questionnaire, patientId, parameters, bundle, dataEndpopint, contentEndpoint, terminologyEndpoint);
        var response = new QuestionnaireResponse();
        response.setId(populatedQuestionnaire.getId() + "-response");
        response.setQuestionnaire(new Reference(populatedQuestionnaire));
        response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
        response.setSubject(new Reference(new IdType("Patient", patientId)));
        var responseItems = new ArrayList<QuestionnaireResponseItemComponent>();
        processResponseItems(populatedQuestionnaire.getItem(), responseItems);
        response.setItem(responseItems);

        return response;
    }

    public void processResponseItems(List<QuestionnaireItemComponent> items, List<QuestionnaireResponseItemComponent> responseItems) {
        items.forEach(item -> {
            var responseItem = new QuestionnaireResponseItemComponent(item.getLinkIdElement());
            responseItem.setDefinition(item.getDefinition());
            responseItem.setTextElement(item.getTextElement());
            if (item.hasItem()) {
                var nestedResponseItems = new ArrayList<QuestionnaireResponseItemComponent>();
                processResponseItems(item.getItem(), nestedResponseItems);
                responseItem.setItem(nestedResponseItems);
            } else if (item.hasInitial()) {
                var answer = item.getInitial();
                responseItem.addAnswer(new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().setValue(answer));
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
