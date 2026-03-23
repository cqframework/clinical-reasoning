package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.COUNTRY_CODING_SYSTEM_CODE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_DEFINITION_DATE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_VERSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.US_COUNTRY_CODE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.US_COUNTRY_DISPLAY;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.util.BundleBuilder;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ContactDetail;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.SearchParameter;
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
    private final Dstu3MeasureProcessor processor;
    private final MeasureEvaluationService evaluationService;

    public Dstu3MeasureService(
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            MeasurePeriodValidator measurePeriodValidator) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.processor = new Dstu3MeasureProcessor(repository, measureEvaluationOptions);
        this.evaluationService = new MeasureEvaluationService(
                measureEvaluationOptions,
                FhirContext.forDstu3Cached(),
                new Dstu3PopulationBasisValidator(),
                measurePeriodValidator);
    }

    public static final List<ContactDetail> CQI_CONTACT_DETAIL = Collections.singletonList(new ContactDetail()
            .addTelecom(new ContactPoint()
                    .setSystem(ContactPoint.ContactPointSystem.URL)
                    .setValue("http://www.hl7.org/Special/committees/cqi/index.cfm")));

    public static final List<CodeableConcept> US_JURISDICTION_CODING = Collections.singletonList(new CodeableConcept()
            .addCoding(new Coding(COUNTRY_CODING_SYSTEM_CODE, US_COUNTRY_CODE, US_COUNTRY_DISPLAY)));

    public static final SearchParameter SUPPLEMENTAL_DATA_SEARCHPARAMETER = (SearchParameter) new SearchParameter()
            .setUrl(MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_URL)
            .setVersion(MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_VERSION)
            .setName("DEQMMeasureReportSupplementalData")
            .setStatus(Enumerations.PublicationStatus.ACTIVE)
            .setDate(MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_DEFINITION_DATE)
            .setPublisher("HL7 International - Clinical Quality Information Work Group")
            .setContact(CQI_CONTACT_DETAIL)
            .setDescription(
                    "Returns resources (supplemental data) from references on extensions on the MeasureReport with urls matching %s."
                            .formatted(MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION))
            .setJurisdiction(US_JURISDICTION_CODING)
            .addBase("MeasureReport")
            .setCode("supplemental-data")
            .setType(Enumerations.SearchParamType.REFERENCE)
            .setExpression(
                    "MeasureReport.extension('%s').value".formatted(MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION))
            .setXpath("f:MeasureReport/f:extension[@url='%s'].value"
                    .formatted(MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION))
            .setXpathUsage(SearchParameter.XPathUsageType.NORMAL)
            .setTitle("Supplemental Data")
            .setId("deqm-measurereport-supplemental-data");

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

        ensureSupplementalDataElementSearchParameter();

        // Version-specific: read measure
        var measure = repository.read(Measure.class, id);

        // Version-specific: resolve to domain types
        var resolved = processor.buildResolvedMeasure(measure);
        var params = processor.resolveParameterMap(parameters);

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
        var report = buildMeasureReport(scored, measure, results.evalType(), results.measurementPeriod());

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
            Measure fhirMeasure,
            MeasureEvalType evalType,
            org.opencds.cqf.cql.engine.runtime.Interval measurementPeriod) {
        return new Dstu3MeasureReportBuilder()
                .build(
                        fhirMeasure,
                        scored.measureDef(),
                        scored.state(),
                        processor.evalTypeToReportType(evalType),
                        measurementPeriod,
                        scored.subjects());
    }

    protected void ensureSupplementalDataElementSearchParameter() {
        // create a transaction bundle
        BundleBuilder builder = new BundleBuilder(repository.fhirContext());

        // set the request to be condition on code == supplemental data
        builder.addTransactionCreateEntry(SUPPLEMENTAL_DATA_SEARCHPARAMETER).conditional("code=supplemental-data");
        repository.transaction(builder.getBundle());
    }
}
