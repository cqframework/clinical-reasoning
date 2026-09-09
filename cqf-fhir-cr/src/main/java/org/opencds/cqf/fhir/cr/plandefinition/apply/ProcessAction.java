package org.opencds.cqf.fhir.cr.plandefinition.apply;

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
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cr.common.DynamicValueProcessor;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.ExtensionProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Constants.CqfApplicabilityBehavior;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementAdapter;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;
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
            IResourceAdapter requestOrchestration,
            List<String> metConditions,
            IPlanDefinitionActionAdapter action) {
        return processAction(request, requestOrchestration, metConditions, action, meetsConditions(request, action));
    }

    private IBaseBackboneElement processAction(
            ApplyRequest request,
            IResourceAdapter requestOrchestration,
            List<String> metConditions,
            IPlanDefinitionActionAdapter action,
            Boolean applicable) {
        // Keep the reached action's question available, including when its answer is unknown.
        // Create Questionnaire items for any input profiles that are present on the action
        if (!request.getFhirVersion().equals(FhirVersionEnum.DSTU3) && request.getQuestionnaire() != null) {
            addQuestionnaireItemForInput(request, action);
        }

        if (Boolean.TRUE.equals(applicable)) {
            metConditions.add(action.hasId() ? action.getId() : request.getNextActionId());
            var requestAction = generateRequestAction(action);
            extensionProcessor.processExtensions(request, requestAction, (IElement) action.get(), new ArrayList<>());
            processChildActions(request, requestOrchestration, metConditions, action, requestAction);
            var resource = processDefinition.resolveDefinition(request, requestOrchestration, action, requestAction);
            var adapter = resource == null ? null : request.getAdapterFactory().createResource(resource);
            dynamicValueProcessor.processDynamicValues(
                    request, request.getPlanDefinitionAdapter(), adapter, (IElement) action.get(), (IElement)
                            requestAction.get());
            return (IBaseBackboneElement) requestAction.get();
        }

        return null;
    }

    protected void processChildActions(
            ApplyRequest request,
            IResourceAdapter requestOrchestration,
            List<String> metConditions,
            IPlanDefinitionActionAdapter action,
            IRequestActionAdapter requestAction) {
        var childActions = action.getAction();
        if (childActions.isEmpty()) {
            return;
        }
        var applicabilityBehavior = CqfApplicabilityBehavior.ALL;
        try {
            applicabilityBehavior = action.getApplicabilityBehavior();
        } catch (Exception e) {
            logger.error(e.getMessage());
            request.logException(e.getMessage());
        }
        var metConditionsCount = metConditions.size();
        for (var childAction : childActions) {
            var applicable = meetsConditions(request, childAction);
            var childRequestAction =
                    processAction(request, requestOrchestration, metConditions, childAction, applicable);
            if (childRequestAction != null) {
                requestAction.addAction(childRequestAction);
            }
            if (applicabilityBehavior.equals(CqfApplicabilityBehavior.ANY)
                    && (metConditionsCount < metConditions.size()
                            || (request.isPauseOnUnknownApplicability() && applicable == null))) {
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
                        ? request.getPlanDefinitionAdapter().resolvePath(input, "requirement")
                        : input);
    }

    protected IBaseParameters resolveInputParameters(ApplyRequest request, IBaseBackboneElement action) {
        var actionInput = request.getPlanDefinitionAdapter().resolvePathList(action, "input", IElement.class);
        return request.resolveInputParameters(actionInput.stream()
                .map(input -> getDataRequirementElement(request, input))
                .collect(Collectors.toList()));
    }

    protected Boolean meetsConditions(ApplyRequest request, IPlanDefinitionActionAdapter action) {
        if (request.isPauseOnUnknownApplicability()) {
            return evaluateNullableConditions(request, action).value;
        }
        var conditions = action.getCondition().stream()
                .filter(c -> "applicability"
                        .equals(request.getPlanDefinitionAdapter().resolvePathString(c, "kind")))
                .map(c -> request.getAdapterFactory().createBase(c))
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

    /** Three-state conjunction, with evaluation failures taking precedence over false. */
    private ConditionResult evaluateNullableConditions(ApplyRequest request, IPlanDefinitionActionAdapter action) {
        var conditions = action.getCondition().stream()
                .filter(c -> "applicability"
                        .equals(request.getPlanDefinitionAdapter().resolvePathString(c, "kind")))
                .map(c -> request.getAdapterFactory().createBase(c))
                .toList();
        if (conditions.isEmpty()) {
            return ConditionResult.TRUE;
        }
        var combined = ConditionResult.TRUE;
        try {
            var inputParams = request.resolveInputParameters(action.getInputDataRequirement().stream()
                    .map(IDataRequirementAdapter::get)
                    .map(ICompositeType.class::cast)
                    .toList());
            for (var condition : conditions) {
                combined = combined.and(evaluateCondition(request, action, condition, inputParams));
            }
        } catch (Exception e) {
            request.logException(
                    "Error resolving applicability inputs for action %s: %s".formatted(action.getId(), e.getMessage()));
            return ConditionResult.FAILED;
        }
        return combined;
    }

    private ConditionResult evaluateCondition(
            ApplyRequest request,
            IPlanDefinitionActionAdapter action,
            IAdapter<?> condition,
            IBaseParameters inputParams) {
        try {
            var expression = expressionProcessor.getCqfExpressionForElement(request, condition);
            validateConditionExpression(expression);
            var results = request.getLibraryEngine()
                    .resolveExpression(
                            request.getSubjectId().getIdPart(),
                            expression,
                            inputParams == null ? request.getParameters() : inputParams,
                            request.getRawParameters(),
                            request.getData(),
                            request.getContextVariable(),
                            request.getResourceVariable());
            return conditionResult(results);
        } catch (Exception e) {
            request.logException(
                    "Error evaluating applicability for action %s: %s".formatted(action.getId(), e.getMessage()));
            return ConditionResult.FAILED;
        }
    }

    private static void validateConditionExpression(CqfExpression expression) {
        if (expression == null
                || expression.getExpression() == null
                || expression.getExpression().isBlank()
                || expression.getLanguage() == null
                || expression.getLanguage().isBlank()) {
            throw new IllegalArgumentException("Applicability condition has no executable expression");
        }
    }

    private static ConditionResult conditionResult(List<IBase> results) {
        if (results != null && results.size() > 1) {
            throw new IllegalArgumentException("Applicability condition must return a single Boolean value");
        }
        var result = results == null || results.isEmpty() ? null : results.get(0);
        if (result == null) {
            return ConditionResult.UNKNOWN;
        }
        if (!(result instanceof IBaseBooleanDatatype value)) {
            throw new IllegalArgumentException("Applicability condition returned a non-Boolean value");
        }
        if (value.getValue() == null) {
            return ConditionResult.UNKNOWN;
        }
        return Boolean.FALSE.equals(value.getValue()) ? ConditionResult.FALSE : ConditionResult.TRUE;
    }

    // Preserve the protected nullable Boolean contract: UNKNOWN and FAILED both return null,
    // but FAILED records an issue and must take precedence over FALSE in the conjunction.
    private enum ConditionResult {
        TRUE(Boolean.TRUE),
        FALSE(Boolean.FALSE),
        UNKNOWN(null),
        FAILED(null);

        private final Boolean value;

        ConditionResult(Boolean value) {
            this.value = value;
        }

        private ConditionResult and(ConditionResult other) {
            if (this == FAILED || other == FAILED) {
                return FAILED;
            }
            if (this == FALSE || other == FALSE) {
                return FALSE;
            }
            if (this == UNKNOWN || other == UNKNOWN) {
                return UNKNOWN;
            }
            return TRUE;
        }
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
