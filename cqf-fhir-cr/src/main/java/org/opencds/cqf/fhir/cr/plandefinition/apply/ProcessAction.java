package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.opencds.cqf.fhir.utility.Constants.CQF_APPLICABILITY_BEHAVIOR;
import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IElement;
import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cr.common.DynamicValueProcessor;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.ExtensionProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Constants.CqfApplicabilityBehavior;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementAdapter;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class ProcessAction {
    private static final Logger logger = LoggerFactory.getLogger(ProcessAction.class);

    final IRepository repository;
    final ProcessDefinition processDefinition;
    final GenerateProcessor generateProcessor;
    final ExtensionProcessor extensionProcessor;
    final ExpressionProcessor expressionProcessor;
    final DynamicValueProcessor dynamicValueProcessor;

    public ProcessAction(IRepository repository, ApplyProcessor applyProcessor, GenerateProcessor generateProcessor) {
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
            List<String> metConditions,
            IPlanDefinitionActionAdapter action) {
        // Create Questionnaire items for any input profiles that are present on the action
        if (!request.getFhirVersion().equals(FhirVersionEnum.DSTU3) && request.getQuestionnaire() != null) {
            addQuestionnaireItemForInput(request, action);
        }

        if (Boolean.TRUE.equals(meetsConditions(request, action))) {
            metConditions.add(action.hasId() ? action.getId() : request.getNextActionId());
            var requestAction = generateRequestAction(action);
            extensionProcessor.processExtensions(
                    request, requestAction.get(), (IElement) action.get(), new ArrayList<>());
            processChildActions(request, requestOrchestration, metConditions, action, requestAction);
            var resource = processDefinition.resolveDefinition(request, requestOrchestration, action, requestAction);
            dynamicValueProcessor.processDynamicValues(
                    request, request.getPlanDefinition(), resource, (IElement) action.get(), (IElement)
                            requestAction.get());
            return (IBaseBackboneElement) requestAction.get();
        }

        return null;
    }

    protected void processChildActions(
            ApplyRequest request,
            IBaseResource requestOrchestration,
            List<String> metConditions,
            IPlanDefinitionActionAdapter action,
            IRequestActionAdapter requestAction) {
        var childActions = action.getAction();
        if (childActions.isEmpty()) {
            return;
        }
        var applicabilityBehavior = CqfApplicabilityBehavior.ALL;
        var applicabilityBehaviorExt = action.getExtensionByUrl(CQF_APPLICABILITY_BEHAVIOR);
        if (applicabilityBehaviorExt != null
                && applicabilityBehaviorExt.getValue() instanceof IPrimitiveType<?> primitiveType) {
            try {
                applicabilityBehavior = CqfApplicabilityBehavior.valueOf(
                        primitiveType.getValueAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                var message =
                        "Encountered invalid value for applicabilityBehavior extension %s.  Expected `all` or `any`."
                                .formatted(primitiveType.getValueAsString());
                logger.error(message);
                request.logException(message);
            }
        }
        var metConditionsCount = metConditions.size();
        for (var childAction : childActions) {
            var childRequestAction = processAction(request, requestOrchestration, metConditions, childAction);
            if (childRequestAction != null) {
                requestAction.addAction(childRequestAction);
            }
            if (applicabilityBehavior.equals(CqfApplicabilityBehavior.ANY)
                    && metConditionsCount < metConditions.size()) {
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void addQuestionnaireItemForInput(ApplyRequest request, IPlanDefinitionActionAdapter action) {
        try {
            for (var input : action.getInputDataRequirement().stream()
                    .filter(IDataRequirementAdapter::hasProfile)
                    .toList()) {
                for (var profileUrl : input.getProfile()) {
                    if (!request.questionnaireItemExistsForProfile(profileUrl)) {
                        var profile = searchRepositoryByCanonical(repository, profileUrl);
                        var generateRequest = request.toGenerateRequest(profile);
                        var item = generateProcessor.generateItem(generateRequest);
                        if (item != null) {
                            // If input has text extension use it to override
                            if (input.hasExtension(Constants.CPG_INPUT_TEXT)) {
                                item.getLeft()
                                        .setText(((IPrimitiveType<String>)
                                                        input.getExtensionByUrl(Constants.CPG_INPUT_TEXT)
                                                                .getValue())
                                                .getValueAsString());
                                // item Constants.CPG_INPUT_DESCRIPTION
                            }
                            request.addQuestionnaireItem(item.getLeft());
                            request.addLaunchContextExtensions(item.getRight());
                        }
                    }
                }
            }
        } catch (Exception e) {
            var message = "An error occurred while generating Questionnaire items for action input: %s"
                    .formatted(e.getMessage());
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

    protected Boolean meetsConditions(ApplyRequest request, IPlanDefinitionActionAdapter action) {
        var conditions = action
                .getCondition()
                .stream() // request.resolvePathList(action, "condition", IBaseBackboneElement.class).stream()
                .filter(c -> "applicability".equals(request.resolvePathString(c, "kind")))
                .toList();
        if (conditions.isEmpty()) {
            return true;
        }
        var inputParams = request.resolveInputParameters(action.getInputDataRequirement().stream()
                .map(IDataRequirementAdapter::get)
                .map(ICompositeType.class::cast)
                .toList());
        for (var condition : conditions) {
            var conditionExpression = expressionProcessor.getCqfExpressionForElement(request, condition);
            if (conditionExpression != null) {
                IBase result = null;
                try {
                    var expressionResult =
                            expressionProcessor.getExpressionResult(request, conditionExpression, inputParams, null);
                    result = expressionResult.isEmpty() ? null : expressionResult.get(0);
                } catch (Exception e) {
                    var message = "Condition expression %s encountered exception: %s"
                            .formatted(conditionExpression.getExpression(), e.getMessage());
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

    protected IRequestActionAdapter generateRequestAction(IPlanDefinitionActionAdapter action) {
        var requestAction = action.newRequestAction()
                .setId(action.getId())
                .setTitle(action.getTitle())
                .setDescription(action.getDescription())
                .setTextEquivalent(action.getTextEquivalent())
                .setCode(action.getCode())
                .setDocumentation(action.getDocumentation())
                .setTiming(action.getTiming())
                .setType(action.getType())
                .setPriority(action.getPriority())
                .setSelectionBehavior(action.getSelectionBehavior());

        if (action.hasCondition()) {
            action.getCondition().forEach(requestAction::addCondition);
        }

        if (action.hasRelatedAction()) {
            action.getRelatedAction().forEach(requestAction::addRelatedAction);
        }

        return requestAction;
    }
}
