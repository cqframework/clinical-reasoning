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
package org.opencds.cqf.fhir.cr.hapi.cdshooks.api;

import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestJson;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceFeedbackJson;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceJson;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceResponseJson;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServicesJson;
import java.util.function.Function;

/**
 * This registry holds all CDS Hooks services registered with the server.
 */
public interface ICdsServiceRegistry {
	/**
	 * This is the json returned by calling https://example.com/cds-services
	 *
	 * @return a list of CDS Hooks service descriptors
	 */
	CdsServicesJson getCdsServicesJson();

	/**
	 * This is the REST method available at https://example.com/cds-services/{serviceId}
	 *
	 * @param serviceId the id of the service to be called
	 * @param cdsServiceRequestJson the service request
	 * @return the service response
	 */
	CdsServiceResponseJson callService(String serviceId, Object cdsServiceRequestJson);

	/**
	 * This is the REST method available at https://example.com/cds-services/{serviceId}/feedback
	 *
	 * @param serviceId the id of the service that feedback is being sent for
	 * @param cdsServiceFeedbackJson the request
	 * @return the response
	 */
	CdsServiceFeedbackJson callFeedback(String serviceId, CdsServiceFeedbackJson cdsServiceFeedbackJson);

	// TODO:  LD:  javadoc?
	void registerService(
		String serviceId,
		CdsServiceJson cdsServiceJson,
		boolean allowAutoFhirClientPrefetch,
		String moduleId);

	/**
	 * Register a new CDS Service with the endpoint.
	 *
	 * @param serviceId                   the id of the service
	 * @param serviceFunction             the function that will be called to invoke the service
	 * @param cdsServiceJson              the service descriptor
	 * @param allowAutoFhirClientPrefetch Whether to allow the server to automatically prefetch resources
	 * @param moduleId                    the moduleId where the service is registered
	 */
	void registerService(
			String serviceId,
			Function<CdsServiceRequestJson, CdsServiceResponseJson> serviceFunction,
			CdsServiceJson cdsServiceJson,
			boolean allowAutoFhirClientPrefetch,
			String moduleId);

	/**
	 * Remove registered CDS service with the service ID, only removes dynamically registered service
	 *
	 * @param serviceId the id of the service to be removed
	 */
	void unregisterService(String serviceId, String moduleId);

	/**
	 * Get registered CDS service with service ID
	 * @param serviceId the id of the service to be retrieved
	 * @return CdsServiceJson
	 * @throws IllegalArgumentException if a CDS service with provided serviceId is not found
	 */
	CdsServiceJson getCdsServiceJson(String serviceId);
}
