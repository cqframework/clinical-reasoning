package org.opencds.cqf.cql.evaluator.questionnaire.dstu3;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.questionnaire.BaseQuestionnaireProcessor;

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
    public Questionnaire prePopulate(Questionnaire questionnaire, String patientId, IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpopint) {
        if (questionnaire == null) {
            throw new IllegalArgumentException("No questionnaire passed in");
        }
        this.patientId = patientId;
        this.parameters = parameters;
        this.bundle = bundle;
        this.dataEndpoint = dataEndpopint;

//        var libraryUrl = (CanonicalType) questionnaire.getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/cqf-library").getValue();
//
//        processItems(questionnaire.getItem(), libraryUrl.getValue());

        return questionnaire;
    }

    public void processItems(List<QuestionnaireItemComponent> items, String defaultLibrary) {
        items.forEach(item -> {
            if (item.hasItem()) {
                processItems(item.getItem(), defaultLibrary);
            } else {
                if (item.hasExtension("http://hl7.org/fhir/StructureDefinition/cqf-expression")) {
                    // evaluate expression and set the result as the initialAnswer on the item
//                    var expression = (Expression) item.getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/cqf-expression").getValue();
//                    var libraryUrl = expression.hasReference() ? expression.getReference() : defaultLibrary;
//                    var result = getExpressionResult(expression.getExpression(), expression.getLanguage(), libraryUrl, this.parameters);
//                    item.addInitial(new Questionnaire.QuestionnaireItemInitialComponent().setValue((Type)result));
                }
            }
        });
    }

    @Override
    public Questionnaire generateQuestionnaire() {
        var retVal = new Questionnaire();

        return retVal;
    }
}
