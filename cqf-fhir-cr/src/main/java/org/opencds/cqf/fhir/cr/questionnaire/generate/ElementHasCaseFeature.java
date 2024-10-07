package org.opencds.cqf.fhir.cr.questionnaire.generate;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;

public class ElementHasCaseFeature {
    public IBaseBackboneElement addProperties(
            GenerateRequest request,
            CqfExpression caseFeature,
            ICompositeType element,
            IBaseBackboneElement questionnaireItem) {
        var elementPath = request.resolvePathString(element, "path");
        var path = elementPath.substring(elementPath.indexOf(".")).replaceAll("\\[[^\\[]*\\]", "");
        var expression = caseFeature.getName();
        var expressionType = createExpression(
                request.getFhirVersion(),
                "%" + String.format("%s%s", expression, path),
                caseFeature.getLibraryUrl(),
                null);
        if (expressionType != null) {
            var initialExpressionExt = questionnaireItem.addExtension();
            initialExpressionExt.setUrl(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);
            initialExpressionExt.setValue(expressionType);
        }
        return questionnaireItem;
    }

    private ICompositeType createExpression(
            FhirVersionEnum fhirVersion, String expression, String libraryRef, String name) {
        switch (fhirVersion) {
            case R4:
                var r4Expression = new org.hl7.fhir.r4.model.Expression()
                        .setLanguage("text/cql-expression")
                        .setExpression(expression)
                        .setName(name);
                if (StringUtils.isNotBlank(libraryRef)) {
                    r4Expression.setReference(libraryRef);
                }
                return r4Expression;
            case R5:
                var r5Expression = new org.hl7.fhir.r5.model.Expression()
                        .setLanguage("text/cql-expression")
                        .setExpression(expression)
                        .setName(name);
                if (StringUtils.isNotBlank(libraryRef)) {
                    r5Expression.setReference(libraryRef);
                }
                return r5Expression;

            default:
                return null;
        }
    }
}
