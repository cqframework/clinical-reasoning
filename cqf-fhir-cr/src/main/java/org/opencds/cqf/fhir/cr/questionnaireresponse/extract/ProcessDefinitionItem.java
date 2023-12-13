package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;

public class ProcessDefinitionItem {
    protected static final Logger logger = LoggerFactory.getLogger(ProcessDefinitionItem.class);
    final ExpressionProcessor expressionProcessor;

    public ProcessDefinitionItem() {
        this(new ExpressionProcessor());
    }

    private ProcessDefinitionItem(ExpressionProcessor expressionProcessor) {
        this.expressionProcessor = expressionProcessor;
    }

    public void processDefinitionItem(ExtractRequest request,
            IBaseBackboneElement item,
            List<IBaseResource> resources,
            IBaseReference subject) {
        // Definition-based extraction -
        // http://build.fhir.org/ig/HL7/sdc/extraction.html#definition-based-extraction
        // TODO: item extraction context
        // var contextExtension = Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT;
        // // var itemExtractionContext = item.hasExtension(contextExtension)
        // //         ? item.getExtensionByUrl(contextExtension)
        // //         : questionnaireResponse.getExtensionByUrl(contextExtension);
        // // var itemExtractionContext = request.getExtensions(item).stream().filter(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)).findFirst().orElse(null);
        // var itemExtractionContext = expressionProcessor.getCqfExpression(request, request.getExtensions(item), contextExtension);
        // if (itemExtractionContext == null) {
        //     // itemExtractionContext = request.getItemExtractionContext();
        //     itemExtractionContext = expressionProcessor.getCqfExpression(request, request.getExtensions(request.getQuestionnaireResponse()), contextExtension);
        //     if (itemExtractionContext == null) {
        //         itemExtractionContext = expressionProcessor.getCqfExpression(request, request.getExtensions(request.getQuestionnaire()), contextExtension);
        //     }
        // }
        // if (itemExtractionContext != null) {
        //     var context = expressionProcessor.getExpressionResult(request, itemExtractionContext, request.resolvePathString(item, "linkId").getValue());
        //     if (context != null && !context.isEmpty()) {
        //         // TODO: edit context instead of creating new resources
        //     }
        // }

        var definition = request.resolvePathString(item, "definition");
        var resourceType = getDefinitionType(definition);
        var resource = (IBaseResource) newValue(request, resourceType);
        var resourceDefinition = request.getFhirContext().getElementDefinition(resource.getClass());
        resource.setId(new IdType(resourceType, request.getExtractId() + "-" + request.resolvePathString(item, "linkId")));
        resolveMeta(resource, definition);
        var subjectPath = getSubjectPath(resourceDefinition);
        if (subjectPath != null) {
            request.getModelResolver().setValue(resource, subjectPath, subject);
        }
        // var subjectProperty = getSubjectProperty(resource);
        // if (subjectProperty != null) {
        //     resource.setProperty(subjectProperty.getName(), subject);
        // }
        var authorPath = getAuthorPath(resourceDefinition);
        if (authorPath != null) {
            var authorValue = request.resolvePath(request.getQuestionnaireResponse(), "author");
            if (authorValue != null) {
                request.getModelResolver().setValue(resource, authorPath, authorValue);
            }
        }
        // var authorProperty = getAuthorProperty(resource);
        // if (authorProperty != null && questionnaireResponse.hasAuthor()) {
        //     resource.setProperty(authorProperty.getName(), questionnaireResponse.getAuthor());
        // }
        var dateAuthored = request.resolvePath(request.getQuestionnaireResponse(), "authored");
        if (dateAuthored != null) {
            var datePaths = getDatePaths(resourceDefinition);
            if (datePaths != null && !datePaths.isEmpty()) {
                datePaths.forEach(datePath -> {

                });
            }
        }
        // var dateProperties = getDateProperties(resource);
        // if (dateProperties != null && !dateProperties.isEmpty() && questionnaireResponse.hasAuthored()) {
        //     dateProperties.forEach(p -> {
        //         try {
        //             var propertyDef = fhirContext()
        //                     .getElementDefinition(
        //                             p.getTypeCode().contains("|")
        //                                     ? p.getTypeCode().split("\\|")[0]
        //                                     : p.getTypeCode());
        //             if (propertyDef != null) {
        //                 modelResolver.setValue(
        //                         resource,
        //                         p.getName(),
        //                         propertyDef
        //                                 .getImplementingClass()
        //                                 .getConstructor(Date.class)
        //                                 .newInstance(questionnaireResponse.getAuthored()));
        //             }
        //         } catch (Exception ex) {
        //             var message = String.format(
        //                     "Error encountered attempting to set property (%s) on resource type (%s): %s",
        //                     p.getName(), resource.fhirType(), ex.getMessage());
        //             logger.error(message);
        //             request.logException(message);
        //         }
        //     });
        // }
        request.getItems(item).forEach(childItem -> {
            var childDefinition = request.resolvePathString(childItem, "definition");
            if (childDefinition != null) {
                var path = childDefinition.split("#")[1];
                // First element is always the resource type, so it can be ignored
                path = path.replace(resourceType + ".", "");
                var answers = request.resolvePathList(childItem, "answer", IBaseBackboneElement.class);
                var answerValue = answers.isEmpty() ? null : request.resolvePath(answers.get(0), "value");
                //var answerValue = childItem.getAnswerFirstRep().getValue();
                if (answerValue != null) {
                    request.getModelResolver().setValue(resource, path, answerValue);
                }
            }
        });

        resources.add(resource);
    }

    private String getDefinitionType(String definition) {
        if (!definition.contains("#")) {
            throw new IllegalArgumentException(
                    String.format("Unable to determine resource type from item definition: %s", definition));
        }
        return definition.split("#")[1];
    }

    private String getSubjectPath(BaseRuntimeElementDefinition<?> definition) {
        return definition.getChildByName("subject") != null ? "subject" :
            definition.getChildByName("patient") != null ? "patient" : null;
    }

    // private Property getSubjectProperty(ExtractRequest request, IBaseResource resource) {
    //     var property = resource.getNamedProperty("subject");
    //     if (property == null) {
    //         property = resource.getNamedProperty("patient");
    //     }

    //     return property;
    // }

    private String getAuthorPath(BaseRuntimeElementDefinition<?> definition) {
        return definition.getChildByName("recorder") != null ? "recorder" :
            definition.getName().equals("Observation") ? "performer" : null;
    }

    // private Property getAuthorProperty(Resource resource) {
    //     var property = resource.getNamedProperty("recorder");
    //     if (property == null && resource.fhirType().equals(FHIRAllTypes.OBSERVATION.toCode())) {
    //         property = resource.getNamedProperty("performer");
    //     }

    //     return property;
    // }

    private List<String> getDatePaths(BaseRuntimeElementDefinition<?> definition) {
        List<String> results = new ArrayList<>();
        results.add(definition.getChildByName("onset") != null ? "onset" : null);
        results.add(definition.getChildByName("issued") != null ? "issued" : null);
        results.add(definition.getChildByName("effective") != null ? "effective" : null);
        results.add(definition.getChildByName("recordDate") != null ? "recordDate" : null);

        return results.stream().filter(p -> p != null).collect(Collectors.toList());
    }

    // private List<Property> getDateProperties(Resource resource) {
    //     List<Property> results = new ArrayList<>();
    //     results.add(resource.getNamedProperty("onset"));
    //     results.add(resource.getNamedProperty("issued"));
    //     results.add(resource.getNamedProperty("effective"));
    //     results.add(resource.getNamedProperty("recordDate"));

    //     return results.stream().filter(p -> p != null).collect(Collectors.toList());
    // }

    private void resolveMeta(IBaseResource resource, String definition) {
        var meta = resource.getMeta();
        // Consider setting source and lastUpdated here?
        if (definition != null && !definition.isEmpty()) {
            meta.addProfile(definition.split("#")[0]);
        }
    }

    private IBase newValue(ExtractRequest request, String type) {
        try {
            return (IBase) Class.forName(String.format("org.hl7.fhir.%s.model.%s", request.getFhirVersion().toString().toLowerCase(), type))
                    .getConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
