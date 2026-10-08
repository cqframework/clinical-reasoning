package org.opencds.cqf.fhir.utility.adapter

import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.instance.model.api.IBaseExtension
import org.hl7.fhir.instance.model.api.ICompositeType
import org.opencds.cqf.fhir.utility.Constants

class DependencyInfo(
    override var referenceSource: String? = null,
    reference: String? = null,
    private var extensionList: MutableList<out IBaseExtension<*, *>?>? = null,
    private var updateReferenceConsumer: ((String?) -> Unit)? = null,
) : IDependencyInfo {
    // TODO: Need for figuring out how to determine which package the dependency is in.

    override var reference: String? = reference
        set(reference) {
            field = reference
            this.updateReferenceConsumer!!(reference)
        }

    override var referencePackageId: String? = null

    override var roles = mutableListOf<String?>()
    override val fhirPaths = mutableListOf<String?>()

    override fun <E : IBaseExtension<*, *>> getExtension(): MutableList<E?>? {
        @Suppress("UNCHECKED_CAST")
        return this.extensionList as MutableList<E?>?
    }

    override fun addRole(role: String?) {
        if (role != null && !this.roles.contains(role)) {
            this.roles.add(role)
        }
    }

    override fun addFhirPath(fhirPath: String?) {
        if (fhirPath != null && !this.fhirPaths.contains(fhirPath)) {
            this.fhirPaths.add(fhirPath)
        }
    }

    override fun <E : IBaseExtension<*, *>> buildDependencyExtensions(
        fhirVersion: FhirVersionEnum?,
        sourceArtifactUrl: String?,
    ): MutableList<E?> {
        val extensions = mutableListOf<E?>()

        // Add role extensions
        for (role in roles) {
            val roleExt = buildDependencyRoleExtension<E?>(fhirVersion, role)
            if (roleExt != null) {
                extensions.add(roleExt)
            }
        }

        // Add package-source extension if available
        if (referencePackageId != null && referencePackageId!!.isNotEmpty()) {
            val packageExt = buildPackageSourceExtension<E>(fhirVersion, referencePackageId)
            if (packageExt != null) {
                extensions.add(packageExt)
            }
        }

        // Add reference-source extensions for each FHIRPath
        if (!sourceArtifactUrl.isNullOrEmpty()) {
            for (fhirPath in fhirPaths) {
                val refSourceExt =
                    buildReferenceSourceExtension<E>(fhirVersion, sourceArtifactUrl, fhirPath)
                if (refSourceExt != null) {
                    extensions.add(refSourceExt)
                }
            }
        }

        return extensions
    }

    private fun <E : IBaseExtension<*, *>?> buildDependencyRoleExtension(
        fhirVersion: FhirVersionEnum?,
        roleCode: String?,
    ): E? {
        @Suppress("UNCHECKED_CAST")
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                org.hl7.fhir.dstu3.model.Extension(
                    Constants.CRMI_DEPENDENCY_ROLE,
                    org.hl7.fhir.dstu3.model.CodeType(roleCode),
                )

            FhirVersionEnum.R4 ->
                org.hl7.fhir.r4.model.Extension(
                    Constants.CRMI_DEPENDENCY_ROLE,
                    org.hl7.fhir.r4.model.CodeType(roleCode),
                )

            FhirVersionEnum.R5 ->
                org.hl7.fhir.r5.model.Extension(
                    Constants.CRMI_DEPENDENCY_ROLE,
                    org.hl7.fhir.r5.model.CodeType(roleCode),
                )

            else -> null
        }
            as E?
    }

    private fun <E : IBaseExtension<*, *>> buildPackageSourceExtension(
        fhirVersion: FhirVersionEnum?,
        packageSource: String?,
    ): E? {
        @Suppress("UNCHECKED_CAST")
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                org.hl7.fhir.dstu3.model.Extension(
                    Constants.PACKAGE_SOURCE,
                    org.hl7.fhir.dstu3.model.StringType(packageSource),
                )

            FhirVersionEnum.R4 ->
                org.hl7.fhir.r4.model.Extension(
                    Constants.PACKAGE_SOURCE,
                    org.hl7.fhir.r4.model.StringType(packageSource),
                )

            FhirVersionEnum.R5 ->
                org.hl7.fhir.r5.model.Extension(
                    Constants.PACKAGE_SOURCE,
                    org.hl7.fhir.r5.model.StringType(packageSource),
                )

            else -> null
        }
            as E?
    }

    private fun <E : IBaseExtension<*, *>> buildReferenceSourceExtension(
        fhirVersion: FhirVersionEnum?,
        artifactCanonical: String?,
        fhirPath: String?,
    ): E? {
        @Suppress("UNCHECKED_CAST")
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 -> {
                val ext = org.hl7.fhir.dstu3.model.Extension(Constants.CRMI_REFERENCE_SOURCE)
                ext.addExtension("artifact", org.hl7.fhir.dstu3.model.UriType(artifactCanonical))
                ext.addExtension("path", org.hl7.fhir.dstu3.model.StringType(fhirPath))
                ext
            }

            FhirVersionEnum.R4 -> {
                val ext = org.hl7.fhir.r4.model.Extension(Constants.CRMI_REFERENCE_SOURCE)
                ext.addExtension("artifact", org.hl7.fhir.r4.model.CanonicalType(artifactCanonical))
                ext.addExtension("path", org.hl7.fhir.r4.model.StringType(fhirPath))
                ext
            }

            FhirVersionEnum.R5 -> {
                val ext = org.hl7.fhir.r5.model.Extension(Constants.CRMI_REFERENCE_SOURCE)
                ext.addExtension("artifact", org.hl7.fhir.r5.model.CanonicalType(artifactCanonical))
                ext.addExtension("path", org.hl7.fhir.r5.model.StringType(fhirPath))
                ext
            }

            else -> null
        }
            as E?
    }

    companion object {
        @JvmStatic
        fun convertRelatedArtifact(ra: ICompositeType?, source: String?): IDependencyInfo {
            return when (ra) {
                is org.hl7.fhir.dstu3.model.RelatedArtifact ->
                    DependencyInfo(
                        source,
                        ra.resource.reference,
                        ra.extension,
                        { ref -> ra.resource.reference = ref },
                    )

                is org.hl7.fhir.r4.model.RelatedArtifact ->
                    DependencyInfo(
                        source,
                        ra.resource,
                        ra.extension,
                        { value -> ra.resource = value },
                    )

                // R5 can have either a Resource (canonical URL) or a ResourceReference
                //      we'll take the canonicalURL if it's there, but fallback to ResourceReference
                //      if it's not.
                is org.hl7.fhir.r5.model.RelatedArtifact ->
                    DependencyInfo(
                        source,
                        listOf(
                                ra.resource,
                                if (ra.resourceReference == null) null
                                else ra.resourceReference.reference,
                            )
                            .firstNotNullOfOrNull { it },
                        ra.extension,
                        { value -> ra.resource = value },
                    )

                else ->
                    throw UnprocessableEntityException(
                        "A valid RelatedArtifact object must be provided"
                    )
            }
        }
    }
}
