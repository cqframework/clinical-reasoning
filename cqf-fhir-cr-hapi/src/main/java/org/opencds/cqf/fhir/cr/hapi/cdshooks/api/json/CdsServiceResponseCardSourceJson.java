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

import ca.uhn.fhir.model.api.IModelJson;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a CDS Hooks Service Response Card Source
 */
public class CdsServiceResponseCardSourceJson implements IModelJson {
	@JsonProperty(value = "label", required = true)
	String label;

	@JsonProperty("url")
	String url;

	@JsonProperty("icon")
	String icon;

	@JsonProperty("topic")
    CdsServiceResponseCodingJson topic;

	public String getLabel() {
		return label;
	}

	public CdsServiceResponseCardSourceJson setLabel(String label) {
        this.label = label;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public CdsServiceResponseCardSourceJson setUrl(String url) {
        this.url = url;
		return this;
	}

	public String getIcon() {
		return icon;
	}

	public CdsServiceResponseCardSourceJson setIcon(String icon) {
		this.icon = icon;
		return this;
	}

	public CdsServiceResponseCodingJson getTopic() {
		return topic;
	}

	public CdsServiceResponseCardSourceJson setTopic(CdsServiceResponseCodingJson topic) {
		this.topic = topic;
		return this;
	}
}
