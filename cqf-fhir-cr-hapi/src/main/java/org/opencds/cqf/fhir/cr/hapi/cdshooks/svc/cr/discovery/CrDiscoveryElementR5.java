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
package org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.discovery;

import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.CdsResolutionStrategyEnum;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceJson;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.TriggerDefinition;
import org.hl7.fhir.r5.model.PlanDefinition;

public class CrDiscoveryElementR5 implements ICrDiscoveryElement {
	protected PlanDefinition planDefinition;
	protected PrefetchUrlList prefetchUrlList;

	public CrDiscoveryElementR5(PlanDefinition planDefinition, PrefetchUrlList prefetchUrlList) {
		this.planDefinition = planDefinition;
        this.prefetchUrlList = prefetchUrlList;
	}

	public CdsServiceJson getCdsServiceJson() {
		if (planDefinition == null
				|| !planDefinition.hasAction()
				|| planDefinition.getAction().stream().noneMatch(a -> a.hasTrigger())) {
			return null;
		}

		var triggerDefs = planDefinition.getAction().stream()
				.filter(a -> a.hasTrigger())
				.flatMap(a -> a.getTrigger().stream())
				.filter(t -> t.getType().equals(TriggerDefinition.TriggerType.NAMEDEVENT))
				.collect(Collectors.toList());
		if (triggerDefs == null || triggerDefs.isEmpty()) {
			return null;
		}

		var service = new CdsServiceJson()
				.setId(planDefinition.getIdElement().getIdPart())
				.setTitle(planDefinition.getTitle())
				.setDescription(planDefinition.getDescription())
				.setHook(triggerDefs.get(0).getName());

		if (prefetchUrlList == null) {
			prefetchUrlList = new PrefetchUrlList();
		}

		int itemNo = 0;
		if (!prefetchUrlList.stream()
				.anyMatch(p -> p.equals("Patient/{{context.patientId}}")
						|| p.equals("Patient?_id={{context.patientId}}")
						|| p.equals("Patient?_id=Patient/{{context.patientId}}"))) {
			String key = getKey(++itemNo);
			service.addPrefetch(key, "Patient?_id={{context.patientId}}");
			service.addSource(key, CdsResolutionStrategyEnum.FHIR_CLIENT);
		}

		for (String item : prefetchUrlList) {
			String key = getKey(++itemNo);
			service.addPrefetch(key, item);
			service.addSource(key, CdsResolutionStrategyEnum.FHIR_CLIENT);
		}

		return service;
	}
}
