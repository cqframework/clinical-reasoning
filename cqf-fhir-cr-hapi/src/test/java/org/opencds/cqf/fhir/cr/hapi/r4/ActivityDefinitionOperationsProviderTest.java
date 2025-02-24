package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.r4.activitydefinition.ActivityDefinitionApplyProvider;
import org.springframework.beans.factory.annotation.Autowired;

class ActivityDefinitionOperationsProviderTest extends BaseCrR4TestServer {

    @Autowired
    ActivityDefinitionApplyProvider activityDefinitionApplyProvider;

    @Test
    void testActivityDefinitionApply() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/r4/Bundle-ActivityDefinitionTest.json");
        var requestDetails = setupRequestDetails();
        var result = activityDefinitionApplyProvider.apply(
                new IdType("activityDefinition-test"),
                null,
                null,
                "patient-1",
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
                requestDetails);
        assertInstanceOf(MedicationRequest.class, result);
        MedicationRequest request = (MedicationRequest) result;
        assertTrue(request.getDoNotPerform());
    }
}
