package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.SerializationUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;
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
        var extractionExt = request.getDefinitionExtract();
        if (extractionExt != null) {
            processDefinitionItem(request, new ItemPair(null, null), resources);
        } else {
            var questionnaireCodeMap = CodeMap.create(request);
            request.getQuestionnaireResponseAdapter().getItem().forEach(item -> {
                var itemPair = new ItemPair(request.getQuestionnaireItem(item), item);
                if (!item.getItem().isEmpty()) {
                    processGroupItem(request, itemPair, questionnaireCodeMap, resources, subject);
                } else {
                    processItem(request, itemPair, questionnaireCodeMap, resources, subject);
                }
            });
        }

        return resources;
    }

    protected IBaseBundle createBundle(ExtractRequest request, List<IBaseResource> resources) {
        var bundle = BundleHelper.newBundle(request.getFhirVersion(), request.getExtractId(), "transaction");
        resources.forEach(r -> {
            var entry = BundleHelper.newEntryWithResource(r);
            BundleHelper.setEntryRequest(
                    request.getFhirVersion(), entry, BundleHelper.newRequest(request.getFhirVersion(), "POST"));
            BundleHelper.addEntry(bundle, entry);
        });
        return bundle;
    }

    protected void processGroupItem(
            ExtractRequest request,
            ItemPair item,
            Map<String, List<IBaseCoding>> questionnaireCodeMap,
            List<IBaseResource> resources,
            IBaseReference subject) {
        var subjectItems = item.getResponseItem().getItem().stream()
                .filter(child -> child.getExtension().stream()
                        .anyMatch(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT)))
                .map(IQuestionnaireResponseItemComponentAdapter.class::cast)
                .toList();
        var groupSubject = !subjectItems.isEmpty()
                ? (IBaseReference) subjectItems.get(0).getAnswer().get(0).getValue()
                : SerializationUtils.clone(subject);
        if (request.isDefinitionItem(item)) {
            processDefinitionItem(request, item, resources);
        } else {
            item.getResponseItem().getItem().forEach(childResponseItem -> {
                if (childResponseItem.getExtension().stream()
                        .noneMatch(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT))) {
                    var childPair = new ItemPair(
                            request.getQuestionnaireItem(
                                    childResponseItem, item.getItem().getItem()),
                            childResponseItem);
                    if (childResponseItem.hasItem()) {
                        processGroupItem(request, childPair, questionnaireCodeMap, resources, groupSubject);
                    } else {
                        processItem(request, childPair, questionnaireCodeMap, resources, groupSubject);
                    }
                }
            });
        }
    }

    protected void processItem(
            ExtractRequest request,
            ItemPair item,
            Map<String, List<IBaseCoding>> questionnaireCodeMap,
            List<IBaseResource> resources,
            IBaseReference subject) {
        if (request.isDefinitionItem(item)) {
            processDefinitionItem(request, item, resources);
        } else {
            processObservationItem(request, item, questionnaireCodeMap, resources, subject);
        }
    }

    protected void processObservationItem(
            ExtractRequest request,
            ItemPair item,
            Map<String, List<IBaseCoding>> questionnaireCodeMap,
            List<IBaseResource> resources,
            IBaseReference subject) {
        try {
            itemProcessor.processItem(request, item, questionnaireCodeMap, resources, subject);
        } catch (Exception e) {
            request.logException(e.getMessage());
            throw e;
        }
    }

    protected void processDefinitionItem(ExtractRequest request, ItemPair item, List<IBaseResource> resources) {
        try {
            var resource = definitionItemProcessor.processDefinitionItem(request, item);
            if (resource != null) {
                resources.add(resource);
            }
        } catch (Exception e) {
            request.logException(e.getMessage());
            throw e;
        }
    }
}
