package org.opencds.cqf.fhir.cr.questionnaire.populate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PopulateProcessor implements IPopulateProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(PopulateProcessor.class);
    private final ProcessItem processItem;
    private final ProcessItemWithContext processItemWithContext;

    public PopulateProcessor() {
        this(new ProcessItem(), new ProcessItemWithContext());
    }

    private PopulateProcessor(ProcessItem processItem, ProcessItemWithContext processItemWithExtension) {
        this.processItem = processItem;
        this.processItemWithContext = processItemWithExtension;
    }

    @Override
    public IBaseResource populate(PopulateRequest request) {
        logger.info(
                "Performing $populate operation on Questionnaire/{}",
                request.getQuestionnaire().getIdElement().getIdPart());
        return processResponse(request, processItems(request, request.getItems(request.getQuestionnaire())));
    }

    @Override
    public IBaseResource processResponse(PopulateRequest request, List<IBaseBackboneElement> items) {
        final IBaseResource response = createQuestionnaireResponse(request);
        response.setId(request.getQuestionnaire().getIdElement().getIdPart() + "-"
                + request.getSubjectId().getIdPart());
        request.getModelResolver().setValue(response, "item", items);
        request.resolveOperationOutcome(response);
        request.getModelResolver()
                .setValue(response, "contained", Collections.singletonList(request.getQuestionnaire()));
        logger.info("$populate operation completed");
        return response;
    }

    public List<IBaseBackboneElement> processItems(PopulateRequest request, List<IBaseBackboneElement> items) {
        final List<IBaseBackboneElement> responseItems = new ArrayList<>();
        items.forEach(item -> {
            logger.info("Processing item {}", request.getItemLinkId(item));
            var populationContextExt = item.getExtension().stream()
                    .filter(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT))
                    .findFirst()
                    .orElse(null);
            if (populationContextExt != null) {
                responseItems.addAll(processItemWithContext(request, item));
            } else {
                var childItems = request.getItems(item);
                if (!childItems.isEmpty()) {
                    final var responseItem = processItem.createResponseItem(request.getFhirVersion(), item);
                    final var responseChildItems = processItems(request, childItems);
                    request.getModelResolver().setValue(responseItem, "item", responseChildItems);
                    responseItems.add(responseItem);
                } else {
                    var responseItem = processItem(request, item);
                    responseItems.add(responseItem);
                }
            }
        });
        return responseItems;
    }

    protected List<IBaseBackboneElement> processItemWithContext(PopulateRequest request, IBaseBackboneElement item) {
        try {
            return processItemWithContext.processContextItem(request, item);
        } catch (Exception e) {
            logger.error(e.getMessage());
            request.logException(e.getMessage());
            return new ArrayList<>();
        }
    }

    protected IBaseBackboneElement processItem(PopulateRequest request, IBaseBackboneElement item) {
        try {
            return processItem.processItem(request, item);
        } catch (Exception e) {
            logger.error(e.getMessage());
            request.logException(e.getMessage());
            return processItem.createResponseItem(request.getFhirVersion(), item);
        }
    }

    protected IBaseResource createQuestionnaireResponse(PopulateRequest request) {
        switch (request.getFhirVersion()) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.QuestionnaireResponse()
                        .setStatus(
                                org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS)
                        .setQuestionnaire(new org.hl7.fhir.dstu3.model.Reference("#"
                                + ((org.hl7.fhir.dstu3.model.Questionnaire) request.getQuestionnaire()).getIdPart()))
                        .setSubject(new org.hl7.fhir.dstu3.model.Reference(request.getSubjectId()));
            case R4:
                return new org.hl7.fhir.r4.model.QuestionnaireResponse()
                        .setStatus(org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS)
                        .setQuestionnaire(
                                "#" + ((org.hl7.fhir.r4.model.Questionnaire) request.getQuestionnaire()).getIdPart())
                        .setSubject(new org.hl7.fhir.r4.model.Reference(request.getSubjectId()));
            case R5:
                return new org.hl7.fhir.r5.model.QuestionnaireResponse()
                        .setStatus(org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS)
                        .setQuestionnaire(
                                "#" + ((org.hl7.fhir.r5.model.Questionnaire) request.getQuestionnaire()).getIdPart())
                        .setSubject(new org.hl7.fhir.r5.model.Reference(request.getSubjectId()));

            default:
                return null;
        }
    }
}
