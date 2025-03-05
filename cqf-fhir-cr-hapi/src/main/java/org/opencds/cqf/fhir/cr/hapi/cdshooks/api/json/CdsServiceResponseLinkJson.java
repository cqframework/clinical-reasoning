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
 * Link used within a Cds Service Response
 */
public class CdsServiceResponseLinkJson implements IModelJson {
	@JsonProperty(value = "label", required = true)
	String label;

	@JsonProperty(value = "url", required = true)
	String url;

	@JsonProperty(value = "type", required = true)
	String type;

	@JsonProperty(value = "appContext")
	String appContext;

	public String getLabel() {
		return label;
	}

	public CdsServiceResponseLinkJson setLabel(String label) {
        this.label = label;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public CdsServiceResponseLinkJson setUrl(String url) {
        this.url = url;
		return this;
	}

	public String getType() {
		return type;
	}

	public CdsServiceResponseLinkJson setType(String type) {
        this.type = type;
		return this;
	}

	public String getAppContext() {
		return appContext;
	}

	public CdsServiceResponseLinkJson setAppContext(String appContext) {
        this.appContext = appContext;
		return this;
	}
}
