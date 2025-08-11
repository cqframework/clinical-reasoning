package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;

public class EndpointHelper {

    private EndpointHelper() {}

    public static IBaseResource getEndpoint(IParametersParameterComponentAdapter param) {
        return param.hasResource()
                ? param.getResource()
                : newEndpointResource(param.fhirContext().getVersion().getVersion(), param.getPrimitiveValue());
    }

    public static IBaseResource getEndpoint(FhirVersionEnum fhirVersion, IBaseBackboneElement param) {
        if (param == null) {
            return null;
        }
        var adapter = IAdapterFactory.forFhirVersion(fhirVersion).createParametersParameter(param);
        return getEndpoint(adapter);
    }

    private static IBaseResource newEndpointResource(FhirVersionEnum fhirVersion, String address) {
        return switch (fhirVersion) {
            case DSTU3 -> new org.hl7.fhir.dstu3.model.Endpoint().setAddress(address);
            case R4 -> new org.hl7.fhir.r4.model.Endpoint().setAddress(address);
            case R5 -> new org.hl7.fhir.r5.model.Endpoint().setAddress(address);
            default -> null;
        };
    }
}
