package org.opencds.cqf.fhir.cr.questionnaire.dstu3.generator.nestedquestionnaireitem;

import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NestedQuestionnaireItemService {
    protected final ModelResolver modelResolver;
    protected final QuestionnaireTypeIsChoice questionnaireTypeIsChoice;
    protected final ElementHasDefaultValue elementHasDefaultValue;
    protected static final String ITEM_TYPE_ERROR = "Unable to determine type for element: %s";
    protected static final Logger logger = LoggerFactory.getLogger(NestedQuestionnaireItemService.class);

    public static NestedQuestionnaireItemService of(Repository repository) {
        final QuestionnaireTypeIsChoice questionnaireTypeIsChoice = QuestionnaireTypeIsChoice.of(repository);
        final ElementHasDefaultValue elementHasDefault = new ElementHasDefaultValue();
        return new NestedQuestionnaireItemService(
                questionnaireTypeIsChoice,
                elementHasDefault,
                FhirModelResolverCache.resolverForVersion(
                        repository.fhirContext().getVersion().getVersion()));
    }

    NestedQuestionnaireItemService(
            QuestionnaireTypeIsChoice questionnaireTypeIsChoice,
            ElementHasDefaultValue elementHasDefaultValue,
            ModelResolver modelResolver) {
        this.questionnaireTypeIsChoice = questionnaireTypeIsChoice;
        this.elementHasDefaultValue = elementHasDefaultValue;
        this.modelResolver = modelResolver;
    }

    public QuestionnaireItemComponent getNestedQuestionnaireItem(
            String profileUrl, ElementDefinition element, String childLinkId) {
        final QuestionnaireItemType itemType = getItemType(element);
        final QuestionnaireItemComponent item = initializeQuestionnaireItem(itemType, profileUrl, element, childLinkId);
        if (itemType == QuestionnaireItemType.CHOICE) {
            questionnaireTypeIsChoice.addProperties(element, item);
        }
        if (element.hasFixed()) {
            elementHasDefaultValue.addProperties(element.getFixed(), item);
        } else if (element.hasPattern()) {
            elementHasDefaultValue.addProperties(element.getPattern(), item);
        } else if (element.hasDefaultValue()) {
            elementHasDefaultValue.addProperties(element.getDefaultValue(), item);
        }
        item.setRequired(element.hasMin() && element.getMin() == 1);
        // set repeat based on? if expression result type is a list?
        // set enableWhen based on? use
        // http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-enableWhenExpression
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

    public QuestionnaireItemType getItemType(ElementDefinition element) {
        final String elementType = element.getType().get(0).getCode();
        final QuestionnaireItemType itemType = parseItemType(elementType, element.hasBinding());
        if (itemType == null) {
            final String message = String.format(ITEM_TYPE_ERROR, element.getId());
            throw new IllegalArgumentException(message);
        }
        return itemType;
    }

    public QuestionnaireItemType parseItemType(String elementType, Boolean hasBinding) {
        if (Boolean.TRUE.equals(hasBinding)) {
            return QuestionnaireItemType.CHOICE;
        }
        switch (elementType) {
            case "CodeableConcept":
                return QuestionnaireItemType.CHOICE;
            case "uri":
                return QuestionnaireItemType.URL;
            case "BackboneElement":
                return QuestionnaireItemType.GROUP;
            case "Quantity":
                return QuestionnaireItemType.QUANTITY;
            case "Reference":
                return QuestionnaireItemType.REFERENCE;
            case "code":
                return QuestionnaireItemType.STRING;
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
