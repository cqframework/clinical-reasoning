package org.opencds.cqf.fhir.cr.questionnaireresponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.cr.questionnaireresponse.TestQuestionnaireResponse.given;

import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Ids;

public class QuestionnaireResponseProcessorTests {
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();

    private void testExtract(FhirContext fhirContext, String path, String questionnaireResponse) {
        given().repositoryFor(fhirContext, path)
                .when()
                .questionnaireResponseId(questionnaireResponse)
                .extract()
                .isEqualsToExpected(Ids.newId(fhirContext, "Bundle", "extract-" + questionnaireResponse));
    }

    @Test
    void test() {
        testExtract(fhirContextDstu3, "dstu3", "QRSharonDecision");
        testExtract(fhirContextR4, "r4", "QRSharonDecision");
        testExtract(fhirContextR4, "r5", "QRSharonDecision");
    }

    @Test
    void testExtract_noQuestionnaireReference_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            testExtract(fhirContextR4, "r4", "mypain-no-url");
        });
    }

    @Test
    void testIsSubjectExtension() {
        testExtract(fhirContextDstu3, "dstu3", "sdc-profile-example-multi-subject");
        testExtract(fhirContextR4, "r4", "sdc-profile-example-multi-subject");
        testExtract(fhirContextR5, "r5", "sdc-profile-example-multi-subject");
    }

    @Test
    void testDefinitionBasedExtraction() {
        var questionnaireResponseId = "OutpatientPriorAuthorizationRequest-OPA-Patient1";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .questionnaireResponseId(questionnaireResponseId)
                .extract()
                .hasEntry(2);
    }

    @Test
    void testNestedDefinitionBasedExtraction() {
        var questionnaireResponseId = "cc-screening-pathway-definition-answers";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .questionnaireResponseId(questionnaireResponseId)
                .extract()
                .hasEntry(3);
    }
}
