package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opencds.cqf.fhir.cr.questionnaire.TestItemGenerator.given;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.r4.model.CanonicalType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.helpers.RequestHelpers;
import org.opencds.cqf.fhir.utility.Ids;

@SuppressWarnings({"squid:S2699", "UnstableApiUsage"})
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

    @Mock
    IRepository repository;

    @Spy
    @InjectMocks
    ItemGenerator fixture;

    @Test
    void testBuildSdcLaunchContextExt() {
        var dstu3Request = RequestHelpers.newGenerateRequestForVersion(FhirVersionEnum.DSTU3, libraryEngine);
        var dstu3Ext = (org.hl7.fhir.dstu3.model.Extension)
                fixture.buildSdcLaunchContextExt(dstu3Request, "patient", "Patient");
        assertInstanceOf(
                org.hl7.fhir.dstu3.model.Coding.class,
                dstu3Ext.getExtensionByUrl("name").getValue());
        assertInstanceOf(
                org.hl7.fhir.dstu3.model.CodeType.class,
                dstu3Ext.getExtensionByUrl("type").getValue());

        var r4Request = RequestHelpers.newGenerateRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        var r4Ext = (org.hl7.fhir.r4.model.Extension)
                fixture.buildSdcLaunchContextExt(r4Request, "user", "PractitionerRole");
        assertNull(r4Ext.getValue());
        assertInstanceOf(
                org.hl7.fhir.r4.model.Coding.class,
                r4Ext.getExtensionByUrl("name").getValue());
        assertInstanceOf(
                org.hl7.fhir.r4.model.CodeType.class,
                r4Ext.getExtensionByUrl("type").getValue());

        var r5Request = RequestHelpers.newGenerateRequestForVersion(FhirVersionEnum.R5, libraryEngine);
        var r5Ext = (org.hl7.fhir.r5.model.Extension) fixture.buildSdcLaunchContextExt(r5Request, "patient", "Patient");
        assertNull(r5Ext.getValue());
        assertInstanceOf(
                org.hl7.fhir.r5.model.Coding.class,
                r5Ext.getExtensionByUrl("name").getValue());
        assertInstanceOf(
                org.hl7.fhir.r5.model.CodeType.class,
                r5Ext.getExtensionByUrl("type").getValue());
    }

    @Test
    void testGenerateRequest() {
        var request = RequestHelpers.newGenerateRequestForVersion(FhirVersionEnum.R4, libraryEngine);
        assertThrows(UnsupportedOperationException.class, request::getSubjectId);
        assertThrows(UnsupportedOperationException.class, request::getData);
        assertThrows(UnsupportedOperationException.class, request::getParameters);
    }

    @Test
    void testErrorItemR4() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileUrl(new CanonicalType(
                        "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOneOrganization-noLibrary"))
                .then()
                .hasItemCount(1)
                .itemHasText("1", "An error occurred")
                .itemHasType("1", "display")
                .itemRepeats("1", false);
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .profileUrl(
                        new CanonicalType(
                                "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOneOrganization-missingLibrary"))
                .then()
                .hasItemCount(1)
                .itemHasText("1", "An error occurred")
                .itemHasType("1", "display")
                .itemRepeats("1", false);
    }

    @Test
    void testErrorItemR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .profileUrl(new CanonicalType(
                        "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOneOrganization-noLibrary"))
                .then()
                .hasItemCount(1)
                .itemHasText("1", "An error occurred")
                .itemHasType("1", "display")
                .itemRepeats("1", false);
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .profileUrl(
                        new CanonicalType(
                                "http://fhir.org/guides/cdc/opioid-cds/StructureDefinition/RouteOneOrganization-missingLibrary"))
                .then()
                .hasItemCount(1)
                .itemHasText("1", "An error occurred")
                .itemHasType("1", "display")
                .itemRepeats("1", false);
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
