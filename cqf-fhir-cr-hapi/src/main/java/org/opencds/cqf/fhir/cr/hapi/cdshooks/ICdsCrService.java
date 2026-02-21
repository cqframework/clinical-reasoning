package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IModelJson;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestJson;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseJson;
import java.util.Collections;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface ICdsCrService {
    IBaseParameters encodeParams(CdsServiceRequestJson json);

    CdsServiceResponseJson encodeResponse(Object response);

    FhirVersionEnum getFhirVersion();

    IRepository getRepository();

    default Object invoke(IModelJson json) {
        IBaseParameters params = encodeParams((CdsServiceRequestJson) json);
        IBaseResource response = invokeApply(params);
        return encodeResponse(response);
    }

    /**
     * This method was intentionally declared in the interface definition to promote and facilitate extensibility.
     * @param params parameters specifying the 'apply' operation.
     * @return an encoded response to the operation invocation.
     */
    default IBaseResource invokeApply(IBaseParameters params) {
        var operationName = getFhirVersion() == FhirVersionEnum.R4
                ? ProviderConstants.CR_OPERATION_R5_APPLY
                : ProviderConstants.CR_OPERATION_APPLY;
        return switch (getFhirVersion()) {
            case DSTU3 ->
                getRepository()
                        .invoke(
                                org.hl7.fhir.dstu3.model.PlanDefinition.class,
                                operationName,
                                params,
                                org.hl7.fhir.dstu3.model.CarePlan.class,
                                Collections.singletonMap(Constants.HEADER_CONTENT_TYPE, Constants.CT_FHIR_JSON));
            case R4 ->
                getRepository()
                        .invoke(
                                org.hl7.fhir.r4.model.PlanDefinition.class,
                                operationName,
                                params,
                                org.hl7.fhir.r4.model.Parameters.class,
                                Collections.singletonMap(Constants.HEADER_CONTENT_TYPE, Constants.CT_FHIR_JSON));
            case R5 ->
                getRepository()
                        .invoke(
                                org.hl7.fhir.r5.model.PlanDefinition.class,
                                operationName,
                                params,
                                org.hl7.fhir.r5.model.Parameters.class,
                                Collections.singletonMap(Constants.HEADER_CONTENT_TYPE, Constants.CT_FHIR_JSON));
            default -> null;
        };
    }
}
