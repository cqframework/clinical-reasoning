package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.adapter.IMeasureAdapter;
import org.opencds.cqf.fhir.utility.monad.Either3;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Describe operations provided to process caregaps for downstream implementors
 */
public interface R4CareGapsProcessorInterface_Prototype<
        C extends IPrimitiveType<String>, I extends IIdType, P extends IBaseParameters, R extends IBaseResource> {

    P getCareGapsReport(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String subject,
            List<String> status,
            List<Either3<I, String, C>> measure,
            boolean notDocument);

    R4CareGapsParameters setCareGapParameters(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String subject,
            List<String> status,
            List<Either3<I, String, C>> measure,
            boolean notDocument);

    List<IMeasureAdapter> resolveMeasure(List<Either3<I, String, C>> measure);

    List<String> getSubjects(String subject);

    void addConfiguredResource(String id, String key);

    void checkMeasureImprovementNotation(IMeasureAdapter measure);

    P initializeResult();

    void checkValidStatusCode(List<Either3<I, String, C>> measure, List<String> statuses);

    void measureCompatibilityCheck(List<IMeasureAdapter> measures);

    void checkMeasureBasis(IMeasureAdapter measure);

    void checkMeasureGroupComponents(IMeasureAdapter measure);

    void checkMeasureScoringType(IMeasureAdapter measure);

    void checkConfigurationReferences();
}
