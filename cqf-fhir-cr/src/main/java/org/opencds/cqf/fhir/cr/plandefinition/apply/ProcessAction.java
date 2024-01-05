package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReference;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;

import java.util.Collections;
import java.util.Map;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r5.model.Enumerations.FHIRTypes;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.common.DynamicValueProcessor;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.ExtensionProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.r4.SearchHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessAction {
    private static final Logger logger = LoggerFactory.getLogger(ProcessAction.class);

    final Repository repository;
    final ApplyProcessor applyProcessor;
    final GenerateProcessor generateProcessor;
    final ExtensionProcessor extensionProcessor;
    final ExpressionProcessor expressionProcessor;
    final DynamicValueProcessor dynamicValueProcessor;

    public ProcessAction(Repository repository, ApplyProcessor applyProcessor, GenerateProcessor generateProcessor) {
        this.repository = repository;
        this.applyProcessor = applyProcessor;
        this.generateProcessor = generateProcessor;
        extensionProcessor = new ExtensionProcessor();
        expressionProcessor = new ExpressionProcessor();
        dynamicValueProcessor = new DynamicValueProcessor();
    }

    @SuppressWarnings("unchecked")
    public IBaseBackboneElement processAction(
            ApplyRequest request,
            IBaseResource requestOrchestration,
            Map<String, IBaseBackboneElement> metConditions,
            IBaseBackboneElement action) {
        if (request.hasExtension(request.getPlanDefinition(), Constants.CPG_QUESTIONNAIRE_GENERATE)) {
            var actionInput = request.resolvePathList(action, "input", ICompositeType.class);
            for (var input : actionInput) {
                addQuestionnaireItemForInput(request, input);
            }
        }

        if (Boolean.TRUE.equals(meetsConditions(request, action, requestOrchestration))) {
            // TODO: Figure out why this was here and what it was trying to do
            // if (action.hasRelatedAction()) {
            // for (var relatedActionComponent : action.getRelatedAction()) {
            // if
            // (relatedActionComponent.getRelationship().equals(ActionRelationshipType.AFTER)
            // && metConditions.containsKey(relatedActionComponent.getActionId())) {
            // metConditions.put(action.getId(), action);
            // resolveDefinition(planDefinition, requestGroup, action);
            // resolveDynamicValues(planDefinition, requestGroup, action);
            // }
            // }
            // }
            metConditions.put(request.resolvePathString(action, "id"), action);
            var requestAction = createRequestAction(action);
            extensionProcessor.processExtensions(request, requestAction, action, null);
            var childActions = request.resolvePathList(action, "action", IBaseBackboneElement.class);
            for (var childAction : childActions) {
                request.getModelResolver()
                        .setValue(
                                requestAction,
                                "action",
                                Collections.singletonList(
                                        processAction(request, requestOrchestration, metConditions, childAction)));
            }
            IBaseResource resource = null;
            var definition = request.resolvePath(action, "definition", IPrimitiveType.class);
            if (isDefinitionCanonical(request, definition)) {
                resource = resolveDefinition(request, definition);
                if (resource != null) {
                    resolveAction(requestOrchestration, resource, action);
                    request.getModelResolver()
                            .setValue(
                                    requestAction,
                                    "resource",
                                    buildReference(
                                            request.getFhirVersion(),
                                            resource.getIdElement().getIdPart()));
                    if (Boolean.TRUE.equals(request.getContainResources())) {
                        request.getModelResolver()
                                .setValue(requestOrchestration, "contained", Collections.singletonList(resource));
                    } else {
                        request.getRequestResources().add(resource);
                    }
                }
            } else if (isDefinitionUri(request, definition)) {
                request.getModelResolver()
                        .setValue(
                                requestAction,
                                "resource",
                                buildReference(
                                        request.getFhirVersion(), ((IPrimitiveType<String>) definition).getValue()));
            }
            dynamicValueProcessor.processDynamicValues(request, resource, action, requestAction);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    protected void addQuestionnaireItemForInput(ApplyRequest request, ICompositeType input) {
        var profiles = request.resolvePathList(input, "profile", IPrimitiveType.class);
        if (profiles.isEmpty()) {
            return;
        }

        var profile =
                org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical(repository, profiles.get(0));
        var item = this.generateProcessor.generateItem(
                request, profile, request.getItems(request.getQuestionnaire()).size());

        // If input has text extension use it to override
        // resolve extensions or not?
        request.getModelResolver().setValue(request.getQuestionnaire(), "item", Collections.singletonList(item));
    }

    protected Boolean meetsConditions(
            ApplyRequest request, IBaseBackboneElement action, IBaseResource requestOrchestration) {
        var conditions = request.resolvePathList(action, "condition", IBaseBackboneElement.class);
        if (conditions.isEmpty()) {
            return true;
        }
        var input = request.resolvePathList(action, "input", ICompositeType.class);
        var inputParams = request.resolveInputParameters(input);
        for (var condition : conditions) {
            var conditionExpression = expressionProcessor.getCqfExpression(request, condition);
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
                if (result == null) {
                    logger.warn("Condition expression {} returned null", conditionExpression.getExpression());
                    return false;
                }
                if (!(result instanceof BooleanType)) {
                    logger.warn(
                            "The condition expression {} returned a non-boolean value: {}",
                            conditionExpression.getExpression(),
                            result.getClass().getSimpleName());
                    continue;
                }
                if (!((BooleanType) result).booleanValue()) {
                    logger.debug("The result of condition expression {} is false", conditionExpression.getExpression());
                    return false;
                }
                logger.debug("The result of condition expression {} is true", conditionExpression.getExpression());
            }
        }
        return true;
    }

    protected IBaseBackboneElement createRequestAction(IBaseBackboneElement action) {
        // TODO:
        return null;
    }

    protected Boolean isDefinitionCanonical(ApplyRequest request, IBase definition) {
        switch (request.getFhirVersion()) {
            case DSTU3:
                return Boolean.TRUE;
            case R4:
                return definition instanceof org.hl7.fhir.r4.model.CanonicalType;
            case R5:
                return definition instanceof org.hl7.fhir.r5.model.CanonicalType;

            default:
                return Boolean.FALSE;
        }
    }

    protected Boolean isDefinitionUri(ApplyRequest request, IBase definition) {
        switch (request.getFhirVersion()) {
            case DSTU3:
                return Boolean.FALSE;
            case R4:
                return definition instanceof org.hl7.fhir.r4.model.UriType;
            case R5:
                return definition instanceof org.hl7.fhir.r5.model.UriType;

            default:
                return Boolean.FALSE;
        }
    }

    protected IBaseResource resolveDefinition(ApplyRequest request, IPrimitiveType<String> definition) {
        logger.debug("Resolving definition {}", definition.getValue());
        var resourceName = resolveResourceName(request, definition);
        switch (FHIRTypes.fromCode(requireNonNull(resourceName))) {
            case PLANDEFINITION:
                return applyNestedPlanDefinition(request, definition);
            case ACTIVITYDEFINITION:
                return applyActivityDefinition(request, definition);
            case QUESTIONNAIRE:
                return applyQuestionnaireDefinition(request, definition);
            default:
                throw new FHIRException(String.format("Unknown action definition: %s", definition));
        }
    }

    protected IBaseResource applyQuestionnaireDefinition(ApplyRequest request, IPrimitiveType<String> definition) {
        IBaseResource result = null;
        try {
            var referenceToContained = definition.getValue().startsWith("#");
            if (referenceToContained) {
                result = resolveContained(request, definition.getValue());
            } else {
                result = SearchHelper.searchRepositoryByCanonical(repository, definition);
            }
        } catch (Exception e) {
            var message = String.format(
                    "ERROR: Questionnaire %s could not be applied and threw exception %s",
                    definition.getValue(), e.toString());
            logger.error(message);
            request.logException(message);
        }

        return result;
    }

    protected IBaseResource applyActivityDefinition(ApplyRequest request, IPrimitiveType<String> definition) {
        IBaseResource result = null;
        try {
            var referenceToContained = definition.getValue().startsWith("#");
            var activityDefinition = (ActivityDefinition)
                    (referenceToContained
                            ? resolveContained(request, definition.getValue())
                            : SearchHelper.searchRepositoryByCanonical(repository, definition));
            var params = parameters();
            result = repository.invoke(ActivityDefinition.class, "$apply", params, IBaseResource.class);
            // result = this.activityDefinitionProcessor.apply(
            //         activityDefinition,
            //         subjectId.getValue(),
            //         encounterId == null ? null : encounterId.getValue(),
            //         practitionerId == null ? null : practitionerId.getValue(),
            //         organizationId == null ? null : organizationId.getValue(),
            //         userType,
            //         userLanguage,
            //         userTaskContext,
            //         setting,
            //         settingContext,
            //         parameters,
            //         useServerData,
            //         bundle,
            //         libraryEngine);
            result.setId((IIdType)
                    (referenceToContained
                            ? Ids.newId(
                                    request.getFhirVersion(),
                                    result.fhirType(),
                                    activityDefinition.getIdPart().replaceFirst("#", ""))
                            : activityDefinition.getIdElement().withResourceType(result.fhirType())));
        } catch (Exception e) {
            var message = String.format(
                    "ERROR: ActivityDefinition %s could not be applied and threw exception %s",
                    definition.getValue(), e.toString());
            logger.error(message);
            request.logException(message);
        }

        return result;
    }

    protected IBaseResource applyNestedPlanDefinition(ApplyRequest request, IPrimitiveType<String> definition) {
        try {
            var referenceToContained = definition.getValue().startsWith("#");
            var nextPlanDefinition = (PlanDefinition)
                    (referenceToContained
                            ? resolveContained(request, definition.getValue())
                            : SearchHelper.searchRepositoryByCanonical(repository, definition));
            var nestedRequest = request.copy(nextPlanDefinition);
            return applyProcessor.applyPlanDefinition(nestedRequest);
        } catch (Exception e) {
            var message = String.format(
                    "ERROR: PlanDefinition %s could not be applied and threw exception %s",
                    definition.getValue(), e.toString());
            logger.error(message);
            request.logException(message);
            return null;
        }
    }

    protected String resolveResourceName(ApplyRequest request, IPrimitiveType<String> canonical) {
        if (canonical.hasValue()) {
            var id = canonical.getValue();
            if (id.contains("/")) {
                id = id.replace(id.substring(id.lastIndexOf("/")), "");
                return id.contains("/") ? id.substring(id.lastIndexOf("/") + 1) : id;
            } else if (id.startsWith("#")) {
                return resolveContained(request, id).fhirType();
            }
            return null;
        }

        throw new FHIRException("CanonicalType must have a value for resource name extraction");
    }

    protected IBaseResource resolveContained(ApplyRequest request, String id) {
        var contained = request.resolvePathList(request.getPlanDefinition(), "contained", IBaseResource.class);
        var first = contained.stream()
                .filter(r -> r.getIdElement().getIdPart().equals(id))
                .findFirst();
        return first.orElse(null);
    }

    protected void resolveAction(
            IBaseResource requestOrchestration, IBaseResource result, IBaseBackboneElement action) {
        if ("Task".equals(result.fhirType())) {
            // TODO: action resolvers, is Task the only one?
            // resolveTask(requestOrchestration, result, action);
        }
    }
}
