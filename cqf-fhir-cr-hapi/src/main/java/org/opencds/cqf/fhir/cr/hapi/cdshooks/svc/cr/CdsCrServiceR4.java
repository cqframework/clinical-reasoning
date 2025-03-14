/*-
 * #%L
 * HAPI FHIR - CDS Hooks
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr;

import static org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.CdsCrConstants.APPLY_PARAMETER_DATA;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.CdsCrConstants.APPLY_PARAMETER_ENCOUNTER;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.CdsCrConstants.APPLY_PARAMETER_PARAMETERS;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.CdsCrConstants.APPLY_PARAMETER_PRACTITIONER;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.CdsCrConstants.APPLY_PARAMETER_SUBJECT;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.CdsCrConstants.CDS_PARAMETER_DRAFT_ORDERS;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.CdsCrConstants.CDS_PARAMETER_ENCOUNTER_ID;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.CdsCrConstants.CDS_PARAMETER_PATIENT_ID;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.CdsCrConstants.CDS_PARAMETER_USER_ID;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestAuthorizationJson;
import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestJson;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.ICdsConfigService;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceIndicatorEnum;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceResponseCardJson;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceResponseCardSourceJson;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceResponseJson;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceResponseLinkJson;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceResponseSuggestionActionJson;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceResponseSuggestionJson;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceResponseSystemActionJson;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.ICdsCrService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RequestGroup;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Canonicals;

public class CdsCrServiceR4 implements ICdsCrService {
	protected final RequestDetails requestDetails;
	protected final Repository repository;
	protected final ICdsConfigService cdsConfigService;
	protected Bundle responseBundle;
	protected CdsServiceResponseJson serviceResponse;

	public CdsCrServiceR4(
			RequestDetails requestDetails, Repository repository, ICdsConfigService cdsConfigService) {
        this.cdsConfigService = cdsConfigService;
        this.requestDetails = requestDetails;
        this.repository = repository;
	}

	public FhirVersionEnum getFhirVersion() {
		return FhirVersionEnum.R4;
	}

	public Repository getRepository() {
		return repository;
	}

	public Parameters encodeParams(CdsServiceRequestJson json) {
		Parameters parameters = parameters()
				.addParameter(part(APPLY_PARAMETER_SUBJECT, json.getContext().getString(CDS_PARAMETER_PATIENT_ID)));
		if (json.getContext().containsKey(CDS_PARAMETER_USER_ID)) {
			parameters.addParameter(
					part(APPLY_PARAMETER_PRACTITIONER, json.getContext().getString(CDS_PARAMETER_USER_ID)));
		}
		if (json.getContext().containsKey(CDS_PARAMETER_ENCOUNTER_ID)) {
			parameters.addParameter(
					part(APPLY_PARAMETER_ENCOUNTER, json.getContext().getString(CDS_PARAMETER_ENCOUNTER_ID)));
		}
		var cqlParameters = parameters();
		if (json.getContext().containsKey(CDS_PARAMETER_DRAFT_ORDERS)) {
			addCqlParameters(
					cqlParameters,
					json.getContext().getResource(CDS_PARAMETER_DRAFT_ORDERS),
					CDS_PARAMETER_DRAFT_ORDERS);
		}
		if (cqlParameters.hasParameter()) {
			parameters.addParameter(part(APPLY_PARAMETER_PARAMETERS, cqlParameters));
		}
		Bundle data = getPrefetchResources(json);
		if (data.hasEntry()) {
			parameters.addParameter(part(APPLY_PARAMETER_DATA, data));
		}
		return parameters;
	}

	protected String getTokenType(CdsServiceRequestAuthorizationJson json) {
		String tokenType = json.getTokenType();
		return tokenType == null || tokenType.isEmpty() ? "Bearer" : tokenType;
	}

	protected Parameters addCqlParameters(
			Parameters parameters, IBaseResource contextResource, String paramName) {
		// We are making the assumption that a Library created for a hook will provide parameters for the fields
		// specified for the hook
		if (contextResource instanceof Bundle) {
			((Bundle) contextResource)
					.getEntry()
					.forEach(x -> parameters.addParameter(part(paramName, x.getResource())));
		} else {
			parameters.addParameter(part(paramName, (Resource) contextResource));
		}
		if (parameters.getParameter().size() == 1) {
			Extension listExtension = new Extension(
					"http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition",
					new ParameterDefinition()
							.setMax("*")
							.setName(parameters.getParameterFirstRep().getName()));
			parameters.getParameterFirstRep().addExtension(listExtension);
		}
		return parameters;
	}

	protected Map<String, Resource> getResourcesFromBundle(Bundle bundle) {
		// using HashMap to avoid duplicates
		Map<String, Resource> resourceMap = new HashMap<>();
		bundle
				.getEntry()
				.forEach(x -> resourceMap.put(x.fhirType() + x.getResource().getId(), x.getResource()));
		return resourceMap;
	}

	protected Bundle getPrefetchResources(CdsServiceRequestJson json) {
		// using HashMap to avoid duplicates
		Map<String, Resource> resourceMap = new HashMap<>();
		Bundle prefetchResources = new Bundle();
		Resource resource;
		for (String key : json.getPrefetchKeys()) {
			resource = (Resource) json.getPrefetch(key);
			if (resource == null) {
				continue;
			}
			if (resource instanceof Bundle) {
				resourceMap.putAll(getResourcesFromBundle((Bundle) resource));
			} else {
				resourceMap.put(resource.fhirType() + resource.getId(), resource);
			}
		}
		resourceMap.forEach((key, value) -> prefetchResources.addEntry().setResource(value));
		return prefetchResources;
	}

	public CdsServiceResponseJson encodeResponse(Object response) {
		assert response instanceof Bundle;
		responseBundle = (Bundle) response;
		serviceResponse = new CdsServiceResponseJson();
		if (responseBundle.hasEntry()) {
			RequestGroup mainRequest =
					(RequestGroup) responseBundle.getEntry().get(0).getResource();
			CanonicalType canonical = mainRequest.getInstantiatesCanonical().get(0);
			PlanDefinition planDef = repository.read(
					PlanDefinition.class,
					new IdType(Canonicals.getResourceType(canonical), Canonicals.getIdPart(canonical)));
			List<CdsServiceResponseLinkJson> links = resolvePlanLinks(planDef);
			mainRequest.getAction().forEach(action -> serviceResponse.addCard(resolveAction(action, links)));
		}

		return serviceResponse;
	}

	protected List<CdsServiceResponseLinkJson> resolvePlanLinks(PlanDefinition planDefinition) {
		List<CdsServiceResponseLinkJson> links = new ArrayList<>();
		// links - listed on each card
		if (planDefinition.hasRelatedArtifact()) {
			planDefinition.getRelatedArtifact().forEach(ra -> {
				String linkUrl = ra.getUrl();
				if (linkUrl != null) {
					CdsServiceResponseLinkJson link = new CdsServiceResponseLinkJson().setUrl(linkUrl);
					if (ra.hasDisplay()) {
						link.setLabel(ra.getDisplay());
					}
					if (ra.hasExtension()) {
						link.setType(ra.getExtensionFirstRep().getValue().primitiveValue());
					} else link.setType("absolute"); // default
					links.add(link);
				}
			});
		}
		return links;
	}

	protected CdsServiceResponseCardJson resolveAction(
			RequestGroup.RequestGroupActionComponent action, List<CdsServiceResponseLinkJson> links) {
		CdsServiceResponseCardJson card = new CdsServiceResponseCardJson()
				.setSummary(action.getTitle())
				.setDetail(action.getDescription())
				.setLinks(links);

		if (action.hasPriority()) {
			card.setIndicator(resolveIndicator(action.getPriority().toCode()));
		}

		if (action.hasDocumentation()) {
			card.setSource(resolveSource(action));
		}

		if (action.hasSelectionBehavior()) {
			card.setSelectionBehaviour(action.getSelectionBehavior().toCode());
			action.getAction().forEach(this::resolveSuggestion);
		}

		// Leaving this out until the spec details how to map system actions.
		//		if (action.hasType() && action.hasResource()) {
		//			resolveSystemAction(action);
		//		}

		return card;
	}

	protected CdsServiceIndicatorEnum resolveIndicator(String code) {
		CdsServiceIndicatorEnum indicator;
		switch (code) {
			case "routine":
				indicator = CdsServiceIndicatorEnum.INFO;
				break;
			case "urgent":
				indicator = CdsServiceIndicatorEnum.WARNING;
				break;
			case "stat":
				indicator = CdsServiceIndicatorEnum.CRITICAL;
				break;
			default:
				indicator = null;
				break;
		}
		if (indicator == null) {
			// Code 2435-2440 are reserved for this error message across versions
			throw new IllegalArgumentException(Msg.code(2435) + "Invalid priority code: " + code);
		}

		return indicator;
	}

	protected void resolveSystemAction(RequestGroup.RequestGroupActionComponent action) {
		if (action.hasType()
				&& action.getType().hasCoding()
				&& action.getType().getCodingFirstRep().hasCode()
				&& !action.getType().getCodingFirstRep().getCode().equals("fire-event")) {
			serviceResponse.addServiceAction(new CdsServiceResponseSystemActionJson()
					.setResource(resolveResource(action.getResource()))
					.setType(action.getType().getCodingFirstRep().getCode()));
		}
	}

	protected CdsServiceResponseCardSourceJson resolveSource(RequestGroup.RequestGroupActionComponent action) {
		RelatedArtifact documentation = action.getDocumentationFirstRep();
		CdsServiceResponseCardSourceJson source = new CdsServiceResponseCardSourceJson()
				.setLabel(documentation.getDisplay())
				.setUrl(documentation.getUrl());

		if (documentation.hasDocument() && documentation.getDocument().hasUrl()) {
			source.setIcon(documentation.getDocument().getUrl());
		}

		return source;
	}

	protected CdsServiceResponseSuggestionJson resolveSuggestion(RequestGroup.RequestGroupActionComponent action) {
		CdsServiceResponseSuggestionJson suggestion = new CdsServiceResponseSuggestionJson()
				.setLabel(action.getTitle())
				.setUuid(action.getId());
		action.getAction().forEach(actionInner -> suggestion.addAction(resolveSuggestionAction(actionInner)));

		return suggestion;
	}

	protected CdsServiceResponseSuggestionActionJson resolveSuggestionAction(
			RequestGroup.RequestGroupActionComponent action) {
		CdsServiceResponseSuggestionActionJson suggestionAction =
				new CdsServiceResponseSuggestionActionJson().setDescription(action.getDescription());
		if (action.hasType()
				&& action.getType().hasCoding()
				&& action.getType().getCodingFirstRep().hasCode()
				&& !action.getType().getCodingFirstRep().getCode().equals("fire-event")) {
			String actionCode = action.getType().getCodingFirstRep().getCode();
			suggestionAction.setType(actionCode);
		}
		if (action.hasResource()) {
			suggestionAction.setResource(resolveResource(action.getResource()));
			// Leaving this out until the spec details how to map system actions.
			//			if (!suggestionAction.getType().isEmpty()) {
			//				resolveSystemAction(action);
			//			}
		}

		return suggestionAction;
	}

	protected IBaseResource resolveResource(Reference reference) {
		String referenceToUse  = reference.getReference();
		String[] split = referenceToUse .split("/");
		String id = referenceToUse .contains("/") ? split[1] : referenceToUse ;
		String resourceType = referenceToUse .contains("/") ? split[0] : reference.getType();
		List<IBaseResource> results = responseBundle.getEntry().stream()
				.filter(entry -> entry.hasResource()
						&& entry.getResource().getResourceType().toString().equals(resourceType)
						&& entry.getResource().getIdPart().equals(id))
				.map(entry -> entry.getResource())
				.collect(Collectors.toList());
		return results.isEmpty() ? null : results.get(0);
	}
}
