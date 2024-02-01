package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.dstu3.model.MeasureReport;
import org.junit.jupiter.api.Test;

class MeasureScorerTest {

    @Test
    void scorerThrowsIfNoScoringSupplied() {
        var mr = new MeasureReport();
        mr.addGroup();
        Dstu3MeasureReportScorer scorer = new Dstu3MeasureReportScorer();

        assertThrows(IllegalArgumentException.class, () -> scorer.score(null, mr));
    }
}
