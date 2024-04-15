package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class R4CollectDataService {
    private final Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;

    public R4CollectDataService(Repository repository, MeasureEvaluationOptions measureEvaluationOptions) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
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
            IdType measureId, String periodStart, String periodEnd, String subject, String practitioner) {

        Parameters parameters = new Parameters();
        var subjectProvider = new R4RepositorySubjectProvider();
        var processor = new R4MeasureProcessor(this.repository, this.measureEvaluationOptions, subjectProvider);

        // getSubjects
        List<String> subjectList = getSubjects(subject, practitioner, subjectProvider);
        // if(subjectList.isEmpty() || subjectList == null){

        // }
        // loop over subjects
        if (!subjectList.isEmpty()) {
            for (String patient : subjectList) {
                var subjects = Collections.singletonList(patient);
                // add resources per subject to Parameters
                addReports(processor, measureId, periodStart, periodEnd, subjects, parameters);
            }
        } else {
            addReports(processor, measureId, periodStart, periodEnd, subjectList, parameters);
        }
        return parameters;
    }

    private void addReports(
            R4MeasureProcessor processor,
            IdType measureId,
            String periodStart,
            String periodEnd,
            List<String> subjects,
            Parameters parameters) {
        MeasureReport report = processor.evaluateMeasure(
                Eithers.forMiddle3(measureId),
                periodStart,
                periodEnd,
                MeasureEvalType.SUBJECT.toCode(),
                subjects,
                null,
                null,
                null);

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

        return subjectProvider
                .getSubjects(repository, MeasureEvalType.SUBJECT, subject)
                .collect(Collectors.toList());
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

            if (resource instanceof Resource) {
                Resource resourceBase = (Resource) resource;
                parameters.addParameter(part("resource-" + subject, resourceBase));
            }
        });
    }
}
