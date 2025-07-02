package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Collections;
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
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureProcessorUtils;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class R4CollectDataService {
    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final R4RepositorySubjectProvider subjectProvider;
    private final R4MeasureServiceUtils measureServiceUtils;
    private final MeasureProcessorUtils measureProcessorUtils = new MeasureProcessorUtils();

    public R4CollectDataService(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            R4MeasureServiceUtils measureServiceUtils) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.subjectProvider = new R4RepositorySubjectProvider(measureEvaluationOptions.getSubjectProviderOptions());
        this.measureServiceUtils = measureServiceUtils;
    }

    /**
     * Implements the <a href=
     * "http://hl7.org/fhir/R4/measure-operation-collect-data.html">$collect-data</a>
     * operation found in the
     * <a href="http://hl7.org/fhir/R4/clinicalreasoning-module.html">FHIR Clinical
     * Reasoning Module</a>.
     *
     * <p>
     * Returns a set of parameters with the generated MeasureReport and the
     * resources that were used during the Measure evaluation
     *
     * @param measureId         the measureId of the Measure to sub data for
     * @param periodStart       The start of the reporting period
     * @param periodEnd         The end of the reporting period
     * @param subject           the subject to use for the evaluation
     * @param practitioner      the practitioner to use for the evaluation
     *                          received.
     * @return Parameters the parameters containing the MeasureReport and the
     *         evaluated Resources
     */
    public Parameters collectData(
            IdType measureId,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String subject,
            String practitioner) {

        Parameters parameters = new Parameters();
        var processor = new R4MeasureProcessor(
                this.repository, this.measureEvaluationOptions, this.subjectProvider, this.measureServiceUtils,
            measureProcessorUtils);

        // LUKETODO:  subjectList should be null but we get a list of subjects here:
        // getSubjects
        List<String> subjectList = getSubjects(subject, practitioner, subjectProvider);

        var foldedMeasure = measureServiceUtils.foldMeasure(Eithers.forMiddle3(measureId), repository);

        // LUKETODO:  reuse  this within measureServiceUtils
        var measureDef = new R4MeasureDefBuilder().build(foldedMeasure);

        var evaluationResults =
            Map.of(foldedMeasure.getId(),
                processor.evaluateMeasureWithCqlEngine(
                subjectList,
                foldedMeasure,
                periodStart,
                periodEnd,
                parameters,
                measureDef,
                null));

        if (!subjectList.isEmpty()) {
            for (String patient : subjectList) {
                var subjects = Collections.singletonList(patient);
                // add resources per subject to Parameters
                addReports(processor, measureId, periodStart, periodEnd, subjects, parameters, evaluationResults);
            }
        } else {
            addReports(processor, measureId, periodStart, periodEnd, subjectList, parameters, evaluationResults);
        }
        return parameters;
    }

    private void addReports(
            R4MeasureProcessor processor,
            IdType measureId,
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            List<String> subjects,
            Parameters parameters,
            Map<String, Map<String, EvaluationResult>> evaluateMeasureResults) {

        MeasureReport report = processor.evaluateMeasure(
                Eithers.forMiddle3(measureId),
                periodStart,
                periodEnd,
                MeasureEvalType.SUBJECT.toCode(),
                subjects,
                null,
                null,
                null,
                evaluateMeasureResults);

        report.setType(MeasureReport.MeasureReportType.DATACOLLECTION);
        report.setGroup(null);
        String subjectId = null;
        if (!subjects.isEmpty()) {
            subjectId = subjects.get(0).replace("Patient/", "");
        } else {
            subjectId = "no-subjectId";
        }
        parameters.addParameter(part("measureReport-" + subjectId, report));
        if (!report.getEvaluatedResource().isEmpty()) {
            populateEvaluatedResources(report, parameters, subjectId);
        }
    }

    private List<String> getSubjects(String subject, String practitioner, R4RepositorySubjectProvider subjectProvider) {
        if (StringUtils.isNotBlank(practitioner)) {
            if (!practitioner.contains("/")) {
                practitioner = "Practitioner/".concat(practitioner);
            }
            subject = practitioner;
        }

        return subjectProvider.getSubjects(repository, subject).collect(Collectors.toList());
    }

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
