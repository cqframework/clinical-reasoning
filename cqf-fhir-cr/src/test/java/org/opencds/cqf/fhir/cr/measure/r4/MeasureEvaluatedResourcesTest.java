package org.opencds.cqf.fhir.cr.measure.r4;

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

/**
 * When Measure Evaluation produces a Measure Report of type 'Individual', a evaluatedResources field
 * should populate reference values of resources interacted with to produce a MeasureScore or SDE.
 * This should apply for:
 * * supplementalDataElements (SDE)
 * * populations (ex: initial-population), which include stratifiers
 * No duplicate references in evaluated resources
 */
public class MeasureEvaluatedResourcesTest {
    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final Repository repository = new IgRepository(
            FhirContext.forR4Cached(),
            Paths.get(getResourcePath(MeasureStratifierTest.class) + "/" + CLASS_PATH + "/" + "MeasureTest"));
    protected Given given = Measure.given().repository(repository);
    private static final TestDataGenerator testDataGenerator = new TestDataGenerator(repository);

    @BeforeAll
    static void init() {
        Period period = new Period();
        period.setStartElement(new DateTimeType("2024-01-01T01:00:00Z"));
        period.setEndElement(new DateTimeType("2024-01-01T03:00:00Z"));
        testDataGenerator.makePatient(null, null, period);
    }

    /**
     * Individual Measure Report
     * EvaluatedResources will show references to each resource
     * Each Referenced Resource will have an extension referring to the SDE.id
     */
    @Test
    void cohortBooleanSDE() {

        given.when()
                .measureId("CohortBooleanSDESingleValue")
                .subject("Patient/patient-9")
                .reportType("subject")
                .evaluate()
                .then()
                .hasEvaluatedResourceCount(3)
                .evaluatedResourceHasNoDuplicateReferences()
                .evaluatedResource("Patient/patient-9")
                .referenceHasExtension("sde-patient-sex")
                .hasNoDuplicateExtensions()
                .up()
                .evaluatedResource("Encounter/patient-9-encounter-1")
                .referenceHasExtension("sde-patient-sex")
                .hasNoDuplicateExtensions()
                .up()
                .evaluatedResource("Encounter/patient-9-encounter-2")
                .referenceHasExtension("sde-patient-sex")
                .hasNoDuplicateExtensions()
                .up()
                .report();
    }

    /**
     * Individual Measure Report
     * EvaluatedResources will show references to each resource touched
     * Each Referenced Resource will have an extension referring to the population.id
     */
    @Test
    void ratioBooleanPopulationCheck() {

        given.when()
                .measureId("RatioBooleanStratValue")
                .subject("Patient/patient-9")
                .reportType("subject")
                .evaluate()
                .then()
                .hasEvaluatedResourceCount(3)
                .evaluatedResourceHasNoDuplicateReferences()
                .evaluatedResource("Patient/patient-9")
                .referenceHasExtension("initial-population")
                .referenceHasExtension("denominator")
                .referenceHasExtension("numerator")
                .hasNoDuplicateExtensions()
                .up()
                .evaluatedResource("Encounter/patient-9-encounter-1")
                .referenceHasExtension("initial-population")
                .referenceHasExtension("denominator")
                .referenceHasExtension("numerator")
                .hasNoDuplicateExtensions()
                .up()
                .evaluatedResource("Encounter/patient-9-encounter-2")
                .referenceHasExtension("initial-population")
                .referenceHasExtension("denominator")
                .referenceHasExtension("numerator")
                .hasNoDuplicateExtensions()
                .up()
                .report();
    }
    /**
     * Summary Measure Report
     * EvaluatedResources should be empty
     */
    @Test
    void ratioBooleanNoEvaluatedResourcesCheck() {

        given.when()
                .measureId("RatioBooleanStratValue")
                .evaluate()
                .then()
                .hasEvaluatedResourceCount(0)
                .report();
    }
}
