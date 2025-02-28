package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.utility.monad.Either3;

/**
 * Describe operations provided to process caregaps for downstream implementors
 */
public interface R4CareGapsProcessorInterface {

    Parameters getCareGapsReport(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String subject,
            List<String> status,
            List<Either3<IdType, String, CanonicalType>> measure,
            boolean notDocument);

    R4CareGapsParameters setCareGapParameters(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String subject,
            List<String> status,
            List<Either3<IdType, String, CanonicalType>> measure,
            boolean notDocument);

    List<Measure> resolveMeasure(List<Either3<IdType, String, CanonicalType>> measure);

    List<String> getSubjects(String subject);

    void addConfiguredResource(String id, String key);

    void checkMeasureImprovementNotation(Measure measure);

    Parameters initializeResult();

    void checkValidStatusCode(List<Either3<IdType, String, CanonicalType>> measure, List<String> statuses);

    void measureCompatibilityCheck(List<Measure> measures);

    void checkMeasureBasis(Measure measure);

    void checkMeasureGroupComponents(Measure measure);

    void checkMeasureScoringType(Measure measure);

    void checkConfigurationReferences();
}
