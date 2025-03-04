package org.opencds.cqf.fhir.cr.hapi.r4.measure;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.hapi.common.StringTimePeriodHandler;
import org.opencds.cqf.fhir.cr.hapi.r4.ICollectDataServiceFactory;

public class CollectDataOperationProvider {
    private final ICollectDataServiceFactory r4CollectDataServiceFactory;
    private final StringTimePeriodHandler stringTimePeriodHandler;

    public CollectDataOperationProvider(
            ICollectDataServiceFactory r4CollectDataServiceFactory, StringTimePeriodHandler stringTimePeriodHandler) {
        this.r4CollectDataServiceFactory = r4CollectDataServiceFactory;
        this.stringTimePeriodHandler = stringTimePeriodHandler;
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
     * @param requestDetails generally auto-populated by the HAPI server
     *                          framework.
     * @param id             the Id of the Measure to sub data for
     * @param periodStart       The start of the reporting period
     * @param periodEnd         The end of the reporting period
     * @param subject           the subject to use for the evaluation
     * @param practitioner      the practitioner to use for the evaluation
     *
     * @return Parameters the parameters containing the MeasureReport and the
     *         evaluated Resources
     */
    @Description(
            shortDefinition = "$collect-data",
            value =
                    "Implements the <a href=\"http://hl7.org/fhir/R4/measure-operation-collect-data.html\">$collect-data</a> operation found in the <a href=\"http://hl7.org/fhir/R4/clinicalreasoning-module.html\">FHIR Clinical Reasoning Module</a>.")
    @Operation(name = ProviderConstants.CR_OPERATION_COLLECTDATA, idempotent = true, type = Measure.class)
    public Parameters collectData(
            @IdParam IdType id,
            @OperationParam(name = "periodStart") String periodStart,
            @OperationParam(name = "periodEnd") String periodEnd,
            @OperationParam(name = "subject") String subject,
            @OperationParam(name = "practitioner") String practitioner,
            RequestDetails requestDetails) {
        return r4CollectDataServiceFactory
                .create(requestDetails)
                .collectData(
                        id,
                        stringTimePeriodHandler.getStartZonedDateTime(periodStart, requestDetails),
                        stringTimePeriodHandler.getEndZonedDateTime(periodEnd, requestDetails),
                        subject,
                        practitioner);
    }
}
