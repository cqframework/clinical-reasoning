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
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.CdsResolutionStrategyEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a CDS Hooks Service descriptor
 *
 * @see <a href="https://cds-hooks.hl7.org/ballots/2020Sep/">Version 1.1 of the CDS Hooks Specification</a>
 */
public class CdsServiceJson extends BaseCdsServiceJson {
	public static final String HOOK = "hook";
	public static final String TITLE = "title";
	public static final String DESCRIPTION = "description";
	public static final String ID = "id";
	public static final String PREFETCH = "prefetch";

	@JsonProperty(value = HOOK, required = true)
	String hook;

	@JsonProperty(value = TITLE)
	String title;

	@JsonProperty(value = DESCRIPTION, required = true)
	String description;

	@JsonProperty(value = ID, required = true)
	String id;

	@JsonProperty(PREFETCH)
	private Map<String, String> prefetch;

	private Map<String, CdsResolutionStrategyEnum> source;

	public String getHook() {
		return hook;
	}

	public CdsServiceJson setHook(String hook) {
        this.hook = hook;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public CdsServiceJson setTitle(String title) {
        this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public CdsServiceJson setDescription(String description) {
        this.description = description;
		return this;
	}

	public String getId() {
		return id;
	}

	public CdsServiceJson setId(String id) {
        this.id = id;
		return this;
	}

	public void addPrefetch(String key, String query) {
		if (prefetch == null) {
			prefetch = new LinkedHashMap<>();
		}
		prefetch.put(key, query);
	}

	public Map<String, String> getPrefetch() {
		if (prefetch == null) {
			prefetch = new LinkedHashMap<>();
		}
		return Collections.unmodifiableMap(prefetch);
	}

	public void addSource(String key, CdsResolutionStrategyEnum source) {
		if (this.source == null) {
			this.source = new LinkedHashMap<>();
		}
        this.source.put(key, source);
	}

	public Map<String, CdsResolutionStrategyEnum> getSource() {
		if (source == null) {
			source = new LinkedHashMap<>();
		}
		return Collections.unmodifiableMap(source);
	}
}
