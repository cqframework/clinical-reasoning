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
import ca.uhn.fhir.model.api.IModelJson;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestJson;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.api.json.CdsServiceResponseJson;
import java.util.Collections;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;

public interface ICdsCrService {
	IBaseParameters encodeParams(CdsServiceRequestJson json);

	CdsServiceResponseJson encodeResponse(Object response);

	FhirVersionEnum getFhirVersion();

	Repository getRepository();

	default Object invoke(IModelJson json) {
		IBaseParameters params = encodeParams((CdsServiceRequestJson) json);
		IBaseResource response = invokeApply(params);
		return encodeResponse(response);
	}

	default IBaseResource invokeApply(IBaseParameters params) {
		var operationName = getFhirVersion() == FhirVersionEnum.R4
				? ProviderConstants.CR_OPERATION_R5_APPLY
				: ProviderConstants.CR_OPERATION_APPLY;
		switch (getFhirVersion()) {
			case DSTU3:
				return getRepository()
						.invoke(
								org.hl7.fhir.dstu3.model.PlanDefinition.class,
								operationName,
								params,
								org.hl7.fhir.dstu3.model.CarePlan.class,
								Collections.singletonMap(Constants.HEADER_CONTENT_TYPE, Constants.CT_FHIR_JSON));
			case R4:
				return getRepository()
						.invoke(
								org.hl7.fhir.r4.model.PlanDefinition.class,
								operationName,
								params,
								org.hl7.fhir.r4.model.Bundle.class,
								Collections.singletonMap(Constants.HEADER_CONTENT_TYPE, Constants.CT_FHIR_JSON));
			case R5:
				return getRepository()
						.invoke(
								org.hl7.fhir.r5.model.PlanDefinition.class,
								operationName,
								params,
								org.hl7.fhir.r5.model.Bundle.class,
								Collections.singletonMap(Constants.HEADER_CONTENT_TYPE, Constants.CT_FHIR_JSON));
			default:
				return null;
		}
	}
}
