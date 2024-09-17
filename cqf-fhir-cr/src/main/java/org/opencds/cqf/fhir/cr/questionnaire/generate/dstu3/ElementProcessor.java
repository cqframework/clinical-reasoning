package org.opencds.cqf.fhir.cr.questionnaire.generate.dstu3;

import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.questionnaire.generate.ElementHasCaseFeature;
import org.opencds.cqf.fhir.cr.questionnaire.generate.ElementHasDefaultValue;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.generate.IElementProcessor;
import org.opencds.cqf.fhir.utility.CqfExpression;

public class ElementProcessor implements IElementProcessor {
    protected static final String ITEM_TYPE_ERROR = "Unable to determine type for element: %s";
    protected final QuestionnaireTypeIsChoice questionnaireTypeIsChoice;
    protected final ElementHasDefaultValue elementHasDefaultValue;
    protected final ElementHasCaseFeature elementHasCaseFeature;

    public ElementProcessor(Repository repository) {
        questionnaireTypeIsChoice = new QuestionnaireTypeIsChoice(repository);
        elementHasDefaultValue = new ElementHasDefaultValue();
        elementHasCaseFeature = new ElementHasCaseFeature();
    }

    @Override
    public IBaseBackboneElement processElement(
            GenerateRequest request,
            ICompositeType baseElement,
            String elementType,
            String childLinkId,
            CqfExpression caseFeature,
            Boolean isGroup) {
        var element = (ElementDefinition) baseElement;
        final var itemType = isGroup ? QuestionnaireItemType.GROUP : parseItemType(elementType, element.hasBinding());
        final var item = initializeQuestionnaireItem(
                itemType, request.getProfileAdapter().getUrl(), element, childLinkId);
        item.setRequired(element.hasMin() && element.getMin() > 0);
        item.setRepeats(element.hasMax() && !element.getMax().equals("1"));
        if (itemType == QuestionnaireItemType.GROUP) {
            return item;
        }
        if (itemType == QuestionnaireItemType.CHOICE) {
            questionnaireTypeIsChoice.addProperties(element, item);
        }
        if (element.hasFixed()) {
            elementHasDefaultValue.addProperties(request, element.getFixed(), item);
        } else if (element.hasPattern()) {
            elementHasDefaultValue.addProperties(request, element.getPattern(), item);
        } else if (element.hasDefaultValue()) {
            elementHasDefaultValue.addProperties(request, element.getDefaultValue(), item);
        } else if (caseFeature != null) {
            elementHasCaseFeature.addProperties(request, caseFeature, baseElement, item);
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
                return QuestionnaireItemType.URL;
            case "BackboneElement":
                return QuestionnaireItemType.GROUP;
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

    public String getElementText(ElementDefinition element) {
        return element.hasLabel() ? element.getLabel() : getElementDescription(element);
    }

    protected String getElementDescription(ElementDefinition element) {
        return element.hasShort() ? element.getShort() : element.getPath();
    }
}
