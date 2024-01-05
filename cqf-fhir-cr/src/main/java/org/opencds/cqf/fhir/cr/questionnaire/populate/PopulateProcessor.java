package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.build;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.crmiMessagesExtension;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.dtrQuestionnaireResponseExtension;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.prepopulateSubjectExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PopulateProcessor implements IPopulateProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(PopulateProcessor.class);
    private final ProcessItem processItem;
    private final ProcessItemWithExtension processItemWithExtension;
    private final ProcessResponseItem processResponseItem;

    public PopulateProcessor() {
        this(new ProcessItem(), new ProcessItemWithExtension(), new ProcessResponseItem());
    }

    private PopulateProcessor(
            ProcessItem processItem,
            ProcessItemWithExtension processItemWithExtension,
            ProcessResponseItem processResponseItem) {
        this.processItem = processItem;
        this.processItemWithExtension = processItemWithExtension;
        this.processResponseItem = processResponseItem;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends IBaseResource> R prePopulate(PopulateRequest request) {
        final String questionnaireId = request.getQuestionnaire().getIdElement().getIdPart() + "-"
                + request.getSubjectId().getIdPart();
        final IBaseResource populatedQuestionnaire = SerializationUtils.clone(request.getQuestionnaire());
        request.getModelResolver().setValue(populatedQuestionnaire, "item", null);
        populatedQuestionnaire.setId(questionnaireId);
        request.getModelResolver()
                .setValue(
                        populatedQuestionnaire,
                        "extension",
                        Collections.singletonList(build(
                                request.getFhirVersion(),
                                prepopulateSubjectExtension(
                                        "Patient", request.getSubjectId().getIdPart()))));
        var items = request.getItems(request.getQuestionnaire());
        final List<IBaseBackboneElement> processedItems = processItems(request, items);
        request.getModelResolver().setValue(populatedQuestionnaire, "item", processedItems);
        resolveOperationOutcome(request, populatedQuestionnaire);
        return (R) populatedQuestionnaire;
    }

    @Override
    public IBaseResource populate(PopulateRequest request) {
        final IBaseResource response = createQuestionnaireResponse(request);
        response.setId(request.getQuestionnaire().getIdElement().getIdPart() + "-"
                + request.getSubjectId().getIdPart());
        var items = request.getItems(request.getQuestionnaire());
        var responseItems = processResponseItems(request, processItems(request, items));
        request.getModelResolver().setValue(response, "item", responseItems);
        resolveOperationOutcome(request, response);
        request.getModelResolver()
                .setValue(response, "contained", Collections.singletonList(request.getQuestionnaire()));
        request.getModelResolver()
                .setValue(
                        response,
                        "extension",
                        Collections.singletonList(build(
                                request.getFhirVersion(),
                                dtrQuestionnaireResponseExtension(request.getQuestionnaire()
                                        .getIdElement()
                                        .getIdPart()))));
        return response;
    }

    protected void resolveOperationOutcome(PopulateRequest request, IBaseResource resource) {
        var issues = request.resolvePathList(request.getOperationOutcome(), "issue");
        if (issues != null && !issues.isEmpty()) {
            request.getOperationOutcome()
                    .setId("populate-outcome-" + resource.getIdElement().getIdPart());
            request.getModelResolver()
                    .setValue(resource, "contained", Collections.singletonList(request.getOperationOutcome()));
            request.getModelResolver()
                    .setValue(
                            resource,
                            "extension",
                            Collections.singletonList(build(
                                    request.getFhirVersion(),
                                    crmiMessagesExtension(request.getOperationOutcome()
                                            .getIdElement()
                                            .getIdPart()))));
        }
    }

    protected List<IBaseBackboneElement> processItems(PopulateRequest request, List<IBaseBackboneElement> items) {
        final List<IBaseBackboneElement> populatedItems = new ArrayList<>();
        items.forEach(item -> {
            var populationContextExt = item.getExtension().stream()
                    .filter(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT))
                    .findFirst()
                    .orElse(null);
            if (populationContextExt != null) {
                populatedItems.addAll(processItemWithExtension(request, item));
            } else {
                final IBaseBackboneElement populatedItem = SerializationUtils.clone(item);
                request.getModelResolver().setValue(populatedItem, "item", null);
                var childItems = request.getItems(item);
                if (!childItems.isEmpty()) {
                    final var processedChildItems = processItems(request, childItems);
                    request.getModelResolver().setValue(populatedItem, "item", processedChildItems);
                    populatedItems.add(populatedItem);
                } else {
                    populatedItems.add(processItem(request, populatedItem));
                }
            }
        });
        return populatedItems;
    }

    protected List<IBaseBackboneElement> processItemWithExtension(PopulateRequest request, IBaseBackboneElement item) {
        try {
            // extension value is the context resource we're using to populate
            // Expression-based Population
            return processItemWithExtension.processItem(request, item);
        } catch (ResolveExpressionException e) {
            // would return empty list if exception thrown
            logger.error(e.getMessage());
            request.logException(e.getMessage());
            return new ArrayList<>();
        }
    }

    protected IBaseBackboneElement processItem(PopulateRequest request, IBaseBackboneElement item) {
        try {
            return processItem.processItem(request, item);
        } catch (ResolveExpressionException e) {
            // would return just the item.copy if exception thrown
            logger.error(e.getMessage());
            request.logException(e.getMessage());
            // return questionnaireItem.copy();
            return item;
        }
    }

    protected IBaseResource createQuestionnaireResponse(PopulateRequest request) {
        switch (request.getFhirVersion()) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.QuestionnaireResponse()
                        .setStatus(
                                org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS)
                        .setQuestionnaire(new org.hl7.fhir.dstu3.model.Reference(
                                ((org.hl7.fhir.dstu3.model.Questionnaire) request.getQuestionnaire()).getUrl()))
                        .setSubject(new org.hl7.fhir.dstu3.model.Reference(request.getSubjectId()));
            case R4:
                return new org.hl7.fhir.r4.model.QuestionnaireResponse()
                        .setStatus(org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS)
                        .setQuestionnaire(((org.hl7.fhir.r4.model.Questionnaire) request.getQuestionnaire()).getUrl())
                        .setSubject(new org.hl7.fhir.r4.model.Reference(request.getSubjectId()));
            case R5:
                return new org.hl7.fhir.r5.model.QuestionnaireResponse()
                        .setStatus(org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS)
                        .setQuestionnaire(((org.hl7.fhir.r5.model.Questionnaire) request.getQuestionnaire()).getUrl())
                        .setSubject(new org.hl7.fhir.r5.model.Reference(request.getSubjectId()));

            default:
                return null;
        }
    }

    protected List<IBaseBackboneElement> processResponseItems(
            PopulateRequest request, List<IBaseBackboneElement> items) {
        try {
            return processResponseItem.processItems(request, items);
        } catch (Exception e) {
            logger.error(e.getMessage());
            request.logException(e.getMessage());
            return new ArrayList<>();
        }
    }
}
