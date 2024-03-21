package org.opencds.cqf.fhir.cr.questionnaireresponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.questionnaireresponse.TestQuestionnaireResponse.given;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.BundleHelper;
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

    @Test
    void testTherapyMonitoringRecommendation() {
        var questionnaireResponseId = "TherapyMonitoringRecommendation";
        var result = given().repositoryFor(fhirContextR4, "r4")
                .when()
                .questionnaireResponseId(questionnaireResponseId)
                .extract()
                .hasEntry(2)
                .getBundle();
        var resources = BundleHelper.getEntryResources(result);
        var obs1 = (Observation) resources.get(0);
        var obs2 = (Observation) resources.get(1);
        assertTrue(obs1.hasCode());
        assertTrue(obs1.hasStatus());
        assertTrue(obs1.hasValueCodeableConcept());
        assertTrue(obs2.hasCode());
        assertTrue(obs2.hasStatus());
        assertTrue(obs2.hasValueDateTimeType());
    }

    @Test
    void testExtractWithHiddenItems() {
        var questionnaireResponseId = "sigmoidoscopy-complication-casefeature-definition";
        var result = given().repositoryFor(fhirContextR4, "r4")
                .when()
                .questionnaireResponseId(questionnaireResponseId)
                .extract()
                .hasEntry(1)
                .getBundle();
        var resources = BundleHelper.getEntryResources(result);
        var obs = (Observation) resources.get(0);
        assertTrue(obs.hasCode());
        assertTrue(obs.hasSubject());
        assertTrue(obs.hasValueBooleanType());
    }
}
