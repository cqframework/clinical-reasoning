package org.opencds.cqf.fhir.cr.questionnaire.generate;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.fhir.cr.questionnaire.Helpers;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;

public class ElementHasCaseFeature {
    public IBaseBackboneElement addProperties(
            GenerateRequest request,
            CqfExpression caseFeature,
            IStructureDefinitionAdapter profile,
            IElementDefinitionAdapter element,
            IBaseBackboneElement questionnaireItem) {
        var elementId = element.getId();
        var elementIdentifiers = element.getPath().split("\\.");
        var path = elementId.substring(elementId.indexOf(".")).replaceAll("\\[[^\\[]*\\]", "");
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
        var expressionType = createExpression(request.getFhirVersion(), expression, null);
        if (expressionType != null) {
            var initialExpressionExt = questionnaireItem.addExtension();
            initialExpressionExt.setUrl(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);
            initialExpressionExt.setValue(expressionType);
        }
        return questionnaireItem;
    }

    private ICompositeType createExpression(FhirVersionEnum fhirVersion, String expression, String name) {
        return switch (fhirVersion) {
            case R4 -> new org.hl7.fhir.r4.model.Expression()
                    .setLanguage("text/cql-expression")
                    .setExpression(expression)
                    .setName(name);
            case R5 -> new org.hl7.fhir.r5.model.Expression()
                    .setLanguage("text/cql-expression")
                    .setExpression(expression)
                    .setName(name);
            default -> null;
        };
    }
}
