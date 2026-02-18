package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;

import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.Test;

class QuestionnaireResponseOperationsProviderIT extends BaseCrR4TestServer {
    @Test
    void testExtract() {
        var requestDetails = setupRequestDetails();
        loadResource(
                Questionnaire.class,
                "org/opencds/cqf/fhir/cr/hapi/r4/Questionnaire-MyPainQuestionnaire.json",
                requestDetails);
        var questionnaireResponse = readResource(
                QuestionnaireResponse.class,
                "org/opencds/cqf/fhir/cr/hapi/r4/QuestionnaireResponse-QRSharonDecision.json");
        var parameters = newParameters(
                getFhirContext(), newPart(getFhirContext(), "questionnaire-response", questionnaireResponse));
        var result = ourClient
                .operation()
                .onType(QuestionnaireResponse.class)
                .named(ProviderConstants.CR_OPERATION_EXTRACT)
                .withParameters(parameters)
                .returnResourceType(Bundle.class)
                .execute();

        assertNotNull(result);
        assertThat(result.getEntry()).hasSize(5);
    }
}
