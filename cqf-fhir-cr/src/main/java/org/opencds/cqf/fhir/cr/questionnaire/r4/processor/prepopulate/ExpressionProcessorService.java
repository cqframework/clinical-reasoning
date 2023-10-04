package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class ExpressionProcessorService {
    protected static final Logger logger = LoggerFactory.getLogger(ExpressionProcessorService.class);
    protected static final String EXCEPTION_MESSAGE_TEMPLATE = "Error encountered evaluating expression (%s) for item (%s): %s";

    public static ExpressionProcessorService of() {
        return new ExpressionProcessorService();
    }
    private ExpressionProcessorService() {}

    public Expression getInitialExpression(QuestionnaireItemComponent item) {
        if (item.hasExtension(Constants.CQF_EXPRESSION)) {
            return (Expression) item.getExtensionByUrl(Constants.CQF_EXPRESSION).getValue();
        } else if (item.hasExtension(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION)) {
            return (Expression) item.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION).getValue();
        }
        return null;
    }

    public List<IBase> getExpressionResult(
        PrePopulateRequest thePrePopulateRequest,
        Expression theExpression,
        String theItemLinkId
    ) throws ResolveExpressionException {
        if (theExpression == null || !theExpression.hasExpression()) {
            return new ArrayList<>();
        }
        try {
            final CqfExpression cqfExpression = getCqfExpression(thePrePopulateRequest, theExpression);
            return thePrePopulateRequest.getLibraryEngine().resolveExpression(
                thePrePopulateRequest.getPatientId(),
                cqfExpression,
                thePrePopulateRequest.getParameters(),
                thePrePopulateRequest.getBundle()
            );
        } catch (Exception ex) {
            final String message = String.format(EXCEPTION_MESSAGE_TEMPLATE, theExpression.getExpression(), theItemLinkId, ex.getMessage());
            throw new ResolveExpressionException(message);
        }
    }

    protected CqfExpression getCqfExpression(
        PrePopulateRequest thePrePopulateRequest,
        Expression theExpression
    ) {
        final String libraryUrl = getLibraryUrl(thePrePopulateRequest.getQuestionnaire());
        return new CqfExpression(theExpression, libraryUrl, null);
    }

    protected String getLibraryUrl(Questionnaire theQuestionnaire) {
        return theQuestionnaire.hasExtension(Constants.CQF_LIBRARY)
            ? ((CanonicalType) theQuestionnaire
            .getExtensionByUrl(Constants.CQF_LIBRARY)
            .getValue())
            .getValue()
            : null;
    }

}
