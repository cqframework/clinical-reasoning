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
 * Represents the reason a specific service suggestion was overridden
 */
public class CdsServiceOverrideReasonJson implements IModelJson {
	@JsonProperty("reason")
    CdsServiceResponseCodingJson reason;

	@JsonProperty("userComment")
	String userComment;

	public CdsServiceResponseCodingJson getReason() {
		return reason;
	}

	public CdsServiceOverrideReasonJson setReason(CdsServiceResponseCodingJson reason) {
        this.reason = reason;
		return this;
	}

	public String getUserComment() {
		return userComment;
	}

	public CdsServiceOverrideReasonJson setUserComment(String userComment) {
        this.userComment = userComment;
		return this;
	}
}
