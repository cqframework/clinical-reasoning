package org.opencds.cqf.fhir.cr.questionnaire.r4;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class ExpressionProcessorService {
    protected static final Logger logger = LoggerFactory.getLogger(ExpressionProcessorService.class);
    OperationOutcome myOperationOutcome;
    Questionnaire myQuestionnaire;
    String myPatientId;
    IBaseParameters myParameters;
    IBaseBundle myBundle;
    LibraryEngine myLibraryEngine;

    public ExpressionProcessorService(
        Questionnaire questionnaire,
        String patientId,
        IBaseParameters parameters,
        IBaseBundle bundle,
        LibraryEngine libraryEngine
    ) {
        this.myQuestionnaire = questionnaire;
        this.myPatientId = patientId;
        this.myParameters = parameters;
        this.myBundle = bundle;
        this.myLibraryEngine = libraryEngine;
    }

    Expression getInitialExpression(QuestionnaireItemComponent item) {
        if (item.hasExtension(Constants.CQF_EXPRESSION)) {
            return (Expression) item.getExtensionByUrl(Constants.CQF_EXPRESSION).getValue();
        } else if (item.hasExtension(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION)) {
            return (Expression) item.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION).getValue();
        }
        return null;
    }

    List<IBase> getExpressionResult(
        Expression expression,
        String itemLinkId
    ) {
        if (expression == null || !expression.hasExpression()) {
            return new ArrayList<>();
        }
        try {
            final String libraryUrl = getLibraryUrl(myQuestionnaire);
            return myLibraryEngine.resolveExpression(myPatientId, new CqfExpression(expression, libraryUrl, null), myParameters, myBundle);
        } catch (Exception ex) {
            var message = String.format(
                "Error encountered evaluating expression (%s) for item (%s): %s",
                expression.getExpression(), itemLinkId, ex.getMessage());
            logger.error(message);
            myOperationOutcome.addIssue()
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setDiagnostics(message);
        }
        return new ArrayList<>();
    }

    protected String getLibraryUrl(Questionnaire questionnaire) {
        return questionnaire.hasExtension(Constants.CQF_LIBRARY)
            ? ((CanonicalType) questionnaire
            .getExtensionByUrl(Constants.CQF_LIBRARY)
            .getValue())
            .getValue()
            : null;
    }

}
