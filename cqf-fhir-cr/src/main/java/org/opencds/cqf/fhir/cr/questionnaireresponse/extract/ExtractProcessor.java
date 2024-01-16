package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static org.opencds.cqf.fhir.cr.questionnaireresponse.extract.CodeMap.create;
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
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractProcessor implements IExtractProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(ExtractProcessor.class);
    protected final ProcessItem processItem;
    protected final ProcessDefinitionItem processDefinitionItem;

    public ExtractProcessor() {
        this(new ProcessItem(), new ProcessDefinitionItem());
    }

    private ExtractProcessor(ProcessItem processItem, ProcessDefinitionItem processDefinitionItem) {
        this.processItem = processItem;
        this.processDefinitionItem = processDefinitionItem;
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
            request.getItems(request.getQuestionnaireResponse())
                    .forEach(item -> processDefinitionItem(request, item, resources, subject));
        } else {
            var questionnaireCodeMap = create(request);
            request.getItems(request.getQuestionnaireResponse()).forEach(item -> {
                if (!request.getItems(item).isEmpty()) {
                    processGroupItem(request, item, questionnaireCodeMap, resources, subject);
                } else if (request.resolvePathString(item, "definition") != null) {
                    processDefinitionItem(request, item, resources, subject);
                } else {
                    processItem(request, item, questionnaireCodeMap, resources, subject);
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
        if (request.resolvePathString(item, "definition") != null) {
            processDefinitionItem(request, item, resources, groupSubject);
        } else {
            request.getItems(item).forEach(childItem -> {
                if (!childItem.getExtension().stream()
                        .anyMatch(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT))) {
                    if (!request.getItems(childItem).isEmpty()) {
                        processGroupItem(request, childItem, questionnaireCodeMap, resources, groupSubject);
                    } else {
                        processItem(request, childItem, questionnaireCodeMap, resources, groupSubject);
                    }
                }
            });
        }
    }

    protected void processItem(
            ExtractRequest request,
            IBaseBackboneElement item,
            Map<String, List<IBaseCoding>> questionnaireCodeMap,
            List<IBaseResource> resources,
            IBaseReference subject) {
        try {
            processItem.processItem(request, item, questionnaireCodeMap, resources, subject);
        } catch (Exception e) {
            request.logException(e.getMessage());
            throw e;
        }
    }

    protected void processDefinitionItem(
            ExtractRequest request, IBaseBackboneElement item, List<IBaseResource> resources, IBaseReference subject) {
        try {
            processDefinitionItem.processDefinitionItem(request, item, resources, subject);
        } catch (ResolveExpressionException e) {
            request.logException(e.getMessage());
        }
    }
}
