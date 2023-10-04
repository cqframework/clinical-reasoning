package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.COUNTRY_CODING_SYSTEM_CODE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_DEFINITION_DATE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_VERSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.US_COUNTRY_CODE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.US_COUNTRY_DISPLAY;

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
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.SearchParameter;
import org.hl7.fhir.dstu3.model.StringType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;

public class Dstu3MeasureService {
    private final Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;

    public Dstu3MeasureService(Repository theRepository, MeasureEvaluationOptions theMeasureEvaluationOptions) {
        repository = theRepository;
        measureEvaluationOptions = theMeasureEvaluationOptions;
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
            .setDescription(String.format(
                    "Returns resources (supplemental data) from references on extensions on the MeasureReport with urls matching %s.",
                    MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION))
            .setJurisdiction(US_JURISDICTION_CODING)
            .addBase("MeasureReport")
            .setCode("supplemental-data")
            .setType(Enumerations.SearchParamType.REFERENCE)
            .setExpression(String.format(
                    "MeasureReport.extension('%s').value", MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION))
            .setXpath(String.format(
                    "f:MeasureReport/f:extension[@url='%s'].value", MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION))
            .setXpathUsage(SearchParameter.XPathUsageType.NORMAL)
            .setTitle("Supplemental Data")
            .setId("deqm-measurereport-supplemental-data");

    /**
     * Get The details (such as tenant) of this request. Usually auto-populated HAPI.
     *
     */

    /**
     * Implements the <a href=
     * "https://www.hl7.org/fhir/operation-measure-evaluate-measure.html">$evaluate-measure</a>
     * operation found in the
     * <a href="http://www.hl7.org/fhir/clinicalreasoning-module.html">FHIR Clinical
     * Reasoning Module</a>. This implementation aims to be compatible with the CQF
     * IG.
     *
     * @param theId                  the Id of the Measure to evaluate
     * @param thePeriodStart         The start of the reporting period
     * @param thePeriodEnd           The end of the reporting period
     * @param theReportType          The type of MeasureReport to generate
     * @param thePractitioner        the practitioner to use for the evaluation
     * @param theLastReceivedOn      the date the results of this measure were last
     *                               received.
     * @param theProductLine         the productLine (e.g. Medicare, Medicaid, etc) to use
     *                               for the evaluation. This is a non-standard parameter.
     * @param theAdditionalData      the data bundle containing additional data
     * @param theTerminologyEndpoint the endpoint of terminology services for your measure valuesets
     * @return the calculated MeasureReport
     */
    public MeasureReport evaluateMeasure(
            IdType theId,
            String thePeriodStart,
            String thePeriodEnd,
            String theReportType,
            String theSubject,
            String thePractitioner,
            String theLastReceivedOn,
            String theProductLine,
            Bundle theAdditionalData,
            Endpoint theTerminologyEndpoint) {

        ensureSupplementalDataElementSearchParameter();

        var dstu3MeasureProcessor = new Dstu3MeasureProcessor(repository, measureEvaluationOptions);

        MeasureReport report = dstu3MeasureProcessor.evaluateMeasure(
                theId,
                thePeriodStart,
                thePeriodEnd,
                theReportType,
                Collections.singletonList(theSubject),
                theAdditionalData);

        if (theProductLine != null) {
            Extension ext = new Extension();
            ext.setUrl("http://hl7.org/fhir/us/cqframework/cqfmeasures/StructureDefinition/cqfm-productLine");
            ext.setValue(new StringType(theProductLine));
            report.addExtension(ext);
        }

        return report;
    }

    protected void ensureSupplementalDataElementSearchParameter() {
        // create a transaction bundle
        BundleBuilder builder = new BundleBuilder(repository.fhirContext());

        // set the request to be condition on code == supplemental data
        builder.addTransactionCreateEntry(SUPPLEMENTAL_DATA_SEARCHPARAMETER).conditional("code=supplemental-data");
        repository.transaction(builder.getBundle());
    }
}
