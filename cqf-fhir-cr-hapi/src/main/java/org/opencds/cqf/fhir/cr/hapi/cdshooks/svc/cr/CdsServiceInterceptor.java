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
package org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr;

import static org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.CdsCrConstants.CDS_CR_MODULE_ID;

import ca.uhn.fhir.jpa.cache.IResourceChangeEvent;
import ca.uhn.fhir.jpa.cache.IResourceChangeListener;
import ca.uhn.fhir.jpa.cache.ResourceChangeEvent;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.ICdsServiceRegistry;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.discovery.ICrDiscoveryServiceFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdsServiceInterceptor implements IResourceChangeListener {
	static final Logger ourLog = LoggerFactory.getLogger(CdsServiceInterceptor.class);

	private final ICdsServiceRegistry cdsServiceRegistry;
	private final String moduleId;
	private final boolean allowFhirClientPrefetch;
	private final ICrDiscoveryServiceFactory discoveryServiceFactory;

	public CdsServiceInterceptor(
            ICdsServiceRegistry cdsServiceRegistry,
			String moduleId,
			boolean allowFhirClientPrefetch,
			ICrDiscoveryServiceFactory discoveryServiceFactory) {
		this.cdsServiceRegistry = cdsServiceRegistry;
        this.moduleId = moduleId;
        this.allowFhirClientPrefetch = allowFhirClientPrefetch;
        this.discoveryServiceFactory = discoveryServiceFactory;
	}

	@Override
	public void handleInit(Collection<IIdType> resourceIds) {
		handleChange(ResourceChangeEvent.fromCreatedUpdatedDeletedResourceIds(
				new ArrayList<>(resourceIds), Collections.emptyList(), Collections.emptyList()));
	}

	@Override
	public void handleChange(IResourceChangeEvent resourceChangeEvent) {
		if (resourceChangeEvent == null) return;
		if (resourceChangeEvent.getCreatedResourceIds() != null
				&& !resourceChangeEvent.getCreatedResourceIds().isEmpty()) {
			insert(resourceChangeEvent.getCreatedResourceIds());
		}
		if (resourceChangeEvent.getUpdatedResourceIds() != null
				&& !resourceChangeEvent.getUpdatedResourceIds().isEmpty()) {
			update(resourceChangeEvent.getUpdatedResourceIds());
		}
		if (resourceChangeEvent.getDeletedResourceIds() != null
				&& !resourceChangeEvent.getDeletedResourceIds().isEmpty()) {
			delete(resourceChangeEvent.getDeletedResourceIds());
		}
	}

	private void insert(List<IIdType> createdIds) {
		for (IIdType id : createdIds) {
			try {
				final String serviceId = id.getIdPart();
				cdsServiceRegistry.registerService(
					serviceId,
					discoveryServiceFactory.create(serviceId).resolveService(),
					allowFhirClientPrefetch,
					moduleId);
			} catch (Exception e) {
				ourLog.info(String.format("Failed to create service for %s", id.getIdPart()));
			}
		}
	}

	private void update(List<IIdType> updatedIds) {
		try {
			delete(updatedIds);
			insert(updatedIds);
		} catch (Exception e) {
			ourLog.info(String.format("Failed to update service(s) for %s", updatedIds));
		}
	}

	private void delete(List<IIdType> deletedIds) {
		for (IIdType id : deletedIds) {
			cdsServiceRegistry.unregisterService(id.getIdPart(), CDS_CR_MODULE_ID);
		}
	}
}
