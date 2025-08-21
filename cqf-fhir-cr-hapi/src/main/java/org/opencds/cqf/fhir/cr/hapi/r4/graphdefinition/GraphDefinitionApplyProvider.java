package org.opencds.cqf.fhir.cr.hapi.r4.graphdefinition;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.GraphDefinition;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.hapi.common.IGraphDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.StringTimePeriodHandler;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class GraphDefinitionApplyProvider {

    private final IGraphDefinitionProcessorFactory graphDefinitionProcessorFactory;
    private final StringTimePeriodHandler stringTimePeriodHandler;

    public GraphDefinitionApplyProvider(
            IGraphDefinitionProcessorFactory graphDefinitionProcessorFactory,
            StringTimePeriodHandler stringTimePeriodHandler) {
        this.graphDefinitionProcessorFactory = graphDefinitionProcessorFactory;
        this.stringTimePeriodHandler = stringTimePeriodHandler;
    }

    @Operation(name = ProviderConstants.CR_OPERATION_APPLY, idempotent = true, type = GraphDefinition.class)
    public IBaseParameters apply(
            @IdParam IdType id,
            @OperationParam(name = "subject") String subject,
            @OperationParam(name = "periodStart") String periodStart,
            @OperationParam(name = "periodEnd") String periodEnd,
            @OperationParam(name = "parameters") Parameters parameters,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {

        return graphDefinitionProcessorFactory
                .create(requestDetails)
                .apply(
                        Eithers.forMiddle3(id),
                        subject,
                        stringTimePeriodHandler.getStartZonedDateTime(periodStart, requestDetails),
                        stringTimePeriodHandler.getEndZonedDateTime(periodEnd, requestDetails),
                        parameters);
    }
}
