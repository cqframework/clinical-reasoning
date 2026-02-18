package org.opencds.cqf.fhir.cr.hapi.r4.library;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;
import static org.opencds.cqf.fhir.cr.hapi.common.IdHelper.getIdType;
import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;
import static org.opencds.cqf.fhir.utility.PackageHelper.packageParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class LibraryPackageProvider {
    private final ILibraryProcessorFactory libraryProcessorFactory;
    private final FhirVersionEnum fhirVersion;

    public LibraryPackageProvider(ILibraryProcessorFactory libraryProcessorFactory) {
        this.libraryProcessorFactory = libraryProcessorFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    /**
     * Implements a $package operation following the <a href="https://build.fhir.org/ig/HL7/crmi-ig/branches/master/packaging.html">CRMI IG</a>.
     *
     * @param id the id of the Resource.
     * @param canonical the canonical identifier for the Resource (optionally version-specific).
     * @param url canonical URL of the Resource when invoked at the resource type level. This is exclusive with the id and canonical parameters.
     * @param version version of the Resource when invoked at the resource type level. This is exclusive with the id and canonical parameters.
     * @param offset                        Paging support - where to start if a subset is desired
     *                                      (default = 0). Offset is number of records (not number
     *                                      of pages). It is invalid to request a 'transaction'
     *                                      bundle, via the bundleType parameter, and use paging.
     *                                      Doing so will result in an error. When requesting
     *                                      paging, a bundle of type searchset will be returned.
     * @param count                         Paging support - how many resources should be provided
     *                                      in a partial page view. It is invalid to request a
     *                                      'transaction' bundle, via the bundleType parameter, and
     *                                      use paging. Doing so will result in an error. When
     *                                      requesting paging, a bundle of type searchset will be
     *                                      returned.
     * @param bundleType                    Determines the type of output Bundle. If not specified,
     *                                      the output bundle will be a transaction bundle. Allowed
     *                                      values include 'transaction', and 'collection'.
     *                                      It is invalid to request a 'transaction' bundle and use
     *                                      paging. Doing so will result in an error.
     * @param include Specifies what contents should only be included in the resulting package.
     * @param artifactEndpointConfiguration Configuration information to resolve canonical artifacts.
     *                                      Contains parts: artifactRoute, endpointUri, endpoint.
     * @param terminologyEndpoint the FHIR {@link Endpoint} Endpoint resource or url to use to access terminology (i.e. valuesets, codesystems, naming systems, concept maps, and membership testing) referenced by the Resource. If no terminology endpoint is supplied, the evaluation will attempt to use the server on which the operation is being performed as the terminology server.
     * @param usePut the boolean value to determine if the Bundle returned uses PUT or POST request methods.  Defaults to false.
     * @param requestDetails the details (such as tenant) of this request. Usually autopopulated by HAPI.
     * @return a Bundle containing the ValueSet and all related CodeSystem and ValueSet resources
     */
    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = Library.class)
    public IBaseBundle packageLibrary(
            @IdParam IdType id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "offset", typeName = "integer") IPrimitiveType<Integer> offset,
            @OperationParam(name = "count", typeName = "integer") IPrimitiveType<Integer> count,
            @OperationParam(name = "bundleType") String bundleType,
            @OperationParam(name = "include") List<CodeType> include,
            @OperationParam(name = "artifactEndpointConfiguration")
                    List<Parameters.ParametersParameterComponent> artifactEndpointConfiguration,
            @OperationParam(name = "terminologyEndpoint") Parameters.ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "usePut") BooleanType usePut,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        var canonicalType = getCanonicalType(fhirVersion, canonical, url, version);
        var terminologyEndpointParam = getEndpoint(fhirVersion, terminologyEndpoint);
        List<IBase> artifactEndpointConfigurationParam = artifactEndpointConfiguration == null
                ? null
                : artifactEndpointConfiguration.stream().map(p -> (IBase) p).collect(Collectors.toList());
        var params = packageParameters(
                fhirVersion,
                offset,
                count,
                bundleType,
                include == null
                        ? null
                        : include.stream()
                                .distinct()
                                .map(PrimitiveType::getValueAsString)
                                .collect(Collectors.toList()),
                artifactEndpointConfigurationParam,
                terminologyEndpointParam,
                usePut == null ? Boolean.FALSE : usePut.booleanValue());
        return libraryProcessorFactory
                .create(requestDetails)
                .packageLibrary(Eithers.for3(canonicalType, id, null), params);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = Library.class)
    public IBaseBundle packageLibrary(
            @OperationParam(name = "id") String id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "offset", typeName = "integer") IPrimitiveType<Integer> offset,
            @OperationParam(name = "count", typeName = "integer") IPrimitiveType<Integer> count,
            @OperationParam(name = "bundleType") String bundleType,
            @OperationParam(name = "include") List<CodeType> include,
            @OperationParam(name = "artifactEndpointConfiguration")
                    List<Parameters.ParametersParameterComponent> artifactEndpointConfiguration,
            @OperationParam(name = "terminologyEndpoint") Parameters.ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "usePut") BooleanType usePut,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        var idToUse = getIdType(fhirVersion, "Library", id);
        var canonicalType = getCanonicalType(fhirVersion, canonical, url, version);
        var terminologyEndpointParam = getEndpoint(fhirVersion, terminologyEndpoint);
        List<IBase> artifactEndpointConfigurationParam = artifactEndpointConfiguration == null
                ? null
                : artifactEndpointConfiguration.stream().map(p -> (IBase) p).collect(Collectors.toList());
        var params = packageParameters(
                fhirVersion,
                offset,
                count,
                bundleType,
                include == null
                        ? null
                        : include.stream()
                                .distinct()
                                .map(PrimitiveType::getValueAsString)
                                .collect(Collectors.toList()),
                artifactEndpointConfigurationParam,
                terminologyEndpointParam,
                usePut == null ? Boolean.FALSE : usePut.booleanValue());
        return libraryProcessorFactory
                .create(requestDetails)
                .packageLibrary(Eithers.for3(canonicalType, idToUse, null), params);
    }
}
