package org.opencds.cqf.fhir.cr.questionnaire.dstu3.processor.prepopulate;

import static ca.uhn.fhir.util.ExtensionUtil.getExtensionByUrl;

import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.utility.Constants;

class PrePopulateHelper {
    PrePopulateHelper() {}

    CqfExpression getExpressionByExtension(
            Questionnaire questionnaire, QuestionnaireItemComponent item, String extensionUrl) {
        final Extension extension = item.getExtensionByUrl(extensionUrl);
        final IBaseExtension<?, ?> languageExtension = getExtensionByUrl(item, Constants.CQF_EXPRESSION_LANGUAGE);
        return new CqfExpression(
                languageExtension.getValue().toString(), extension.getValue().toString(), getLibraryUrl(questionnaire));
    }

    private String getLibraryUrl(Questionnaire questionnaire) {
        return questionnaire.hasExtension(Constants.CQF_LIBRARY)
                ? ((UriType) questionnaire
                                .getExtensionByUrl(Constants.CQF_LIBRARY)
                                .getValue())
                        .getValue()
                : null;
    }
}
