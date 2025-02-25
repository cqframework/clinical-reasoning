package org.opencds.cqf.fhir.cr.questionnaireresponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.questionnaireresponse.TestQuestionnaireResponse.given;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.questionnaireresponse.extract.ExtractProcessor;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Ids;

@SuppressWarnings("squid:S2699")
class QuestionnaireResponseProcessorTests {
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
    void processors() {
        var bundle = given().repositoryFor(fhirContextR4, "r4")
                .extractProcessor(new ExtractProcessor())
                .when()
                .questionnaireResponseId("QRSharonDecision")
                .extract()
                .getBundle();
        assertNotNull(bundle);
    }

    @Test
    void test() {
        testExtract(fhirContextR4, "r4", "QRSharonDecision");
        testExtract(fhirContextR5, "r5", "QRSharonDecision");
    }

    @Test
    void extractNoQuestionnaireReferenceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            testExtract(fhirContextR4, "r4", "mypain-no-url");
        });
    }

    @Test
    void isSubjectExtension() {
        testExtract(fhirContextR4, "r4", "sdc-profile-example-multi-subject");
        testExtract(fhirContextR5, "r5", "sdc-profile-example-multi-subject");
    }

    @Test
    void definitionBasedExtraction() {
        var questionnaireResponseId = "OutpatientPriorAuthorizationRequest-OPA-Patient1";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .questionnaireResponseId(questionnaireResponseId)
                .extract()
                .hasEntry(4);
    }

    @Test
    void nestedDefinitionBasedExtraction() {
        var questionnaireResponseId = "cc-screening-pathway-definition-answers";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .questionnaireResponseId(questionnaireResponseId)
                .extract()
                .hasEntry(3);
    }

    @Test
    void therapyMonitoringRecommendation() {
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
    void extractWithHiddenItems() {
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
        assertTrue(obs.hasValueBooleanType());
    }

    @Test
    void extractWithQuestionnaireUnitExt() {
        var questionnaireResponseId = "NumericExtract";
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .questionnaireResponseId(questionnaireResponseId)
                .extract()
                .hasEntry(2);
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .questionnaireResponseId(questionnaireResponseId)
                .extract()
                .hasEntry(2);
    }

    @Test
    void definitionExtractAtRoot() {
        var questionnaireResponseId = "definition-OPA-Patient1";
        var bundle = given().repositoryFor(fhirContextR4, "r4")
                .when()
                .questionnaireResponseId(questionnaireResponseId)
                .extract()
                .hasEntry(1)
                .getBundle();
        var organization = (Organization) BundleHelper.getEntryResourceFirstRep(bundle);
        assertNotNull(organization);
        assertEquals(String.format("extract-%s", questionnaireResponseId), organization.getIdPart());
        assertEquals("Acme Clinic", organization.getName());
    }

    @Test
    void definitionExtractWithProfileWithSlices() {
        var questionnaireResponseId = "extract-defn-walkthrough-10";
        var bundle = given().repositoryFor(fhirContextR4, "r4")
                .when()
                .questionnaireResponseId(questionnaireResponseId)
                .extract()
                .hasEntry(1)
                .getBundle();
        var patient = (Patient) BundleHelper.getEntryResourceFirstRep(bundle);
        assertNotNull(patient);
        assertEquals("test", patient.getNameFirstRep().getGiven().get(0).getValue());
        assertEquals("test2", patient.getNameFirstRep().getGiven().get(1).getValue());
        assertEquals("family", patient.getNameFirstRep().getFamily());
        assertEquals("1912-12-12", patient.getBirthDateElement().getValueAsString());
        assertEquals("male", patient.getGender().toCode());
        assertEquals("123", patient.getIdentifierFirstRep().getValue());
        assertEquals("official", patient.getIdentifierFirstRep().getUse().toCode());
        assertEquals(
                "https://standards.digital.health.nz/ns/nhi-id",
                patient.getIdentifierFirstRep().getSystem());
        assertEquals(
                "dasdasd",
                ((CodeableConcept) patient.getExtensionByUrl("http://hl7.org.nz/fhir/StructureDefinition/dhb")
                                .getValue())
                        .getText());
    }
}
