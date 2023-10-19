package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils.ExtensionBuilders;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrePopulateService {
    protected static final Logger logger = LoggerFactory.getLogger(PrePopulateService.class);
    private final PrePopulateItem prePopulateItem;
    private final PrePopulateItemWithExtension prePopulateItemWithExtension;
    OperationOutcome operationOutcome;

    public PrePopulateService() {
        this(new PrePopulateItem(), new PrePopulateItemWithExtension());
    }

    private PrePopulateService(
        PrePopulateItem prePopulateItem,
        PrePopulateItemWithExtension prePopulateItemWithExtension
    ) {
        this.prePopulateItem = prePopulateItem;
        this.prePopulateItemWithExtension = prePopulateItemWithExtension;
    }

    public Optional<OperationOutcome> getOperationOutcome() {
        return Optional.ofNullable(operationOutcome);
    }

    public Questionnaire prePopulate(PrePopulateRequest prePopulateRequest) {
        final String questionnaireId = getQuestionnaireId(prePopulateRequest);
        this.operationOutcome = getBaseOperationOutcome(questionnaireId);
        final Questionnaire prepopulatedQuestionnaire =
                prePopulateRequest.getQuestionnaire().copy();
        prepopulatedQuestionnaire.setId(questionnaireId);
        prepopulatedQuestionnaire.addExtension(
                ExtensionBuilders.prepopulateSubjectExtension(prePopulateRequest.getPatientId()));
        final List<QuestionnaireItemComponent> processedItems = processItems(
                prePopulateRequest, prePopulateRequest.getQuestionnaire().getItem());
        prepopulatedQuestionnaire.setItem(processedItems);
        if (!operationOutcome.getIssue().isEmpty()) {
            prepopulatedQuestionnaire.addContained(operationOutcome);
            prepopulatedQuestionnaire.addExtension(
                    ExtensionBuilders.crmiMessagesExtension(operationOutcome.getIdPart()));
        }
        return prepopulatedQuestionnaire;
    }

    List<QuestionnaireItemComponent> processItems(
            PrePopulateRequest prePopulateRequest, List<QuestionnaireItemComponent> questionnaireItems) {
        final List<QuestionnaireItemComponent> populatedItems = new ArrayList<>();
        questionnaireItems.forEach(item -> {
            if (item.hasExtension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT)) {
                populatedItems.addAll(prePopulateItemWithExtension(prePopulateRequest, item));
            } else {
                final QuestionnaireItemComponent populatedItem = copyQuestionnaireItem(item);
                if (item.hasItem()) {
                    final List<QuestionnaireItemComponent> processedSubItems =
                            processItems(prePopulateRequest, item.getItem());
                    populatedItem.setItem(processedSubItems);
                    populatedItems.add(populatedItem);
                } else {
                    populatedItems.add(prePopulateItem(prePopulateRequest, populatedItem));
                }
            }
        });
        return populatedItems;
    }

    List<QuestionnaireItemComponent> prePopulateItemWithExtension(
            PrePopulateRequest prePopulateRequest, QuestionnaireItemComponent questionnaireItem) {
        try {
            // extension value is the context resource we're using to populate
            // Expression-based Population
            return prePopulateItemWithExtension.processItem(prePopulateRequest, questionnaireItem);
        } catch (ResolveExpressionException e) {
            // would return empty list if exception thrown
            addExceptionToOperationOutcome(e.getMessage());
            return List.of();
        }
    }

    QuestionnaireItemComponent prePopulateItem(
            PrePopulateRequest prePopulateRequest, QuestionnaireItemComponent questionnaireItem) {
        try {
            return prePopulateItem.processItem(prePopulateRequest, questionnaireItem);
        } catch (ResolveExpressionException e) {
            // would return just the item.copy if exception thrown
            addExceptionToOperationOutcome(e.getMessage());
            return questionnaireItem.copy();
        }
    }

    void addExceptionToOperationOutcome(String exceptionMessage) {
        logger.error(exceptionMessage);
        operationOutcome
                .addIssue()
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setDiagnostics(exceptionMessage);
    }

    OperationOutcome getBaseOperationOutcome(String questionnaireId) {
        OperationOutcome baseOperationOutcome = new OperationOutcome();
        baseOperationOutcome.setId("populate-outcome-" + questionnaireId);
        return baseOperationOutcome;
    }

    String getQuestionnaireId(PrePopulateRequest prePopulateRequest) {
        return prePopulateRequest.getQuestionnaire().getIdPart() + "-" + prePopulateRequest.getPatientId();
    }

    QuestionnaireItemComponent copyQuestionnaireItem(QuestionnaireItemComponent questionnaireItem) {
        return questionnaireItem.copy();
    }
}
