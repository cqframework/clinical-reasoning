package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.opencds.cqf.fhir.cr.questionnaire.TestItemGenerator.given;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.CanonicalType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Ids;

class ItemGeneratorTests {
    private final String ROUTE_ONE_PATIENT_PROFILE =
            "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOnePatient";
    private final String SLEEP_STUDY_PATIENT = "positive";
    private final String SLEEP_STUDY_PROFILE =
            "http://example.org/sdh/dtr/aslp/StructureDefinition/aslp-sleep-study-order";
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();

    @Test
    void generateItemR4() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileUrl(new CanonicalType(ROUTE_ONE_PATIENT_PROFILE))
                .then()
                .hasItemCount(9)
                .itemHasInitialValue("1.4.1")
                .itemHasHiddenValueExtension("1.4.1")
                .itemHasInitialExpression("1.1.1");
    }

    @Test
    void generateItemR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .profileUrl(new org.hl7.fhir.r5.model.CanonicalType(ROUTE_ONE_PATIENT_PROFILE))
                .then()
                .hasItemCount(9)
                .itemHasInitialValue("1.4.1")
                .itemHasHiddenValueExtension("1.4.1")
                .itemHasInitialExpression("1.1.1");
    }

    @Test
    void sleepStudyOrderR4() {
        given().repositoryFor(fhirContextR4, "r4/pa-aslp")
                .when()
                .profileUrl(new CanonicalType(SLEEP_STUDY_PROFILE))
                .then()
                .hasItemCount(3)
                .hasId("aslp-sleep-study-order");
    }

    @Test
    void sleepStudyOrderR5() {
        given().repositoryFor(fhirContextR5, "r5/pa-aslp")
                .when()
                .profileUrl(new org.hl7.fhir.r5.model.CanonicalType(SLEEP_STUDY_PROFILE))
                .then()
                .hasItemCount(3)
                .hasId("aslp-sleep-study-order");
    }

    @Test
    void generateQuestionnaire() {
        given().repositoryFor(fhirContextR4, "r4").when().id("test").then().hasId("test");
    }

    @Test
    void generateItemForElementWithChildren() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileId(Ids.newId(fhirContextR4, "sigmoidoscopy-complication-casefeature-definition"))
                .then()
                .hasItemCount(6);
    }

    @Test
    void generateHiddenItem() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileId(Ids.newId(fhirContextR4, "sigmoidoscopy-complication-casefeature-definition2"))
                .then()
                .hasItemCount(3);
    }

    @Test
    void generate() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileId(Ids.newId(fhirContextR4, "chf-bodyweight-change"))
                .then()
                .hasItemCount(11);
    }

    @Test
    void generateWithLaunchContexts() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileId(Ids.newId(fhirContextR4, "LaunchContexts"))
                .then()
                .hasItemCount(3)
                .hasLaunchContextExtension(5);
    }
}
