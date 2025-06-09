package org.opencds.cqf.fhir.cr.valueset;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.cr.valueset.TestValueSet.CLASS_PATH;
import static org.opencds.cqf.fhir.cr.valueset.TestValueSet.given;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.common.DataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

@SuppressWarnings("squid:S2699")
class ValueSetProcessorTests {
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();
    private final Repository repositoryDstu3 =
            new IgRepository(fhirContextDstu3, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/dstu3"));
    private final Repository repositoryR4 =
            new IgRepository(fhirContextR4, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4"));
    private final Repository repositoryR5 =
            new IgRepository(fhirContextR5, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r5"));

    @Test
    void processors() {
        var when = given().repository(repositoryR4)
                .packageProcessor(new PackageProcessor(repositoryR4))
                .dataRequirementsProcessor(new DataRequirementsProcessor(repositoryR4))
                .when()
                .valueSetId(Ids.newId(fhirContextR4, "ValueSet", "AdministrativeGender"))
                .isPut(Boolean.FALSE);
        var bundle = when.thenPackage().getBundle();
        assertNotNull(bundle);
        var library = when.thenDataRequirements().getLibrary();
        assertNotNull(library);
    }

    @Test
    void packageDstu3() {
        given().repository(repositoryDstu3)
                .when()
                .valueSetId(Ids.newId(fhirContextDstu3, "ValueSet", "AdministrativeGender"))
                .thenPackage()
                .hasEntry(1)
                .firstEntryIsType(org.hl7.fhir.dstu3.model.ValueSet.class);
    }

    @Test
    void packageR4() {
        given().repository(repositoryR4)
                .when()
                .valueSetId(Ids.newId(fhirContextR4, "ValueSet", "AdministrativeGender"))
                .thenPackage()
                .hasEntry(1)
                .firstEntryIsType(org.hl7.fhir.r4.model.ValueSet.class);
    }

    @Test
    void packageR5() {
        given().repository(repositoryR5)
                .when()
                .valueSetId(Ids.newId(fhirContextR5, "ValueSet", "AdministrativeGender"))
                .thenPackage()
                .hasEntry(1)
                .firstEntryIsType(org.hl7.fhir.r5.model.ValueSet.class);
    }

    @Test
    void dataRequirementsDstu3() {
        given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .valueSetId(Ids.newId(fhirContextDstu3, "ValueSet", "AdministrativeGender"))
                .thenDataRequirements()
                .hasDataRequirements(0);
    }

    @Test
    void dataRequirementsR4() {
        given().repositoryFor(fhirContextR4, "r4")
                .when()
                .valueSetId(Ids.newId(fhirContextR4, "ValueSet", "AdministrativeGender"))
                .thenDataRequirements()
                .hasDataRequirements(0);
    }

    @Test
    void dataRequirementsR5() {
        given().repositoryFor(fhirContextR5, "r5")
                .when()
                .valueSetId(Ids.newId(fhirContextR5, "ValueSet", "AdministrativeGender"))
                .thenDataRequirements()
                .hasDataRequirements(0);
    }
}
