package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils.getFullUrl;

import com.google.common.base.Strings;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.builder.BundleBuilder;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.Repositories;

// Alternate MeasureService call to Process MeasureEvaluation for the selected population of subjects against n-number
// of measure resources. The output of this operation would be a bundle of MeasureReports instead of MeasureReport.

public class R4MultiMeasureService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(R4MultiMeasureService.class);
    private final Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private String serverBase;

    public R4MultiMeasureService(
            Repository repository, MeasureEvaluationOptions measureEvaluationOptions, String serverBase) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.serverBase = serverBase;
    }

    public Bundle evaluate(
            List<Either3<CanonicalType, IdType, Measure>> measures,
            String periodStart,
            String periodEnd,
            String reportType,
            String subjectId,
            Endpoint contentEndpoint,
            Endpoint terminologyEndpoint,
            Endpoint dataEndpoint,
            Bundle additionalData,
            Parameters parameters,
            String productLine,
            String practitioner,
            String reporter) {

        var repo = Repositories.proxy(repository, true, dataEndpoint, contentEndpoint, terminologyEndpoint);

        var subjectProvider = new R4RepositorySubjectProvider();

        var processor = new R4MeasureProcessor(repo, this.measureEvaluationOptions, subjectProvider);

        R4MeasureServiceUtils r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
        r4MeasureServiceUtils.ensureSupplementalDataElementSearchParameter();

        log.info("multi-evaluate-measure, measures to evaluate: {}", measures.size());

        var evalType = MeasureEvalType.fromCode(reportType)
                .orElse(
                        subjectId == null || subjectId.isEmpty()
                                ? MeasureEvalType.POPULATION
                                : MeasureEvalType.SUBJECT);

        // get subjects
        var subjects = getSubjects(subjectProvider, practitioner, subjectId, evalType);

        // create bundle
        Bundle bundle = new BundleBuilder<>(Bundle.class)
                .withType(BundleType.SEARCHSET.toString())
                .build();

        for (Either3<CanonicalType, IdType, Measure> measure : measures) {
            MeasureReport measureReport;
            // evaluate each measure
            measureReport = processor.evaluateMeasure(
                    measure, periodStart, periodEnd, reportType, subjects, additionalData, parameters, evalType);

            // add ProductLine after report is generated
            measureReport = r4MeasureServiceUtils.addProductLineExtension(measureReport, productLine);

            // add subject reference for non-individual reportTypes
            measureReport = r4MeasureServiceUtils.addSubjectReference(measureReport, practitioner, subjectId);

            // add reporter if available
            if (reporter != null && !reporter.isEmpty()) {
                measureReport.setReporter(r4MeasureServiceUtils.getReporter(reporter)
                    .orElse(null));
            }
            // add id to measureReport
            initializeReport(measureReport);

            // add report to bundle
            bundle.addEntry(getBundleEntry(serverBase, measureReport));

            // progress feedback
            var measureUrl = measureReport.getMeasure();
            if (!measureUrl.isEmpty()) {
                log.info("MeasureReport complete for Measure: {}", measureUrl);
            }
        }

        return bundle;
    }

    protected List<String> getSubjects(
            R4RepositorySubjectProvider subjectProvider,
            String practitioner,
            String subjectId,
            MeasureEvalType evalType) {
        // check for practitioner parameter before subject
        if (StringUtils.isNotBlank(practitioner)) {
            if (!practitioner.contains("/")) {
                practitioner = "Practitioner/".concat(practitioner);
            }
            subjectId = practitioner;
        }

        return subjectProvider.getSubjects(repository, evalType, subjectId).collect(Collectors.toList());
    }

    protected void initializeReport(MeasureReport measureReport) {
        if (Strings.isNullOrEmpty(measureReport.getId())) {
            IIdType id = Ids.newId(MeasureReport.class, UUID.randomUUID().toString());
            measureReport.setId(id);
        }
    }

    protected Bundle.BundleEntryComponent getBundleEntry(String serverBase, Resource resource) {
        return new Bundle.BundleEntryComponent().setResource(resource).setFullUrl(getFullUrl(serverBase, resource));
    }
}
