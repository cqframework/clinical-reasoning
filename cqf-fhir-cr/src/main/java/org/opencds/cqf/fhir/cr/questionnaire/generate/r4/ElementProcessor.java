package org.opencds.cqf.fhir.cr.questionnaire.generate.r4;

import static org.opencds.cqf.fhir.cr.common.ItemValueTransformer.transformValueToItem;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.questionnaire.generate.ElementHasCaseFeature;
import org.opencds.cqf.fhir.cr.questionnaire.generate.ElementHasCqfExpression;
import org.opencds.cqf.fhir.cr.questionnaire.generate.ElementHasDefaultValue;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.generate.IElementProcessor;
import org.opencds.cqf.fhir.utility.Constants;

public class ElementProcessor implements IElementProcessor {
    protected static final String ITEM_TYPE_ERROR = "Unable to determine type for element: %s";
    protected final QuestionnaireTypeIsChoice questionnaireTypeIsChoice;
    protected final ElementHasDefaultValue elementHasDefaultValue;
    protected final ElementHasCqfExpression elementHasCqfExpression;
    protected final ElementHasCaseFeature elementHasCaseFeature;

    public ElementProcessor(Repository repository) {
        questionnaireTypeIsChoice = new QuestionnaireTypeIsChoice(repository);
        elementHasDefaultValue = new ElementHasDefaultValue();
        elementHasCqfExpression = new ElementHasCqfExpression();
        elementHasCaseFeature = new ElementHasCaseFeature();
    }

    @Override
    public IBaseBackboneElement processElement(
            GenerateRequest request,
            ICompositeType baseElement,
            String elementType,
            String childLinkId,
            IBaseResource caseFeature,
            Boolean isGroup) {
        var element = (ElementDefinition) baseElement;
        final var itemType = isGroup ? QuestionnaireItemType.GROUP : parseItemType(elementType, element.hasBinding());
        final var item = initializeQuestionnaireItem(itemType, request.getProfileUrl(), element, childLinkId);
        item.setRequired(element.hasMin() && element.getMin() > 0);
        item.setRepeats(element.hasMax() && !element.getMax().equals("1"));
        // set enableWhen based on? use
        // http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-enableWhenExpression
        if (itemType == QuestionnaireItemType.GROUP) {
            return item;
        }
        if (itemType == QuestionnaireItemType.CHOICE) {
            questionnaireTypeIsChoice.addProperties(element, item);
        }
        if (element.hasFixedOrPattern()) {
            elementHasDefaultValue.addProperties(request, element.getFixedOrPattern(), item);
        } else if (element.hasDefaultValue()) {
            elementHasDefaultValue.addProperties(request, element.getDefaultValue(), item);
        } else if (element.hasExtension(Constants.CQF_EXPRESSION)) {
            elementHasCqfExpression.addProperties(request, request.getExtensions(element), item);
        } else if (caseFeature != null) {
            var pathValue = elementHasCaseFeature.getPathValue(request, caseFeature, element);
            if (pathValue instanceof Type) {
                item.addInitial().setValue(transformValueToItem((Type) pathValue));
            }
        }
        return item;
    }

    protected QuestionnaireItemComponent initializeQuestionnaireItem(
            QuestionnaireItemType itemType, String profileUrl, ElementDefinition element, String childLinkId) {
        final String definition = profileUrl + "#" + element.getPath();
        return new QuestionnaireItemComponent()
                .setType(itemType)
                .setDefinition(definition)
                .setLinkId(childLinkId)
                .setText(getElementText(element));
    }

    public QuestionnaireItemType parseItemType(String elementType, Boolean hasBinding) {
        if (Boolean.TRUE.equals(hasBinding)) {
            return QuestionnaireItemType.CHOICE;
        }
        if (elementType == null) {
            throw new IllegalArgumentException("Unable to determine item type.");
        }
        switch (elementType) {
            case "code":
            case "coding":
            case "CodeableConcept":
                return QuestionnaireItemType.CHOICE;
            case "uri":
            case "url":
            case "canonical":
                return QuestionnaireItemType.URL;
            case "Quantity":
                return QuestionnaireItemType.QUANTITY;
            case "Reference":
                return QuestionnaireItemType.REFERENCE;
            case "oid":
            case "uuid":
            case "base64Binary":
                return QuestionnaireItemType.STRING;
            case "positiveInt":
            case "unsignedInt":
                return QuestionnaireItemType.INTEGER;
            case "instant":
                return QuestionnaireItemType.DATETIME;
            default:
                return QuestionnaireItemType.fromCode(elementType);
        }
    }

    protected String getElementText(ElementDefinition element) {
        return element.hasLabel() ? element.getLabel() : getElementDescription(element);
    }

    protected String getElementDescription(ElementDefinition element) {
        return element.hasShort() ? element.getShort() : element.getPath();
    }
}
