package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.cr.questionnaire.TestItemGenerator.given;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r4.model.CanonicalType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.helpers.RequestHelpers;
import org.opencds.cqf.fhir.utility.Ids;

@SuppressWarnings("squid:S2699")
@ExtendWith(MockitoExtension.class)
class ItemGeneratorTests {
    private static final String ROUTE_ONE_PATIENT_PROFILE =
            "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOnePatient";
    private static final String SLEEP_STUDY_PROFILE =
            "http://example.org/sdh/dtr/aslp/StructureDefinition/aslp-sleep-study-order";
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();

    @Mock
    LibraryEngine libraryEngine;

    @Test
    void testGenerateRequest() {
        var request = RequestHelpers.newGenerateRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        assertThrows(UnsupportedOperationException.class, request::getSubjectId);
        assertThrows(UnsupportedOperationException.class, request::getData);
        assertThrows(UnsupportedOperationException.class, request::getUseServerData);
        assertThrows(UnsupportedOperationException.class, request::getParameters);
    }

    @Test
    void generateItemR4() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileUrl(new CanonicalType(ROUTE_ONE_PATIENT_PROFILE))
                .then()
                .hasItemCount(8)
                .itemHasInitialExpression("1.1.1");
    }

    @Test
    void generateItemR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .profileUrl(new org.hl7.fhir.r5.model.CanonicalType(ROUTE_ONE_PATIENT_PROFILE))
                .then()
                .hasItemCount(8)
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
                .hasItemCount(2);
    }

    @Test
    void generateItemWithDefaults() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileId(Ids.newId(fhirContextR4, "sigmoidoscopy-complication-casefeature-definition2"))
                .then()
                .hasItemCount(2);
    }

    @Test
    void generateWithLaunchContexts() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileId(Ids.newId(fhirContextR4, "LaunchContexts"))
                .then()
                .hasItemCount(2)
                .hasLaunchContextExtension(5)
                .itemHasExtractionValueExtension("1");
    }

    @Test
    @Disabled("nested profile generation not yet implemented")
    void generateItemForProfileWithExtensionSlices() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileId(Ids.newId(fhirContextR4, "NzPatient"))
                .then()
                .hasItemCount(11)
                .itemHasExtractionValueExtension("1");
    }
}
