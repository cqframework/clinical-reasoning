package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.cqframework.cql.cql2elm.CqlIncludeException;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.VersionedIdentifiers;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.search.Searches;

public class R4MeasureProcessor {
    private final Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final SubjectProvider subjectProvider;

    public R4MeasureProcessor(
            Repository repository, MeasureEvaluationOptions measureEvaluationOptions, SubjectProvider subjectProvider) {
        this.repository = Objects.requireNonNull(repository);
        this.measureEvaluationOptions =
                measureEvaluationOptions != null ? measureEvaluationOptions : MeasureEvaluationOptions.defaultOptions();
        this.subjectProvider = subjectProvider;
    }

    public MeasureReport evaluateMeasure(
            Either3<CanonicalType, IdType, Measure> measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData,
            Parameters parameters) {

        var evalType = MeasureEvalType.fromCode(reportType)
                .orElse(
                        subjectIds == null || subjectIds.isEmpty() || subjectIds.get(0) == null
                                ? MeasureEvalType.POPULATION
                                : MeasureEvalType.SUBJECT);

        var actualRepo = this.repository;
        if (additionalData != null) {
            actualRepo = new FederatedRepository(
                    this.repository, new InMemoryFhirRepository(this.repository.fhirContext(), additionalData));
        }
        var subjects =
                subjectProvider.getSubjects(actualRepo, evalType, subjectIds).collect(Collectors.toList());

        return this.evaluateMeasure(
                measure, periodStart, periodEnd, reportType, subjects, additionalData, parameters, evalType);
    }

    public MeasureReport evaluateMeasure(
            Either3<CanonicalType, IdType, Measure> measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData,
            Parameters parameters,
            MeasureEvalType evalType) {
        var m = measure.fold(this::resolveByUrl, this::resolveById, Function.identity());
        return this.evaluateMeasure(
                m, periodStart, periodEnd, reportType, subjectIds, additionalData, parameters, evalType);
    }

    protected MeasureReport evaluateMeasure(
            Measure measure,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String reportType,
            List<String> subjectIds,
            IBaseBundle additionalData,
            Parameters parameters,
            MeasureEvalType evalType) {

        if (!measure.hasLibrary()) {
            throw new IllegalArgumentException(
                    String.format("Measure %s does not have a primary library specified", measure.getUrl()));
        }

        Interval measurementPeriod = null;
        if (periodStart != null && periodEnd != null) {
            var helper = new R4DateHelper();
            measurementPeriod = helper.buildMeasurementPeriodInterval(periodStart, periodEnd);
        }

        var url = measure.getLibrary().get(0).asStringValue();

        Bundle b = this.repository.search(Bundle.class, Library.class, Searches.byCanonical(url), null);
        if (b.getEntry().isEmpty()) {
            var errorMsg = String.format("Unable to find Library with url: %s", url);
            throw new ResourceNotFoundException(errorMsg);
        }

        var id = VersionedIdentifiers.forUrl(url);
        var context = Engines.forRepositoryAndSettings(
                this.measureEvaluationOptions.getEvaluationSettings(), this.repository, additionalData);

        CompiledLibrary lib;
        try {
            lib = context.getEnvironment().getLibraryManager().resolveLibrary(id);
        } catch (CqlIncludeException e) {
            throw new IllegalStateException(
                    String.format(
                            "Unable to load CQL/ELM for library: %s. Verify that the Library resource is available in your environment and has CQL/ELM content embedded.",
                            id.getId()),
                    e);
        }

        context.getState().init(lib.getLibrary());

        if (parameters != null) {
            Map<String, Object> paramMap = resolveParameterMap(parameters);
            context.getState().setParameters(lib.getLibrary(), paramMap);
            // Set parameters for included libraries
            // Note: this may not be the optimal method (e.g. libraries with the same
            // parameter name, but different
            // values)
            if (lib.getLibrary().getIncludes() != null) {
                lib.getLibrary()
                        .getIncludes()
                        .getDef()
                        .forEach(includeDef -> paramMap.forEach((paramKey, paramValue) -> context.getState()
                                .setParameter(includeDef.getLocalIdentifier(), paramKey, paramValue)));
            }
        }

        if (evalType == null) {
            evalType = MeasureEvalType.fromCode(reportType)
                    .orElse(
                            subjectIds == null || subjectIds.isEmpty() || subjectIds.get(0) == null
                                    ? MeasureEvalType.POPULATION
                                    : MeasureEvalType.SUBJECT);
        }
        // Library Evaluate
        var libraryEngine = new LibraryEngine(repository, this.measureEvaluationOptions.getEvaluationSettings());
        var params = makeParameters(measurementPeriod);
        R4MeasureEvaluation measureEvaluator = new R4MeasureEvaluation(context, measure, libraryEngine, id, params);
        return measureEvaluator.evaluate(evalType, subjectIds, measurementPeriod, libraryEngine, id, params);
    }

    public Parameters makeParameters(Interval measurementPeriod) {
        Parameters parameters = new Parameters();
        if (measurementPeriod != null) {
            var helper = new R4DateHelper();
            parameters.setParameter(
                    MEASUREMENT_PERIOD_PARAMETER_NAME, helper.buildMeasurementPeriod(measurementPeriod));
        }
        return parameters;
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

    private Map<String, Object> resolveParameterMap(Parameters parameters) {
        Map<String, Object> parameterMap = new HashMap<>();
        R4FhirModelResolver modelResolver = new R4FhirModelResolver();
        parameters.getParameter().forEach(param -> {
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
                    if (value != null) {
                        ((List) parameterMap.get(param.getName())).add(value);
                    }
                } else {
                    parameterMap.put(param.getName(), Arrays.asList(parameterMap.get(param.getName()), value));
                }
            } else {
                parameterMap.put(param.getName(), value);
            }
        });
        return parameterMap;
    }
}
