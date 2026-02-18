package org.opencds.cqf.fhir.cr.hapi.r4.measure;

import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.opencds.cqf.fhir.cr.hapi.r4.IDataRequirementsServiceFactory;

public class DataRequirementsOperationProvider {
    private final IDataRequirementsServiceFactory r4DataRequirementsServiceFactory;
    private final FhirVersionEnum fhirVersion;

    public DataRequirementsOperationProvider(IDataRequirementsServiceFactory r4DataRequirementsServiceFactory) {
        this.r4DataRequirementsServiceFactory = r4DataRequirementsServiceFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    /**
     * Implements the <a href=
     * "https://www.hl7.org/fhir/R4/measure-operation-data-requirements.html">$evaluate-measure</a>
     * operation found in the
     * <a href="http://www.hl7.org/fhir/clinicalreasoning-module.html">FHIR Clinical
     * Reasoning Module</a>. This implementation aims to be compatible with the CQF
     * IG.
     *
     * @param id             the id of the Measure to evaluate
     * @param periodStart    The start of the reporting period
     * @param periodEnd      The end of the reporting period
     * @param requestDetails The details (such as tenant) of this request. Usually
     *                          autopopulated HAPI.
     * @return the calculated Library dataRequirements
     */
    @Operation(name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS, idempotent = true, type = Measure.class)
    public Library dataRequirements(
            @IdParam IdType id,
            @OperationParam(name = "periodStart") DateType periodStart,
            @OperationParam(name = "periodEnd") DateType periodEnd,
            RequestDetails requestDetails) {
        return r4DataRequirementsServiceFactory
                .create(requestDetails)
                .dataRequirements(id, getStringValue(periodStart), getStringValue(periodEnd));
    }
}
