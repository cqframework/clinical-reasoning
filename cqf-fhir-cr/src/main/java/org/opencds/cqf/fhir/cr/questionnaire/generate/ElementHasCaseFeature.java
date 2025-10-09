package org.opencds.cqf.fhir.cr.questionnaire.generate;

import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.opencds.cqf.fhir.cr.questionnaire.Helpers;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;

public class ElementHasCaseFeature {
    public void addProperties(
            GenerateRequest request,
            CqfExpression caseFeature,
            IStructureDefinitionAdapter profile,
            IElementDefinitionAdapter element,
            IQuestionnaireItemComponentAdapter questionnaireItem) {
        var elementId = element.getId();
        var elementIdentifiers = element.getPath().split("\\.");
        var path = elementId.substring(elementId.indexOf(".")).replaceAll("\\[[^\\[]*]", "");
        var sliceName = Helpers.getSliceName(elementId);
        if (StringUtils.isNotBlank(sliceName)) {
            var sliceElements = profile.getSliceElements(sliceName);
            if (!sliceElements.isEmpty()) {
                var filter = new StringBuilder(".where(");
                sliceElements.stream()
                        .filter(e -> !e.getId().equals(elementId) && e.hasDefaultOrFixedOrPattern())
                        .forEach(slice -> {
                            var filterIdentifiers = new ArrayList<String>();
                            var sliceIdentifiers = slice.getPath().split("\\.");
                            for (int i = 0; i < elementIdentifiers.length; i++) {
                                if (!elementIdentifiers[i].equals(sliceIdentifiers[i])) {
                                    filterIdentifiers.add(sliceIdentifiers[i]);
                                }
                            }
                            var filterPath = StringUtils.join(filterIdentifiers, ".");
                            var filterValue = slice.getDefaultOrFixedOrPattern();

                            filter.append("%s = '%s'"
                                    .formatted(
                                            filterPath,
                                            request.getFhirContext()
                                                    .newJsonParser()
                                                    .encodeToString(filterValue)));
                        });
                filter.append(").single()");
                path = path.replace(":" + sliceName, filter);
            }
        }
        var expression = "%" + "%s%s".formatted(caseFeature.getName(), path);
        var expressionType = questionnaireItem.newExpression(expression);
        var initialExpressionExt = questionnaireItem.addExtension();
        initialExpressionExt.setUrl(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);
        initialExpressionExt.setValue(expressionType);
    }
}
