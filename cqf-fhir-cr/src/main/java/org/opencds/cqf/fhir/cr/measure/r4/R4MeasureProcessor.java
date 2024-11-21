package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R4MeasureProcessor {

    private static final Logger log = LoggerFactory.getLogger(R4MeasureProcessor.class);
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

        var evalType = MeasureEvalType.fromCode(
                        // validate in R4 accepted values
                        MeasureEvalType.fromCode(reportType)
                                .orElse(
                                        // map null reportType parameter to evalType if no subject parameter is provided
                                        subjectIds == null || subjectIds.isEmpty() || subjectIds.get(0) == null
                                                ? MeasureEvalType.POPULATION
                                                : MeasureEvalType.SUBJECT)
                                .toCode())
                .orElse(MeasureEvalType.SUBJECT);
//        var evalType = R4MeasureProcessor.someSortOfMeasureTypeCodeConversion(null, reportType, subjectIds);

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
//        evalType = someSortOfMeasureTypeCodeConversion(evalType, reportType, subjectIds);
        log.info("592: NEW evalType: {}, reportType: {}, subjectIds: {}", evalType, reportType, subjectIds);
        // Library Evaluate
        var libraryEngine = new LibraryEngine(repository, this.measureEvaluationOptions.getEvaluationSettings());
        R4MeasureEvaluation measureEvaluator = new R4MeasureEvaluation(context, measure, libraryEngine, id);
        return measureEvaluator.evaluate(evalType, subjectIds, measurementPeriod, libraryEngine, id);
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

    // LUKETODO:  this is one part of the logic for converting null to INDIVIDUAL
    /*

| Number | ReportType   | subject           | EvalType     | MeasureReportType | Note                                                 |
|        | param        | param             | used         | returned          |                                                      |
| ------ | ------------ | ----------------- | ------------ | ----------------- | ---------------------------------------------------- |
| 1      | empty        | empty             | population   | summary           | default behavior, when NO subject parameter provided |
| 2      | empty        | Patient/{id}      | subject      | individual        | default behavior, when subject parameter provided    |
| 2      | empty        | Practitioner/{id} | subject      | individual        | default behavior, when subject parameter provided    |
| 2      | empty        | Organization/{id} | subject      | individual        | default behavior, when subject parameter provided    |
| 3      | empty        | Group/{id}        | subject      | individual        | default behavior, when subject parameter provided    |
| 4      | subject      | empty             | subject      | individual        |                                                      | >>> currently this is an error?
| 5      | subject      | Patient/{id}      | subject      | individual        |                                                      |
| 5      | subject      | Practitioner/{id} | subject      | individual        |                                                      |
| 5      | subject      | Organization/{id} | subject      | individual        |                                                      |
| 6      | subject      | Group/{id}        | subject      | individual        |                                                      |
| 7      | population   | empty             | population   | summary           |                                                      |
| 8      | population   | Patient/{id}      | population   | summary           |                                                      |
| 8      | population   | Practitioner/{id} | population   | summary           |                                                      |
| 8      | population   | Organization/{id} | population   | summary           |                                                      |
| 9      | population   | Group/{id}        | population   | summary           |                                                      |
| 10     | subject-list | empty             | subject-list | subject-list      |                                                      |
| 11     | subject-list | Patient/{id}      | subject-list | subject-list      |                                                      |
| 11     | subject-list | Practitioner/{id} | subject-list | subject-list      |                                                      |
| 11     | subject-list | Organization/{id} | subject-list | subject-list      |                                                      |
| 12     | subject-list | Group/{id}        | subject-list | subject-list      |                                                      |
     */
    public static MeasureEvalType someSortOfMeasureTypeCodeConversion(MeasureEvalType evalType, String reportType, List<String> subjectIds) {
        log.info("592: OLD evalType: {}, reportType: {}, subjectIds: {}", evalType, reportType, subjectIds);

        return Optional.ofNullable(evalType)
            .orElse(MeasureEvalType.fromCode(reportType)
                .orElse(
                    subjectIds == null || subjectIds.isEmpty() || subjectIds.get(0) == null
                        ? MeasureEvalType.POPULATION
                        : MeasureEvalType.SUBJECT));

        // LUKETODO:  will this work?
        // LUKETODO:  does this conflict with the changes we made in cdr?
//        return Optional.ofNullable(evalType)
//            .orElse(MeasureEvalType.fromCode(reportType)
//                .orElseGet(() -> {
//                  if (isSubjectListEffectivelyEmpty(subjectIds)) {
//                      return MeasureEvalType.POPULATION;
//                  }
//
//                  return MeasureEvalType.SUBJECT;
//                }));
    }

    public static boolean isSubjectListEffectivelyEmpty(List<String> subjectIds) {
        return subjectIds == null || subjectIds.isEmpty() || subjectIds.get(0) == null;
    }
}