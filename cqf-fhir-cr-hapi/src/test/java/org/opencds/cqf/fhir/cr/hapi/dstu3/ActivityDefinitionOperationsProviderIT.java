package org.opencds.cqf.fhir.cr.hapi.dstu3;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.dstu3.activitydefinition.ActivityDefinitionApplyProvider;
import org.springframework.beans.factory.annotation.Autowired;

class ActivityDefinitionOperationsProviderIT extends BaseCrDstu3TestServer {

    @Autowired
    ActivityDefinitionApplyProvider activityDefinitionApplyProvider;

    @Test
    void testActivityDefinitionApply() {
        loadBundle("org/opencds/cqf/fhir/cr/hapi/dstu3/Bundle-ActivityDefinitionTest.json");
        var requestDetails = setupRequestDetails();
        var result = activityDefinitionApplyProvider.apply(
                new IdType("activityDefinition-test"),
                null,
                null,
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
        assertInstanceOf(ProcedureRequest.class, result);
        ProcedureRequest request = (ProcedureRequest) result;
        assertTrue(request.getDoNotPerform());
    }
}
