package org.opencds.cqf.fhir.cr.questionnaire.r5.processor.prepopulate;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cr.questionnaire.common.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpressionProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(ExpressionProcessor.class);
    protected static final String EXCEPTION_MESSAGE_TEMPLATE =
            "Error encountered evaluating expression (%s) for item (%s): %s";

    Expression getInitialExpression(QuestionnaireItemComponent item) {
        if (item.hasExtension(Constants.CQF_EXPRESSION)) {
            return (Expression) item.getExtensionByUrl(Constants.CQF_EXPRESSION).getValue();
        } else if (item.hasExtension(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION)) {
            return (Expression) item.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION)
                    .getValue();
        }
        return null;
    }

    List<IBase> getExpressionResult(
            PrePopulateRequest prePopulateRequest,
            Expression expression,
            String itemLinkId,
            Questionnaire questionnaire)
            throws ResolveExpressionException {
        if (expression == null || !expression.hasExpression()) {
            return new ArrayList<>();
        }
        try {
            final CqfExpression cqfExpression = getCqfExpression(expression, questionnaire);
            return prePopulateRequest
                    .getLibraryEngine()
                    .resolveExpression(
                            prePopulateRequest.getPatientId(),
                            cqfExpression,
                            prePopulateRequest.getParameters(),
                            prePopulateRequest.getBundle());
        } catch (Exception ex) {
            final String message =
                    String.format(EXCEPTION_MESSAGE_TEMPLATE, expression.getExpression(), itemLinkId, ex.getMessage());
            throw new ResolveExpressionException(message);
        }
    }

    CqfExpression getCqfExpression(Expression expression, Questionnaire questionnaire) {
        final String libraryUrl = getLibraryUrl(questionnaire);
        return new CqfExpression(expression, libraryUrl, null);
    }

    String getLibraryUrl(Questionnaire questionnaire) {
        return questionnaire.hasExtension(Constants.CQF_LIBRARY)
                ? ((CanonicalType) questionnaire
                                .getExtensionByUrl(Constants.CQF_LIBRARY)
                                .getValue())
                        .getValue()
                : null;
    }
}
