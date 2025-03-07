package org.opencds.cqf.fhir.cr.hapi.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CarePlan;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.RequestGroup;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.dstu3.plandefinition.PlanDefinitionApplyProvider;
import org.springframework.beans.factory.annotation.Autowired;

class PlanDefinitionOperationsProviderIT extends BaseCrDstu3TestServer {
    @Autowired
    PlanDefinitionApplyProvider planDefinitionApplyProvider;

    @Test
    void testApply() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/dstu3/hello-world/hello-world-patient-view-bundle.json");
        loadBundle("org/opencds/cqf/fhir/cr/hapi/dstu3/hello-world/hello-world-patient-data.json");
        var planDefinition = (PlanDefinition) read(new IdType("PlanDefinition", "hello-world-patient-view"));

        var requestDetails = setupRequestDetails();
        var patientId = "Patient/helloworld-patient-1";
        var result = (CarePlan) planDefinitionApplyProvider.apply(
                null,
                planDefinition,
                null,
                null,
                null,
                patientId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new BooleanType(true),
                null,
                null,
                null,
                null,
                null,
                requestDetails);

        assertNotNull(result);
        assertEquals(patientId, result.getSubject().getReference());
        assertEquals(1, result.getContained().size());
        var requestGroup = (RequestGroup) result.getContained().get(0);
        assertEquals(1, requestGroup.getAction().size());
        var action = requestGroup.getAction().get(0);
        assertEquals("Hello World!", action.getTitle());
    }
}
