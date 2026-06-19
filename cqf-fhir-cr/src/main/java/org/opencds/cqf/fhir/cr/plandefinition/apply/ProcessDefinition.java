package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReference;
import static org.opencds.cqf.fhir.utility.BundleHelper.*;
import static org.opencds.cqf.fhir.utility.Canonicals.*;
import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;

import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class ProcessDefinition {

    private static final Logger logger = LoggerFactory.getLogger(ProcessDefinition.class);
    private static final List<String> SUPPORTED_DEFINITION_TYPES =
            List.of("Questionnaire", "ActivityDefinition", "PlanDefinition");

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
        var definition = action.getDefinition();
        if (Boolean.TRUE.equals(isDefinitionCanonical(request, definition))) {
            resource = resolveDefinition(request, definition);
            if (resource != null) {
                var actionId = action.getId();
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

        var referenceToContained = definition.getValue().startsWith("#");
        var resource = referenceToContained
                ? resolveContained(request, definition.getValue())
                : resolveCanonicalByType(request, definition);
        if (resource == null) {
            return null;
        }
        return switch (resource.fhirType()) {
            case "PlanDefinition" -> applyNestedPlanDefinition(request, resource);
            case "ActivityDefinition" -> applyActivityDefinition(request, resource);
            default -> resource;
        };
    }

    /**
     * Resolves a canonical reference by issuing a single transaction Bundle that searches every
     * supported definition resource type in parallel, instead of inferring the type from the URL.
     *
     * <p>Resolution rules:
     * <ul>
     *   <li>If exactly one resource matches (with or without version) — return it.</li>
     *   <li>If no resources match — return null.</li>
     *   <li>If multiple resources match — throw an {@link IllegalStateException}.</li>
     * </ul>
     */
    private IBaseResource resolveCanonicalByType(ApplyRequest request, IPrimitiveType<String> definition) {
        var canonical = definition.getValue();
        var url = getUrl(canonical);
        var version = getVersion(canonical);
        var hasVersion = version != null && !version.isEmpty();
        var fhirVersion = request.getFhirVersion();

        var transaction = newBundle(fhirVersion, "transaction");
        for (var type : SUPPORTED_DEFINITION_TYPES) {
            var searchUrl = hasVersion
                    ? "%s?url=%s&version=%s".formatted(type, url, version)
                    : "%s?url=%s".formatted(type, url);
            var requestEntry = newRequest(fhirVersion, "GET", searchUrl);
            var entry = setEntryRequest(fhirVersion, newEntry(fhirVersion), requestEntry);
            addEntry(transaction, entry);
        }

        var response = repository.transaction(transaction);
        var matches = collectMatchesFromResponse(response);

        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        var errorHint = hasVersion
                ? "Even with the specified version, multiple resources matched."
                : "Specify a version to resolve the ambiguity.";
        throw new IllegalStateException(
                "Multiple resources (%d) found for canonical '%s'. %s".formatted(matches.size(), canonical, errorHint));
    }

    private List<IBaseResource> collectMatchesFromResponse(IBaseBundle response) {
        var matches = new ArrayList<IBaseResource>();
        for (var resource : getEntryResources(response)) {
            if (resource instanceof IBaseBundle resultBundle) {
                matches.addAll(getEntryResources(resultBundle));
            }
        }
        return matches;
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

    protected IBaseResource applyActivityDefinition(ApplyRequest request, IPrimitiveType<String> definition) {
        requireNonNull(definition);
        var referenceToContained = definition.getValue().startsWith("#");
        var activityDefinition = (referenceToContained
                ? resolveContained(request, definition.getValue())
                : resolveRepository(definition));
        return applyActivityDefinition(request, activityDefinition);
    }

    private IBaseResource applyActivityDefinition(ApplyRequest request, IBaseResource activityDefinition) {
        // Running into issues with invoking ActivityDefinition/$apply on a HapiFhirRepository that was created with
        // RequestDetails from PlanDefinition/$apply
        IBaseResource result = null;
        try {
            var activityRequest = request.toActivityRequest(activityDefinition);
            result = applyProcessor.applyActivityDefinition(activityRequest);
            // appending a count to the id when an ActivityDefinition is used in multiple actions
            // resulting in multiple request resources with the same id
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
                    .formatted(activityDefinition.getIdElement().getValue(), e.toString());
            logger.error(message);
            request.logException(message);
        }
        return result;
    }

    protected IBaseResource applyNestedPlanDefinition(ApplyRequest request, IPrimitiveType<String> definition) {
        requireNonNull(definition);
        var referenceToContained = definition.getValue().startsWith("#");
        var nextPlanDefinition = (referenceToContained
                ? resolveContained(request, definition.getValue())
                : resolveRepository(definition));
        return applyNestedPlanDefinition(request, nextPlanDefinition);
    }

    private IBaseResource applyNestedPlanDefinition(ApplyRequest request, IBaseResource planDefinition) {
        try {
            var nestedRequest = request.copy(planDefinition);
            var result = applyProcessor.applyPlanDefinition(nestedRequest);
            nestedRequest.resolveOperationOutcome(result);
            request.getRequestResources().addAll(nestedRequest.getRequestResources());
            request.getExtractedResources().addAll(nestedRequest.getExtractedResources());
            request.setQuestionnaire(nestedRequest.getQuestionnaireAdapter());
            return result;
        } catch (Exception e) {
            var message = "ERROR: PlanDefinition %s could not be applied and threw exception %s"
                    .formatted(planDefinition.getIdElement().getValue(), e.toString());
            logger.error(message);
            request.logException(message);
            return null;
        }
    }

    protected IBaseResource resolveRepository(IPrimitiveType<String> definition) {
        return searchRepositoryByCanonical(repository, definition);
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
