package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import static org.opencds.cqf.fhir.cr.questionnaire.common.ExtensionBuilders.buildR4;
import static org.opencds.cqf.fhir.cr.questionnaire.common.ExtensionBuilders.crmiMessagesExtension;
import static org.opencds.cqf.fhir.cr.questionnaire.common.ExtensionBuilders.prepopulateSubjectExtension;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.fhir.cr.questionnaire.common.PrePopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrePopulateProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(PrePopulateProcessor.class);
    private final PrePopulateItem prePopulateItem;
    private final PrePopulateItemWithExtension prePopulateItemWithExtension;
    OperationOutcome operationOutcome;

    public PrePopulateProcessor() {
        this(new PrePopulateItem(), new PrePopulateItemWithExtension());
    }

    private PrePopulateProcessor(
            PrePopulateItem prePopulateItem, PrePopulateItemWithExtension prePopulateItemWithExtension) {
        this.prePopulateItem = prePopulateItem;
        this.prePopulateItemWithExtension = prePopulateItemWithExtension;
    }

    public Questionnaire prePopulate(Questionnaire questionnaire, PrePopulateRequest prePopulateRequest) {
        final String questionnaireId = getQuestionnaireId(prePopulateRequest, questionnaire);
        this.operationOutcome = getBaseOperationOutcome(questionnaireId);
        final Questionnaire prepopulatedQuestionnaire = questionnaire.copy();
        prepopulatedQuestionnaire.setId(questionnaireId);
        prepopulatedQuestionnaire.addExtension(
                buildR4(prepopulateSubjectExtension(FHIRAllTypes.PATIENT.toCode(), prePopulateRequest.getPatientId())));
        final List<QuestionnaireItemComponent> processedItems =
                processItems(prePopulateRequest, questionnaire.getItem(), questionnaire);
        prepopulatedQuestionnaire.setItem(processedItems);
        if (!operationOutcome.getIssue().isEmpty()) {
            prepopulatedQuestionnaire.addContained(operationOutcome);
            prepopulatedQuestionnaire.addExtension(buildR4(crmiMessagesExtension(operationOutcome.getIdPart())));
        }
        return prepopulatedQuestionnaire;
    }

    List<QuestionnaireItemComponent> processItems(
            PrePopulateRequest prePopulateRequest,
            List<QuestionnaireItemComponent> questionnaireItems,
            Questionnaire questionnaire) {
        final List<QuestionnaireItemComponent> populatedItems = new ArrayList<>();
        questionnaireItems.forEach(item -> {
            if (item.hasExtension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT)) {
                populatedItems.addAll(prePopulateItemWithExtension(prePopulateRequest, item, questionnaire));
            } else {
                final QuestionnaireItemComponent populatedItem = copyQuestionnaireItem(item);
                if (item.hasItem()) {
                    final List<QuestionnaireItemComponent> processedSubItems =
                            processItems(prePopulateRequest, item.getItem(), questionnaire);
                    populatedItem.setItem(processedSubItems);
                    populatedItems.add(populatedItem);
                } else {
                    populatedItems.add(prePopulateItem(prePopulateRequest, populatedItem, questionnaire));
                }
            }
        });
        return populatedItems;
    }

    List<QuestionnaireItemComponent> prePopulateItemWithExtension(
            PrePopulateRequest prePopulateRequest,
            QuestionnaireItemComponent questionnaireItem,
            Questionnaire questionnaire) {
        try {
            // extension value is the context resource we're using to populate
            // Expression-based Population
            return prePopulateItemWithExtension.processItem(prePopulateRequest, questionnaireItem, questionnaire);
        } catch (ResolveExpressionException e) {
            // would return empty list if exception thrown
            addExceptionToOperationOutcome(e.getMessage());
            return List.of();
        }
    }

    QuestionnaireItemComponent prePopulateItem(
            PrePopulateRequest prePopulateRequest,
            QuestionnaireItemComponent questionnaireItem,
            Questionnaire questionnaire) {
        try {
            return prePopulateItem.processItem(prePopulateRequest, questionnaireItem, questionnaire);
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

    String getQuestionnaireId(PrePopulateRequest prePopulateRequest, Questionnaire questionnaire) {
        return questionnaire.getIdPart() + "-" + prePopulateRequest.getPatientId();
    }

    QuestionnaireItemComponent copyQuestionnaireItem(QuestionnaireItemComponent questionnaireItem) {
        return questionnaireItem.copy();
    }
}
