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
import java.util.List;

/**
 * Represents a CDS Hooks Service Feedback Request
 *
 * @see <a href="https://cds-hooks.hl7.org/ballots/2020Sep/">Version 1.1 of the CDS Hooks Specification</a>
 */
public class CdsServiceFeedbackJson implements IModelJson {
	@JsonProperty(value = "card", required = true)
	String card;

	@JsonProperty(value = "outcome", required = true)
    CdsServiceFeebackOutcomeEnum outcome;

	@JsonProperty(value = "acceptedSuggestions")
	List<CdsServiceAcceptedSuggestionJson> acceptedSuggestions;

	@JsonProperty(value = "overrideReason")
	CdsServiceOverrideReasonJson overrideReason;

	@JsonProperty(value = "outcomeTimestamp", required = true)
	String outcomeTimestamp;

	public String getCard() {
		return card;
	}

	public CdsServiceFeedbackJson setCard(String card) {
        this.card = card;
		return this;
	}

	public CdsServiceFeebackOutcomeEnum getOutcome() {
		return outcome;
	}

	public CdsServiceFeedbackJson setOutcome(CdsServiceFeebackOutcomeEnum outcome) {
        this.outcome = outcome;
		return this;
	}

	public List<CdsServiceAcceptedSuggestionJson> getAcceptedSuggestions() {
		return acceptedSuggestions;
	}

	public CdsServiceFeedbackJson setAcceptedSuggestions(
			List<CdsServiceAcceptedSuggestionJson> acceptedSuggestions) {
        this.acceptedSuggestions = acceptedSuggestions;
		return this;
	}

	public CdsServiceOverrideReasonJson getOverrideReason() {
		return overrideReason;
	}

	public CdsServiceFeedbackJson setOverrideReason(CdsServiceOverrideReasonJson overrideReason) {
        this.overrideReason = overrideReason;
		return this;
	}

	public String getOutcomeTimestamp() {
		return outcomeTimestamp;
	}

	public CdsServiceFeedbackJson setOutcomeTimestamp(String outcomeTimestamp) {
        this.outcomeTimestamp = outcomeTimestamp;
		return this;
	}
}
