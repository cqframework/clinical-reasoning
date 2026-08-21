package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReference;
import static org.opencds.cqf.fhir.utility.BundleHelper.addEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.getEntryResources;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;
import static org.opencds.cqf.fhir.utility.BundleHelper.newEntry;
import static org.opencds.cqf.fhir.utility.BundleHelper.newRequest;
import static org.opencds.cqf.fhir.utility.BundleHelper.setEntryRequest;
import static org.opencds.cqf.fhir.utility.Canonicals.getResourceType;
import static org.opencds.cqf.fhir.utility.Canonicals.getUrl;
import static org.opencds.cqf.fhir.utility.Canonicals.getVersion;
import static org.opencds.cqf.fhir.utility.SearchHelper.getBundleClass;
import static org.opencds.cqf.fhir.utility.SearchHelper.getResourceClass;
import static org.opencds.cqf.fhir.utility.search.Searches.byUrl;
import static org.opencds.cqf.fhir.utility.search.Searches.byUrlAndVersion;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class ProcessDefinition {
    private static final Logger logger = LoggerFactory.getLogger(ProcessDefinition.class);
    private static final List<String> SUPPORTED_DEFINITION_TYPES = List.of(
            "Questionnaire",
            "ActivityDefinition",
            "PlanDefinition",
            "MessageDefinition",
            "ObservationDefinition",
            "SpecimenDefinition");

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
            IResourceAdapter requestOrchestration,
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
                    requestOrchestration.addContained(resource);
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
            request.logException(
                    String.format("Unable to find resource for definition: %s", definition.getValueAsString()));
            return null;
        }
        return switch (resource.fhirType()) {
            case "PlanDefinition" -> applyNestedPlanDefinition(request, resource);
            case "ActivityDefinition" -> applyActivityDefinition(request, resource);
            default -> applyDefinition(request, resource);
        };
    }

    /**
     * Resolves a canonical reference by first attempting to determine the Resource type.  If able
     * a search against only that type will be issued.  Otherwise, a single transaction Bundle
     * will be issued that searches every supported definition resource type in parallel.
     *
     * <p>Resolution rules:
     * <ul>
     *   <li>If exactly one resource matches (with or without version) — return it.</li>
     *   <li>If no resources match — return null.</li>
     *   <li>If multiple resources match — throw an {@link IllegalStateException}.</li>
     * </ul>
     */
    protected IBaseResource resolveCanonicalByType(ApplyRequest request, IPrimitiveType<String> definition) {
        var canonical = definition.getValue();
        var url = getUrl(canonical);
        var version = getVersion(canonical);
        var type = getResourceType(definition);
        IBaseBundle response = null;
        try {
            response = type == null || !SUPPORTED_DEFINITION_TYPES.contains(type)
                    ? searchAllDefinitionTypes(request.getFhirVersion(), url, version)
                    : searchByCanonicalType(type, url, version);
        } catch (Exception e) {
            var message = "Error encountered searching for definition (%s): %s".formatted(canonical, e.getMessage());
            logger.debug(message);
            request.logException(message);
        }
        var matches = collectMatchesFromResponse(response);
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        var errorHint = isNotBlank(version)
                ? "Even with the specified version, multiple resources matched."
                : "Specify a version to resolve the ambiguity.";
        throw new IllegalStateException(
                "Multiple resources (%d) found for canonical '%s'. %s".formatted(matches.size(), canonical, errorHint));
    }

    protected IBaseBundle searchAllDefinitionTypes(FhirVersionEnum fhirVersion, String url, String version) {
        var searchQuery = isNotBlank(version) ? "?url=%s&version=%s".formatted(url, version) : "?url=%s".formatted(url);
        var transaction = newBundle(fhirVersion, "transaction");
        for (var type : SUPPORTED_DEFINITION_TYPES) {
            var searchUrl = "%s%s".formatted(type, searchQuery);
            var requestEntry = newRequest(fhirVersion, "GET", searchUrl);
            var entry = setEntryRequest(fhirVersion, newEntry(fhirVersion), requestEntry);
            addEntry(transaction, entry);
        }

        return repository.transaction(transaction);
    }

    protected IBaseBundle searchByCanonicalType(String type, String url, String version) {
        var searchParams = isNotBlank(version) ? byUrlAndVersion(url, version) : byUrl(url);
        return repository.search(getBundleClass(repository), getResourceClass(repository, type), searchParams);
    }

    protected List<IBaseResource> collectMatchesFromResponse(IBaseBundle response) {
        var matches = new ArrayList<IBaseResource>();
        if (response != null) {
            for (var resource : getEntryResources(response)) {
                if (resource instanceof IBaseBundle resultBundle) {
                    matches.addAll(getEntryResources(resultBundle));
                } else {
                    matches.add(resource);
                }
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

    protected IBaseResource applyDefinition(ApplyRequest request, IBaseResource definition) {
        // TODO: Should wrap this in a Task?
        return definition;
    }

    protected IBaseResource applyActivityDefinition(ApplyRequest request, IBaseResource activityDefinition) {
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
            activityRequest.resolveOperationOutcome(request.getAdapterFactory().createResource(result));
        } catch (Exception e) {
            var message = "ERROR: ActivityDefinition %s could not be applied and threw exception %s"
                    .formatted(activityDefinition.getIdElement().getValue(), e.toString());
            logger.error(message);
            request.logException(message);
        }
        return result;
    }

    protected IBaseResource applyNestedPlanDefinition(ApplyRequest request, IBaseResource planDefinition) {
        try {
            var nestedRequest = request.copy(planDefinition);
            var result = applyProcessor.applyPlanDefinition(nestedRequest);
            nestedRequest.resolveOperationOutcome(request.getAdapterFactory().createResource(result));
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

    protected IBaseResource resolveContained(ApplyRequest request, String id) {
        requireNonNull(id);
        var contained = request.getPlanDefinitionAdapter().getContained();
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
