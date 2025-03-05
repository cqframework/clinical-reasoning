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
 * Coding using within CdsService responses
 */
public class CdsServiceResponseCodingJson implements IModelJson {
	@JsonProperty(value = "code", required = true)
	String code;

	@JsonProperty("system")
	String system;

	@JsonProperty("display")
	String display;

	public String getCode() {
		return code;
	}

	public CdsServiceResponseCodingJson setCode(String code) {
        this.code = code;
		return this;
	}

	public String getSystem() {
		return system;
	}

	public CdsServiceResponseCodingJson setSystem(String system) {
        this.system = system;
		return this;
	}

	public String getDisplay() {
		return display;
	}

	public CdsServiceResponseCodingJson setDisplay(String display) {
        this.display = display;
		return this;
	}
}
