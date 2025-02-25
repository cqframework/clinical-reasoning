package org.opencds.cqf.fhir.cr.hapi.r4.library;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.List;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class LibraryEvaluateProvider {
    private final ILibraryProcessorFactory libraryProcessorFactory;

    public LibraryEvaluateProvider(ILibraryProcessorFactory libraryProcessorFactory) {
        this.libraryProcessorFactory = libraryProcessorFactory;
    }

    /**
     * Evaluates a CQL library and returns the results as a Parameters resource.
     *
     * @param id					the library resource's Id
     * @param subject             Subject for which the library will be evaluated.
     *                            This corresponds to the context in which the
     *                            library
     *                            will be evaluated and is represented as a relative
     *                            FHIR id (e.g. Patient/123), which establishes both
     *                            the context and context value for the evaluation
     * @param expression          Expression(s) to be evaluated. If no expression
     *                            names
     *                            are provided, the operation evaluates all public
     *                            expression definitions in the library
     * @param parameters          Any input parameters for the expression.
     *                            {@link Parameters} Parameters defined in this
     *                            input will be made available by name to the CQL
     *                            expression. Parameter types are mapped to CQL as
     *                            specified in the Using CQL section of the CPG
     *                            Implementation guide. If a parameter appears more
     *                            than once in the input Parameters resource, it is
     *                            represented with a List in the input CQL. If a
     *                            parameter has parts, it is represented as a Tuple
     *                            in the input CQL
     * @param useServerData 	  Whether to use data from the server performing the
     *                             evaluation. If this parameter is true (the default),
     *                             then the operation will use data first from any
     *                             bundles provided as parameters (through the data
     *                             and prefetch parameters), second data from the
     *                             server performing the operation, and third, data
     *                             from the dataEndpoint parameter (if provided).
     *                             If this parameter is false, the operation will use
     *                             data first from the bundles provided in the data or
     *                             prefetch parameters, and second from the dataEndpoint
     *                             parameter (if provided).
     * @param data                Data to be made available to the library
     *                            evaluation. This parameter is exclusive with the
     *                            prefetchData parameter (i.e. either provide all
     *                            data as a single bundle, or provide data using
     *                            multiple bundles with prefetch descriptions)
     * @param prefetchData        ***Not Yet Implemented***
     * @param dataEndpoint        An {@link Endpoint} endpoint to use to access data
     *                            referenced by retrieve operations in the library.
     *                            If provided, this endpoint is used after the data
     *                            or prefetchData bundles, and the server, if the
     *                            useServerData parameter is true.
     * @param contentEndpoint     An {@link Endpoint} endpoint to use to access
     *                            content (i.e. libraries) referenced by the
     *                            library. If no content endpoint is supplied, the
     *                            evaluation will attempt to retrieve content from
     *                            the server on which the operation is being
     *                            performed
     * @param terminologyEndpoint An {@link Endpoint} endpoint to use to access
     *                            terminology (i.e. valuesets, codesystems, and
     *                            membership testing) referenced by the library. If
     *                            no terminology endpoint is supplied, the
     *                            evaluation will attempt to use the server on which
     *                            the operation is being performed as the
     *                            terminology server
     * @param requestDetails      the {@link RequestDetails RequestDetails}
     * @return The results of the library evaluation, returned as a
     *         {@link Parameters} resource
     *         with a parameter for each named expression defined in the library.
     *         The value of
     *         each expression is returned as a FHIR type, either a resource, or a
     *         FHIR-defined
     *         type corresponding to the CQL return type, as defined in the Using
     *         CQL section of
     *         this implementation guide. If the result of an expression is a list
     *         of resources,
     *         that parameter will be repeated for each element in the result
     */
    @Operation(name = ProviderConstants.CR_OPERATION_EVALUATE, idempotent = true, type = Library.class)
    public Parameters evaluate(
            @IdParam IdType id,
            @OperationParam(name = "subject") String subject,
            @OperationParam(name = "expression") List<String> expression,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "prefetchData") List<Parameters.ParametersParameterComponent> prefetchData,
            @OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
            @OperationParam(name = "contentEndpoint") Endpoint contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
            RequestDetails requestDetails) {
        return (Parameters) libraryProcessorFactory
                .create(requestDetails)
                .evaluate(
                        Eithers.forMiddle3(id),
                        subject,
                        expression,
                        parameters,
                        useServerData == null ? Boolean.TRUE : useServerData.booleanValue(),
                        data,
                        prefetchData,
                        dataEndpoint,
                        contentEndpoint,
                        terminologyEndpoint);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_EVALUATE, idempotent = true, type = Library.class)
    public Parameters evaluate(
            @OperationParam(name = "library") String library,
            @OperationParam(name = "subject") String subject,
            @OperationParam(name = "expression") List<String> expression,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "prefetchData") List<Parameters.ParametersParameterComponent> prefetchData,
            @OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
            @OperationParam(name = "contentEndpoint") Endpoint contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
            RequestDetails requestDetails) {
        CanonicalType canonicalType = getCanonicalType(FhirVersionEnum.R4, library, null, null);
        return (Parameters) libraryProcessorFactory
                .create(requestDetails)
                .evaluate(
                        Eithers.forLeft3(canonicalType),
                        subject,
                        expression,
                        parameters,
                        useServerData == null ? Boolean.TRUE : useServerData.booleanValue(),
                        data,
                        prefetchData,
                        dataEndpoint,
                        contentEndpoint,
                        terminologyEndpoint);
    }
}
