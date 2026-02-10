package org.opencds.cqf.fhir.cr.hapi.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.dstu3.model.CarePlan;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.RequestGroup;
import org.junit.jupiter.api.Test;

class PlanDefinitionOperationsProviderIT extends BaseCrDstu3TestServer {
    @Test
    void testApply() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/dstu3/hello-world/hello-world-patient-view-bundle.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/dstu3/hello-world/hello-world-patient-data.json");
        var id = new IdType("PlanDefinition", "hello-world-patient-view");
        var planDefinition = (PlanDefinition) read(id);
        assertNotNull(planDefinition);

        var patientId = "Patient/helloworld-patient-1";
        var parameters = newParameters(getFhirContext(), newStringPart(getFhirContext(), "subject", patientId));
        var result = ourClient
                .operation()
                .onInstance(id)
                .named(ProviderConstants.CR_OPERATION_APPLY)
                .withParameters(parameters)
                .returnResourceType(CarePlan.class)
                .execute();

        assertNotNull(result);
        assertEquals(patientId, result.getSubject().getReference());
        assertEquals(1, result.getContained().size());
        var requestGroup = (RequestGroup) result.getContained().get(0);
        assertEquals(1, requestGroup.getAction().size());
        var action = requestGroup.getAction().get(0);
        assertEquals("Hello World!", action.getTitle());
    }
}
