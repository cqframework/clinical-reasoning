package ca.uhn.fhir.cr.dstu3.library;

import static ca.uhn.fhir.cr.common.CanonicalHelper.getCanonicalType;
import static ca.uhn.fhir.cr.common.IdHelper.getIdType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.cr.common.ILibraryProcessorFactory;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class LibraryPackageProvider {
    private final ILibraryProcessorFactory libraryProcessorFactory;

    public LibraryPackageProvider(ILibraryProcessorFactory theLibraryProcessorFactory) {
        libraryProcessorFactory = theLibraryProcessorFactory;
    }

    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = Library.class)
    public IBaseBundle packagePlanDefinition(
            @IdParam IdType id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "usePut") BooleanType isPut,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        StringType canonicalType = getCanonicalType(FhirVersionEnum.DSTU3, canonical, url, version);
        return libraryProcessorFactory
                .create(requestDetails)
                .packageLibrary(
                        Eithers.for3(canonicalType, id, null), isPut == null ? Boolean.FALSE : isPut.booleanValue());
    }

    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = Library.class)
    public IBaseBundle packagePlanDefinition(
            @OperationParam(name = "id") String id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "usePut") BooleanType isPut,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        IIdType idToUse = getIdType(FhirVersionEnum.DSTU3, "Library", id);
        StringType canonicalType = getCanonicalType(FhirVersionEnum.DSTU3, canonical, url, version);
        return libraryProcessorFactory
                .create(requestDetails)
                .packageLibrary(
                        Eithers.for3(canonicalType, idToUse, null),
                        isPut == null ? Boolean.FALSE : isPut.booleanValue());
    }
}
