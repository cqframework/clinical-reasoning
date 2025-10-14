package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.amazon.awssdk.utils.StringUtils.replacePrefixIgnoreCase;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceIndicatorEnum;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseCardJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseCardSourceJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseLinkJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseSuggestionActionJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseSuggestionJson;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;

public class CdsResponseEncoderService {

    private final IAdapterFactory adapterFactory;
    private final IRepository repository;

    protected IResourceAdapter responseAdapter;

    public CdsResponseEncoderService(IRepository repository, IAdapterFactory adapterFactory) {

        this.adapterFactory = adapterFactory;
        this.repository = repository;
    }

    public CdsServiceResponseJson encodeResponse(Object response, RequestDetails requestDetails) {
        if (!(response instanceof IBaseResource)) {
            throw new InternalErrorException("response is not an instance of a Resource");
        }
        CdsServiceResponseJson serviceResponse = new CdsServiceResponseJson();
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

            responseAdapter = adapterFactory.createResource(bundle);

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
                .map(adapterFactory::createRequestAction)
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
                    if (isNotBlank(display)) {
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
        return switch (code) {
            case "routine" -> CdsServiceIndicatorEnum.INFO;
            case "urgent" -> CdsServiceIndicatorEnum.WARNING;
            case "stat" -> CdsServiceIndicatorEnum.CRITICAL;
            default -> throw new IllegalArgumentException("Invalid priority code: " + code);
        };
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
            IBaseReference reference = action.getResource();
            IBaseResource resource = resolveResource(reference);
            suggestionAction.setResource(resource);
            // Leaving this out until  spec details how to map system actions.
            //			if (!suggestionAction.getType().isEmpty()) {
            //				resolveSystemAction(Action);
            //			}
        }

        return suggestionAction;
    }

    protected IBaseResource resolveResource(IBaseReference reference) {
        IResourceAdapter localResponseAdapter = getResponseAdapter();

        if (localResponseAdapter.get() instanceof IBaseBundle bundle) {
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
            IBaseResource retVal = null;
            String referenceValue =
                    replacePrefixIgnoreCase(reference.getReferenceElement().getValueAsString(), "#", EMPTY);

            for (IBaseResource contained : localResponseAdapter.getContained()) {
                if (contained.getIdElement().getValueAsString().equals(referenceValue)) {
                    retVal = contained;
                    break;
                }
            }

            return retVal;
        }
    }

    protected IResourceAdapter getResponseAdapter() {
        return responseAdapter;
    }
}
