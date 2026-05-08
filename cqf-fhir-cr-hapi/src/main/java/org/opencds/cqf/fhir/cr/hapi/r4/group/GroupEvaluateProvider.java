package org.opencds.cqf.fhir.cr.hapi.r4.group;

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
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.common.IGroupProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class GroupEvaluateProvider {
    private final IGroupProcessorFactory groupProcessorFactory;
    private final FhirVersionEnum fhirVersion;

    public GroupEvaluateProvider(IGroupProcessorFactory groupProcessorFactory) {
        this.groupProcessorFactory = groupProcessorFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    /**
     * Evaluates a Group definition and returns the results as a Group resource.
     *
     * @param id				  The group resource's ID
     * @param subject             Subject for which the group will be evaluated.
     *                            This corresponds to the context in which the
     *                            group
     *                            will be evaluated and is represented as a relative
     *                            FHIR id (e.g. Patient/123), which establishes both
     *                            the context and context value for the evaluation
     * @param parameters          Any input parameters to the evaluation. Parameters
     *                            defined in this input will be bound by name to the
     *                            evaluation context. If the membership criteria is a
     *                            CQL expression, these parameters will be available to
     *                            the expression. If the membership criteria is a CQL
     *                            identifier, these parameters will be bound by name
     *                            to the parameters defined in the CQL library (or
     *                            included libraries, recursively). Parameter types are
     *                            mapped to CQL as specified in the Using CQL section of
     *                            this implementation guide. If a parameter appears more
     *                            than once in the input Parameters resource, it is
     *                            represented with a List in the input CQL. If a parameter
     *                            has parts, it is represented as a Tuple in the input CQL.
     *                            If parameter names are qualified, the parameter will be
     *                            bound only to parameters in the library with the qualifier
     *                            name, and the qualifier name must be the name of a library
     *                            included by the library being evaluated (or an included
     *                            library, recursively).
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
    @Operation(name = ProviderConstants.CR_OPERATION_EVALUATE, idempotent = true, type = Group.class)
    public Group evaluate(
            @IdParam IdType id,
            @OperationParam(name = "subject") StringType subject,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "prefetchData") List<ParametersParameterComponent> prefetchData,
            @OperationParam(name = "dataEndpoint") ParametersParameterComponent dataEndpoint,
            @OperationParam(name = "contentEndpoint") ParametersParameterComponent contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails) {
        return (Group) groupProcessorFactory
                .create(requestDetails)
                .evaluate(
                        Eithers.forMiddle3(id),
                        getStringValue(subject),
                        parameters,
                        useServerData == null ? Boolean.TRUE : useServerData.booleanValue(),
                        data,
                        prefetchData,
                        getEndpoint(fhirVersion, dataEndpoint),
                        getEndpoint(fhirVersion, contentEndpoint),
                        getEndpoint(fhirVersion, terminologyEndpoint));
    }

    /**
     * Evaluates a Group definition and returns the results as a Group resource.
     *
     * @param group               The group to be evaluated, provided as an input.
     *                            This parameter is only used when the operation is
     *                            invoked at the type level, and is exclusive with
     *                            the url parameter.
     * @param url                 The canonical url (with optional version) of the
     *                            group to be evaluated. This parameter is only used
     *                            when the operation is invoked at the type level, and
     *                            is exclusive with the group parameter.
     * @param subject             Subject for which the group will be evaluated.
     *                            This corresponds to the context in which the
     *                            group
     *                            will be evaluated and is represented as a relative
     *                            FHIR id (e.g. Patient/123), which establishes both
     *                            the context and context value for the evaluation
     * @param parameters          Any input parameters to the evaluation. Parameters
     *                            defined in this input will be bound by name to the
     *                            evaluation context. If the membership criteria is a
     *                            CQL expression, these parameters will be available to
     *                            the expression. If the membership criteria is a CQL
     *                            identifier, these parameters will be bound by name
     *                            to the parameters defined in the CQL library (or
     *                            included libraries, recursively). Parameter types are
     *                            mapped to CQL as specified in the Using CQL section of
     *                            this implementation guide. If a parameter appears more
     *                            than once in the input Parameters resource, it is
     *                            represented with a List in the input CQL. If a parameter
     *                            has parts, it is represented as a Tuple in the input CQL.
     *                            If parameter names are qualified, the parameter will be
     *                            bound only to parameters in the library with the qualifier
     *                            name, and the qualifier name must be the name of a library
     *                            included by the library being evaluated (or an included
     *                            library, recursively).
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
    @Operation(name = ProviderConstants.CR_OPERATION_EVALUATE, idempotent = true, type = Group.class)
    public Group evaluate(
            @OperationParam(name = "group") Group group,
            @OperationParam(name = "url", typeName = "uri") IPrimitiveType<String> url,
            @OperationParam(name = "subject") StringType subject,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "prefetchData") List<ParametersParameterComponent> prefetchData,
            @OperationParam(name = "dataEndpoint") ParametersParameterComponent dataEndpoint,
            @OperationParam(name = "contentEndpoint") ParametersParameterComponent contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails) {
        return (Group) groupProcessorFactory
                .create(requestDetails)
                .evaluate(
                        Eithers.for3(newCanonicalType(fhirVersion, getStringValue(url)), null, group),
                        getStringValue(subject),
                        parameters,
                        useServerData == null ? Boolean.TRUE : useServerData.booleanValue(),
                        data,
                        prefetchData,
                        getEndpoint(fhirVersion, dataEndpoint),
                        getEndpoint(fhirVersion, contentEndpoint),
                        getEndpoint(fhirVersion, terminologyEndpoint));
    }
}
