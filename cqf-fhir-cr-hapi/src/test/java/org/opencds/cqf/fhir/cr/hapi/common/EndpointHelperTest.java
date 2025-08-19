package org.opencds.cqf.fhir.cr.hapi.common;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencds.cqf.fhir.cr.hapi.common.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Date;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

class EndpointHelperTest {

    @Test
    void test() {
        assertNull(getEndpoint(null, null));
        assertInstanceOf(
                org.hl7.fhir.dstu3.model.Endpoint.class,
                getEndpoint(
                        FhirVersionEnum.DSTU3, new org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent()));
        assertInstanceOf(
                org.hl7.fhir.r4.model.Endpoint.class,
                getEndpoint(FhirVersionEnum.R4, new org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent()));
        assertInstanceOf(
                org.hl7.fhir.r5.model.Endpoint.class,
                getEndpoint(FhirVersionEnum.R5, new org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent()));

        ParametersParameterComponent parametersParameterComponent = new ParametersParameterComponent();
        parametersParameterComponent.setResource(new Patient().setBirthDate(new Date()));
        assertInstanceOf(
                org.hl7.fhir.r4.model.Resource.class, getEndpoint(FhirVersionEnum.R4, parametersParameterComponent));
    }
}
