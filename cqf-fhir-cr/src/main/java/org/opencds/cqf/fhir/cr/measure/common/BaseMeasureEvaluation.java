package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import java.util.Objects;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

public abstract class BaseMeasureEvaluation<MeasureT, MeasureReportT, SubjectT> {

    protected MeasureDefBuilder<MeasureT> measureDefBuilder;
    protected MeasureReportBuilder<MeasureT, MeasureReportT, SubjectT> measureReportBuilder;
    protected CqlEngine context;
    protected MeasureT measure;
    protected LibraryEngine libraryEngine;
    protected String measurementPeriodParameterName;
    protected VersionedIdentifier versionIdentifier;

    protected BaseMeasureEvaluation(
            CqlEngine context,
            MeasureT measure,
            MeasureDefBuilder<MeasureT> measureDefBuilder,
            MeasureReportBuilder<MeasureT, MeasureReportT, SubjectT> measureReportBuilder,
            LibraryEngine libraryEngine,
            VersionedIdentifier versionIdentifier) {
        this(
                context,
                measure,
                measureDefBuilder,
                measureReportBuilder,
                MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME,
                libraryEngine,
                versionIdentifier);
    }

    protected BaseMeasureEvaluation(
            CqlEngine context,
            MeasureT measure,
            MeasureDefBuilder<MeasureT> measureDefBuilder,
            MeasureReportBuilder<MeasureT, MeasureReportT, SubjectT> measureReportBuilder,
            String measurementPeriodParameterName,
            LibraryEngine libraryEngine,
            VersionedIdentifier versionIdentifier) {
        this.context = Objects.requireNonNull(context, "context is a required argument");
        this.measure = Objects.requireNonNull(measure, "measure is a required argument");
        this.measureDefBuilder = Objects.requireNonNull(measureDefBuilder, "measureDefBuilder is a required argument");
        this.measureReportBuilder =
                Objects.requireNonNull(measureReportBuilder, "measureReportBuilder is a required argument");
        this.measurementPeriodParameterName = Objects.requireNonNull(
                measurementPeriodParameterName, "measurementPeriodParameterName is a required argument");
        this.libraryEngine = libraryEngine;
        this.versionIdentifier = versionIdentifier;
    }

    public MeasureReportT evaluate(
            MeasureEvalType measureEvalType,
            List<String> subjectIds,
            LibraryEngine libraryEngine,
            VersionedIdentifier id) {
        return this.evaluate(measureEvalType, subjectIds, null, libraryEngine, id);
    }

    public MeasureReportT evaluate(
            MeasureEvalType measureEvalType,
            List<String> subjectIds,
            Interval measurementPeriod,
            LibraryEngine libraryEngine,
            VersionedIdentifier id) {
        Objects.requireNonNull(subjectIds, "subjectIds is a required parameter");
        Objects.requireNonNull(measureEvalType, "measureEvalType is a required parameter");

        MeasureDef measureDef = this.measureDefBuilder.build(measure);
        MeasureEvaluator measureEvaluation =
                new MeasureEvaluator(context, this.measurementPeriodParameterName, libraryEngine);
        measureDef = measureEvaluation.evaluate(measureDef, measureEvalType, subjectIds, measurementPeriod, id);

        Interval measurementPeriodInterval;
        if (measurementPeriod == null) {
            measurementPeriodInterval =
                    (Interval) context.getState().getParameters().get(this.measurementPeriodParameterName);
        } else {
            measurementPeriodInterval = measurementPeriod;
        }
        return this.measureReportBuilder.build(
                measure, measureDef, this.evalTypeToReportType(measureEvalType), measurementPeriodInterval, subjectIds);
    }

    protected MeasureReportType evalTypeToReportType(MeasureEvalType measureEvalType) {
        switch (measureEvalType) {
            case PATIENT:
            case SUBJECT:
                return MeasureReportType.INDIVIDUAL;
            case PATIENTLIST:
                return MeasureReportType.PATIENTLIST;
            case SUBJECTLIST:
                return MeasureReportType.SUBJECTLIST;
            case POPULATION:
                return MeasureReportType.SUMMARY;
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported MeasureEvalType: %s", measureEvalType.toCode()));
        }
    }
}
