package org.opencds.cqf.fhir.cr.hapi.r4.library;

import static org.opencds.cqf.fhir.cr.hapi.common.IdHelper.getIdType;
import static org.opencds.cqf.fhir.utility.Constants.CRMI_OPERATION_ARTIFACT_DIFF;
import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class LibraryArtifactDiffProvider {

    private final ILibraryProcessorFactory libraryProcessorFactory;

    private final FhirVersionEnum fhirVersion;

    public LibraryArtifactDiffProvider(ILibraryProcessorFactory libraryProcessorFactory) {
        this.libraryProcessorFactory = libraryProcessorFactory;
        this.fhirVersion = FhirVersionEnum.R4;
    }

    @Operation(name = CRMI_OPERATION_ARTIFACT_DIFF, idempotent = true, global = true, type = Library.class)
    @Description(shortDefinition = CRMI_OPERATION_ARTIFACT_DIFF, value = "Diff two knowledge artifacts")
    public IBaseParameters crmiArtifactDiff(
            RequestDetails requestDetails,
            @OperationParam(name = "source") StringType source,
            @OperationParam(name = "target") StringType target,
            @OperationParam(name = "compareExecutable", typeName = "Boolean") IPrimitiveType<Boolean> compareExecutable,
            @OperationParam(name = "compareComputable", typeName = "Boolean") IPrimitiveType<Boolean> compareComputable,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint)
            throws UnprocessableEntityException, ResourceNotFoundException {
        return libraryProcessorFactory
                .create(requestDetails)
                .artifactDiff(
                        Eithers.forMiddle3(getIdType(fhirVersion, "Library", source)),
                        Eithers.forMiddle3(getIdType(fhirVersion, "Library", target)),
                        compareComputable.getValue(),
                        compareExecutable.getValue(),
                        getEndpoint(fhirVersion, terminologyEndpoint));
    }
}
