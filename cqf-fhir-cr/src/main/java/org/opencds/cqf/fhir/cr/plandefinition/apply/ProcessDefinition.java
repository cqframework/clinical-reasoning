package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReference;
import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Collections;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.Enumerations.FHIRTypes;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDefinition {
    private static final Logger logger = LoggerFactory.getLogger(ProcessDefinition.class);

    final Repository repository;
    final ApplyProcessor applyProcessor;
    final ActionResolver actionResolver;

    public ProcessDefinition(Repository repository, ApplyProcessor applyProcessor) {
        requireNonNull(repository);
        requireNonNull(applyProcessor);
        this.repository = repository;
        this.applyProcessor = applyProcessor;
        actionResolver = new ActionResolver();
    }

    public IBaseResource resolveDefinition(
            ApplyRequest request,
            IBaseResource requestOrchestration,
            IBaseBackboneElement action,
            IBaseBackboneElement requestAction) {
        requireNonNull(request);
        requireNonNull(requestOrchestration);
        requireNonNull(action);
        requireNonNull(requestAction);
        IBaseResource resource = null;
        var definition = getDefinition(request, action);
        if (isDefinitionCanonical(request, definition)) {
            resource = resolveDefinition(request, definition);
            if (resource != null) {
                var actionId = request.resolvePathString(action, "id");
                if (actionId != null) {
                    resource.setId(String.format(
                            "%s-%s", actionId, resource.getIdElement().getIdPart()));
                }
                actionResolver.resolveAction(request, requestOrchestration, resource, action);
                request.getModelResolver()
                        .setValue(
                                requestAction,
                                "resource",
                                buildReference(
                                        request.getFhirVersion(),
                                        resource.getIdElement().getValue()));
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
                            buildReference(request.getFhirVersion(), ((IPrimitiveType<String>) definition).getValue()));
        }
        return resource;
    }

    @SuppressWarnings("unchecked")
    protected IPrimitiveType<String> getDefinition(ApplyRequest request, IBaseBackboneElement action) {
        requireNonNull(request);
        requireNonNull(action);
        return request.getFhirVersion().isOlderThan(FhirVersionEnum.R4)
                ? request.resolvePath(request.resolvePath(action, "definition"), "reference", IPrimitiveType.class)
                : request.resolvePath(action, "definition", IPrimitiveType.class);
    }

    protected IBaseResource resolveDefinition(ApplyRequest request, IPrimitiveType<String> definition) {
        requireNonNull(definition);
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
                throw new FHIRException(String.format("Unknown action definition: %s", definition.getValue()));
        }
    }

    protected Boolean isDefinitionCanonical(ApplyRequest request, IBase definition) {
        requireNonNull(request);
        switch (request.getFhirVersion()) {
            case R4:
                return definition instanceof org.hl7.fhir.r4.model.CanonicalType;
            case R5:
                return definition instanceof org.hl7.fhir.r5.model.CanonicalType;
            default:
                return definition != null;
        }
    }

    protected Boolean isDefinitionUri(ApplyRequest request, IBase definition) {
        requireNonNull(request);
        switch (request.getFhirVersion()) {
            case R4:
                return definition instanceof org.hl7.fhir.r4.model.UriType;
            case R5:
                return definition instanceof org.hl7.fhir.r5.model.UriType;
            default:
                return Boolean.FALSE;
        }
    }

    protected IBaseResource applyQuestionnaireDefinition(ApplyRequest request, IPrimitiveType<String> definition) {
        requireNonNull(definition);
        IBaseResource result = null;
        try {
            var referenceToContained = definition.getValue().startsWith("#");
            if (referenceToContained) {
                result = resolveContained(request, definition.getValue());
            } else {
                result = resolveRepository(definition);
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
        requireNonNull(definition);
        // Running into issues with invoking ActivityDefinition/$apply on a HapiFhirRepository that was created with
        // RequestDetails from PlanDefinition/$apply
        IBaseResource result = null;
        try {
            var referenceToContained = definition.getValue().startsWith("#");
            var activityDefinition = (referenceToContained
                    ? resolveContained(request, definition.getValue())
                    : resolveRepository(definition));
            // var activityDefParams = request.transformRequestParameters(activityDefinition);
            // Map<String, String> headers = new HashMap<>();
            // headers.put("Content-Type", "application/json+fhir");
            // result = repository.invoke(
            //         activityDefinition.getClass(), "$apply", activityDefParams, IBaseResource.class, headers);
            var activityRequest = request.toActivityRequest(activityDefinition);
            result = applyProcessor.applyActivityDefinition(activityRequest);
            result.setId((IIdType)
                    (referenceToContained
                            ? Ids.newId(
                                    request.getFhirVersion(),
                                    result.fhirType(),
                                    activityDefinition
                                            .getIdElement()
                                            .getIdPart()
                                            .replaceFirst("#", ""))
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
        requireNonNull(definition);
        try {
            var referenceToContained = definition.getValue().startsWith("#");
            var nextPlanDefinition = (referenceToContained
                    ? resolveContained(request, definition.getValue())
                    : resolveRepository(definition));
            var nestedRequest = request.copy(nextPlanDefinition);
            var result = applyProcessor.applyPlanDefinition(nestedRequest);
            request.getRequestResources().addAll(nestedRequest.getRequestResources());
            request.getExtractedResources().addAll(nestedRequest.getExtractedResources());
            request.setQuestionnaire(nestedRequest.getQuestionnaire());
            return result;
        } catch (Exception e) {
            var message = String.format(
                    "ERROR: PlanDefinition %s could not be applied and threw exception %s",
                    definition.getValue(), e.toString());
            logger.error(message);
            request.logException(message);
            return null;
        }
    }

    protected IBaseResource resolveRepository(IPrimitiveType<String> definition) {
        return searchRepositoryByCanonical(repository, definition);
    }

    protected String resolveResourceName(ApplyRequest request, IPrimitiveType<String> canonical) {
        requireNonNull(canonical);
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
        requireNonNull(id);
        var contained = request.resolvePathList(request.getPlanDefinition(), "contained", IBaseResource.class);
        var first = contained.stream()
                .filter(r -> r.getIdElement().getIdPart().equals(id))
                .findFirst();
        return first.orElse(null);
    }
}
