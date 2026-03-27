package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;

/**
 * Describe operations provided to process caregaps for downstream implementors
 */
public interface R4CareGapsProcessorInterface {

    Parameters getCareGapsReport(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String subject,
            List<String> status,
            List<MeasureReference> measureRefs,
            boolean notDocument);

    R4CareGapsParameters setCareGapParameters(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String subject,
            List<String> status,
            List<MeasureReference> measureRefs,
            boolean notDocument);

    List<Measure> resolveMeasure(List<MeasureReference> measureRefs);

    List<String> getSubjects(String subject);

    void addConfiguredResource(String id, String key);

    void checkMeasureImprovementNotation(Measure measure);

    Parameters initializeResult();

    void checkValidStatusCode(List<MeasureReference> measureRefs, List<String> statuses);

    void measureCompatibilityCheck(List<Measure> measures);

    void checkMeasureBasis(Measure measure);

    void checkMeasureGroupComponents(Measure measure);

    void checkMeasureScoringType(Measure measure);

    void checkConfigurationReferences();
}
