package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.When;
import org.opencds.cqf.fhir.cr.measure.r4.utils.TestDataGenerator;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

/**
 * This test is to verify and confirm that unsupported reportType value is appropriately handled
 *     invalid reportType value
 */
@SuppressWarnings("squid:S2699")
class MeasureReportTypeInvalidTest {
    private static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/measure/r4";
    private static final IRepository repository = new IgRepository(
            FhirContext.forR4Cached(),
            Path.of(getResourcePath(MeasureReportTypeInvalidTest.class) + "/" + CLASS_PATH + "/" + "MeasureTest"));
    private final Given given = Measure.given().repository(repository);
    private static final TestDataGenerator testDataGenerator = new TestDataGenerator(repository);

    @BeforeAll
    static void init() {
        Period period = new Period();
        period.setStartElement(new DateTimeType("2024-01-01T01:00:00Z"));
        period.setEndElement(new DateTimeType("2024-01-01T03:00:00Z"));
        testDataGenerator.makePatient("practitioner-1", "organization-1", period);
    }

    @Test
    void invalidReportTypeValue() {
        final When evaluate = given.when()
                .measureId("ProportionResourceAllPopulations")
                .subject("Group/group-patients-1")
                .reportType("summary")
                .evaluate();

        try {
            evaluate.then();
            fail("'summary' is not a valid value and should fail");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("ReportType: summary, is not an accepted R4 EvalType value."));
        }
    }

    @Test
    void unsupportedReportTypeValue() {
        final When evaluate = given.when()
                .measureId("ProportionResourceAllPopulations")
                .subject("Group/group-patients-1")
                .reportType("patient-list")
                .evaluate();

        try {
            evaluate.then();
            fail("'patient-list' is not a valid value for R4 and should fail");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("ReportType: patient-list, is not an accepted R4 EvalType value."));
        }
    }
}
