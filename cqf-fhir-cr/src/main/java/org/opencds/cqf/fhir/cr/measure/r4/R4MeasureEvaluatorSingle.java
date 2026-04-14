package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;

/**
 * Interface to capture measure evaluation on a single measure.
 */
public interface R4MeasureEvaluatorSingle {

    MeasureReport evaluate(
            MeasureReference measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            String subjectId,
            String lastReceivedOn,
            Parameters parameters,
            String productLine,
            String practitioner);
}
