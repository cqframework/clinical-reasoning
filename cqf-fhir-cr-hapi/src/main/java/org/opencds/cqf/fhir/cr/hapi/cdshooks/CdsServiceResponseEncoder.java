package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.amazon.awssdk.utils.StringUtils.replacePrefixIgnoreCase;

import ca.uhn.fhir.repository.IRepository;
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

public class CdsServiceResponseEncoder {

    private final Object response;
    private final IAdapterFactory adapterFactory;
    private final IRepository repository;

    private IResponseEncoderResourceResolverFactory iResponseEncoderResourceResolverFactory;
    private IResponseEncoderActionResolverFactory iResponseEncoderActionResolverFactory;

    public CdsServiceResponseEncoder(Object response, IAdapterFactory adapterFactory,
        IRepository repository) {
        this.response = response;
        this.adapterFactory = adapterFactory;
        this.repository = repository;

        iResponseEncoderResourceResolverFactory = new IResponseEncoderResourceResolverFactory() {};
        iResponseEncoderActionResolverFactory = new IResponseEncoderActionResolverFactory() {};
    }

    public CdsServiceResponseJson encodeResponse() {
        if (!(response instanceof IBaseResource)) {
            throw new InternalErrorException("response is not an instance of a Resource");
        }
        CdsServiceResponseJson serviceResponse = new CdsServiceResponseJson();
        IResourceAdapter mainRequest = null;
        IPrimitiveType<String> canonical = null;
        IResourceAdapter responseAdapter = adapterFactory.createResource((IBaseResource) response);

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
            var activity = responseAdapter.getProperty("activity");
            if (activity != null && activity.length > 0) {
                ResponseEncoderResourceResolver resourceResolver = iResponseEncoderResourceResolverFactory.create(responseAdapter);
                var requestGroupRef = responseAdapter.resolvePath(activity[0], "reference", IBaseReference.class);

                IBaseResource resolvedResource = resourceResolver.resolveResource(requestGroupRef);
                mainRequest = adapterFactory.createResource(resolvedResource);
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
            adapterFactory.createResource(
                SearchHelper.searchRepositoryByCanonical(repository, canonical));
        var links = resolvePlanLinks(planDef);


        ResponseEncoderActionResolver actionResolver = iResponseEncoderActionResolverFactory.create(responseAdapter);

        Stream.of(mainRequest.getProperty("action"))
            .map(adapterFactory::createRequestAction)
            .forEach(action -> serviceResponse.addCard(actionResolver.resolveAction(action, links)));

        return serviceResponse;
    }

    interface IResponseEncoderResourceResolverFactory {
        default ResponseEncoderResourceResolver create(IResourceAdapter iResponseAdapter){
            return new ResponseEncoderResourceResolver(iResponseAdapter);
        }
    }

    interface IResponseEncoderActionResolverFactory{

        default ResponseEncoderActionResolver create(IResourceAdapter iResponseAdapter){
            ResponseEncoderResourceResolver resourceResolver = new ResponseEncoderResourceResolver(iResponseAdapter);
            ResponseEncoderSuggestionActionResolver suggestionActionResolver = new ResponseEncoderSuggestionActionResolver(resourceResolver);
            ResponseEncoderIndicatorResolver indicatorResolver = new ResponseEncoderIndicatorResolver();
            ResponseEncoderSourceResolver sourceResolver = new ResponseEncoderSourceResolver();

            ResponseEncoderSuggestionResolver suggestionResolver = new ResponseEncoderSuggestionResolver(suggestionActionResolver);

            return new ResponseEncoderActionResolver(indicatorResolver, sourceResolver, suggestionResolver);
        }
    }

    public void setIResponseEncoderActionResolverFactory(IResponseEncoderActionResolverFactory iResponseEncoderActionResolverFactory) {
        this.iResponseEncoderActionResolverFactory = iResponseEncoderActionResolverFactory;
    }

    public void setIResponseEncoderResourceResolverFactory(IResponseEncoderResourceResolverFactory iResponseEncoderResourceResolverFactory) {
        this.iResponseEncoderResourceResolverFactory = iResponseEncoderResourceResolverFactory;
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

    protected static class ResponseEncoderResourceResolver{
        private IResourceAdapter responseAdapter;

        protected ResponseEncoderResourceResolver(IResourceAdapter theResponseAdapter) {
            responseAdapter = theResponseAdapter;
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
                IBaseResource retVal = null;
                String referenceValue = replacePrefixIgnoreCase(reference.getReferenceElement().getValueAsString(), "#", EMPTY);

                for (IBaseResource contained : responseAdapter.getContained()) {
                    if (contained.getIdElement().getValueAsString().equals(referenceValue)) {
                        retVal = contained;
                        break;
                    }
                }

                return retVal;
            }
        }

    }

    protected static class ResponseEncoderActionResolver {

        private ResponseEncoderIndicatorResolver indicatorResolver;
        private ResponseEncoderSourceResolver sourceResolver;
        private ResponseEncoderSuggestionResolver suggestionResolver;

        protected ResponseEncoderActionResolver(ResponseEncoderIndicatorResolver theIndicatorResolver,
            ResponseEncoderSourceResolver theSourceResolver,
            ResponseEncoderSuggestionResolver theSuggestionResolver) {
            indicatorResolver = theIndicatorResolver;
            sourceResolver = theSourceResolver;
            suggestionResolver = theSuggestionResolver;
        }

        protected CdsServiceResponseCardJson resolveAction(
            IRequestActionAdapter action, List<CdsServiceResponseLinkJson> links) {
            var card = new CdsServiceResponseCardJson()
                .setSummary(action.getTitle())
                .setDetail(action.getDescription())
                .setLinks(links);

            if (action.hasPriority()) {
                card.setIndicator(indicatorResolver.resolveIndicator(action.getPriority()));
            }

            if (action.hasDocumentation()) {
                card.setSource(sourceResolver.resolveSource(action));
            }

            if (action.hasSelectionBehavior()) {
                card.setSelectionBehaviour(action.getSelectionBehavior());
                action.getAction().forEach(suggestionResolver::resolveSuggestion);
            }

            // Leaving this out until spec details how to map system actions.
            //		if (Action.hasType() && Action.hasResource()) {
            //			resolveSystemAction(Action);
            //		}

            return card;
        }
    }

    protected static class ResponseEncoderIndicatorResolver{

        protected CdsServiceIndicatorEnum resolveIndicator(String code) {
            return
                switch (defaultString(code)) {
                    case "routine" -> CdsServiceIndicatorEnum.INFO;
                    case "urgent" -> CdsServiceIndicatorEnum.WARNING;
                    case "stat" -> CdsServiceIndicatorEnum.CRITICAL;
                    default -> throw new IllegalArgumentException("Invalid priority code: " + code);
                };

        }
    }

    protected static class ResponseEncoderSourceResolver {

        protected CdsServiceResponseCardSourceJson resolveSource(IRequestActionAdapter action) {
            var retVal = new CdsServiceResponseCardSourceJson();
            if(action.hasDocumentation()) {
                var documentation = action.getDocumentation().get(0);
                retVal
                    .setLabel(action.resolvePathString(documentation, "display"))
                    .setUrl(action.resolvePathString(documentation, "url"));

                var document = action.resolvePath(documentation, "document");
                String documentUrl =
                    document == null ? null : action.resolvePathString(document, "url");
                if (isNotBlank(documentUrl)) {
                    retVal.setIcon(documentUrl);
                }
            }

            return retVal;
        }
    }

    protected static class ResponseEncoderSuggestionResolver{
        private ResponseEncoderSuggestionActionResolver suggestionActionResolver;

        protected ResponseEncoderSuggestionResolver(ResponseEncoderSuggestionActionResolver theSuggestionActionResolver) {
            suggestionActionResolver = theSuggestionActionResolver;
        }

        protected CdsServiceResponseSuggestionJson resolveSuggestion(IRequestActionAdapter action) {
            CdsServiceResponseSuggestionJson suggestion = new CdsServiceResponseSuggestionJson();

            if(nonNull(action)) {
                suggestion
                    .setLabel(action.getTitle())
                    .setUuid(action.getId());

                action.getAction().forEach(
                    x -> suggestion.addAction(suggestionActionResolver.resolveSuggestionAction(x)));
            }

            return suggestion;
        }
    }

    protected static class ResponseEncoderSuggestionActionResolver{
        private ResponseEncoderResourceResolver resourceResolver;

        protected ResponseEncoderSuggestionActionResolver(ResponseEncoderResourceResolver theResourceResolver) {
            resourceResolver = theResourceResolver;
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
                suggestionAction.setResource(resourceResolver.resolveResource(action.getResource()));
                // Leaving this out until  spec details how to map system actions.
                //			if (!suggestionAction.getType().isEmpty()) {
                //				resolveSystemAction(Action);
                //			}
            }

            return suggestionAction;
        }
    }
}
