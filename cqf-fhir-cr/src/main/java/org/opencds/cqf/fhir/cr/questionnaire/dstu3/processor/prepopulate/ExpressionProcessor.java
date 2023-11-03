package org.opencds.cqf.fhir.cr.questionnaire.dstu3.processor.prepopulate;

import java.util.List;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cr.questionnaire.common.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ca.uhn.fhir.util.ExtensionUtil.getExtensionByUrl;

public class ExpressionProcessor {
    private PrePopulateHelper prePopulateHelper;
    ExpressionProcessor() {
        new ExpressionProcessor(new PrePopulateHelper());
    }

    private ExpressionProcessor(PrePopulateHelper prePopulateHelper) {
        this.prePopulateHelper = prePopulateHelper;
    }

    protected static final Logger logger = LoggerFactory.getLogger(ExpressionProcessor.class);
    protected static final String EXCEPTION_MESSAGE_TEMPLATE =
            "Error encountered evaluating expression (%s) for item (%s): %s";

    CqfExpression getInitialExpression(Questionnaire questionnaire, QuestionnaireItemComponent item) {
        if (item.hasExtension(Constants.CQF_EXPRESSION)) {
            return prePopulateHelper.getExpressionByExtension(questionnaire, item, Constants.CQF_EXPRESSION);
        } else if (item.hasExtension(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION)) {
            return prePopulateHelper.getExpressionByExtension(questionnaire, item, Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);
        }
        return null;
    }

    List<IBase> getExpressionResult(
            PrePopulateRequest prePopulateRequest, CqfExpression expression, String itemLinkId)
            throws ResolveExpressionException {
        try {
            return prePopulateRequest
                .getLibraryEngine()
                .resolveExpression(prePopulateRequest.getPatientId(), expression, prePopulateRequest.getParameters(), prePopulateRequest.getBundle());
        } catch (Exception ex) {
            final String message = String.format(
                    EXCEPTION_MESSAGE_TEMPLATE, expression.getExpression(), itemLinkId, ex.getMessage());
            throw new ResolveExpressionException(message);
        }
    }
}
