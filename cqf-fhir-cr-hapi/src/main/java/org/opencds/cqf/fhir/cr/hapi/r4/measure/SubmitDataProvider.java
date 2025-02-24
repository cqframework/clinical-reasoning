package org.opencds.cqf.fhir.cr.hapi.r4.measure;

import org.opencds.cqf.fhir.cr.hapi.r4.ISubmitDataProcessorFactory;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;

public class SubmitDataProvider {
    private final ISubmitDataProcessorFactory r4SubmitDataProcessorFactory;

    public SubmitDataProvider(ISubmitDataProcessorFactory r4SubmitDataProcessorFactory) {
        this.r4SubmitDataProcessorFactory = r4SubmitDataProcessorFactory;
    }

    /**
     * Implements the <a href=
     * "http://hl7.org/fhir/R4/measure-operation-submit-data.html">$submit-data</a>
     * operation found in the
     * <a href="http://hl7.org/fhir/R4/clinicalreasoning-module.html">FHIR Clinical
     * Reasoning Module</a> per the
     * <a href="http://build.fhir.org/ig/HL7/davinci-deqm/datax.html#submit-data">Da
     * Vinci DEQM FHIR Implementation Guide</a>.
     *
     *
     * The submitted MeasureReport and Resources will be saved to the local server.
     * A Bundle reporting the result of the transaction will be returned.
     *
     * Usage:
     * URL: [base]/Measure/$submit-data
     * URL: [base]/Measure/[id]/$submit-data
     *
     * @param requestDetails generally auto-populated by the HAPI server
     *                          framework.
     * @param id             the Id of the Measure to submit data for
     * @param report         the MeasureReport to be submitted
     * @param resources      the resources to be submitted
     * @return Bundle the transaction result
     */
    @Description(
            shortDefinition = "$submit-data",
            value =
                    "Implements the <a href=\"http://hl7.org/fhir/R4/measure-operation-submit-data.html\">$submit-data</a> operation found in the <a href=\"http://hl7.org/fhir/R4/clinicalreasoning-module.html\">FHIR Clinical Reasoning Module</a> per the <a href=\"http://build.fhir.org/ig/HL7/davinci-deqm/datax.html#submit-data\">Da Vinci DEQM FHIR Implementation Guide</a>.")
    @Operation(name = ProviderConstants.CR_OPERATION_SUBMIT_DATA, type = Measure.class)
    public Bundle submitData(
            RequestDetails requestDetails,
            @IdParam IdType id,
            @OperationParam(name = "measureReport", min = 1, max = 1) MeasureReport report,
            @OperationParam(name = "resource") List<IBaseResource> resources) {
        return r4SubmitDataProcessorFactory.create(requestDetails).submitData(report, resources);
    }
}
