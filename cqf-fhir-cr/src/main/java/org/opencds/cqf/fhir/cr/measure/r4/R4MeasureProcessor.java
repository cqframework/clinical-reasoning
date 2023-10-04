package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.VersionedIdentifiers;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.cr.measure.helper.DateHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.search.Searches;

@Named
public class R4MeasureProcessor {
    private final Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final SubjectProvider subjectProvider;

    public R4MeasureProcessor(Repository repository, MeasureEvaluationOptions measureEvaluationOptions) {
        this(repository, measureEvaluationOptions, new R4RepositorySubjectProvider());
    }

    public R4MeasureProcessor(
            Repository repository, MeasureEvaluationOptions measureEvaluationOptions, SubjectProvider subjectProvider) {
        this.repository = Objects.requireNonNull(repository);
        this.measureEvaluationOptions =
                measureEvaluationOptions != null ? measureEvaluationOptions : MeasureEvaluationOptions.defaultOptions();
        this.subjectProvider = subjectProvider;
    }

    public MeasureReport evaluateMeasure(
            Either3<CanonicalType, IdType, Measure> measure,
            String periodStart,
            String periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData) {
        var m = measure.fold(this::resolveByUrl, this::resolveById, Function.identity());
        return this.evaluateMeasure(m, periodStart, periodEnd, reportType, subjectIds, additionalData);
    }

    protected MeasureReport evaluateMeasure(
            Measure measure,
            String periodStart,
            String periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData) {

        if (!measure.hasLibrary()) {
            throw new IllegalArgumentException(
                    String.format("Measure %s does not have a primary library specified", measure.getUrl()));
        }

        Interval measurementPeriod = null;
        if (StringUtils.isNotBlank(periodStart) && StringUtils.isNotBlank(periodEnd)) {
            measurementPeriod = this.buildMeasurementPeriod(periodStart, periodEnd);
        }

        var id = VersionedIdentifiers.forUrl(measure.getLibrary().get(0).asStringValue());
        var context = Engines.forRepositoryAndSettings(
                this.measureEvaluationOptions.getEvaluationSettings(), this.repository, additionalData);

        var lib = context.getEnvironment().getLibraryManager().resolveLibrary(id);

        context.getState().init(lib.getLibrary());

        var evalType = MeasureEvalType.fromCode(reportType)
                .orElse(
                        subjectIds.get(0) == null || subjectIds == null || subjectIds.isEmpty()
                                ? MeasureEvalType.POPULATION
                                : MeasureEvalType.SUBJECT);

        var actualRepo = this.repository;
        if (additionalData != null) {
            actualRepo = new FederatedRepository(
                    this.repository, new InMemoryFhirRepository(this.repository.fhirContext(), additionalData));
        }

        var subjects =
                subjectProvider.getSubjects(actualRepo, evalType, subjectIds).collect(Collectors.toList());
        /*
        if (subjects.size() > 1) {
            evalType = MeasureEvalType.POPULATION;
        } else {
            evalType = MeasureEvalType.SUBJECT;
        }*/

        R4MeasureEvaluation measureEvaluator = new R4MeasureEvaluation(context, measure);
        return measureEvaluator.evaluate(evalType, subjects, measurementPeriod);
    }

    protected Measure resolveByUrl(CanonicalType url) {
        var parts = Canonicals.getParts(url);
        var result = this.repository.search(
                Bundle.class, Measure.class, Searches.byNameAndVersion(parts.idPart(), parts.version()));
        return (Measure) result.getEntryFirstRep().getResource();
    }

    protected Measure resolveById(IdType id) {
        return this.repository.read(Measure.class, id);
    }

    protected MeasureReportType evalTypeToReportType(MeasureEvalType measureEvalType) {
        switch (measureEvalType) {
            case PATIENT:
            case SUBJECT:
                return MeasureReportType.INDIVIDUAL;
            case PATIENTLIST:
            case SUBJECTLIST:
                return MeasureReportType.PATIENTLIST;
            case POPULATION:
                return MeasureReportType.SUMMARY;
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported MeasureEvalType: %s", measureEvalType.toCode()));
        }
    }

    private Interval buildMeasurementPeriod(String periodStart, String periodEnd) {
        // resolve the measurement period
        return new Interval(
                DateTime.fromJavaDate(DateHelper.resolveRequestDate(periodStart, true)),
                true,
                DateTime.fromJavaDate(DateHelper.resolveRequestDate(periodEnd, false)),
                true);
    }
}
