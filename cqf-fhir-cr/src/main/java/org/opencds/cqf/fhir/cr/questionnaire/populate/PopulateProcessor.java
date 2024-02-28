package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReferenceExt;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.dtrQuestionnaireResponseExtension;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.prepopulateSubjectExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PopulateProcessor implements IPopulateProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(PopulateProcessor.class);
    private final ProcessItem processItem;
    private final ProcessItemWithContext processItemWithContext;
    private final ProcessResponseItem processResponseItem;

    public PopulateProcessor() {
        this(new ProcessItem(), new ProcessItemWithContext(), new ProcessResponseItem());
    }

    private PopulateProcessor(
            ProcessItem processItem,
            ProcessItemWithContext processItemWithExtension,
            ProcessResponseItem processResponseItem) {
        this.processItem = processItem;
        this.processItemWithContext = processItemWithExtension;
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
                        Collections.singletonList(buildReferenceExt(
                                request.getFhirVersion(),
                                prepopulateSubjectExtension(
                                        "Patient", request.getSubjectId().getIdPart()),
                                false)));
        var items = request.getItems(request.getQuestionnaire());
        final List<IBaseBackboneElement> processedItems = processItems(request, items);
        request.getModelResolver().setValue(populatedQuestionnaire, "item", processedItems);
        request.resolveOperationOutcome(populatedQuestionnaire);
        return (R) populatedQuestionnaire;
    }

    @Override
    public IBaseResource populate(PopulateRequest request) {
        final IBaseResource response = createQuestionnaireResponse(request);
        response.setId(request.getQuestionnaire().getIdElement().getIdPart() + "-"
                + request.getSubjectId().getIdPart());
        var items = request.getItems(request.getQuestionnaire());
        var processedItems = processItems(request, items);
        var responseItems = processResponseItems(request, processedItems);
        request.getModelResolver().setValue(response, "item", responseItems);
        request.resolveOperationOutcome(response);
        request.getModelResolver()
                .setValue(response, "contained", Collections.singletonList(request.getQuestionnaire()));
        request.getModelResolver()
                .setValue(
                        response,
                        "extension",
                        Collections.singletonList(buildReferenceExt(
                                request.getFhirVersion(),
                                dtrQuestionnaireResponseExtension(request.getQuestionnaire()
                                        .getIdElement()
                                        .getIdPart()),
                                true)));
        return response;
    }

    public List<IBaseBackboneElement> processItems(PopulateRequest request, List<IBaseBackboneElement> items) {
        final List<IBaseBackboneElement> populatedItems = new ArrayList<>();
        items.forEach(item -> {
            var populationContextExt = item.getExtension().stream()
                    .filter(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT))
                    .findFirst()
                    .orElse(null);
            if (populationContextExt != null) {
                populatedItems.addAll(processItemWithContext(request, item));
            } else {
                var childItems = request.getItems(item);
                if (!childItems.isEmpty()) {
                    final IBaseBackboneElement populatedItem = SerializationUtils.clone(item);
                    request.getModelResolver().setValue(populatedItem, "item", null);
                    final var processedChildItems = processItems(request, childItems);
                    request.getModelResolver().setValue(populatedItem, "item", processedChildItems);
                    populatedItems.add(populatedItem);
                } else {
                    var populatedItem = processItem(request, item);
                    populatedItems.add(populatedItem);
                }
            }
        });
        return populatedItems;
    }

    protected List<IBaseBackboneElement> processItemWithContext(PopulateRequest request, IBaseBackboneElement item) {
        try {
            // extension value is the context resource we're using to populate
            // Expression-based Population
            return processItemWithContext.processItem(request, item);
        } catch (ResolveExpressionException e) {
            logger.error(e.getMessage());
            request.logException(e.getMessage());
            return new ArrayList<>();
        }
    }

    protected IBaseBackboneElement processItem(PopulateRequest request, IBaseBackboneElement item) {
        try {
            return processItem.processItem(request, item);
        } catch (ResolveExpressionException e) {
            logger.error(e.getMessage());
            request.logException(e.getMessage());
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

    public List<IBaseBackboneElement> processResponseItems(
            PopulateRequest request, List<? extends IBaseBackboneElement> items) {
        try {
            return processResponseItem.processResponseItems(request, items);
        } catch (Exception e) {
            logger.error(e.getMessage());
            request.logException(e.getMessage());
            return new ArrayList<>();
        }
    }
}
