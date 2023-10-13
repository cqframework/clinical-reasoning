package org.opencds.cqf.fhir.cr.measure.dstu3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.cr.measure.helper.DateHelper;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

@Named
public class Dstu3MeasureProcessor {
    private final Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final SubjectProvider subjectProvider;

    public Dstu3MeasureProcessor(Repository repository, MeasureEvaluationOptions measureEvaluationOptions) {
        this(repository, measureEvaluationOptions, new Dstu3RepositorySubjectProvider());
    }

    public Dstu3MeasureProcessor(
            Repository repository, MeasureEvaluationOptions measureEvaluationOptions, SubjectProvider subjectProvider) {
        this.repository = Objects.requireNonNull(repository);
        this.measureEvaluationOptions =
                measureEvaluationOptions != null ? measureEvaluationOptions : MeasureEvaluationOptions.defaultOptions();
        this.subjectProvider = subjectProvider;
    }

    public MeasureReport evaluateMeasure(
            IdType measureId,
            String periodStart,
            String periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData,
            Parameters parameters) {
        var measure = this.repository.read(Measure.class, measureId);
        return this.evaluateMeasure(measure, periodStart, periodEnd, reportType, subjectIds, additionalData, parameters);
    }

    // NOTE: Do not make a top-level function that takes a Measure resource. This ensures that
    // the repositories are set up correctly.
    protected MeasureReport evaluateMeasure(
            Measure measure,
            String periodStart,
            String periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData,
            Parameters parameters) {

        if (!measure.hasLibrary()) {
            throw new IllegalArgumentException(
                    String.format("Measure %s does not have a primary library specified", measure.getUrl()));
        }

        Interval measurementPeriod = null;
        if (StringUtils.isNotBlank(periodStart) && StringUtils.isNotBlank(periodEnd)) {
            measurementPeriod = this.buildMeasurementPeriod(periodStart, periodEnd);
        }

        var reference = measure.getLibrary().get(0);

        var library = this.repository.read(Library.class, reference.getReferenceElement());

        var context = Engines.forRepositoryAndSettings(
                this.measureEvaluationOptions.getEvaluationSettings(), this.repository, additionalData);

        var lib = context.getEnvironment()
                .getLibraryManager()
                .resolveLibrary(
                        new VersionedIdentifier().withId(library.getName()).withVersion(library.getVersion()));

        context.getState().init(lib.getLibrary());

        if (parameters != null) {
            Map<String, Object> paramMap = resolveParameterMap(parameters);
            context.getState().setParameters(lib.getLibrary(), paramMap);
            // Set parameters for included libraries
            // Note: this may not be the optimal method (e.g. libraries with the same parameter name, but different values)
            if (lib.getLibrary().getIncludes() != null) {
                lib.getLibrary().getIncludes().getDef().forEach(
                    includeDef -> paramMap.forEach(
                        (paramKey, paramValue) -> context.getState().setParameter(
                            includeDef.getLocalIdentifier(), paramKey, paramValue)));
            }
        }

        var actualRepo = this.repository;
        if (additionalData != null) {
            actualRepo = new FederatedRepository(
                    this.repository, new InMemoryFhirRepository(this.repository.fhirContext(), additionalData));
        }

        var evalType = MeasureEvalType.fromCode(reportType)
                .orElse(
                        subjectIds == null || subjectIds.isEmpty() || subjectIds.get(0) == null
                                ? MeasureEvalType.POPULATION
                                : MeasureEvalType.SUBJECT);

        var subjects =
                subjectProvider.getSubjects(actualRepo, evalType, subjectIds).collect(Collectors.toList());

        Dstu3MeasureEvaluation measureEvaluator = new Dstu3MeasureEvaluation(context, measure);
        return measureEvaluator.evaluate(evalType, subjects, measurementPeriod);
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

    private Map<String, Object> resolveParameterMap(Parameters parameters) {
        Map<String, Object> parameterMap = new HashMap<>();
        Dstu3FhirModelResolver modelResolver = new Dstu3FhirModelResolver();
        parameters.getParameter().forEach(
            param -> {
                Object value;
                if (param.hasResource()) {
                    value = param.getResource();
                } else {
                    value = param.getValue();
                    if (value instanceof IPrimitiveType) {
                        // TODO: handle Code, CodeableConcept, Quantity, etc
                        // resolves Date/Time values
                        value = modelResolver.toJavaPrimitive(((IPrimitiveType<?>) value).getValue(), value);
                    }
                }
                if (parameterMap.containsKey(param.getName())) {
                    if (parameterMap.get(param.getName()) instanceof List) {
                        CollectionUtils.addIgnoreNull((List<?>) parameterMap.get(param.getName()), value);
                    } else {
                        parameterMap.put(param.getName(), Arrays.asList(parameterMap.get(param.getName()), value));
                    }
                } else {
                    parameterMap.put(param.getName(), value);
                }
            }
        );
        return parameterMap;
    }
}
