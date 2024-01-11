package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.opencds.cqf.fhir.cr.questionnaire.TestItemGenerator.given;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.junit.jupiter.api.Test;

public class ItemGeneratorTests {
    private final String ROUTE_ONE_PATIENT = "OPA-Patient1";
    private final String ROUTE_ONE_PATIENT_PROFILE =
            "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOnePatient";
    private final String SLEEP_STUDY_PATIENT = "positive";
    private final String SLEEP_STUDY_PROFILE =
            "http://example.org/sdh/dtr/aslp/StructureDefinition/aslp-sleep-study-order";
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();

    @Test
    void testGenerateItemDstu3() {
        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .subjectId(ROUTE_ONE_PATIENT)
                .profileUrl(new StringType(ROUTE_ONE_PATIENT_PROFILE))
                .then()
                .hasItemCount(6)
                .itemHasInitialValue();
    }

    @Test
    void testGenerateItemR4() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .subjectId(ROUTE_ONE_PATIENT)
                .profileUrl(new CanonicalType(ROUTE_ONE_PATIENT_PROFILE))
                .then()
                .hasItemCount(6)
                .itemHasInitialValue();
    }

    @Test
    void testGenerateItemR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .subjectId(ROUTE_ONE_PATIENT)
                .profileUrl(new org.hl7.fhir.r5.model.CanonicalType(ROUTE_ONE_PATIENT_PROFILE))
                .then()
                .hasItemCount(6)
                .itemHasInitialValue();
    }

    @Test
    void testSleepStudyOrderR4() {
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .profileUrl(new CanonicalType(SLEEP_STUDY_PROFILE))
                .then()
                .hasItemCount(2)
                .hasId("aslp-sleep-study-order");
    }

    @Test
    void testSleepStudyOrderR5() {
        given().repositoryFor(fhirContextR5, "r5/pa-aslp")
                .when()
                .profileUrl(new org.hl7.fhir.r5.model.CanonicalType(SLEEP_STUDY_PROFILE))
                .then()
                .hasItemCount(2)
                .hasId("aslp-sleep-study-order");
    }
}
