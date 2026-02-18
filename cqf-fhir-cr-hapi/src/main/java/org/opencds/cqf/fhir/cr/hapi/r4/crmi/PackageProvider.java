package org.opencds.cqf.fhir.cr.hapi.r4.crmi;

import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;
import static org.opencds.cqf.fhir.utility.Constants.CRMI_OPERATION_PACKAGE;
import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.r4.IPackageServiceFactory;

public class PackageProvider {

    private final IPackageServiceFactory r4PackageServiceFactory;
    private final FhirVersionEnum fhirVersion;

    public PackageProvider(IPackageServiceFactory r4PackageServiceFactory) {
        this.r4PackageServiceFactory = r4PackageServiceFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    // TODO: missing parameters from CRMI operation definition:
    // terminologyCapabilities, includeUri, exclude, excludeUri, errorBehavior
    /**
     * Packages a specified canonical resource for use in a target environment, optionally including
     * related content such as dependencies, components, and test cases and data.
     * The package operation supports producing a complete package of a particular artifact
     * supporting the capabilities expected to be available in a particular target environment. For
     * example, a Questionnaire may be packaged together with the value sets referenced by elements
     * of the questionnaire, and those value sets may be definitions (Computable) or expansions
     * (Expanded), depending on the parameters to the operation.
     *
     * @param id                            The logical id of an existing Resource to package on the
     *                                      server.
     * @param capability                    A desired capability of the resulting package.
     *                                      computable to include computable elements in packaged
     *                                      content; executable to include executable elements in
     *                                      packaged content; publishable to include publishable
     *                                      elements in packaged content.
     *                                      If no capabilities are specified, the capabilities of
     *                                      resulting artifacts in the package are decided by the
     *                                      server. If a server has been requested to produce an
     *                                      executable package, and for whatever reason, cannot
     *                                      expand a value set that is part of the package, it SHALL
     *                                      include an operation outcome warning detailed the value
     *                                      sets that could not be expanded, as described in the
     *                                      Outcome Manifest topic.
     *                                      In addition, so long as the errorBehavior parameter is
     *                                      not strict, the server MAY include a computable
     *                                      representation of value sets that could not be expanded.
     * @param artifactVersion               Specifies a version to use for a canonical or artifact
     *                                      resource if the artifact referencing the resource does
     *                                      not already specify a version. The format is the same as
     *                                      a canonical URL: [url]|[version] - e.g.
     *                                      http://loinc.org|2.56 Note that this is a generalization
     *                                      of the system-version parameter to the $expand operation
     *                                      to apply to any canonical resource, including code
     *                                      systems.
     * @param checkArtifactVersion          Edge Case: Specifies a version to use for a canonical or
     *                                      artifact resource. If the artifact referencing the
     *                                      resource specifies a different version, an error is
     *                                      returned instead of the package. The format is the same
     *                                      as a canonical URL: [url]|[version] - e.g.
     *                                      http://loinc.org|2.56 Note that this is a generalization
     *                                      of the check-system-version parameter to the $expand
     *                                      operation to apply to any canonical resource, including
     *                                      code systems.
     * @param forceArtifactVersion          Edge Case: Specifies a version to use for a canonical or
     *                                      artifact resource. This parameter overrides any
     *                                      specified version in the artifact (and any artifacts it
     *                                      depends on). The format is the same as a canonical URL:
     *                                      [system]|[version] - e.g. http://loinc.org|2.56. Note
     *                                      that this has obvious safety issues, in that it may
     *                                      result in a value set expansion giving a different list
     *                                      of codes that is both wrong and unsafe, and implementers
     *                                      should only use this capability reluctantly. It
     *                                      primarily exists to deal with situations where
     *                                      specifications have fallen into decay as time passes. If
     *                                      the version of a canonical is overridden, the version
     *                                      used SHALL explicitly be represented in the expansion
     *                                      parameters. Note that this is a generalization of the
     *                                      force-system-version parameter to the $expand operation
     *                                      to apply to any canonical resource, including code
     *                                      systems.
     * @param manifest                      Specifies a reference to an asset-collection library
     *                                      that defines version bindings for code systems and other
     *                                      canonical resources referenced by the value set(s) being
     *                                      expanded and other canonical resources referenced by the
     *                                      artifact. When specified, code systems and other
     *                                      canonical resources identified as depends-on related
     *                                      artifacts in the manifest library have the same meaning
     *                                      as specifying that code system or other canonical
     *                                      version in the system-version parameter of an expand or
     *                                      the canonicalVersion parameter.
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
     * @param include                       Specifies what contents should only be included in the
     *                                      resulting package. The codes indicate which types of
     *                                      resources should be included, but note that the set of
     *                                      possible resources is determined as all known (i.e.
     *                                      present on the server) dependencies and related
     *                                      artifacts.
     *                                      Possible values are either a code to mean a category of
     *                                      resource types:
     *                                          all (default) - all resource types
     *                                          artifact - the specified artifact
     *                                          canonical - canonical resources (i.e. resources with
     *                                              a defined url element or that can be canonical
     *                                              resources using the artifact-url extension)
     *                                          terminology - terminology resources (i.e.
     *                                              CodeSystem, ValueSet, NamingSystem, ConceptMap)
     *                                          conformance - conformance resources (i.e.
     *                                              StructureDefinition, StructureMap,
     *                                              SearchParameter, CompartmentDefinition)
     *                                          profiles - profile definitions (i.e.
     *                                              StructureDefinition resources that define
     *                                              profiles)
     *                                          extensions - extension definitions (i.e.
     *                                              StructureDefinition resources that define
     *                                              extensions)
     *                                          knowledge - knowledge artifacts (i.e.
     *                                              ActivityDefinition, Library, PlanDefinition,
     *                                              Measure, Questionnaire)
     *                                          tests - test cases and data (i.e. test cases as
     *                                              defined by the testing specification in this
     *                                              implementation guide)
     *                                          examples - example resources (i.e. resources
     *                                              identified as examples in the implementation
     *                                              guide)
     *                                      Or a valid FHIR resource Type (e.g. PlanDefinition,
     *                                      MedicationKnowledge, etc)
     * @param packageOnly                   True to indicate that the resulting package should only
     *                                      include resources that are defined in the implementation
     *                                      guide or specification that defines the artifact being
     *                                      packaged. False (default) to indicate that the resulting
     *                                      package should include resources regardless of what
     *                                      implementation guide or specification they are defined
     *                                      in.
     * @param artifactEndpointConfiguration Configuration information to resolve canonical artifacts
     *                                          artifactRoute: An optional route used to determine
     *                                          whether this endpoint is expected to be able to
     *                                          resolve artifacts that match the route (i.e. start
     *                                          with the route, up to and including the entire url)
     *                                          endpointUri: The URI of the endpoint, exclusive with
     *                                          the endpoint parameter
     *                                          endpoint: An Endpoint resource describing the
     *                                          endpoint, exclusive with the endpointUri parameter
     *                                      Processing semantics:
     *                                          Create a canonical-like reference (e.g.
     *                                          {canonical.url}|{canonical.version} or similar
     *                                          extensions for non-canonical artifacts).
     *                                          Given a single artifactEndpointConfiguration
     *                                              When artifactRoute is present and artifactRoute
     *                                              starts with canonical or artifact reference then
     *                                              attempt to resolve with endpointUri or endpoint
     *                                              When artifactRoute is not present then attempt
     *                                              to resolve with endpointUri or endpoint
     *                                          Given multiple artifactEndpointConfigurations
     *                                              Then rank order each configuration (see below)
     *                                              and attempt to resolve with endpointUri or
     *                                              endpoint in order until resolved
     *                                      Rank each artifactEndpointConfiguration such that:
     *                                          if artifactRoute is present and artifactRoute starts
     *                                          with canonical or artifact reference: rank based on
     *                                          number of matching characters
     *                                          if artifactRoute is not present: include but rank
     *                                          lower
     *                                      NOTE: For evenly ranked artifactEndpointConfigurations,
     *                                      order as defined in the OperationDefinition.
     * @param terminologyEndpoint           An endpoint to use to access terminology (i.e.
     *                                      valuesets, codesystems, naming systems, concept maps,
     *                                      and membership testing) referenced by the Resource. If
     *                                      no terminology endpoint is supplied, the server may use
     *                                      whatever mechanism is appropriate for accessing
     *                                      terminology. This could be the server on which the
     *                                      operation is invoked or a third party server accessible
     *                                      to the environment. When a terminology endpoint is
     *                                      provided, the server or third party servers may still be
     *                                      used as fallbacks.
     * @param requestDetails                The {@link RequestDetails RequestDetails}
     * @return  The result of the packaging. If the resulting bundle is paged using count or offset,
     *          it will be of type collection. In the special case where count = 0 it will be of
     *          type searchset.
     *          The first resource returned in the resulting package will be an outcome manifest
     *          that describes the results of the packaging operation. Clients consuming package
     *          bundles SHALL examine the contents of these OperationOutcome resources to understand
     *          whether the bundle contains the expected content (e.g. if a ValueSet could not be
     *          expanded).
     *          Servers generating packages SHALL include all the dependency resources referenced by
     *          the artifact that are known to the server and specified by the include parameters.
     *          For example, a measure repository SHALL include all the required library resources,
     *          but would not necessarily have the ValueSet resources referenced by the measure.
     */
    @Operation(name = CRMI_OPERATION_PACKAGE, idempotent = true, global = true, type = MetadataResource.class)
    @Description(shortDefinition = CRMI_OPERATION_PACKAGE, value = "Package an artifact and components / dependencies")
    public Bundle packageOperation(
            @IdParam IdType id,
            // TODO: $package - should capability be CodeType?
            @OperationParam(name = "capability") List<StringType> capability,
            @OperationParam(name = "artifactVersion") List<CanonicalType> artifactVersion,
            @OperationParam(name = "checkArtifactVersion") List<CanonicalType> checkArtifactVersion,
            @OperationParam(name = "forceArtifactVersion") List<CanonicalType> forceArtifactVersion,
            @OperationParam(name = "manifest") CanonicalType manifest,
            @OperationParam(name = "offset", typeName = "integer") IPrimitiveType<Integer> offset,
            @OperationParam(name = "count", typeName = "integer") IPrimitiveType<Integer> count,
            @OperationParam(name = "bundleType") StringType bundleType,
            // TODO: $package - should include be CodeType?
            @OperationParam(name = "include") List<StringType> include,
            @OperationParam(name = "packageOnly", typeName = "Boolean") IPrimitiveType<Boolean> packageOnly,
            @OperationParam(name = "artifactEndpointConfiguration")
                    Parameters.ParametersParameterComponent artifactEndpointConfiguration,
            @OperationParam(name = "terminologyEndpoint") Parameters.ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails)
            throws FHIRException {
        return r4PackageServiceFactory
                .create(requestDetails)
                .packageOperation(
                        id,
                        capability == null
                                ? null
                                : capability.stream()
                                        .map(PrimitiveType::getValue)
                                        .toList(),
                        artifactVersion,
                        checkArtifactVersion,
                        forceArtifactVersion,
                        include == null
                                ? null
                                : include.stream().map(PrimitiveType::getValue).toList(),
                        manifest,
                        offset,
                        count,
                        getStringValue(bundleType),
                        packageOnly,
                        artifactEndpointConfiguration,
                        (Endpoint) getEndpoint(fhirVersion, terminologyEndpoint));
    }
}
