package org.opencds.cqf.fhir.cr.hapi.r4.measure;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.hapi.common.StringTimePeriodHandler;
import org.opencds.cqf.fhir.cr.hapi.r4.ICareGapsServiceFactory;

@SuppressWarnings("java:S107")
public class CareGapsOperationProvider {
    private final ICareGapsServiceFactory r4CareGapsProcessorFactory;
    private final StringTimePeriodHandler stringTimePeriodHandler;

    public CareGapsOperationProvider(
            ICareGapsServiceFactory r4CareGapsProcessorFactory, StringTimePeriodHandler stringTimePeriodHandler) {
        this.r4CareGapsProcessorFactory = r4CareGapsProcessorFactory;
        this.stringTimePeriodHandler = stringTimePeriodHandler;
    }

    /**
     * Implements the <a href=
     * "http://build.fhir.org/ig/HL7/davinci-deqm/OperationDefinition-care-gaps.html">$care-gaps</a>
     * operation found in the
     * <a href="http://build.fhir.org/ig/HL7/davinci-deqm/index.html">Da Vinci DEQM
     * FHIR Implementation Guide</a> that overrides the <a href=
     * "http://build.fhir.org/operation-measure-care-gaps.html">$care-gaps</a>
     * operation found in the
     * <a href="http://hl7.org/fhir/R4/clinicalreasoning-module.html">FHIR Clinical
     * Reasoning Module</a>.
     *
     * The operation calculates measures describing gaps in care. For more details,
     * reference the <a href=
     * "http://build.fhir.org/ig/HL7/davinci-deqm/gaps-in-care-reporting.html">Gaps
     * in Care Reporting</a> section of the
     * <a href="http://build.fhir.org/ig/HL7/davinci-deqm/index.html">Da Vinci DEQM
     * FHIR Implementation Guide</a>.
     *
     * A Parameters resource that includes zero to many document bundles that
     * include Care Gap Measure Reports will be returned.
     *
     * Usage:
     * URL: [base]/Measure/$care-gaps
     *
     * @param requestDetails generally auto-populated by the HAPI server
     *                          framework.
     * @param reriodStart       the start of the gaps through period
     * @param reriodEnd         the end of the gaps through period
     * @param subject           a reference to either a Patient or Group for which
     *                          the gaps in care report(s) will be generated
     * @param status            the status code of gaps in care reports that will be
     *                          included in the result
     * @param measureId         the id of Measure(s) for which the gaps in care
     *                          report(s) will be calculated
     * @param measureIdentifier the identifier of Measure(s) for which the gaps in
     *                          care report(s) will be calculated
     * @param measureUrl        the canonical URL of Measure(s) for which the gaps
     *                          in care report(s) will be calculated
     * @param nonDocument    defaults to 'false' which returns standard 'document' bundle for `$care-gaps`.
     *   If 'true', this will return summarized subject bundle with only detectedIssue resource.
     * @return Parameters of bundles of Care Gap Measure Reports
     */
    @Description(
            shortDefinition = "$care-gaps operation",
            value =
                    "Implements the <a href=\"http://build.fhir.org/ig/HL7/davinci-deqm/OperationDefinition-care-gaps.html\">$care-gaps</a> operation found in the <a href=\"http://build.fhir.org/ig/HL7/davinci-deqm/index.html\">Da Vinci DEQM FHIR Implementation Guide</a> which is an extension of the <a href=\"http://build.fhir.org/operation-measure-care-gaps.html\">$care-gaps</a> operation found in the <a href=\"http://hl7.org/fhir/R4/clinicalreasoning-module.html\">FHIR Clinical Reasoning Module</a>.")
    @Operation(name = ProviderConstants.CR_OPERATION_CARE_GAPS, idempotent = true, type = Measure.class)
    public Parameters careGapsReport(
            RequestDetails requestDetails,
            @OperationParam(name = "periodStart") String reriodStart,
            @OperationParam(name = "periodEnd") String reriodEnd,
            @OperationParam(name = "subject") String subject,
            @OperationParam(name = "status") List<String> status,
            @OperationParam(name = "measureId") List<String> measureId,
            @OperationParam(name = "measureIdentifier") List<String> measureIdentifier,
            @OperationParam(name = "measureUrl") List<CanonicalType> measureUrl,
            @OperationParam(name = "nonDocument") BooleanType nonDocument) {

        return r4CareGapsProcessorFactory
                .create(requestDetails)
                .getCareGapsReport(
                        stringTimePeriodHandler.getStartZonedDateTime(reriodStart, requestDetails),
                        stringTimePeriodHandler.getEndZonedDateTime(reriodEnd, requestDetails),
                        subject,
                        status,
                        measureId == null
                                ? null
                                : measureId.stream().map(IdType::new).toList(),
                        measureIdentifier,
                        measureUrl,
                        Optional.ofNullable(nonDocument)
                                .map(BooleanType::getValue)
                                .orElse(false));
    }
}
