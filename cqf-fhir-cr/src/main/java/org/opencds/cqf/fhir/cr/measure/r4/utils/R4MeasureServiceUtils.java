package org.opencds.cqf.fhir.cr.measure.r4.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.COUNTRY_CODING_SYSTEM_CODE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_PRODUCT_LINE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_DEFINITION_DATE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_VERSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_LOCATION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_ORGANIZATION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_PRACTITIONER;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_PRACTITIONER_ROLE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.US_COUNTRY_CODE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.US_COUNTRY_DISPLAY;

import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.SearchParameter;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.utility.Ids;

public class R4MeasureServiceUtils {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(R4MeasureServiceUtils.class);
    private final Repository repository;

    public R4MeasureServiceUtils(Repository repository) {
        this.repository = repository;
    }

    public MeasureReport addProductLineExtension(MeasureReport measureReport, String productLine) {
        if (productLine != null) {
            Extension ext = new Extension();
            ext.setUrl(MEASUREREPORT_PRODUCT_LINE_EXT_URL);
            ext.setValue(new StringType(productLine));
            measureReport.addExtension(ext);
        }
        return measureReport;
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

    public static String getFullUrl(String serverAddress, IBaseResource resource) {
        checkArgument(
                resource.getIdElement().hasIdPart(),
                "Cannot generate a fullUrl because the resource does not have an id.");
        return getFullUrl(serverAddress, resource.fhirType(), Ids.simplePart(resource));
    }

    public static String getFullUrl(String serverAddress, String fhirType, String elementId) {
        return String.format("%s%s/%s", serverAddress + (serverAddress.endsWith("/") ? "" : "/"), fhirType, elementId);
    }

    public void ensureSupplementalDataElementSearchParameter() {
        // create a transaction bundle
        ca.uhn.fhir.util.BundleBuilder builder = new ca.uhn.fhir.util.BundleBuilder(repository.fhirContext());

        // set the request to be condition on code == supplemental data
        builder.addTransactionCreateEntry(R4MeasureServiceUtils.SUPPLEMENTAL_DATA_SEARCHPARAMETER)
                .conditional("code=supplemental-data");
        try {
            repository.transaction(builder.getBundle());
        } catch (NotImplementedOperationException e) {
            log.warn(
                    "Error creating supplemental data search parameter. This may be due to the server not supporting transactions.",
                    e);
        }
    }

    public MeasureReport addSubjectReference(MeasureReport measureReport, String practitioner, String subjectId) {
        if ((StringUtils.isNotBlank(practitioner) || StringUtils.isNotBlank(subjectId))
                && (measureReport.getType().name().equals(MeasureReportType.SUMMARY.name())
                        || measureReport.getType().name().equals(MeasureReportType.SUBJECTLIST.name()))) {
            if (StringUtils.isNotBlank(practitioner)) {
                if (!practitioner.contains("/")) {
                    practitioner = "Practitioner/".concat(practitioner);
                }
                measureReport.setSubject(new Reference(practitioner));
            } else {
                if (!subjectId.contains("/")) {
                    subjectId = "Patient/".concat(subjectId);
                }
                measureReport.setSubject(new Reference(subjectId));
            }
        }
        return measureReport;
    }

    public Optional<Reference> getReporter(String reporter) {
        if (reporter != null && !reporter.isEmpty() && !reporter.contains("/")) {
            throw new IllegalArgumentException(
                    "R4MultiMeasureService requires '[ResourceType]/[ResourceId]' format to set MeasureReport.reporter reference.");
        }
        Reference reference = null;
        if (reporter != null && !reporter.isEmpty()) {
            if (reporter.startsWith(RESOURCE_TYPE_PRACTITIONER_ROLE)) {
                reference = new Reference(Ids.ensureIdType(reporter, RESOURCE_TYPE_PRACTITIONER_ROLE));
            } else if (reporter.startsWith(RESOURCE_TYPE_PRACTITIONER)) {
                reference = new Reference(Ids.ensureIdType(reporter, RESOURCE_TYPE_PRACTITIONER));
            } else if (reporter.startsWith(RESOURCE_TYPE_ORGANIZATION)) {
                reference = new Reference(Ids.ensureIdType(reporter, RESOURCE_TYPE_ORGANIZATION));
            } else if (reporter.startsWith(RESOURCE_TYPE_LOCATION)) {
                reference = new Reference(Ids.ensureIdType(reporter, RESOURCE_TYPE_LOCATION));
            } else {
                throw new IllegalArgumentException("MeasureReport.reporter does not accept ResourceType: " + reporter);
            }
        }

        return Optional.ofNullable(reference);
    }
}
