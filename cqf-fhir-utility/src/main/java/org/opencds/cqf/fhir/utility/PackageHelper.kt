package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.Resources.newBaseForVersion
import org.opencds.cqf.fhir.utility.VersionUtilities.booleanTypeForVersion
import org.opencds.cqf.fhir.utility.VersionUtilities.codeTypeForVersion
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter

// CHECKSTYLE.OFF: all
/** This class consists exclusively of static methods that assist with packaging FHIR Resources. */
open class PackageHelper {
    companion object {
        /**
         * Returns a FHIR Parameters resource of the specified version containing the supplied
         * parameters with the correct parameter name.
         *
         * @param fhirVersion the FHIR version to create Parameters for
         * @param terminologyEndpoint the FHIR Endpoint resource to use to access terminology (i.e.
         *   valuesets, codesystems, naming systems, concept maps, and membership testing)
         *   referenced by the Resource. If no terminology endpoint is supplied, the evaluation will
         *   attempt to use the server on which the operation is being performed as the terminology
         *   server.
         * @param isPut the boolean value to determine if the Bundle returned uses PUT or POST
         *   request methods.
         * @return FHIR Parameters resource
         */
        @JvmStatic
        fun packageParameters(
            fhirVersion: FhirVersionEnum,
            terminologyEndpoint: IBaseResource?,
            isPut: Boolean,
        ): IBaseParameters? {
            return packageParameters(fhirVersion, null, terminologyEndpoint, isPut)
        }

        /**
         * Returns a FHIR Parameters resource of the specified version containing the supplied
         * parameters with the correct parameter name.
         *
         * @param fhirVersion the FHIR version to create Parameters for
         * @param artifactEndpointConfiguration Configuration information to resolve canonical
         *   artifacts. Each element is a Parameters.ParametersParameterComponent with parts:
         *   artifactRoute, endpointUri, endpoint.
         * @param terminologyEndpoint the FHIR Endpoint resource to use to access terminology (i.e.
         *   valuesets, codesystems, naming systems, concept maps, and membership testing)
         *   referenced by the Resource.
         * @param isPut the boolean value to determine if the Bundle returned uses PUT or POST
         *   request methods.
         * @return FHIR Parameters resource
         */
        @JvmStatic
        fun packageParameters(
            fhirVersion: FhirVersionEnum,
            artifactEndpointConfiguration: MutableList<IBase?>?,
            terminologyEndpoint: IBaseResource?,
            isPut: Boolean,
        ): IBaseParameters? {
            return packageParameters(
                fhirVersion,
                null,
                null,
                null,
                null,
                artifactEndpointConfiguration,
                terminologyEndpoint,
                isPut,
            )
        }

        /**
         * Returns a FHIR Parameters resource of the specified version containing the supplied
         * parameters with the correct parameter name.
         *
         * @param fhirVersion the FHIR version to create Parameters for
         * @param include Specifies what contents should only be included in the resulting package.
         * @param terminologyEndpoint the FHIR Endpoint resource to use to access terminology (i.e.
         *   valuesets, codesystems, naming systems, concept maps, and membership testing)
         *   referenced by the Resource. If no terminology endpoint is supplied, the evaluation will
         *   attempt to use the server on which the operation is being performed as the terminology
         *   server.
         * @param isPut the boolean value to determine if the Bundle returned uses PUT or POST
         *   request methods.
         * @return FHIR Parameters resource
         */
        @JvmStatic
        fun packageParameters(
            fhirVersion: FhirVersionEnum,
            offset: IPrimitiveType<Int?>?,
            count: IPrimitiveType<Int?>?,
            bundleType: String?,
            include: MutableList<String?>?,
            terminologyEndpoint: IBaseResource?,
            isPut: Boolean,
        ): IBaseParameters? {
            return packageParameters(
                fhirVersion,
                offset,
                count,
                bundleType,
                include,
                null,
                terminologyEndpoint,
                isPut,
            )
        }

        /**
         * Returns a FHIR Parameters resource of the specified version containing the supplied
         * parameters with the correct parameter name.
         *
         * @param fhirVersion the FHIR version to create Parameters for
         * @param offset Paging support - where to start if a subset is desired.
         * @param count Paging support - how many resources should be provided in a partial page
         *   view.
         * @param bundleType Determines the type of output Bundle.
         * @param include Specifies what contents should only be included in the resulting package.
         * @param artifactEndpointConfiguration Configuration information to resolve canonical
         *   artifacts. Each element is a Parameters.ParametersParameterComponent with parts:
         *   artifactRoute, endpointUri, endpoint.
         * @param terminologyEndpoint the FHIR Endpoint resource to use to access terminology (i.e.
         *   valuesets, codesystems, naming systems, concept maps, and membership testing)
         *   referenced by the Resource.
         * @param isPut the boolean value to determine if the Bundle returned uses PUT or POST
         *   request methods.
         * @return FHIR Parameters resource
         */
        @JvmStatic
        fun packageParameters(
            fhirVersion: FhirVersionEnum,
            offset: IPrimitiveType<Int?>?,
            count: IPrimitiveType<Int?>?,
            bundleType: String?,
            include: List<String?>?,
            artifactEndpointConfiguration: List<IBase?>?,
            terminologyEndpoint: IBaseResource?,
            isPut: Boolean,
        ): IBaseParameters? {
            val params =
                IAdapterFactory.forFhirVersion(fhirVersion)
                    .createParameters(
                        newBaseForVersion("Parameters", fhirVersion) as IBaseParameters
                    )
            if (offset != null) {
                params.addParameter("offset", offset)
            }
            if (count != null) {
                params.addParameter("count", count)
            }
            if (bundleType != null) {
                params.addParameter("bundleType", bundleType)
            }
            if (include != null) {
                for (i in include) {
                    params.addParameter("include", codeTypeForVersion(fhirVersion, i))
                }
            }
            if (artifactEndpointConfiguration != null) {
                for (config in artifactEndpointConfiguration) {
                    params.addParameter(config)
                }
            }
            if (terminologyEndpoint != null) {
                params.addParameter("terminologyEndpoint", terminologyEndpoint)
            }
            params.addParameter("isPut", booleanTypeForVersion(fhirVersion, isPut))
            return params.get() as IBaseParameters?
        }

        @JvmStatic
        fun createEntry(resource: IBaseResource, isPut: Boolean): IBaseBackboneElement {
            val fhirVersion = resource.structureFhirVersionEnum
            val entry = BundleHelper.newEntryWithResource(resource)
            val method: String?
            var requestUrl = resource.fhirType()
            if (isPut) {
                method = "PUT"
                if (resource.idElement != null && !resource.idElement.idPart.isNullOrBlank()) {
                    requestUrl += "/" + resource.idElement.idPart
                }
            } else {
                method = "POST"
            }
            val request = BundleHelper.newRequest(fhirVersion, method, requestUrl)
            BundleHelper.setEntryRequest(fhirVersion, entry, request)
            if (IKnowledgeArtifactAdapter.isSupportedMetadataResource(resource)) {
                val adapter =
                    IAdapterFactory.forFhirVersion(fhirVersion)
                        .createKnowledgeArtifactAdapter(resource as IDomainResource)
                if (adapter.hasUrl()) {
                    val url = adapter.url
                    BundleHelper.setEntryFullUrl(fhirVersion, entry, url)
                    if (!isPut) {
                        if (adapter.hasVersion()) {
                            BundleHelper.setRequestIfNoneExist(
                                fhirVersion,
                                request,
                                "url=$url&version=${adapter.version}",
                            )
                        } else {
                            BundleHelper.setRequestIfNoneExist(fhirVersion, request, "url=$url")
                        }
                    }
                }
            }
            return entry
        }

        @JvmStatic
        fun deleteEntry(resource: IBaseResource): IBaseBackboneElement {
            val fhirVersion = resource.structureFhirVersionEnum
            val entry = BundleHelper.newEntryWithResource(resource)
            val requestUrl = resource.fhirType() + "/" + resource.idElement.idPart

            val request = BundleHelper.newRequest(fhirVersion, "DELETE", requestUrl)
            BundleHelper.setEntryRequest(fhirVersion, entry, request)

            return entry
        }
    }
}
