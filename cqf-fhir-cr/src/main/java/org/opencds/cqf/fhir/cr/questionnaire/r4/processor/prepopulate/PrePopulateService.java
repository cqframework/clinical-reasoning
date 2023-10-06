package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils.ExtensionBuilders;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrePopulateService {
    protected static final Logger logger = LoggerFactory.getLogger(PrePopulateService.class);
    final PrePopulateItem myPrePopulateItem;
    final PrePopulateItemWithExtension myPrePopulateItemWithExtension;
    OperationOutcome myOperationOutcome;

    public static PrePopulateService of() {
        final PrePopulateItem prePopulateItem = PrePopulateItem.of();
        final PrePopulateItemWithExtension prePopulateItemWithExtension = PrePopulateItemWithExtension.of();
        return new PrePopulateService(prePopulateItem, prePopulateItemWithExtension);
    }

    private PrePopulateService(
            PrePopulateItem thePrePopulateItem, PrePopulateItemWithExtension thePrePopulateItemWithExtension) {
        myPrePopulateItem = thePrePopulateItem;
        myPrePopulateItemWithExtension = thePrePopulateItemWithExtension;
    }

    public Questionnaire prePopulate(PrePopulateRequest thePrePopulateRequest) {
        final String questionnaireId = getQuestionnaireId(thePrePopulateRequest);
        this.myOperationOutcome = getBaseOperationOutcome(questionnaireId);
        final Questionnaire prepopulatedQuestionnaire =
                thePrePopulateRequest.getQuestionnaire().copy();
        prepopulatedQuestionnaire.setId(questionnaireId);
        prepopulatedQuestionnaire.addExtension(
                ExtensionBuilders.prepopulateSubjectExtension(thePrePopulateRequest.getPatientId()));
        final List<QuestionnaireItemComponent> processedItems = processItems(
                thePrePopulateRequest, thePrePopulateRequest.getQuestionnaire().getItem());
        prepopulatedQuestionnaire.setItem(processedItems);
        if (!myOperationOutcome.getIssue().isEmpty()) {
            prepopulatedQuestionnaire.addContained(myOperationOutcome);
            prepopulatedQuestionnaire.addExtension(
                    ExtensionBuilders.crmiMessagesExtension(myOperationOutcome.getIdPart()));
        }
        return prepopulatedQuestionnaire;
    }

    List<QuestionnaireItemComponent> processItems(
            PrePopulateRequest thePrePopulateRequest, List<QuestionnaireItemComponent> questionnaireItems) {
        final List<QuestionnaireItemComponent> populatedItems = new ArrayList<>();
        questionnaireItems.forEach(item -> {
            if (item.hasExtension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT)) {
                populatedItems.addAll(prePopulateItemWithExtension(thePrePopulateRequest, item));
            } else {
                final QuestionnaireItemComponent populatedItem = copyQuestionnaireItem(item);
                if (item.hasItem()) {
                    final List<QuestionnaireItemComponent> processedSubItems =
                            processItems(thePrePopulateRequest, item.getItem());
                    populatedItem.setItem(processedSubItems);
                    populatedItems.add(populatedItem);
                } else {
                    populatedItems.add(prePopulateItem(thePrePopulateRequest, populatedItem));
                }
            }
        });
        return populatedItems;
    }

    List<QuestionnaireItemComponent> prePopulateItemWithExtension(
            PrePopulateRequest thePrePopulateRequest, QuestionnaireItemComponent theItem) {
        try {
            // extension value is the context resource we're using to populate
            // Expression-based Population
            return myPrePopulateItemWithExtension.processItem(thePrePopulateRequest, theItem);
        } catch (ResolveExpressionException e) {
            // would return empty list if exception thrown
            addExceptionToOperationOutcome(e.getMessage());
            return new ArrayList<>();
        }
    }

    QuestionnaireItemComponent prePopulateItem(
            PrePopulateRequest thePrePopulateRequest, QuestionnaireItemComponent theItem) {
        try {
            return myPrePopulateItem.processItem(thePrePopulateRequest, theItem);
        } catch (ResolveExpressionException e) {
            // would return just the item.copy if exception thrown
            addExceptionToOperationOutcome(e.getMessage());
            return theItem.copy();
        }
    }

    void addExceptionToOperationOutcome(String theExceptionMessage) {
        logger.error(theExceptionMessage);
        myOperationOutcome
                .addIssue()
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setDiagnostics(theExceptionMessage);
    }

    OperationOutcome getBaseOperationOutcome(String theQuestionnaireId) {
        OperationOutcome operationOutcome = new OperationOutcome();
        operationOutcome.setId("populate-outcome-" + theQuestionnaireId);
        return operationOutcome;
    }

    String getQuestionnaireId(PrePopulateRequest thePrePopulateRequest) {
        return thePrePopulateRequest.getQuestionnaire().getIdPart() + "-" + thePrePopulateRequest.getPatientId();
    }

    QuestionnaireItemComponent copyQuestionnaireItem(QuestionnaireItemComponent theQuestionnaireItem) {
        return theQuestionnaireItem.copy();
    }
}
