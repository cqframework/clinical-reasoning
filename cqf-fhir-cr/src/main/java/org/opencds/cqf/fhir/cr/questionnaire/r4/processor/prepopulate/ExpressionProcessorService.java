package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpressionProcessorService {
    protected static final Logger logger = LoggerFactory.getLogger(ExpressionProcessorService.class);
    protected static final String EXCEPTION_MESSAGE_TEMPLATE =
            "Error encountered evaluating expression (%s) for item (%s): %s";

    public Expression getInitialExpression(QuestionnaireItemComponent item) {
        if (item.hasExtension(Constants.CQF_EXPRESSION)) {
            return (Expression) item.getExtensionByUrl(Constants.CQF_EXPRESSION).getValue();
        } else if (item.hasExtension(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION)) {
            return (Expression) item.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION)
                    .getValue();
        }
        return null;
    }

    public List<IBase> getExpressionResult(
            PrePopulateRequest prePopulateRequest, Expression expression, String itemLinkId)
            throws ResolveExpressionException {
        if (expression == null || !expression.hasExpression()) {
            return new ArrayList<>();
        }
        try {
            final CqfExpression cqfExpression = getCqfExpression(prePopulateRequest, expression);
            return prePopulateRequest
                    .getLibraryEngine()
                    .resolveExpression(
                            prePopulateRequest.getPatientId(),
                            cqfExpression,
                            prePopulateRequest.getParameters(),
                            prePopulateRequest.getBundle());
        } catch (Exception ex) {
            final String message = String.format(
                    EXCEPTION_MESSAGE_TEMPLATE, expression.getExpression(), itemLinkId, ex.getMessage());
            throw new ResolveExpressionException(message);
        }
    }

    protected CqfExpression getCqfExpression(PrePopulateRequest prePopulateRequest, Expression expression) {
        final String libraryUrl = getLibraryUrl(prePopulateRequest.getQuestionnaire());
        return new CqfExpression(expression, libraryUrl, null);
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
