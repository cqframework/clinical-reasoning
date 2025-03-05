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
package org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json;

import ca.uhn.fhir.rest.api.server.cdshooks.BaseCdsServiceJson;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a CDS Hooks Service Response Card
 */
public class CdsServiceResponseCardJson extends BaseCdsServiceJson {
	@JsonProperty("uuid")
	String uuid;

	@JsonProperty(value = "summary", required = true)
	String summary;

	@JsonProperty("detail")
	String detail;

	@JsonProperty(value = "indicator", required = true)
	CdsServiceIndicatorEnum indicator;

	@JsonProperty(value = "source", required = true)
	CdsServiceResponseCardSourceJson source;

	@JsonProperty("suggestions")
	List<CdsServiceResponseSuggestionJson> suggestions;

	@JsonProperty("selectionBehavior")
	String selectionBehaviour;

	@JsonProperty("overrideReasons")
	List<CdsServiceResponseCodingJson> overrideReasons;

	@JsonProperty("links")
	List<CdsServiceResponseLinkJson> links;

	public String getSummary() {
		return summary;
	}

	public CdsServiceResponseCardJson setSummary(String summary) {
		this.summary = summary;
		return this;
	}

	public CdsServiceIndicatorEnum getIndicator() {
		return indicator;
	}

	public CdsServiceResponseCardJson setIndicator(CdsServiceIndicatorEnum indicator) {
        this.indicator = indicator;
		return this;
	}

	public CdsServiceResponseCardSourceJson getSource() {
		return source;
	}

	public CdsServiceResponseCardJson setSource(CdsServiceResponseCardSourceJson source) {
        this.source = source;
		return this;
	}

	public String getDetail() {
		return detail;
	}

	public CdsServiceResponseCardJson setDetail(String detail) {
        this.detail = detail;
		return this;
	}

	public String getUuid() {
		return uuid;
	}

	public CdsServiceResponseCardJson setUuid(String uuid) {
        this.uuid = uuid;
		return this;
	}

	public List<CdsServiceResponseSuggestionJson> getSuggestions() {
		return suggestions;
	}

	public void addSuggestion(CdsServiceResponseSuggestionJson suggestion) {
		if (suggestions == null) {
			suggestions = new ArrayList<>();
		}
		suggestions.add(suggestion);
	}

	public String getSelectionBehaviour() {
		return selectionBehaviour;
	}

	public CdsServiceResponseCardJson setSelectionBehaviour(String selectionBehaviour) {
		this.selectionBehaviour = selectionBehaviour;
		return this;
	}

	public List<CdsServiceResponseCodingJson> getOverrideReasons() {
		return overrideReasons;
	}

	public CdsServiceResponseCardJson setOverrideReasons(List<CdsServiceResponseCodingJson> overrideReasons) {
		this.overrideReasons = overrideReasons;
		return this;
	}

	public List<CdsServiceResponseLinkJson> getLinks() {
		return links;
	}

	public CdsServiceResponseCardJson setLinks(List<CdsServiceResponseLinkJson> links) {
        this.links = links;
		return this;
	}
}
