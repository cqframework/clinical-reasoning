package org.opencds.cqf.fhir.cr.questionnaire.dstu3.generator.questionnaireitem;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaire.dstu3.generator.nestedquestionnaireitem.NestedQuestionnaireItemService;
import org.opencds.cqf.fhir.utility.dstu3.SearchHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionnaireItemGenerator {
    protected static final Logger logger = LoggerFactory.getLogger(QuestionnaireItemGenerator.class);
    protected static final String NO_PROFILE_ERROR = "No profile defined for input. Unable to generate item.";
    protected static final String ITEM_CREATION_ERROR = "An error occurred during item creation: %s";
    protected static final String CHILD_LINK_ID_FORMAT = "%s.%s";
    protected final Repository repository;
    protected final LibraryEngine libraryEngine;
    protected final String patientId;
    protected final IBaseBundle bundle;
    protected final IBaseParameters parameters;
    protected final QuestionnaireItemService questionnaireItemService;
    protected final NestedQuestionnaireItemService nestedQuestionnaireItemService;
    protected QuestionnaireItemComponent questionnaireItem;

    public static QuestionnaireItemGenerator of(
            Repository repository,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine) {
        QuestionnaireItemService questionnaireItemService = new QuestionnaireItemService();
        NestedQuestionnaireItemService nestedQuestionnaireItemService = NestedQuestionnaireItemService.of(repository);
        return new QuestionnaireItemGenerator(
                repository,
                libraryEngine,
                patientId,
                parameters,
                bundle,
                questionnaireItemService,
                nestedQuestionnaireItemService);
    }

    QuestionnaireItemGenerator(
            Repository repository,
            LibraryEngine libraryEngine,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            QuestionnaireItemService questionnaireItemService,
            NestedQuestionnaireItemService nestedQuestionnaireItemService) {
        this.repository = repository;
        this.libraryEngine = libraryEngine;
        this.patientId = patientId;
        this.parameters = parameters;
        this.bundle = bundle;
        this.questionnaireItemService = questionnaireItemService;
        this.nestedQuestionnaireItemService = nestedQuestionnaireItemService;
    }

    public Questionnaire.QuestionnaireItemComponent generateItem(DataRequirement actionInput, int itemCount) {
        if (!actionInput.hasProfile()) {
            throw new IllegalArgumentException(NO_PROFILE_ERROR);
        }
        final String linkId = String.valueOf(itemCount + 1);
        try {
            final StructureDefinition profile = (StructureDefinition) getProfileDefinition(actionInput);
            this.questionnaireItem = questionnaireItemService.createQuestionnaireItem(actionInput, linkId, profile);
            processElements(profile);
        } catch (Exception ex) {
            final String message = String.format(ITEM_CREATION_ERROR, ex.getMessage());
            logger.error(message);
            this.questionnaireItem = createErrorItem(linkId, message);
        }
        return questionnaireItem;
    }

    protected void processElements(StructureDefinition profile) {
        int childCount = questionnaireItem.getItem().size();
        for (ElementDefinition element : getElementsWithNonNullElementType(profile)) {
            childCount++;
            processElement(profile, element, childCount);
        }
    }

    protected void processElement(StructureDefinition profile, ElementDefinition element, int childCount) {
        final String childLinkId = String.format(CHILD_LINK_ID_FORMAT, questionnaireItem.getLinkId(), childCount);
        try {
            final QuestionnaireItemComponent nestedQuestionnaireItem =
                    nestedQuestionnaireItemService.getNestedQuestionnaireItem(profile.getUrl(), element, childLinkId);
            questionnaireItem.addItem(nestedQuestionnaireItem);
        } catch (Exception ex) {
            final String message = String.format(ITEM_CREATION_ERROR, ex.getMessage());
            logger.warn(message);
            questionnaireItem.addItem(createErrorItem(childLinkId, message));
        }
    }

    protected List<ElementDefinition> getElementsWithNonNullElementType(StructureDefinition profile) {
        final List<ElementDefinition> elements = profile.getDifferential().getElement();
        return elements.stream()
                .filter(element -> getElementType(element) != null)
                .collect(Collectors.toList());
    }

    protected Questionnaire.QuestionnaireItemComponent createErrorItem(String linkId, String errorMessage) {
        return new QuestionnaireItemComponent()
                .setLinkId(linkId)
                .setType(QuestionnaireItemType.DISPLAY)
                .setText(errorMessage);
    }

    protected String getElementType(ElementDefinition element) {
        return element.hasType() ? element.getType().get(0).getCode() : null;
    }

    protected Resource getProfileDefinition(DataRequirement actionInput) {
        return SearchHelper.searchRepositoryByCanonical(
                repository, actionInput.getProfile().get(0));
    }
}