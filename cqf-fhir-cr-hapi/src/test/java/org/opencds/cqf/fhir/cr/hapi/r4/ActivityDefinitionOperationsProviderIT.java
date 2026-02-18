package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.junit.jupiter.api.Test;

class ActivityDefinitionOperationsProviderIT extends BaseCrR4TestServer {
    @Test
    void testActivityDefinitionApply() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-ActivityDefinitionTest.json");
        var patientId = "patient-1";
        var parameters = newParameters(getFhirContext(), newStringPart(getFhirContext(), "subject", patientId));
        var result = ourClient
                .operation()
                .onInstance(new IdType("ActivityDefinition", "activityDefinition-test"))
                .named(ProviderConstants.CR_OPERATION_APPLY)
                .withParameters(parameters)
                .returnResourceType(MedicationRequest.class)
                .useHttpGet()
                .execute();
        assertInstanceOf(MedicationRequest.class, result);
        assertTrue(result.getDoNotPerform());
        assertEquals(String.format("Patient/%s", patientId), result.getSubject().getReference());
    }
}
