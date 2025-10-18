package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReference;
import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;

import ca.uhn.fhir.repository.IRepository;
import java.util.Collections;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.Enumerations.FHIRTypes;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class ProcessDefinition {
    private static final Logger logger = LoggerFactory.getLogger(ProcessDefinition.class);

    final IRepository repository;
    final ApplyProcessor applyProcessor;
    final ActionResolver actionResolver;

    public ProcessDefinition(IRepository repository, ApplyProcessor applyProcessor) {
        requireNonNull(repository);
        requireNonNull(applyProcessor);
        this.repository = repository;
        this.applyProcessor = applyProcessor;
        actionResolver = new ActionResolver();
    }

    public IBaseResource resolveDefinition(
            ApplyRequest request,
            IBaseResource requestOrchestration,
            IPlanDefinitionActionAdapter action,
            IRequestActionAdapter requestAction) {
        requireNonNull(request);
        requireNonNull(requestOrchestration);
        requireNonNull(action);
        requireNonNull(requestAction);
        IBaseResource resource = null;
        var definition = action.getDefinition(); // getDefinition(request, action);
        if (Boolean.TRUE.equals(isDefinitionCanonical(request, definition))) {
            resource = resolveDefinition(request, definition);
            if (resource != null) {
                var actionId = action.getId(); // request.resolvePathString(action, "id");
                if (actionId != null) {
                    resource.setId(
                            "%s-%s".formatted(actionId, resource.getIdElement().getIdPart()));
                }
                actionResolver.resolveAction(request, requestOrchestration, resource, action);
                var reference = Boolean.TRUE.equals(request.getContainResources())
                        ? "#%s".formatted(resource.getIdElement().getIdPart())
                        : resource.getIdElement().getValue();
                requestAction.setResource(buildReference(request.getFhirVersion(), reference));
                if (Boolean.TRUE.equals(request.getContainResources())) {
                    request.getModelResolver()
                            .setValue(requestOrchestration, "contained", Collections.singletonList(resource));
                } else {
                    request.getRequestResources().add(resource);
                }
            }
        } else if (Boolean.TRUE.equals(isDefinitionUri(request, definition))) {
            requestAction.setResource(buildReference(request.getFhirVersion(), definition.getValue()));
        }
        return resource;
    }

    protected IBaseResource resolveDefinition(ApplyRequest request, IPrimitiveType<String> definition) {
        requireNonNull(definition);
        logger.debug("Resolving definition {}", definition.getValue());

        var resourceName = resolveResourceName(request, definition);
        return switch (FHIRTypes.fromCode(requireNonNull(resourceName))) {
            case PLANDEFINITION -> applyNestedPlanDefinition(request, definition);
            case ACTIVITYDEFINITION -> applyActivityDefinition(request, definition);
            case QUESTIONNAIRE -> applyQuestionnaireDefinition(request, definition);
            default -> throw new FHIRException("Unknown action definition: %s".formatted(definition.getValue()));
        };
    }

    protected Boolean isDefinitionCanonical(ApplyRequest request, IBase definition) {
        requireNonNull(request);
        return switch (request.getFhirVersion()) {
            case R4 -> definition instanceof org.hl7.fhir.r4.model.CanonicalType;
            case R5 -> definition instanceof org.hl7.fhir.r5.model.CanonicalType;
            default -> definition != null;
        };
    }

    protected Boolean isDefinitionUri(ApplyRequest request, IBase definition) {
        requireNonNull(request);
        return switch (request.getFhirVersion()) {
            case R4 -> definition instanceof org.hl7.fhir.r4.model.UriType;
            case R5 -> definition instanceof org.hl7.fhir.r5.model.UriType;
            default -> Boolean.FALSE;
        };
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
            var message = "ERROR: Questionnaire %s could not be applied and threw exception %s"
                    .formatted(definition.getValue(), e.toString());
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
            var activityRequest = request.toActivityRequest(activityDefinition);
            result = applyProcessor.applyActivityDefinition(activityRequest);
            var activityDefinitionId = activityDefinition.getIdElement().getIdPart();
            var requestId =
                    Ids.newId(request.getFhirVersion(), result.fhirType(), activityDefinitionId.replaceFirst("#", ""));
            int counter = 1;
            while (request.getRequestResources().stream()
                    .anyMatch(r -> r.getIdElement().getIdPart().equals(requestId.getIdPart()))) {
                counter++;
                requestId.setValue("%s/%s%s".formatted(result.fhirType(), activityDefinitionId, counter));
            }
            result.setId(requestId);
            activityRequest.resolveOperationOutcome(result);
        } catch (Exception e) {
            var message = "ERROR: ActivityDefinition %s could not be applied and threw exception %s"
                    .formatted(definition.getValue(), e.toString());
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
            nestedRequest.resolveOperationOutcome(result);
            request.getRequestResources().addAll(nestedRequest.getRequestResources());
            request.getExtractedResources().addAll(nestedRequest.getExtractedResources());
            request.setQuestionnaire(nestedRequest.getQuestionnaireAdapter());
            return result;
        } catch (Exception e) {
            var message = "ERROR: PlanDefinition %s could not be applied and threw exception %s"
                    .formatted(definition.getValue(), e.toString());
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
        var containedId = getContainedId(id);
        var first = contained.stream()
                .filter(r -> getContainedId(r.getIdElement().getIdPart()).equals(containedId))
                .findFirst();
        return first.orElse(null);
    }

    private String getContainedId(String id) {
        return id.replaceFirst("#", "");
    }
}
