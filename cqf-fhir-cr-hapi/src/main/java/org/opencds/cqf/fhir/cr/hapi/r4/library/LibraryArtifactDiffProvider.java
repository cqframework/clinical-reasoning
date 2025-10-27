package org.opencds.cqf.fhir.cr.hapi.r4.library;

import static org.opencds.cqf.fhir.cr.hapi.common.IdHelper.getIdType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class LibraryArtifactDiffProvider {

    private final ILibraryProcessorFactory libraryProcessorFactory;

    private final FhirVersionEnum fhirVersion;

    public LibraryArtifactDiffProvider(ILibraryProcessorFactory libraryProcessorFactory) {
        this.libraryProcessorFactory = libraryProcessorFactory;
        this.fhirVersion = FhirVersionEnum.R4;
    }

    @Operation(name = "$artifact-diff", idempotent = true, global = true, type = Library.class)
    @Description(shortDefinition = "$artifact-diff", value = "Diff two knowledge artifacts")
    public IBaseParameters crmiArtifactDiff(
            RequestDetails requestDetails,
            @OperationParam(name = "source") String source,
            @OperationParam(name = "target") String target,
            @OperationParam(name = "compareExecutable", typeName = "Boolean") IPrimitiveType<Boolean> compareExecutable,
            @OperationParam(name = "compareComputable", typeName = "Boolean") IPrimitiveType<Boolean> compareComputable,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint)
            throws UnprocessableEntityException, ResourceNotFoundException {
        IIdType sourceId = getIdType(FhirVersionEnum.R4, "Library", source);
        IIdType targetId = getIdType(FhirVersionEnum.R4, "Library", target);

        // var params = getArtifactDiffParameters(compareComputable.getValue(), compareExecutable.getValue(),
        // terminologyEndpoint);

        return libraryProcessorFactory
                .create(requestDetails)
                .artifactDiff(
                        Eithers.for3(null, sourceId, null),
                        Eithers.for3(null, targetId, null),
                        compareComputable.getValue(),
                        compareExecutable.getValue(),
                        terminologyEndpoint);
    }

    /*    private static Parameters getArtifactDiffParameters(
        Boolean compareComputable,
        Boolean compareExecutable,
        Endpoint terminologyEndpoint) {
        var params = new Parameters();
        if (compareComputable != null) {
            params.addParameter("compareComputable", compareComputable);
        }
        if (compareExecutable != null) {
            params.addParameter("compareExecutable", compareExecutable);
        }
        if (terminologyEndpoint != null) {
            params.addParameter().setName("terminologyEndpoint").setResource(terminologyEndpoint);
        }
        return params;
    }*/

}
