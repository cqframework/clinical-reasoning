package ca.uhn.fhir.cr.r4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.cr.r4.questionnaireresponse.QuestionnaireResponseExtractProvider;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class QuestionnaireResponseOperationsProviderTest extends BaseCrR4TestServer {
    @Autowired
    QuestionnaireResponseExtractProvider questionnaireResponseExtractProvider;

    @Test
    void testExtract() {
        var requestDetails = setupRequestDetails();
        loadResource(Questionnaire.class, "ca/uhn/fhir/cr/r4/Questionnaire-MyPainQuestionnaire.json", requestDetails);
        var questionnaireResponse = readResource(
                QuestionnaireResponse.class, "ca/uhn/fhir/cr/r4/QuestionnaireResponse-QRSharonDecision.json");
        var result = (Bundle) questionnaireResponseExtractProvider.extract(
                null, questionnaireResponse, null, null, null, null, requestDetails);

        assertNotNull(result);
        assertThat(result.getEntry()).hasSize(5);
    }
}
