package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.prepopulate;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.fhir.cr.questionnaire.r4.processor.utils.ExtensionBuilders;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public class PrePopulateService {
    protected static final Logger logger = LoggerFactory.getLogger(PrePopulateService.class);
    final PopulateItemWithNoContext myPopulateItemWithNoContext;
    final PopulateItemWithContext myPopulateItemWithContext;
    OperationOutcome myOperationOutcome;

    public PrePopulateService() {
        this.myPopulateItemWithNoContext = new PopulateItemWithNoContext();
        this.myPopulateItemWithContext = new PopulateItemWithContext();
    }

    public Questionnaire prePopulate(PrePopulateRequest thePrePopulateRequest) {
        final String questionnaireId = getQuestionnaireId(thePrePopulateRequest);
        this.myOperationOutcome = getBaseOperationOutcome(questionnaireId);

        final Questionnaire prepopulatedQuestionnaire = thePrePopulateRequest.getQuestionnaire().copy();

        prepopulatedQuestionnaire.setId(questionnaireId);
        prepopulatedQuestionnaire.addExtension(ExtensionBuilders.prepopulateSubjectExtension(thePrePopulateRequest.getPatientId()));

        final List<QuestionnaireItemComponent> processedItems = processItems(thePrePopulateRequest, thePrePopulateRequest.getQuestionnaire().getItem());
        prepopulatedQuestionnaire.setItem(processedItems);

        if (!myOperationOutcome.getIssue().isEmpty()) {
            prepopulatedQuestionnaire.addContained(myOperationOutcome);
            prepopulatedQuestionnaire.addExtension(ExtensionBuilders.crmiMessagesExtension(myOperationOutcome.getIdPart()));
        }
        return prepopulatedQuestionnaire;
    }

    protected List<QuestionnaireItemComponent> processItems(
        PrePopulateRequest thePrePopulateRequest,
        List<QuestionnaireItemComponent> questionnaireItems
    ) {
        List<QuestionnaireItemComponent> populatedItems = new ArrayList<>();
        questionnaireItems.forEach(item -> {
            if (item.hasExtension(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT)) {
                populatedItems.addAll(processItemWithPopulationContext(thePrePopulateRequest, item));
            } else {
                QuestionnaireItemComponent populatedItem = item.copy();
                if (item.hasItem()) {
                    final List<QuestionnaireItemComponent> processedSubItems = processItems(thePrePopulateRequest, item.getItem());
                    populatedItem.setItem(processedSubItems);
                } else {
                    populatedItems.add(processItemWithNoPopulationContext(thePrePopulateRequest, populatedItem));
                }
            }
        });
        return populatedItems;
    }

    protected List<QuestionnaireItemComponent> processItemWithPopulationContext(
        PrePopulateRequest thePrePopulateRequest,
        QuestionnaireItemComponent theItem
    ) {
        try {
            // extension value is the context resource we're using to populate
            // Expression-based Population
            return myPopulateItemWithContext.processItem(thePrePopulateRequest, theItem);
        } catch (ResolveExpressionException e) {
            // would return empty list if exception thrown
            addExceptionToOperationOutcome(e.getMessage());
            return new ArrayList<>();
        }
    }

    protected QuestionnaireItemComponent processItemWithNoPopulationContext(
        PrePopulateRequest thePrePopulateRequest,
        QuestionnaireItemComponent theItem
    ) {
        try {
            return myPopulateItemWithNoContext.processItem(thePrePopulateRequest, theItem);
        } catch (ResolveExpressionException e) {
            // would return just the item.copy if exception thrown
            addExceptionToOperationOutcome(e.getMessage());
            return theItem.copy();
        }
    }

    protected void addExceptionToOperationOutcome(String theExceptionMessage) {
        logger.error(theExceptionMessage);
        myOperationOutcome.addIssue()
            .setCode(OperationOutcome.IssueType.EXCEPTION)
            .setSeverity(OperationOutcome.IssueSeverity.ERROR)
            .setDiagnostics(theExceptionMessage);
    }

    protected OperationOutcome getBaseOperationOutcome(String theQuestionnaireId) {
        OperationOutcome operationOutcome = new OperationOutcome();
        operationOutcome.setId("populate-outcome-" + theQuestionnaireId);
        return operationOutcome;
    }

    protected String getQuestionnaireId(PrePopulateRequest thePrePopulateRequest) {
        return thePrePopulateRequest.getQuestionnaire().getIdPart() + "-" + thePrePopulateRequest.getPatientId();
    }
}
