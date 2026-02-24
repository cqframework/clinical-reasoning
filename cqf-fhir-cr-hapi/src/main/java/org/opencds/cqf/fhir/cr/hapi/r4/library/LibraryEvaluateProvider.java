package org.opencds.cqf.fhir.cr.hapi.r4.library;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.newCanonicalType;
import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;
import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.List;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.common.ILibraryProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

@SuppressWarnings("java:S107")
public class LibraryEvaluateProvider {
    private final ILibraryProcessorFactory libraryProcessorFactory;
    private final FhirVersionEnum fhirVersion;

    public LibraryEvaluateProvider(ILibraryProcessorFactory libraryProcessorFactory) {
        this.libraryProcessorFactory = libraryProcessorFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    /**
     * Evaluates a CQL library and returns the results as a Parameters resource.
     *
     * @param id				  The library resource's ID
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
     * @param dataEndpoint        The FHIR {@link Endpoint} Endpoint resource or url to use to access data
     *                            referenced by retrieve operations in the library.
     *                            If provided, this endpoint is used after the data
     *                            or prefetchData bundles, and the server, if the
     *                            useServerData parameter is true.
     * @param contentEndpoint     The FHIR {@link Endpoint} Endpoint resource or url to use to access
     *                            content (i.e. libraries) referenced by the
     *                            library. If no content endpoint is supplied, the
     *                            evaluation will attempt to retrieve content from
     *                            the server on which the operation is being
     *                            performed
     * @param terminologyEndpoint The FHIR {@link Endpoint} Endpoint resource or url to use to access
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
            @OperationParam(name = "subject") StringType subject,
            @OperationParam(name = "expression") List<StringType> expression,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "prefetchData") List<ParametersParameterComponent> prefetchData,
            @OperationParam(name = "dataEndpoint") ParametersParameterComponent dataEndpoint,
            @OperationParam(name = "contentEndpoint") ParametersParameterComponent contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails) {
        return (Parameters) libraryProcessorFactory
                .create(requestDetails)
                .evaluate(
                        Eithers.forMiddle3(id),
                        getStringValue(subject),
                        expression == null
                                ? null
                                : expression.stream()
                                        .map(PrimitiveType::getValue)
                                        .toList(),
                        parameters,
                        useServerData == null ? Boolean.TRUE : useServerData.booleanValue(),
                        data,
                        prefetchData,
                        getEndpoint(fhirVersion, dataEndpoint),
                        getEndpoint(fhirVersion, contentEndpoint),
                        getEndpoint(fhirVersion, terminologyEndpoint));
    }

    /**
     * Evaluates a CQL library and returns the results as a Parameters resource.
     *
     * @param library             The library to be evaluated, provided as an input.
     *                            This parameter is only used when the operation is
     *                            invoked at the type level, and is exclusive with
     *                            the url parameter.
     * @param url                 The canonical url (with optional version) of the
     *                            library to be evaluated. This parameter is only used
     *                            when the operation is invoked at the type level, and
     *                            is exclusive with the library parameter.
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
     * @param prefetchData        Data to be made available to the library evaluation, organized as
     *                            prefetch response bundles. Each prefetchData parameter specifies
     *                            either the name of the prefetchKey it is satisfying,
     *                            a DataRequirement describing the prefetch, or both.
     * @param dataEndpoint        The FHIR {@link Endpoint} Endpoint resource or url to use to access data
     *                            referenced by retrieve operations in the library.
     *                            If provided, this endpoint is used after the data
     *                            or prefetchData bundles, and the server, if the
     *                            useServerData parameter is true.
     * @param contentEndpoint     The FHIR {@link Endpoint} Endpoint resource or url to use to access
     *                            content (i.e. libraries) referenced by the
     *                            library. If no content endpoint is supplied, the
     *                            evaluation will attempt to retrieve content from
     *                            the server on which the operation is being
     *                            performed
     * @param terminologyEndpoint The FHIR {@link Endpoint} Endpoint resource or url to use to access
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
            @OperationParam(name = "library") Library library,
            @OperationParam(name = "url", typeName = "uri") IPrimitiveType<String> url,
            @OperationParam(name = "subject") StringType subject,
            @OperationParam(name = "expression") List<StringType> expression,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "prefetchData") List<ParametersParameterComponent> prefetchData,
            @OperationParam(name = "dataEndpoint") ParametersParameterComponent dataEndpoint,
            @OperationParam(name = "contentEndpoint") ParametersParameterComponent contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails) {
        return (Parameters) libraryProcessorFactory
                .create(requestDetails)
                .evaluate(
                        Eithers.for3(newCanonicalType(fhirVersion, getStringValue(url)), null, library),
                        getStringValue(subject),
                        expression == null
                                ? null
                                : expression.stream()
                                        .map(PrimitiveType::getValue)
                                        .toList(),
                        parameters,
                        useServerData == null ? Boolean.TRUE : useServerData.booleanValue(),
                        data,
                        prefetchData,
                        getEndpoint(fhirVersion, dataEndpoint),
                        getEndpoint(fhirVersion, contentEndpoint),
                        getEndpoint(fhirVersion, terminologyEndpoint));
    }
}
