package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static org.opencds.cqf.fhir.cr.questionnaireresponse.extract.ResponseBundle.createBundleDstu3;
import static org.opencds.cqf.fhir.cr.questionnaireresponse.extract.ResponseBundle.createBundleR4;
import static org.opencds.cqf.fhir.cr.questionnaireresponse.extract.ResponseBundle.createBundleR5;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractProcessor implements IExtractProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(ExtractProcessor.class);
    protected final ProcessItem itemProcessor;
    protected final ProcessDefinitionItem definitionItemProcessor;

    public ExtractProcessor() {
        this(new ProcessItem(), new ProcessDefinitionItem());
    }

    private ExtractProcessor(ProcessItem processItem, ProcessDefinitionItem processDefinitionItem) {
        this.itemProcessor = processItem;
        this.definitionItemProcessor = processDefinitionItem;
    }

    @Override
    public IBaseBundle extract(ExtractRequest request) {
        var resources = processItems(request);
        return createBundle(request, resources);
    }

    @Override
    public List<IBaseResource> processItems(ExtractRequest request) {
        var resources = new ArrayList<IBaseResource>();
        var subject = (IBaseReference) request.resolvePath(request.getQuestionnaireResponse(), "subject");
        var extractionContextExt = request.getItemExtractionContext();
        if (extractionContextExt != null) {
            processDefinitionItem(request, null, null, resources, subject);
            // request.getItems(request.getQuestionnaireResponse()).forEach(item -> {
            //     var questionnaireItem = request.getQuestionnaireItem(item);
            //     processDefinitionItem(request, item, questionnaireItem, resources, subject);
            // });
        } else {
            var questionnaireCodeMap = CodeMap.create(request);
            request.getItems(request.getQuestionnaireResponse()).forEach(item -> {
                var questionnaireItem = request.getQuestionnaireItem(item);
                if (!request.getItems(item).isEmpty()) {
                    processGroupItem(request, item, questionnaireItem, questionnaireCodeMap, resources, subject);
                } else {
                    processItem(request, item, questionnaireItem, questionnaireCodeMap, resources, subject);
                }
            });
        }

        return resources;
    }

    protected IBaseBundle createBundle(ExtractRequest request, List<IBaseResource> resources) {
        switch (request.getFhirVersion()) {
            case DSTU3:
                return createBundleDstu3(request.getExtractId(), resources);
            case R4:
                return createBundleR4(request.getExtractId(), resources);
            case R5:
                return createBundleR5(request.getExtractId(), resources);

            default:
                return null;
        }
    }

    protected void processGroupItem(
            ExtractRequest request,
            IBaseBackboneElement item,
            IBaseBackboneElement questionnaireItem,
            Map<String, List<IBaseCoding>> questionnaireCodeMap,
            List<IBaseResource> resources,
            IBaseReference subject) {
        var subjectItems = request.getItems(item).stream()
                .filter(child -> child.getExtension().stream()
                        .anyMatch(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT)))
                .collect(Collectors.toList());
        var groupSubject = !subjectItems.isEmpty()
                ? (IBaseReference) request.resolvePath(
                        request.resolvePathList(subjectItems.get(0), "answer", IBaseBackboneElement.class)
                                .get(0),
                        "value")
                : (IBaseReference) SerializationUtils.clone(subject);
        if (request.isDefinitionItem(questionnaireItem, item)) {
            processDefinitionItem(request, item, questionnaireItem, resources, groupSubject);
        } else {
            request.getItems(item).forEach(childItem -> {
                if (childItem.getExtension().stream()
                        .noneMatch(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT))) {
                    var childQ = request.getQuestionnaireItem(childItem, request.getItems(questionnaireItem));
                    if (!request.getItems(childItem).isEmpty()) {
                        processGroupItem(request, childItem, childQ, questionnaireCodeMap, resources, groupSubject);
                    } else {
                        processObservationItem(
                                request, childItem, childQ, questionnaireCodeMap, resources, groupSubject);
                    }
                }
            });
        }
    }

    protected void processItem(
            ExtractRequest request,
            IBaseBackboneElement item,
            IBaseBackboneElement questionnaireItem,
            Map<String, List<IBaseCoding>> questionnaireCodeMap,
            List<IBaseResource> resources,
            IBaseReference subject) {
        if (request.isDefinitionItem(questionnaireItem, item)) {
            processDefinitionItem(request, item, questionnaireItem, resources, subject);
        } else {
            processObservationItem(request, item, questionnaireItem, questionnaireCodeMap, resources, subject);
        }
    }

    protected void processObservationItem(
            ExtractRequest request,
            IBaseBackboneElement item,
            IBaseBackboneElement questionnaireItem,
            Map<String, List<IBaseCoding>> questionnaireCodeMap,
            List<IBaseResource> resources,
            IBaseReference subject) {
        try {
            itemProcessor.processItem(request, item, questionnaireItem, questionnaireCodeMap, resources, subject);
        } catch (Exception e) {
            request.logException(e.getMessage());
            throw e;
        }
    }

    protected void processDefinitionItem(
            ExtractRequest request,
            IBaseBackboneElement item,
            IBaseBackboneElement questionnaireItem,
            List<IBaseResource> resources,
            IBaseReference subject) {
        try {
            definitionItemProcessor.processDefinitionItem(request, item, questionnaireItem, resources, subject);
        } catch (Exception e) {
            request.logException(e.getMessage());
            throw e;
        }
    }
}
