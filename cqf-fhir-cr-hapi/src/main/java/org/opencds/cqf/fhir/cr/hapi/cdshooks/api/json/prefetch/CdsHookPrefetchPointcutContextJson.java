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
package org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.prefetch;

import ca.uhn.fhir.model.api.IModelJson;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.CdsResolutionStrategyEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contains the pointcut context of a CDS Hooks Prefetch Request
 *
 * @see <a href="https://cds-hooks.hl7.org/ballots/2020Sep/">Version 1.1 of the CDS Hooks Specification</a>
 */
public class CdsHookPrefetchPointcutContextJson implements IModelJson {

	public static final String TEMPLATE = "template";
	public static final String QUERY = "query";
	public static final String RESOLUTION_STRATEGY = "resolutionStrategy";
	public static final String USER_DATA = "userData";

	/**
	 * The prefetch template for the prefetch request
	 */
	@JsonProperty(value = TEMPLATE, required = true)
	String template;

	/**
	 * How the prefetch query will be executed (valid values include FHIR_CLIENT and DAO)
	 */
	@JsonProperty(value = RESOLUTION_STRATEGY, required = true)
	private CdsResolutionStrategyEnum cdsResolutionStrategy;

	/**
	 * The actual prefetch query, generated based on the prefetch template using the prefetch context
	 */
	@JsonProperty(value = QUERY, required = true)
	String query;

	/**
	 * Data to be stored between pointcut invocations of a prefetch request/response
	 */
	@JsonProperty(USER_DATA)
	private Map<String, Object> userData;

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public CdsResolutionStrategyEnum getCdsResolutionStrategy() {
		return cdsResolutionStrategy;
	}

	public void setCdsResolutionStrategy(CdsResolutionStrategyEnum cdsResolutionStrategy) {
        this.cdsResolutionStrategy = cdsResolutionStrategy;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
        this.query = query;
	}

	public void addUserData(String key, Object value) {
		if (userData == null) {
			userData = new LinkedHashMap<>();
		}
		userData.put(key, value);
	}

	public Object getUserData(String key) {
		if (userData == null) {
			userData = new LinkedHashMap<>();
		}
		return userData.get(key);
	}
}
