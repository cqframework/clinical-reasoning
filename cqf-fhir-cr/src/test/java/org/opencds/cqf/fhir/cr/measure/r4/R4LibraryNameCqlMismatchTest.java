package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class R4LibraryNameCqlMismatchTest {
    private static final Given GIVEN_REPO = Measure.given().repositoryFor("LibraryUrlNameCqlMismatch");

    // The point of this test is to ensure that clinical-reasoning builds a VersionedIdentifier
    // properly according to the specification for Library.
    // That is, in the case where the library URL, either in the FHIR Measure or the FHIR Library
    // itself contains a name that doesn't match the CQL library name and/or the Library name,
    // that we'll correctly build the VersionedIdentifier using the FHIR Library name, version,
    // and derive the system from the URL.
    //
    // Measure/library URL = "http://example.org/fhir/Library/SampleReporting-2024.2.0|2024.2.0"
    // Library name = "Sample_Reporting"
    // CQL library = "Sample_Reporting"
    @Test
    void sanityCheck() {

        GIVEN_REPO
                .when()
                .measureId("SampleReporting-2024.2.0")
                .evaluate()
                .then()
                .group("group-1")
                .population("initial-population")
                .hasCount(1);
    }
}
