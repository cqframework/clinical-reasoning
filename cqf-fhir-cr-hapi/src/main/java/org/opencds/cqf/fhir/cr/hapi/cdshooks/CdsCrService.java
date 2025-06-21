package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_DATA;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_ENCOUNTER;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_PARAMETERS;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_PRACTITIONER;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_SUBJECT;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_PARAMETER_DRAFT_ORDERS;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_PARAMETER_ENCOUNTER_ID;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_PARAMETER_PATIENT_ID;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_PARAMETER_USER_ID;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestJson;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceIndicatorEnum;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseCardJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseCardSourceJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseLinkJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseSuggestionActionJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseSuggestionJson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.VersionUtilities;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;

@SuppressWarnings("squid:S125")
public class CdsCrService implements ICdsCrService {
    protected final RequestDetails requestDetails;
    protected final IRepository repository;
    protected final IAdapterFactory adapterFactory;
    protected IResourceAdapter responseAdapter;
    protected CdsServiceResponseJson serviceResponse;

    public CdsCrService(RequestDetails requestDetails, IRepository repository) {
        this.requestDetails = requestDetails;
        this.repository = repository;
        adapterFactory = IAdapterFactory.forFhirContext(this.repository.fhirContext());
    }

    public FhirVersionEnum getFhirVersion() {
        return repository.fhirContext().getVersion().getVersion();
    }

    public IRepository getRepository() {
        return repository;
    }

    public IBaseParameters encodeParams(CdsServiceRequestJson json) {
        var parameters = adapterFactory.createParameters(
                (IBaseParameters) Resources.newBaseForVersion("Parameters", getFhirVersion()));
        parameters.addParameter(APPLY_PARAMETER_SUBJECT, json.getContext().getString(CDS_PARAMETER_PATIENT_ID));
        if (json.getContext().containsKey(CDS_PARAMETER_USER_ID)) {
            parameters.addParameter(
                    APPLY_PARAMETER_PRACTITIONER, json.getContext().getString(CDS_PARAMETER_USER_ID));
        }
        if (json.getContext().containsKey(CDS_PARAMETER_ENCOUNTER_ID)) {
            parameters.addParameter(APPLY_PARAMETER_ENCOUNTER, json.getContext().getString(CDS_PARAMETER_ENCOUNTER_ID));
        }
        var cqlParameters = adapterFactory.createParameters(
                (IBaseParameters) Resources.newBaseForVersion("Parameters", getFhirVersion()));
        if (json.getContext().containsKey(CDS_PARAMETER_DRAFT_ORDERS)) {
            addCqlParameters(
                    cqlParameters,
                    json.getContext().getResource(CDS_PARAMETER_DRAFT_ORDERS),
                    CDS_PARAMETER_DRAFT_ORDERS);
        }
        if (cqlParameters.hasParameter()) {
            parameters.addParameter(APPLY_PARAMETER_PARAMETERS, cqlParameters.get());
        }
        var data = getPrefetchResources(json);
        if (!BundleHelper.getEntry(data).isEmpty()) {
            parameters.addParameter(APPLY_PARAMETER_DATA, data);
        }
        return (IBaseParameters) parameters.get();
    }

    protected void addCqlParameters(IParametersAdapter parameters, IBaseResource contextResource, String paramName) {
        // We are making the assumption that a Library created for a hook will provide parameters for fields
        // specified for the hook
        if (contextResource instanceof IBaseBundle bundle) {
            BundleHelper.getEntryResources(bundle).forEach(r -> parameters.addParameter(paramName, r));
        } else {
            parameters.addParameter(paramName, contextResource);
        }
        if (parameters.getParameter().size() == 1) {
            var listExtension = parameters.getParameter().get(0).get().addExtension();
            listExtension.setUrl(Constants.CPG_PARAMETER_DEFINITION);
            var paramDef = (IBaseDatatype) Resources.newBaseForVersion("ParameterDefinition", getFhirVersion());
            parameters
                    .getModelResolver()
                    .setValue(paramDef, "max", VersionUtilities.stringTypeForVersion(getFhirVersion(), "*"));
            parameters
                    .getModelResolver()
                    .setValue(paramDef, "name", VersionUtilities.codeTypeForVersion(getFhirVersion(), paramName));
            listExtension.setValue(paramDef);
        }
    }

    protected Map<String, IBaseResource> getResourcesFromBundle(IBaseBundle bundle) {
        // using HashMap to avoid duplicates
        Map<String, IBaseResource> resourceMap = new HashMap<>();
        BundleHelper.getEntryResources(bundle)
                .forEach(r -> resourceMap.put(r.fhirType() + r.getIdElement().getIdPart(), r));
        return resourceMap;
    }

    protected IBaseBundle getPrefetchResources(CdsServiceRequestJson json) {
        // using HashMap to avoid duplicates
        Map<String, IBaseResource> resourceMap = new HashMap<>();
        IBaseBundle prefetchResources = BundleHelper.newBundle(getFhirVersion());
        IBaseResource resource;
        for (String key : json.getPrefetchKeys()) {
            resource = json.getPrefetch(key);
            if (resource == null) {
                continue;
            }
            if (resource instanceof IBaseBundle bundle) {
                resourceMap.putAll(getResourcesFromBundle(bundle));
            } else {
                resourceMap.put(resource.fhirType() + resource.getIdElement().getIdPart(), resource);
            }
        }
        resourceMap.forEach(
                (key, value) -> BundleHelper.addEntry(prefetchResources, BundleHelper.newEntryWithResource(value)));
        return prefetchResources;
    }

    @SuppressWarnings("unchecked")
    public CdsServiceResponseJson encodeResponse(Object response) {
        if (!(response instanceof IBaseResource)) {
            throw new InternalErrorException("response is not an instance of a Resource");
        }
        serviceResponse = new CdsServiceResponseJson();
        IResourceAdapter mainRequest = null;
        IPrimitiveType<String> canonical = null;
        if (response instanceof IBaseParameters parameters) {
            var parametersAdapter = adapterFactory.createParameters(parameters);
            var bundle = parametersAdapter.getParameter().stream()
                    .map(p -> (IBaseBundle) p.getResource())
                    .findFirst()
                    .orElse(null);
            if (bundle == null) {
                throw new InternalErrorException("response does not contain a Bundle");
            }
            if (!BundleHelper.getEntry(bundle).isEmpty()) {
                mainRequest = adapterFactory.createResource(BundleHelper.getEntryResourceFirstRep(bundle));
                canonical = (IPrimitiveType<String>) mainRequest.getProperty("instantiatesCanonical")[0];
            }
        } else {
            responseAdapter = adapterFactory.createResource((IBaseResource) response);
            var activity = responseAdapter.getProperty("activity");
            if (activity != null && activity.length > 0) {
                var requestGroupRef = responseAdapter.resolvePath(activity[0], "reference", IBaseReference.class);
                mainRequest = adapterFactory.createResource(resolveResource(requestGroupRef));
                var definition = mainRequest.getProperty("definition");
                if (definition != null && definition.length > 0) {
                    canonical = mainRequest.resolvePath(definition[0], "reference", IPrimitiveType.class);
                }
            }
        }

        if (mainRequest == null || canonical == null) {
            throw new InternalErrorException("unable to resolve response");
        }

        var planDef = (IPlanDefinitionAdapter)
                adapterFactory.createResource(SearchHelper.searchRepositoryByCanonical(repository, canonical));
        var links = resolvePlanLinks(planDef);
        Stream.of(mainRequest.getProperty("action"))
                .map(b -> adapterFactory.createRequestAction((IBaseBackboneElement) b))
                .forEach(action -> serviceResponse.addCard(resolveAction(action, links)));

        return serviceResponse;
    }

    @SuppressWarnings("unchecked")
    protected List<CdsServiceResponseLinkJson> resolvePlanLinks(IPlanDefinitionAdapter planDefinition) {
        List<CdsServiceResponseLinkJson> links = new ArrayList<>();
        // links - listed on each card
        if (planDefinition.hasRelatedArtifact()) {
            planDefinition.getRelatedArtifact().forEach(ra -> {
                var linkUrl = planDefinition.resolvePathString(ra, "url");
                if (linkUrl != null) {
                    CdsServiceResponseLinkJson link = new CdsServiceResponseLinkJson().setUrl(linkUrl);
                    var display = planDefinition.resolvePathString(ra, "display");
                    if (StringUtils.isNotBlank(display)) {
                        link.setLabel(display);
                    }
                    if (ra.hasExtension()) {
                        link.setType(((IPrimitiveType<String>)
                                        ra.getExtension().get(0).getValue())
                                .getValueAsString());
                    } else link.setType("absolute"); // default
                    links.add(link);
                }
            });
        }
        return links;
    }

    protected CdsServiceResponseCardJson resolveAction(
            IRequestActionAdapter action, List<CdsServiceResponseLinkJson> links) {
        var card = new CdsServiceResponseCardJson()
                .setSummary(action.getTitle())
                .setDetail(action.getDescription())
                .setLinks(links);

        if (action.hasPriority()) {
            card.setIndicator(resolveIndicator(action.getPriority()));
        }

        if (action.hasDocumentation()) {
            card.setSource(resolveSource(action));
        }

        if (action.hasSelectionBehavior()) {
            card.setSelectionBehaviour(action.getSelectionBehavior());
            action.getAction().forEach(this::resolveSuggestion);
        }

        // Leaving this out until spec details how to map system actions.
        //		if (Action.hasType() && Action.hasResource()) {
        //			resolveSystemAction(Action);
        //		}

        return card;
    }

    protected CdsServiceIndicatorEnum resolveIndicator(String code) {
        var indicator =
                switch (code) {
                    case "routine" -> CdsServiceIndicatorEnum.INFO;
                    case "urgent" -> CdsServiceIndicatorEnum.WARNING;
                    case "stat" -> CdsServiceIndicatorEnum.CRITICAL;
                    default -> null;
                };
        if (indicator == null) {
            throw new IllegalArgumentException("Invalid priority code: " + code);
        }

        return indicator;
    }

    protected CdsServiceResponseCardSourceJson resolveSource(IRequestActionAdapter action) {
        var documentation = action.getDocumentation().get(0);
        var source = new CdsServiceResponseCardSourceJson()
                .setLabel(action.resolvePathString(documentation, "display"))
                .setUrl(action.resolvePathString(documentation, "url"));

        var document = action.resolvePath(documentation, "document");
        String documentUrl = document == null ? null : action.resolvePathString(document, "url");
        if (StringUtils.isNotBlank(documentUrl)) {
            source.setIcon(documentUrl);
        }

        return source;
    }

    protected CdsServiceResponseSuggestionJson resolveSuggestion(IRequestActionAdapter action) {
        CdsServiceResponseSuggestionJson suggestion = new CdsServiceResponseSuggestionJson()
                .setLabel(action.getTitle())
                .setUuid(action.getId());
        action.getAction().forEach(x -> suggestion.addAction(resolveSuggestionAction(x)));

        return suggestion;
    }

    protected CdsServiceResponseSuggestionActionJson resolveSuggestionAction(IRequestActionAdapter action) {
        CdsServiceResponseSuggestionActionJson suggestionAction =
                new CdsServiceResponseSuggestionActionJson().setDescription(action.getDescription());
        if (action.hasType()
                && action.getType().hasCoding()
                && action.getType().getCoding().get(0).hasCode()
                && !action.getType().getCoding().get(0).getCode().equals("fire-event")) {
            String actionCode = action.getType().getCoding().get(0).getCode();
            suggestionAction.setType(actionCode);
        }
        if (action.hasResource()) {
            suggestionAction.setResource(resolveResource(action.getResource()));
            // Leaving this out until  spec details how to map system actions.
            //			if (!suggestionAction.getType().isEmpty()) {
            //				resolveSystemAction(Action);
            //			}
        }

        return suggestionAction;
    }

    protected IBaseResource resolveResource(IBaseReference reference) {
        if (responseAdapter.get() instanceof IBaseBundle bundle) {
            var ref = reference.getReferenceElement().getValueAsString();
            var split = ref.split("/");
            var id = ref.contains("/") ? split[1] : ref;
            var resourceType = Canonicals.getResourceType(ref);
            var results = BundleHelper.getEntryResources(bundle).stream()
                    .filter(r -> r.fhirType().equals(resourceType)
                            && r.getIdElement().getIdPart().equals(id))
                    .toList();
            return results.isEmpty() ? null : results.get(0);
        } else {
            return responseAdapter.getContained().stream()
                    .filter(resource -> resource.getIdElement()
                            .getIdPart()
                            .equals(reference.getReferenceElement().getValueAsString()))
                    .findFirst()
                    .orElse(null);
        }
    }
}
