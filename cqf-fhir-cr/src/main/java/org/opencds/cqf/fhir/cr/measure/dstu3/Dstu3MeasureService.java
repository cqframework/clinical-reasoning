package org.opencds.cqf.fhir.cr.measure.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.StringType;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEnvironment;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationRequest;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationService;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.ScoredMeasure;
import org.opencds.cqf.fhir.cr.measure.helper.DateHelper;

/**
 * DSTU3 inbound/outbound adapter around {@link MeasureEvaluationService}.
 *
 * <p>Handles version-specific concerns: DSTU3 measure resolution, string-to-ZonedDateTime date
 * parsing, DSTU3 parameter conversion, and DSTU3 MeasureReport building from scored results.
 * All domain logic — period validation, subject resolution, CQL execution, scoring — is
 * delegated to the service.</p>
 */
public class Dstu3MeasureService implements Dstu3MeasureEvaluatorSingle {
    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final Dstu3MeasureResolver resolver;
    private final MeasureEvaluationService evaluationService;

    public Dstu3MeasureService(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            MeasurePeriodValidator measurePeriodValidator) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.resolver = new Dstu3MeasureResolver(repository);
        this.evaluationService = new MeasureEvaluationService(
                measureEvaluationOptions,
                FhirContext.forDstu3Cached(),
                new Dstu3PopulationBasisValidator(),
                measurePeriodValidator);
    }

    @Override
    public MeasureReport evaluateMeasure(
            IdType id,
            String periodStart,
            String periodEnd,
            String reportType,
            String subject,
            String practitioner,
            String lastReceivedOn,
            String productLine,
            Bundle additionalData,
            Parameters parameters,
            Endpoint terminologyEndpoint) {
        // Version-specific: read measure
        var measure = repository.read(Measure.class, id);

        // Version-specific: resolve to domain types
        var resolved = resolver.buildResolvedMeasure(measure);
        var params = resolver.resolveParameterMap(parameters);

        // Version-specific: parse string dates to ZonedDateTime
        var start = DateHelper.toZonedDateTime(periodStart, true);
        var end = DateHelper.toZonedDateTime(periodEnd, false);

        // Build domain request and environment
        var request = new MeasureEvaluationRequest(start, end, reportType, subject, null, lastReceivedOn, productLine);

        var environment = new MeasureEnvironment(null, terminologyEndpoint, null, additionalData);

        // Delegate to version-agnostic service
        var results = evaluationService.evaluate(
                repository, List.of(resolved), request, environment, params, new Dstu3RepositorySubjectProvider());

        // Version-specific: build DSTU3 MeasureReport from scored results
        var scored = results.scoredMeasures().get(0);
        var report = buildMeasureReport(scored, results.evalType(), results.measurementPeriod());

        if (productLine != null) {
            Extension ext = new Extension();
            ext.setUrl("http://hl7.org/fhir/us/cqframework/cqfmeasures/StructureDefinition/cqfm-productLine");
            ext.setValue(new StringType(productLine));
            report.addExtension(ext);
        }

        return report;
    }

    private MeasureReport buildMeasureReport(
            ScoredMeasure scored,
            MeasureEvalType evalType,
            org.opencds.cqf.cql.engine.runtime.Interval measurementPeriod) {
        return new Dstu3MeasureReportBuilder()
                .build(
                        scored.measureDef(),
                        scored.state(),
                        resolver.evalTypeToReportType(evalType),
                        measurementPeriod,
                        scored.subjects());
    }
}
