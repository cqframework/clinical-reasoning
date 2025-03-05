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
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceResponseCardJson;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a CDS Hooks Service Response
 *
 * @see <a href="https://cds-hooks.hl7.org/ballots/2020Sep/">Version 1.1 of the CDS Hooks Specification</a>
 */
public class CdsServiceResponseJson implements IModelJson {
	@JsonProperty(value = "cards", required = true)
	private final List<CdsServiceResponseCardJson> cards = new ArrayList<>();

	@JsonProperty("systemActions")
	List<CdsServiceResponseSystemActionJson> serviceActions;

	public void addCard(CdsServiceResponseCardJson cdsServiceResponseCardJson) {
		cards.add(cdsServiceResponseCardJson);
	}

	public List<CdsServiceResponseCardJson> getCards() {
		return cards;
	}

	public void addServiceAction(CdsServiceResponseSystemActionJson cdsServiceResponseSystemActionJson) {
		if (serviceActions == null) {
			serviceActions = new ArrayList<>();
		}
		serviceActions.add(cdsServiceResponseSystemActionJson);
	}

	public List<CdsServiceResponseSystemActionJson> getServiceActions() {
		return serviceActions;
	}
}
