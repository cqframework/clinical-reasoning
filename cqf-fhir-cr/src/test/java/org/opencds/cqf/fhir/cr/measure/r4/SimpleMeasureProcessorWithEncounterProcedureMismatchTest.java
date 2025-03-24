package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

@SuppressWarnings("java:S2699")
class SimpleMeasureProcessorWithEncounterProcedureMismatchTest {

    protected static Given given = Measure.given().repositoryFor("EncounterBasisMismatchWithProcedure");

    private static final String MEASURE_ID = "EncounterBasisMismatchWithProcedure";

    @Test
    void failMismatchEncounterBasisProcedure() {
        given.when()
                .measureId(MEASURE_ID)
                .subject("numer-EXM108")
                .periodStart("2018-12-31")
                .periodEnd("2019-12-31")
                .reportType("subject")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcomeMsg(
                        "Exception for subjectId: Patient/numer-EXM108, Message: group expression criteria results for expression: [Initial Population] and scoring: [PROPORTION] must fall within accepted types for population basis: [Encounter] for Measure: [http://content.alphora.com/fhir/dqm/Measure/CMS68v12] due to mismatch between total result classes: [Boolean] and matching result classes: []");
    }
}
