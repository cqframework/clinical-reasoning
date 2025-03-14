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

import ca.uhn.fhir.context.FhirVersionEnum;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.CdsCrServiceDstu3;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.CdsCrServiceR4;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.CdsCrServiceR5;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.ICdsCrService;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.svc.cr.ICdsCrServiceRegistry;
import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CdsCrServiceRegistry implements ICdsCrServiceRegistry {
	private final Map<FhirVersionEnum, Class<? extends ICdsCrService>> cdsCrServices;

	public CdsCrServiceRegistry() {
		cdsCrServices = new HashMap<>();
		cdsCrServices.put(FhirVersionEnum.DSTU3, CdsCrServiceDstu3.class);
		cdsCrServices.put(FhirVersionEnum.R4, CdsCrServiceR4.class);
		cdsCrServices.put(FhirVersionEnum.R5, CdsCrServiceR5.class);
	}

	public void register(
			@Nonnull FhirVersionEnum fhirVersion, @Nonnull Class<? extends ICdsCrService> cdsCrService) {
		cdsCrServices.put(fhirVersion, cdsCrService);
	}

	public void unregister(@Nonnull FhirVersionEnum fhirVersion) {
		cdsCrServices.remove(fhirVersion);
	}

	public Optional<Class<? extends ICdsCrService>> find(@Nonnull FhirVersionEnum fhirVersion) {
		return Optional.ofNullable(cdsCrServices.get(fhirVersion));
	}
}
