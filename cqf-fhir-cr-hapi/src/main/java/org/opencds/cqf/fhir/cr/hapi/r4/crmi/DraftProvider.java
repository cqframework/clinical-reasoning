package org.opencds.cqf.fhir.cr.hapi.r4.crmi;

import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.r4.IDraftServiceFactory;

public class DraftProvider {

    private final IDraftServiceFactory r4DraftServiceFactory;

    public DraftProvider(IDraftServiceFactory r4DraftServiceFactory) {
        this.r4DraftServiceFactory = r4DraftServiceFactory;
    }

    /**
     * Creates a draft version of a knowledge artifact and all its children.
     * This operation is used to set the status and version. It also removes effectivePeriod,
     * approvalDate and any extensions which are only valid for active artifacts.
     *
     * @param id                The logical id of the artifact to draft. The server must know the
     *                          artifact (e.g. it is defined explicitly in the server's resources)
     * @param version           A semantic version in the form MAJOR.MINOR.PATCH.REVISION
     * @param requestDetails    The {@link RequestDetails RequestDetails}
     * @return  The {@link Bundle Bundle} result containing the new resource(s). If inputParameters
     *          are present in the manifest being drafted, those parameters are moved to the
     *          expansionParameters extension in the new draft.
     */
    @Operation(
            name = "$draft",
            idempotent = true,
            global = true,
            type = MetadataResource.class,
            canonicalUrl = "http://hl7.org/fhir/uv/crmi/OperationDefinition/crmi-draft")
    @Description(shortDefinition = "$draft", value = "Create a new draft version of the reference artifact.")
    public Bundle draftOperation(
            @IdParam IdType id, @OperationParam(name = "version") StringType version, RequestDetails requestDetails) {
        return r4DraftServiceFactory.create(requestDetails).draft(id, getStringValue(version));
    }
}
