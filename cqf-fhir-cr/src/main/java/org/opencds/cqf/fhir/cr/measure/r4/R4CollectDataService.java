package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEnvironment;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationRequest;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationResults;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationService;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.ResolvedMeasure;
import org.opencds.cqf.fhir.cr.measure.common.SubjectRef;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;

/**
 * Implements the <a href="http://hl7.org/fhir/R4/measure-operation-collect-data.html">$collect-data</a>
 * operation from the FHIR Clinical Reasoning Module.
 *
 * <p>Delegates to {@link MeasureEvaluationService} for CQL execution and scoring, then
 * post-processes each resulting MeasureReport into DATACOLLECTION format with evaluated resources.
 */
public class R4CollectDataService {

    private final IRepository repository;
    private final R4MeasureResolver resolver;
    private final R4RepositorySubjectProvider subjectProvider;
    private final MeasureEvaluationService evaluationService;

    public R4CollectDataService(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            MeasurePeriodValidator measurePeriodValidator) {
        this.repository = repository;
        this.resolver = new R4MeasureResolver(repository);
        this.subjectProvider = new R4RepositorySubjectProvider(measureEvaluationOptions.getSubjectProviderOptions());
        this.evaluationService = new MeasureEvaluationService(
                measureEvaluationOptions,
                FhirContext.forR4Cached(),
                new R4PopulationBasisValidator(),
                measurePeriodValidator);
    }

    /**
     * Collects data for the specified measure: evaluates the measure for each resolved subject
     * and returns per-subject MeasureReports (type DATACOLLECTION) with their evaluated resources.
     *
     * @param measureId    the ID of the Measure to evaluate
     * @param periodStart  start of the reporting period
     * @param periodEnd    end of the reporting period
     * @param subject      the subject reference for evaluation
     * @param practitioner the practitioner reference (overrides subject if set)
     * @return Parameters containing MeasureReport(s) and evaluated resources
     */
    public Parameters collectData(
            IdType measureId,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String subject,
            String practitioner) {

        // 1. Resolve the Measure and build domain types
        var fhirMeasure = R4MeasureServiceUtils.foldMeasure(Eithers.forMiddle3(measureId), repository);
        var resolved = resolver.buildResolvedMeasure(fhirMeasure);

        // 2. Resolve subjects (practitioner overrides subject)
        var effectiveSubject = resolvePractitionerOverride(subject, practitioner);
        var subjects = subjectProvider
                .getSubjects(repository, effectiveSubject)
                .map(SubjectRef::qualified)
                .collect(Collectors.toList());

        // 3. Evaluate per-subject to get per-subject evaluatedResources in each report
        Parameters parameters = new Parameters();
        if (!subjects.isEmpty()) {
            for (String patientId : subjects) {
                var results = evaluateForSubject(resolved, patientId, periodStart, periodEnd);
                addReport(results, resolved, patientId, parameters);
            }
        } else {
            // No subjects resolved: evaluate with the original subject reference
            // (may be a practitioner with no patients) for a minimal empty report
            var results = evaluateForSubject(resolved, effectiveSubject, periodStart, periodEnd);
            addReport(results, resolved, null, parameters);
        }

        return parameters;
    }

    private MeasureEvaluationResults evaluateForSubject(
            ResolvedMeasure resolved,
            @Nullable String subjectId,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd) {
        var request = new MeasureEvaluationRequest(
                periodStart, periodEnd, MeasureEvalType.SUBJECT.toCode(), subjectId, null, null, null);
        return evaluationService.evaluate(
                repository, List.of(resolved), request, MeasureEnvironment.EMPTY, Map.of(), subjectProvider);
    }

    /**
     * Builds a DATACOLLECTION MeasureReport from evaluation results and adds it with its
     * evaluated resources to the output parameters.
     */
    private void addReport(
            MeasureEvaluationResults results,
            ResolvedMeasure resolved,
            @Nullable String subjectId,
            Parameters parameters) {

        var scored = results.scoredMeasures().get(0);
        var report = new R4MeasureReportBuilder()
                .build(
                        scored.measureDef(),
                        scored.state(),
                        resolver.evalTypeToReportType(
                                results.evalType(), scored.measureDef().url()),
                        results.measurementPeriod(),
                        scored.subjects());

        // Post-process for $collect-data: strip groups, set type to DATACOLLECTION
        report.setType(MeasureReport.MeasureReportType.DATACOLLECTION);
        report.setGroup(null);

        var reportSubjectId = subjectId != null ? subjectId.replace("Patient/", "") : "no-subjectId";
        parameters.addParameter(part("measureReport-" + reportSubjectId, report));

        if (!report.getEvaluatedResource().isEmpty()) {
            populateEvaluatedResources(report, parameters, reportSubjectId);
        }
    }

    @Nullable
    private static String resolvePractitionerOverride(@Nullable String subject, @Nullable String practitioner) {
        if (StringUtils.isNotBlank(practitioner)) {
            return practitioner.contains("/") ? practitioner : "Practitioner/".concat(practitioner);
        }
        return subject;
    }

    /**
     * Fetches each evaluated resource from the repository and adds it to the output parameters.
     */
    protected void populateEvaluatedResources(MeasureReport measureReport, Parameters parameters, String subject) {
        measureReport.getEvaluatedResource().forEach(evaluatedResource -> {
            IIdType resourceId = evaluatedResource.getReferenceElement();
            if (resourceId.getResourceType() == null) {
                return;
            } else {
                Ids.simple(resourceId);
            }

            Class<? extends IBaseResource> resourceType = repository
                    .fhirContext()
                    .getResourceDefinition(resourceId.getResourceType())
                    .newInstance()
                    .getClass();
            IBaseResource resource = repository.read(resourceType, resourceId);

            if (resource instanceof Resource resourceBase) {
                parameters.addParameter(part("resource-" + subject, resourceBase));
            }
        });
    }
}
