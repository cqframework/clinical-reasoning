package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static org.opencds.cqf.fhir.cr.common.ItemValueTransformer.transformValueToResource;

import ca.uhn.fhir.context.BaseRuntimeChildDatatypeDefinition;
import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeDeclaredChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.RuntimeChildChoiceDefinition;
import ca.uhn.fhir.context.RuntimeChildCompositeDatatypeDefinition;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.IdType;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDefinitionItem {
    protected static final Logger logger = LoggerFactory.getLogger(ProcessDefinitionItem.class);
    final ExpressionProcessor expressionProcessor;

    public ProcessDefinitionItem() {
        this(new ExpressionProcessor());
    }

    public ProcessDefinitionItem(ExpressionProcessor expressionProcessor) {
        this.expressionProcessor = expressionProcessor;
    }

    public void processDefinitionItem(
            ExtractRequest request,
            IBaseBackboneElement item,
            IBaseBackboneElement questionnaireItem,
            List<IBaseResource> resources,
            IBaseReference subject) {
        // Definition-based extraction -
        // http://build.fhir.org/ig/HL7/sdc/extraction.html#definition-based-extraction

        var linkId = request.getItemLinkId(item);
        var context = getContext(request, linkId, getContextExtension(request, item, questionnaireItem));
        var resourceType = context.left;
        var contextResource = context.right;
        var definition = getDefinition(request, item, questionnaireItem);
        if (resourceType == null && (contextResource == null || contextResource.isEmpty())) {
            if (definition == null) {
                throw new IllegalArgumentException(String.format("Unable to retrieve definition for item: %s", linkId));
            }
            resourceType = getDefinitionType(definition);
        }
        if (contextResource != null && contextResource.size() > 1) {
            contextResource.forEach(r -> processResource(
                    request, linkId, r, false, definition, item, questionnaireItem, resources, subject));
        } else {
            var isCreatedResource = true;
            IBaseResource resource = null;
            if (contextResource != null && !contextResource.isEmpty()) {
                resource = contextResource.get(0);
                isCreatedResource = false;
            } else {
                resource = (IBaseResource) newValue(request, resourceType);
            }
            processResource(
                    request,
                    linkId,
                    resource,
                    isCreatedResource,
                    definition,
                    item,
                    questionnaireItem,
                    resources,
                    subject);
        }
    }

    private IBaseExtension<?, ?> getContextExtension(
            ExtractRequest request, IBaseBackboneElement item, IBaseBackboneElement questionnaireItem) {
        // First, check the Questionnaire.item
        // Second, check the QuestionnaireResponse.item
        // Third, check the Questionnaire
        return request.getExtensionByUrl(
                request.hasExtension(questionnaireItem, Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                        ? questionnaireItem
                        : request.hasExtension(item, Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                                ? item
                                : request.getQuestionnaire(),
                Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT);
    }

    @SuppressWarnings("unchecked")
    private ImmutablePair<String, List<IBaseResource>> getContext(
            ExtractRequest request, String linkId, IBaseExtension<?, ?> contextExtension) {
        String resourceType = null;
        List<IBaseResource> context = null;
        if (contextExtension != null) {
            var contextValue = contextExtension.getValue();
            if (contextValue instanceof IPrimitiveType) {
                // Extension value is a CodeType containing the Type of the Resource to be extracted.
                resourceType = ((IPrimitiveType<String>) contextValue).getValueAsString();
            } else if (contextValue instanceof ICompositeType) {
                // Extension value is an Expression resulting in the Resource(s) to be modified by the extraction.
                var contextExpression = CqfExpression.of(contextExtension, request.getDefaultLibraryUrl());
                if (contextExpression != null) {
                    try {
                        context =
                                expressionProcessor
                                        .getExpressionResultForItem(request, contextExpression, linkId)
                                        .stream()
                                        .map(r -> (IBaseResource) r)
                                        .collect(Collectors.toList());
                    } catch (Exception e) {
                        var message = String.format(
                                "Error encountered processing item %s: Error resolving context expression %s: %s",
                                linkId, contextExpression.getExpression(), e.getMessage());
                        logger.error(message);
                        throw new IllegalArgumentException(message);
                    }
                }
            }
        }
        return new ImmutablePair<String, List<IBaseResource>>(resourceType, context);
    }

    private void processResource(
            ExtractRequest request,
            String linkId,
            IBaseResource resource,
            boolean isCreatedResource,
            String definition,
            IBaseBackboneElement item,
            IBaseBackboneElement questionnaireItem,
            List<IBaseResource> resources,
            IBaseReference subject) {
        var resourceDefinition = request.getFhirContext().getElementDefinition(resource.getClass());
        if (isCreatedResource) {
            resource.setId(new IdType(resource.fhirType(), request.getExtractId() + "-" + linkId));
            resolveMeta(resource, definition);
            var subjectPath = getSubjectPath(resourceDefinition);
            if (subjectPath != null) {
                request.getModelResolver().setValue(resource, subjectPath, subject);
            }
            var authorPath = getAuthorPath(resourceDefinition);
            if (authorPath != null) {
                var authorValue = request.resolvePath(request.getQuestionnaireResponse(), "author");
                if (authorValue != null) {
                    request.getModelResolver().setValue(resource, authorPath, authorValue);
                }
            }
            var dateAuthored =
                    request.resolvePath(request.getQuestionnaireResponse(), "authored", IPrimitiveType.class);
            if (dateAuthored != null) {
                var dateDefs = getDateDefs(resourceDefinition);
                if (dateDefs != null && !dateDefs.isEmpty()) {
                    dateDefs.forEach(dateDef -> {
                        try {
                            var authoredValue = dateDef.getDatatype()
                                    .getConstructor(String.class)
                                    .newInstance(dateAuthored.getValueAsString());

                            request.getModelResolver().setValue(resource, dateDef.getElementName(), authoredValue);
                        } catch (Exception ex) {
                            var message = String.format(
                                    "Error encountered processing item %s: Error setting property (%s) on resource type (%s): %s",
                                    linkId, dateDef.getElementName(), resource.fhirType(), ex.getMessage());
                            logger.error(message);
                            request.logException(message);
                        }
                    });
                }
            }
        }

        processChildren(
                request, resourceDefinition, resource, request.getItems(item), request.getItems(questionnaireItem));

        resources.add(resource);
    }

    private void processChildren(
            ExtractRequest request,
            BaseRuntimeElementDefinition<?> resourceDefinition,
            IBaseResource resource,
            List<IBaseBackboneElement> items,
            List<IBaseBackboneElement> questionnaireItems) {
        items.forEach(childItem -> {
            var questionnaireItem = questionnaireItems.stream()
                    .filter(i -> request.getItemLinkId(i).equals(request.getItemLinkId(childItem)))
                    .findFirst()
                    .orElse(null);
            var childDefinition = getDefinition(request, childItem, questionnaireItem);
            if (childDefinition != null) {
                var path = childDefinition.split("#")[1];
                // First element is always the resource type, so it can be ignored
                path = path.replace(resource.fhirType() + ".", "");
                var children = request.getItems(childItem);
                if (!children.isEmpty()) {
                    processChildren(request, resourceDefinition, resource, children, request.getItems(resource));
                } else {
                    var answers = request.resolvePathList(childItem, "answer", IBaseBackboneElement.class);
                    var answerValue = answers.isEmpty() ? null : request.resolvePath(answers.get(0), "value");
                    if (answerValue != null) {
                        // Check if answer type matches path types available and transform if necessary
                        var pathDefinition =
                                (BaseRuntimeDeclaredChildDefinition) resourceDefinition.getChildByName(path);
                        if (pathDefinition instanceof RuntimeChildChoiceDefinition) {
                            var choices = ((RuntimeChildChoiceDefinition) pathDefinition).getChoices();
                            if (!choices.contains(answerValue.getClass())) {
                                answerValue = transformValueToResource(request.getFhirVersion(), answerValue);
                            }
                        } else if (pathDefinition instanceof RuntimeChildCompositeDatatypeDefinition
                                && !pathDefinition.getField().getType().equals(answerValue.getClass())) {
                            answerValue = transformValueToResource(request.getFhirVersion(), answerValue);
                        }
                        request.getModelResolver().setValue(resource, path, answerValue);
                    }
                }
            }
        });
    }

    private String getDefinition(
            ExtractRequest request, IBaseBackboneElement item, IBaseBackboneElement questionnaireItem) {
        var definition = request.resolvePathString(questionnaireItem, "definition");
        if (definition == null) {
            definition = request.resolvePathString(item, "definition");
        }
        return definition;
    }

    private String getDefinitionType(String definition) {
        if (!definition.contains("#")) {
            throw new IllegalArgumentException(
                    String.format("Unable to determine resource type from item definition: %s", definition));
        }
        return definition.split("#")[1];
    }

    private String getSubjectPath(BaseRuntimeElementDefinition<?> definition) {
        return definition.getChildByName("subject") != null
                ? "subject"
                : definition.getChildByName("patient") != null ? "patient" : null;
    }

    private String getAuthorPath(BaseRuntimeElementDefinition<?> definition) {
        return definition.getChildByName("recorder") != null
                ? "recorder"
                : definition.getName().equals("Observation") ? "performer" : null;
    }

    private List<BaseRuntimeChildDatatypeDefinition> getDateDefs(BaseRuntimeElementDefinition<?> definition) {
        List<BaseRuntimeChildDefinition> results = new ArrayList<>();
        results.add(definition.getChildByName("onset"));
        results.add(definition.getChildByName("issued"));
        results.add(definition.getChildByName("effective"));
        results.add(definition.getChildByName("recordDate"));

        return results.stream()
                .filter(d -> d instanceof BaseRuntimeChildDatatypeDefinition)
                .map(d -> (BaseRuntimeChildDatatypeDefinition) d)
                .collect(Collectors.toList());
    }

    private void resolveMeta(IBaseResource resource, String definition) {
        var meta = resource.getMeta();
        // Consider setting source and lastUpdated here?
        if (definition != null && !definition.isEmpty()) {
            meta.addProfile(definition.split("#")[0]);
        }
    }

    private IBase newValue(ExtractRequest request, String type) {
        try {
            return (IBase) Class.forName(String.format(
                            "org.hl7.fhir.%s.model.%s",
                            request.getFhirVersion().toString().toLowerCase(), type))
                    .getConstructor()
                    .newInstance();
        } catch (ClassNotFoundException
                | IllegalAccessException
                | IllegalArgumentException
                | InstantiationException
                | NoSuchMethodException
                | SecurityException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
