package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.BundleBuilder;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.SearchParameter;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.utility.iterable.BundleIterator;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.Repositories;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.COUNTRY_CODING_SYSTEM_CODE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_PRODUCT_LINE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_DEFINITION_DATE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_VERSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.US_COUNTRY_CODE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.US_COUNTRY_DISPLAY;

public class R4MeasureService {

    private final Repository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;

    public R4MeasureService(Repository repository, MeasureEvaluationOptions measureEvaluationOptions) {
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
    }

    public MeasureReport evaluate(
            Either3<CanonicalType, IdType, Measure> measure,
            String periodStart,
            String periodEnd,
            String reportType,
            String subjectId,
            String lastReceivedOn,
            Endpoint contentEndpoint,
            Endpoint terminologyEndpoint,
            Endpoint dataEndpoint,
            Bundle additionalData,
            String productLine,
            String practitioner,
            String reporter
            ) {

        var repo = Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
        var processor = new R4MeasureProcessor(repo, this.measureEvaluationOptions, new R4RepositorySubjectProvider());

        ensureSupplementalDataElementSearchParameter();

        MeasureReport measureReport = null;

        // SUBJECT LIST SETTERS
        if (StringUtils.isBlank(subjectId) && StringUtils.isNotBlank(practitioner)) {
            List<String> subjectIds = getPractitionerPatients(practitioner);

            measureReport = processor.evaluateMeasure(
                measure,
                periodStart,
                periodEnd,
                reportType,
                subjectIds,
                additionalData);

        } else if (StringUtils.isNotBlank(subjectId)) {
            measureReport = processor.evaluateMeasure(
                measure,
                periodStart,
                periodEnd,
                reportType,
                Collections.singletonList(subjectId),
                additionalData);

        } else if (StringUtils.isBlank(subjectId) && StringUtils.isBlank(practitioner)) {
            measureReport = processor.evaluateMeasure(
                measure,
                periodStart,
                periodEnd,
                reportType,
                null,
                additionalData);
        }
        // add ProductLine after report is generated
        addProductLineExtension(measureReport, productLine);

        // add Reporter after report is generated
        if(StringUtils.isNotBlank(reporter)) {
            measureReport.setReporter(new Reference(reporter));
        }

        // add subject reference for non-individual reportTypes
        if((StringUtils.isNotBlank(practitioner) || StringUtils.isNotBlank(subjectId)) && measureReport.getType().name().equals(
            MeasureReportType.SUMMARY.name())){
            if(StringUtils.isNotBlank(practitioner)){
                measureReport.setSubject(new Reference(practitioner));
            } else {
                measureReport.setSubject(new Reference(subjectId));
            }
        }
        return measureReport;
    }

    

    private List<String> getPractitionersInGroup(String practGroup) {
        List<String> practitioners = new ArrayList<>();

        IdType id = new IdType(practGroup);
        org.hl7.fhir.r4.model.Group r =repository.read(Group.class, id);

        if (r == null) {
            throw new ResourceNotFoundException(id);
        }

        for (Group.GroupMemberComponent gmc : r.getMember()) {
            IIdType ref = gmc.getEntity().getReferenceElement();
            practitioners.add(ref.getResourceType() + "/" + ref.getIdPart());
        }
        return practitioners;
    }

    public List<String> getPractitionerPatients(String thePractitioner) {
        List<String> patients = new ArrayList<>();

        //Group of Practitioners
        if(thePractitioner.startsWith("Group")){
            var practitioners = getPractitionersInGroup(thePractitioner);
            for (String prac : practitioners)
            {
                var pracPatients = getPractitionerSubjectIds(prac);
                pracPatients.addAll(patients);
            }
            return patients;
            //Individual Practitioner Reference
        } else {
            return getPractitionerSubjectIds(thePractitioner);
        }
    }

    public List<String> getPractitionerSubjectIds(String thePractitioner){
        List<String> patients = new ArrayList<>();

        Map<String, List<IQueryParameterType>> map = new HashMap<>();
        map.put(
            "general-practitioner",
            Collections.singletonList(new ReferenceParam(
                thePractitioner.startsWith("Practitioner/")
                    ? thePractitioner
                    : "Practitioner/" + thePractitioner)));

        var bundle = repository.search(Bundle.class, Patient.class, map);
        var iterator = new BundleIterator<>(repository, bundle);

        while (iterator.hasNext()) {
            var patient = iterator.next().getResource();
            var refString = patient.getIdElement().getResourceType() + "/"
                + patient.getIdElement().getIdPart();
            patients.add(refString);
        }
        return patients;
    }

    private void addProductLineExtension(MeasureReport theMeasureReport, String theProductLine) {
        if (theProductLine != null) {
            Extension ext = new Extension();
            ext.setUrl(MEASUREREPORT_PRODUCT_LINE_EXT_URL);
            ext.setValue(new StringType(theProductLine));
            theMeasureReport.addExtension(ext);
        }
    }

    protected void ensureSupplementalDataElementSearchParameter() {
        // create a transaction bundle
        BundleBuilder builder = new BundleBuilder(repository.fhirContext());

        // set the request to be condition on code == supplemental data
        builder.addTransactionCreateEntry(SUPPLEMENTAL_DATA_SEARCHPARAMETER).conditional("code=supplemental-data");
        repository.transaction(builder.getBundle());
    }

    public static final List<ContactDetail> CQI_CONTACTDETAIL = Collections.singletonList(new ContactDetail()
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
        .setContact(CQI_CONTACTDETAIL)
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
}
