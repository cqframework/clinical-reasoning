package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.common.DynamicValueProcessor;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.ExtensionProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessAction {
    private static final Logger logger = LoggerFactory.getLogger(ProcessAction.class);

    final Repository repository;
    final ProcessDefinition processDefinition;
    final GenerateProcessor generateProcessor;
    final ExtensionProcessor extensionProcessor;
    final ExpressionProcessor expressionProcessor;
    final DynamicValueProcessor dynamicValueProcessor;

    public ProcessAction(Repository repository, ApplyProcessor applyProcessor, GenerateProcessor generateProcessor) {
        this.repository = repository;
        this.generateProcessor = generateProcessor;
        this.processDefinition = new ProcessDefinition(repository, applyProcessor);
        extensionProcessor = new ExtensionProcessor();
        expressionProcessor = new ExpressionProcessor();
        dynamicValueProcessor = new DynamicValueProcessor();
    }

    public IBaseBackboneElement processAction(
            ApplyRequest request,
            IBaseResource requestOrchestration,
            Map<String, IBaseBackboneElement> metConditions,
            IBaseBackboneElement action) {
        // Create Questionnaire items for any input profiles that are present on the action
        if (!request.getFhirVersion().equals(FhirVersionEnum.DSTU3) && request.getQuestionnaire() != null) {
            addQuestionnaireItemForInput(request, action);
        }

        if (Boolean.TRUE.equals(meetsConditions(request, action))) {
            metConditions.put(request.resolvePathString(action, "id"), action);
            var requestAction = generateRequestAction(request.getFhirVersion(), action);
            extensionProcessor.processExtensions(request, requestAction, action, new ArrayList<>());
            var childActions = request.resolvePathList(action, "action", IBaseBackboneElement.class);
            for (var childAction : childActions) {
                request.getModelResolver()
                        .setValue(
                                requestAction,
                                "action",
                                Collections.singletonList(
                                        processAction(request, requestOrchestration, metConditions, childAction)));
            }
            var resource = processDefinition.resolveDefinition(request, requestOrchestration, action, requestAction);
            dynamicValueProcessor.processDynamicValues(
                    request, request.getPlanDefinition(), resource, action, requestAction);
            return requestAction;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    protected void addQuestionnaireItemForInput(ApplyRequest request, IBaseBackboneElement action) {
        try {
            var actionInput = request.resolvePathList(action, "input", IElement.class);
            for (var input : actionInput) {
                var dataReqElement = getDataRequirementElement(request, input);
                var profiles = request.resolvePathList(dataReqElement, "profile", IPrimitiveType.class);
                if (profiles.isEmpty()) {
                    return;
                }
                var profile = searchRepositoryByCanonical(repository, profiles.get(0));
                var generateRequest = request.toGenerateRequest(profile);
                var item = generateProcessor.generateItem(generateRequest);
                request.addQuestionnaireItem(item.getLeft());
                request.addLaunchContextExtensions(item.getRight());
                // If input has text extension use it to override
            }
        } catch (Exception e) {
            var message = String.format(
                    "An error occurred while generating Questionnaire items for action input: %s", e.getMessage());
            request.logException(message);
        }
    }

    protected ICompositeType getDataRequirementElement(ApplyRequest request, IElement input) {
        return (ICompositeType)
                (request.getFhirVersion().isEqualOrNewerThan(FhirVersionEnum.R5)
                        ? request.resolvePath(input, "requirement")
                        : input);
    }

    protected IBaseParameters resolveInputParameters(ApplyRequest request, IBaseBackboneElement action) {
        var actionInput = request.resolvePathList(action, "input", IElement.class);
        return request.resolveInputParameters(actionInput.stream()
                .map(input -> getDataRequirementElement(request, input))
                .collect(Collectors.toList()));
    }

    protected Boolean meetsConditions(ApplyRequest request, IBaseBackboneElement action) {
        var conditions = request.resolvePathList(action, "condition", IBaseBackboneElement.class);
        if (conditions.isEmpty()) {
            return true;
        }
        var inputParams = resolveInputParameters(request, action);
        for (var condition : conditions) {
            var conditionExpression = expressionProcessor.getCqfExpressionForElement(request, condition);
            if (conditionExpression != null) {
                IBase result = null;
                try {
                    result = expressionProcessor
                            .getExpressionResult(request, conditionExpression, inputParams)
                            .get(0);
                } catch (Exception e) {
                    var message = String.format(
                            "Condition expression %s encountered exception: %s",
                            conditionExpression.getExpression(), e.getMessage());
                    logger.error(message);
                    request.logException(message);
                }
                var valid = validateResult(result, conditionExpression.getExpression());
                if (!valid) {
                    return false;
                }
                logger.debug("The result of condition expression {} is true", conditionExpression.getExpression());
            }
        }
        return true;
    }

    protected boolean validateResult(IBase result, String expression) {
        if (result == null) {
            logger.warn("Condition expression {} returned null", expression);
            return false;
        }
        if (!(result instanceof IBaseBooleanDatatype)) {
            logger.warn(
                    "Condition expression {} returned a non-boolean value: {}",
                    expression,
                    result.getClass().getSimpleName());
            return false;
        }
        if (Boolean.FALSE.equals(((IBaseBooleanDatatype) result).getValue())) {
            logger.debug("The result of condition expression {} is false", expression);
            return false;
        }
        return true;
    }

    protected IBaseBackboneElement generateRequestAction(FhirVersionEnum fhirVersion, IBaseBackboneElement action) {
        switch (fhirVersion) {
            case DSTU3:
                return generateRequestActionDstu3(action);
            case R4:
                return generateRequestActionR4(action);
            case R5:
                return generateRequestActionR5(action);

            default:
                return null;
        }
    }

    protected IBaseBackboneElement generateRequestActionDstu3(IBaseBackboneElement a) {
        var action = (org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent) a;
        var requestAction = new org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionComponent()
                .setTitle(action.getTitle())
                .setDescription(action.getDescription())
                .setTextEquivalent(action.getTextEquivalent())
                .setCode(action.getCode())
                .setDocumentation(action.getDocumentation())
                .setTiming(action.getTiming())
                .setType(action.getType());
        requestAction.setId(action.getId());

        if (action.hasCondition()) {
            action.getCondition()
                    .forEach(c -> requestAction.addCondition(
                            new org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionConditionComponent()
                                    .setKind(org.hl7.fhir.dstu3.model.RequestGroup.ActionConditionKind.fromCode(
                                            c.getKind().toCode()))
                                    .setExpression(c.getExpression())));
        }
        if (action.hasRelatedAction()) {
            action.getRelatedAction()
                    .forEach(ra -> requestAction.addRelatedAction(
                            new org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionRelatedActionComponent()
                                    .setActionId(ra.getActionId())
                                    .setRelationship(
                                            org.hl7.fhir.dstu3.model.RequestGroup.ActionRelationshipType.fromCode(
                                                    ra.getRelationship().toCode()))
                                    .setOffset(ra.getOffset())));
        }
        if (action.hasSelectionBehavior()) {
            requestAction.setSelectionBehavior(org.hl7.fhir.dstu3.model.RequestGroup.ActionSelectionBehavior.fromCode(
                    action.getSelectionBehavior().toCode()));
        }

        return requestAction;
    }

    protected IBaseBackboneElement generateRequestActionR4(IBaseBackboneElement a) {
        var action = (org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent) a;
        var requestAction = new org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent()
                .setTitle(action.getTitle())
                .setDescription(action.getDescription())
                .setTextEquivalent(action.getTextEquivalent())
                .setCode(action.getCode())
                .setDocumentation(action.getDocumentation())
                .setTiming(action.getTiming())
                .setType(action.getType());
        requestAction.setId(action.getId());

        if (action.hasCondition()) {
            action.getCondition()
                    .forEach(c -> requestAction.addCondition(
                            new org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionConditionComponent()
                                    .setKind(org.hl7.fhir.r4.model.RequestGroup.ActionConditionKind.fromCode(
                                            c.getKind().toCode()))
                                    .setExpression(c.getExpression())));
        }
        if (action.hasPriority()) {
            requestAction.setPriority(org.hl7.fhir.r4.model.RequestGroup.RequestPriority.fromCode(
                    action.getPriority().toCode()));
        }
        if (action.hasRelatedAction()) {
            action.getRelatedAction()
                    .forEach(ra -> requestAction.addRelatedAction(
                            new org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionRelatedActionComponent()
                                    .setActionId(ra.getActionId())
                                    .setRelationship(org.hl7.fhir.r4.model.RequestGroup.ActionRelationshipType.fromCode(
                                            ra.getRelationship().toCode()))
                                    .setOffset(ra.getOffset())));
        }
        if (action.hasSelectionBehavior()) {
            requestAction.setSelectionBehavior(org.hl7.fhir.r4.model.RequestGroup.ActionSelectionBehavior.fromCode(
                    action.getSelectionBehavior().toCode()));
        }

        return requestAction;
    }

    protected IBaseBackboneElement generateRequestActionR5(IBaseBackboneElement a) {
        var action = (org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent) a;
        var requestAction = new org.hl7.fhir.r5.model.RequestOrchestration.RequestOrchestrationActionComponent()
                .setTitle(action.getTitle())
                .setDescription(action.getDescription())
                .setTextEquivalent(action.getTextEquivalent())
                .addCode(action.getCode())
                .setDocumentation(action.getDocumentation())
                .setTiming(action.getTiming())
                .setType(action.getType());
        requestAction.setId(action.getId());

        if (action.hasCondition()) {
            action.getCondition()
                    .forEach(c -> requestAction.addCondition(
                            new org.hl7.fhir.r5.model.RequestOrchestration
                                            .RequestOrchestrationActionConditionComponent()
                                    .setKind(org.hl7.fhir.r5.model.Enumerations.ActionConditionKind.fromCode(
                                            c.getKind().toCode()))
                                    .setExpression(c.getExpression())));
        }
        if (action.hasPriority()) {
            requestAction.setPriority(org.hl7.fhir.r5.model.Enumerations.RequestPriority.fromCode(
                    action.getPriority().toCode()));
        }
        if (action.hasRelatedAction()) {
            action.getRelatedAction()
                    .forEach(ra -> requestAction.addRelatedAction(
                            new org.hl7.fhir.r5.model.RequestOrchestration
                                            .RequestOrchestrationActionRelatedActionComponent()
                                    .setTargetId(ra.getTargetId())
                                    .setRelationship(org.hl7.fhir.r5.model.Enumerations.ActionRelationshipType.fromCode(
                                            ra.getRelationship().toCode()))
                                    .setOffset(ra.getOffset())));
        }
        if (action.hasSelectionBehavior()) {
            requestAction.setSelectionBehavior(org.hl7.fhir.r5.model.Enumerations.ActionSelectionBehavior.fromCode(
                    action.getSelectionBehavior().toCode()));
        }

        return requestAction;
    }
}
