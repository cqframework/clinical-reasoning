package org.opencds.cqf.fhir.utility.adapter

import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import java.util.Date
import java.util.Optional
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.dstu3.model.MetadataResource
import org.hl7.fhir.dstu3.model.Reference
import org.hl7.fhir.dstu3.model.RelatedArtifact
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.BundleHelper.getEntryResources
import org.opencds.cqf.fhir.utility.Canonicals
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical
import org.opencds.cqf.fhir.utility.VersionComparator
import org.opencds.cqf.fhir.utility.VersionUtilities
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo.Companion.convertRelatedArtifact
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.Companion.forFhirVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface IKnowledgeArtifactAdapter : IResourceAdapter {
    override fun get(): IDomainResource?

    override fun copy(): IDomainResource?

    fun hasName(): Boolean {
        return StringUtils.isNotBlank(this.name)
    }

    var name: String?
        get() = resolvePathString(get()!!, "name")
        set(name) {
            setValue(get(), "name", IAdapter.Companion.newStringType(fhirVersion()!!, name))
        }

    fun hasTitle(): Boolean {
        return StringUtils.isNotBlank(this.title)
    }

    var title: String?
        get() = resolvePathString(get()!!, "title")
        set(title) {
            setValue(get(), "title", IAdapter.Companion.newStringType(fhirVersion()!!, title))
        }

    val descriptor: String
        get() =
            "${this.get()!!.fhirType()} ${if (this.hasTitle()) this.title else this.name}${if (this.hasVersion()) ", " + this.version else ""}"

    fun hasUrl(): Boolean {
        return StringUtils.isNotBlank(this.url)
    }

    var url: String?
        get() = resolvePathString(get()!!, "url")
        set(url) {
            setValue(get(), "url", IAdapter.Companion.newUriType(fhirVersion()!!, url))
        }

    fun hasVersion(): Boolean {
        return StringUtils.isNotBlank(this.version)
    }

    var version: String?
        get() = resolvePathString(get()!!, "version")
        set(version) {
            setValue(get(), "version", IAdapter.Companion.newStringType(fhirVersion()!!, version))
        }

    val canonical: String?
        /**
         * Returns the url of the artifact appended with '|' version if the artifact has a version.
         *
         * @return canonical url of artifact
         */
        get() {
            val url = if (hasUrl()) this.url else id
            return if (url == null) null else (url + if (hasVersion()) "|${this.version}" else "")
        }

    val dependencies: MutableList<IDependencyInfo?>?

    fun getDependencies(repository: IRepository?): MutableList<IDependencyInfo?>? {
        // TODO: this should be smarter
        return this.dependencies
    }

    val referenceSource: String?
        get() = if (hasVersion()) this.url + "|" + this.version else this.url

    fun addProfileReferences(references: MutableList<IDependencyInfo?>, referenceSource: String?) {
        get()!!.meta.profile.forEach { x ->
            val p = x as IPrimitiveType<String?>
            val e = x as IBaseHasExtensions?
            references.add(
                DependencyInfo(
                    referenceSource,
                    p.valueAsString,
                    e!!.extension,
                    { theValue -> p.value = theValue },
                )
            )
        }
    }

    var approvalDate: Date?
        get() {
            @Suppress("UNCHECKED_CAST")
            val approvalDate =
                resolvePath(get(), "approvalDate", IPrimitiveType::class.java)
                    as IPrimitiveType<Date?>?
            return approvalDate?.value
        }
        set(approvalDate) {
            setApprovalDateElement(IAdapter.Companion.newDateType(fhirVersion()!!, approvalDate))
        }

    fun setApprovalDateElement(approvalDate: IPrimitiveType<Date?>?) {
        try {
            setValue(get(), "approvalDate", approvalDate)
        } catch (e: Exception) {
            // Do nothing
            logger.debug(
                "Field 'approvalDate' does not exist on Resource type {}",
                get()!!.fhirType(),
            )
        }
    }

    var date: Date?
        get() {
            @Suppress("UNCHECKED_CAST")
            val date =
                resolvePath(get(), "date", IPrimitiveType::class.java) as IPrimitiveType<Date?>?
            return date?.value
        }
        set(date) {
            setDateElement(IAdapter.Companion.newDateTimeType(fhirVersion()!!, date))
        }

    fun setDateElement(date: IPrimitiveType<Date?>?) {
        setValue(get(), "date", date)
    }

    val purpose: String?
        get() = resolvePathString(get()!!, "purpose")

    fun <T : ICompositeType> getUseContext(): MutableList<T?>?

    var status: String?

    var effectivePeriod: ICompositeType?
        get() {
            val effectivePeriod = resolvePath(get(), "effectivePeriod", ICompositeType::class.java)
            return effectivePeriod ?: IAdapter.newPeriod(fhirVersion()!!)
        }
        set(period) {
            try {
                setValue(get(), "effectivePeriod", period)
            } catch (e: Exception) {
                // Do nothing
                logger.debug(
                    "Field 'effectivePeriod' does not exist on Resource type {}",
                    get()!!.fhirType(),
                )
            }
        }

    val experimental: Boolean
        get() {
            val experimental = resolvePath(get(), "experimental", IPrimitiveType::class.java)
            @Suppress("UNCHECKED_CAST")
            return experimental != null && (experimental as IPrimitiveType<Boolean?>).value!!
        }

    fun <T> hasRelatedArtifact(): Boolean where T : ICompositeType, T : IBaseHasExtensions {
        return getRelatedArtifact<T>().isNotEmpty()
    }

    fun <T> addRelatedArtifact(relatedArtifact: T?) where
    T : ICompositeType,
    T : IBaseHasExtensions {
        try {
            setValue(get(), "relatedArtifact", mutableListOf(relatedArtifact))
        } catch (e: Exception) {
            // Do nothing
            logger.debug(
                "Field 'relatedArtifact' does not exist on Resource type {}",
                get()!!.fhirType(),
            )
        }
    }

    fun <T> setRelatedArtifact(relatedArtifacts: MutableList<T?>) where
    T : ICompositeType,
    T : IBaseHasExtensions {
        try {
            setValue(get(), "relatedArtifact", null)
            setValue(get(), "relatedArtifact", relatedArtifacts)
        } catch (e: Exception) {
            // Do nothing
            logger.debug(
                "Field 'relatedArtifact' does not exist on Resource type {}",
                get()!!.fhirType(),
            )
        }
    }

    fun <T> getRelatedArtifactsOfType(codeString: String?): MutableList<T?>? where
    T : ICompositeType,
    T : IBaseHasExtensions

    fun <T> getComponents(): MutableList<T?>? where T : ICompositeType, T : IBaseHasExtensions {
        return getRelatedArtifactsOfType<T>("composed-of")
    }

    fun <T> combineComponentsAndDependencies(): MutableList<IDependencyInfo?> where
    T : ICompositeType,
    T : IBaseHasExtensions {
        val referenceSource = if (hasVersion()) this.url + "|" + this.version else this.url

        return (getComponents<T>()!!.filterNotNull().map { ra ->
                convertRelatedArtifact(ra, referenceSource)
            } + this.dependencies!!)
            .toMutableList()
    }

    fun accept(visitor: IKnowledgeArtifactVisitor, operationParameters: IBaseParameters?): IBase? {
        return visitor.visit(this, operationParameters)
    }

    fun <T> getOwnedRelatedArtifacts(): MutableList<T?> where
    T : ICompositeType,
    T : IBaseHasExtensions {
        return getRelatedArtifactsOfType<T>("composed-of")!!
            .filter { relatedArtifact -> checkIfRelatedArtifactIsOwned(relatedArtifact) }
            .toMutableList()
    }

    val expansionParameters: Optional<IBaseParameters>
        get() = Optional.empty<IBaseParameters>()

    val referencedLibraries: MutableMap<String?, String?>
        get() = resolveCqfLibraries()

    fun retrieveReferencedLibraries(
        repository: IRepository?
    ): MutableMap<String?, ILibraryAdapter?> {
        return this.referencedLibraries.values
            .map { url ->
                adapterFactory!!.createLibrary(
                    searchRepositoryByCanonical(
                        repository!!,
                        VersionUtilities.canonicalTypeForVersion(fhirVersion()!!, url),
                    )
                )
            }
            .associateBy { it.name }
            .toMutableMap()
    }

    fun resolveCqfLibraries(): MutableMap<String?, String?> {
        return getExtension<IBaseExtension<*, *>>()
            .filter { e -> Constants.CQF_LIBRARY == e!!.url }
            .map { obj -> obj!!.value }
            .filterIsInstance<IPrimitiveType<*>>()
            .map { obj -> obj.valueAsString }
            .filter { l -> !Canonicals.getIdPart(l!!).isNullOrBlank() }
            .associateBy { l -> Canonicals.getIdPart(l!!)!! }
            .toMutableMap()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(IKnowledgeArtifactAdapter::class.java)
        const val DEPENDSON: String = "depends-on"

        @JvmStatic
        fun <T> newRelatedArtifact(
            version: FhirVersionEnum,
            type: String?,
            reference: String?,
            display: String?,
        ): T where T : ICompositeType, T : IBaseHasExtensions {
            @Suppress("UNCHECKED_CAST")
            return when (version) {
                FhirVersionEnum.DSTU3 -> {
                    val dstu3 = RelatedArtifact()
                    dstu3
                        .setType(RelatedArtifact.RelatedArtifactType.fromCode(type))
                        .setResource(Reference(reference))
                        .setDisplay(display)
                    dstu3
                }

                FhirVersionEnum.R4 -> {
                    val r4 = org.hl7.fhir.r4.model.RelatedArtifact()
                    r4.setType(
                            org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.fromCode(type)
                        )
                        .setResource(reference)
                        .setDisplay(display)
                    r4
                }

                FhirVersionEnum.R5 -> {
                    val r5 = org.hl7.fhir.r5.model.RelatedArtifact()
                    r5.setType(
                            org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.fromCode(type)
                        )
                        .setResource(reference)
                        .setDisplay(display)
                    r5
                }

                else -> throw UnprocessableEntityException("Unsupported version: $version")
            }
                as T
        }

        @JvmStatic
        fun <T> getRelatedArtifactReference(relatedArtifact: T?): String? where
        T : ICompositeType,
        T : IBaseHasExtensions {
            return when (relatedArtifact) {
                is RelatedArtifact -> relatedArtifact.resource.reference

                is org.hl7.fhir.r4.model.RelatedArtifact -> relatedArtifact.resource

                is org.hl7.fhir.r5.model.RelatedArtifact -> relatedArtifact.resource

                else -> throw UnprocessableEntityException(VALID_RELATED_ARTIFACT)
            }
        }

        @JvmStatic
        fun <T> getRelatedArtifactDisplay(relatedArtifact: T?): String? where
        T : ICompositeType,
        T : IBaseHasExtensions {
            return when (relatedArtifact) {
                is RelatedArtifact -> relatedArtifact.display

                is org.hl7.fhir.r4.model.RelatedArtifact -> relatedArtifact.display

                is org.hl7.fhir.r5.model.RelatedArtifact -> relatedArtifact.display

                else -> throw UnprocessableEntityException(VALID_RELATED_ARTIFACT)
            }
        }

        @JvmStatic
        fun <T> getRelatedArtifactType(relatedArtifact: T?): String? where
        T : ICompositeType,
        T : IBaseHasExtensions {
            return when (relatedArtifact) {
                is RelatedArtifact -> relatedArtifact.type.toCode()

                is org.hl7.fhir.r4.model.RelatedArtifact -> relatedArtifact.type.toCode()

                is org.hl7.fhir.r5.model.RelatedArtifact -> relatedArtifact.type.toCode()

                else -> throw UnprocessableEntityException(VALID_RELATED_ARTIFACT)
            }
        }

        @JvmStatic
        fun <T> setRelatedArtifactReference(
            relatedArtifact: T?,
            reference: String?,
            display: String?,
        ) where T : ICompositeType, T : IBaseHasExtensions {
            when (relatedArtifact) {
                is RelatedArtifact -> {
                    relatedArtifact.resource.setReference(reference).setDisplay(display)
                }

                is org.hl7.fhir.r4.model.RelatedArtifact -> {
                    relatedArtifact.setResource(reference).setDisplay(display)
                }

                is org.hl7.fhir.r5.model.RelatedArtifact -> {
                    relatedArtifact.setResource(reference).setDisplay(display)
                }

                else -> {
                    throw UnprocessableEntityException(VALID_RELATED_ARTIFACT)
                }
            }
        }

        @JvmStatic
        fun <T> checkIfRelatedArtifactIsOwned(relatedArtifact: T?): Boolean where
        T : ICompositeType,
        T : IBaseHasExtensions {
            return relatedArtifact!!.extension.any { ext -> ext!!.url == IS_OWNED_URL }
        }

        fun isSupportedMetadataResource(resource: IBaseResource?): Boolean {
            return resource is MetadataResource ||
                resource is org.hl7.fhir.r4.model.MetadataResource ||
                resource is org.hl7.fhir.r5.model.MetadataResource
        }

        @JvmStatic
        fun findLatestVersion(bundle: IBaseBundle): Optional<IDomainResource> {
            val versionComparator = VersionComparator()
            val sorted =
                getEntryResources(bundle)
                    .filter { resource -> isSupportedMetadataResource(resource) }
                    .map { r ->
                        forFhirVersion(r.structureFhirVersionEnum).createResource(r)
                            as IKnowledgeArtifactAdapter?
                    }
                    .sortedWith { a, b -> versionComparator.compare(a!!.version!!, b!!.version!!) }
            return if (sorted.isNotEmpty()) {
                Optional.of(sorted[sorted.size - 1]!!.get()!!)
            } else {
                Optional.empty()
            }
        }

        const val VALID_RELATED_ARTIFACT: String = "Must be a valid RelatedArtifact"
        const val RELEASE_LABEL_URL: String =
            "http://hl7.org/fhir/StructureDefinition/artifact-releaseLabel"
        const val RELEASE_DESCRIPTION_URL: String =
            "http://hl7.org/fhir/StructureDefinition/artifact-releaseDescription"
        const val US_PH_CONTEXT_TYPE_URL: String =
            "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context-type"
        const val CONTEXT_TYPE_URL: String =
            "http://terminology.hl7.org/CodeSystem/usage-context-type"
        const val CONTEXT_URL: String = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context"
        const val IS_OWNED_URL: String = "http://hl7.org/fhir/StructureDefinition/artifact-isOwned"
    }
}
