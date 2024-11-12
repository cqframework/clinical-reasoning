package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_SDE_REFERENCE_URL;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Paths;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.utils.TestDataGenerator;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class MeasureSDETest {
    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final Repository repository = new IgRepository(
            FhirContext.forR4Cached(),
            Paths.get(getResourcePath(MeasureSDETest.class) + "/" + CLASS_PATH + "/" + "MeasureTest"));
    private final Given given = Measure.given().repository(repository);
    private static final TestDataGenerator testDataGenerator = new TestDataGenerator(repository);

    @BeforeAll
    static void init() {
        Period period = new Period();
        period.setStartElement(new DateTimeType("2024-01-01T01:00:00Z"));
        period.setEndElement(new DateTimeType("2024-01-01T03:00:00Z"));
        testDataGenerator.makePatient(null, null, period);
    }
    /**
     * Individual MeasureReport
     * Single Value SDE
     * Has no Count because 1 Patient
     * SDE Observation has MeasureInfo
     * SDE Observation has SDE Coding
     * SDE Observation value has Matching Gender Code
     * Validates all Contained Observations have matching Extension
     */
    @Test
    void cohortBooleanSDESingleValueIndividualResult() {

        given.when()
                .measureId("CohortBooleanSDESingleValue")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .hasContainedResourceCount(1)
                .containedObservationsHaveMatchingExtension()
                .containedByValue("M")
                .observationHasExtensionUrl()
                .observationHasSDECoding()
                .up()
                .extension("sde-patient-sex")
                .extensionHasSDEUrl()
                .up()
                .report();
    }
    /**
     * Summary MeasureReport
     * Single Value SDE
     * SDE Has Count because 10 Patients
     * SDE Observation has MeasureInfo
     * SDE Observation has SDE Coding with Matching Gender Code
     * SDE Observation value for Count of Matching Subjects 5
     * Validates all Contained Observations have matching Extension. 5 Observations, 5 Matching references
     */
    @Test
    void cohortBooleanSDESingleValueSummaryResult() {

        given.when()
                .measureId("CohortBooleanSDESingleValue")
                .evaluate()
                .then()
                .hasContainedResourceCount(2)
                .containedObservationsHaveMatchingExtension()
                .containedByCoding("M")
                .observationHasExtensionUrl()
                .observationCount(5)
                .up()
                .containedByCoding("F")
                .observationHasExtensionUrl()
                .observationCount(5)
                .up()
                .report();
    }

    /**
     * Individual MeasureReport
     * SDE List of Values
     * Produces no Contained Observation
     * Creates Extension with reference values from SDE
     */
    @Test
    void CohortBooleanSDEListIndividualResult() {

        given.when()
                .measureId("CohortBooleanSDEList")
                .subject("Patient/patient-9")
                .evaluate()
                .then()
                .hasContainedResourceCount(0)
                .hasExtension(EXT_SDE_REFERENCE_URL, 2)
                .extensionByValueReference("Encounter/patient-9-encounter-1")
                .extensionHasSDEId("sde-patient-encounters")
                .up()
                .extensionByValueReference("Encounter/patient-9-encounter-2")
                .extensionHasSDEId("sde-patient-encounters")
                .up()
                .report();
    }
    /**
     * Summary MeasureReport
     * SDE List of Values
     * Produces no Contained Observation
     * Creates Extension with reference values from SDE
     */
    @Test
    void CohortBooleanSDEListSummaryResult() {

        given.when()
                .measureId("CohortBooleanSDEList")
                .evaluate()
                .then()
                .hasContainedResourceCount(0)
                .hasExtension(EXT_SDE_REFERENCE_URL, 11)
                .extensionByValueReference("Encounter/patient-9-encounter-1")
                .extensionHasSDEId("sde-patient-encounters")
                .up()
                .extensionByValueReference("Encounter/patient-9-encounter-2")
                .extensionHasSDEId("sde-patient-encounters")
                .up()
                .report();
    }
    /**
     * Required SDE usage missing
     * {
     *       "coding": [ {
     *         "system": "http://terminology.hl7.org/CodeSystem/measure-data-usage",
     *         "code": "supplemental-data"
     *       } ]
     *     }
     * This should throw an error in MeasureDefBuilder
     */
    @Test
    void CohortBooleanSDEMissingUsage() {
        try {
            given.when()
                    .measureId("CohortBooleanSDEMissingUsage")
                    .evaluate()
                    .then()
                    .report();
            fail("should throw error");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("SupplementalDataComponent usage is missing code: supplemental-data"));
        }
    }
}
