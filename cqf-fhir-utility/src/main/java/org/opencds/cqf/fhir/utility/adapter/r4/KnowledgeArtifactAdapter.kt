package org.opencds.cqf.fhir.utility.adapter.r4

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import java.util.*
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.instance.model.api.IBaseHasExtensions
import org.hl7.fhir.instance.model.api.ICompositeType
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.instance.model.api.IPrimitiveType
import org.hl7.fhir.r4.model.*
import org.opencds.cqf.fhir.utility.RelatedArtifactUtil.getRelatedArtifactType
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter

open class KnowledgeArtifactAdapter : ResourceAdapter, IKnowledgeArtifactAdapter {
    var adaptedResource: MetadataResource? = null

    constructor(resource: IDomainResource) : super(resource) {
        if (resource is MetadataResource) {
            adaptedResource = resource
        }
    }

    constructor(resource: MetadataResource) : super(resource) {
        adaptedResource = resource
    }

    override fun get(): DomainResource {
        return adaptedResource!!
    }

    val metadataResource: MetadataResource
        get() {
            checkAdaptedResource()
            return adaptedResource!!
        }

    // TODO: All these elements should be implemented as a super that handles the case where the
    // element does not exist,
    // but the appropriate artifact-xxx extension does
    protected fun checkAdaptedResource() {
        requireNotNull(adaptedResource) {
            "resource passed as a resource argument is not a MetadataResource"
        }
    }

    override fun copy(): DomainResource {
        return get().copy()
    }

    override fun setDateElement(date: IPrimitiveType<Date?>?) {
        if (date != null && date !is DateTimeType) {
            throw UnprocessableEntityException("Date must be " + DateTimeType::class.java.name)
        }
        super.setDateElement(date)
    }

    override var effectivePeriod: ICompositeType?
        get() = super.effectivePeriod
        set(effectivePeriod) {
            if (effectivePeriod != null && effectivePeriod !is Period) {
                throw UnprocessableEntityException(
                    "EffectivePeriod must be a valid ${Period::class.java.name}"
                )
            }
            super.effectivePeriod = effectivePeriod
        }

    override fun <T : ICompositeType> getUseContext(): MutableList<T?>? {
        @Suppress("UNCHECKED_CAST")
        return this.metadataResource.useContext as MutableList<T?>?
    }

    override val dependencies: MutableList<IDependencyInfo?>
        get() {
            val references = mutableListOf<IDependencyInfo?>()
            val referenceSource = this.referenceSource
            addProfileReferences(references, referenceSource)
            return references
        }

    override fun <T> getRelatedArtifactsOfType(codeString: String?): MutableList<T?>? where
    T : ICompositeType,
    T : IBaseHasExtensions {
        val type =
            getRelatedArtifactType<RelatedArtifact.RelatedArtifactType>(codeString, fhirVersion()!!)
        @Suppress("UNCHECKED_CAST")
        return getRelatedArtifact<RelatedArtifact>()
            .filter { ra -> ra!!.type == type }
            .toMutableList() as MutableList<T?>
    }

    override var status: String?
        get() {
            return if (adaptedResource!!.status == null) null else adaptedResource!!.status.toCode()
        }
        set(statusCodeString) {
            val status: Enumerations.PublicationStatus?
            try {
                status = Enumerations.PublicationStatus.fromCode(statusCodeString)
            } catch (e: FHIRException) {
                throw UnprocessableEntityException("Invalid status code")
            }
            adaptedResource!!.status = status
        }

    @Throws(UnprocessableEntityException::class)
    override fun <T> setRelatedArtifact(relatedArtifacts: MutableList<T?>) where
    T : ICompositeType,
    T : IBaseHasExtensions {
        super.setRelatedArtifact(
            relatedArtifacts
                .map { ra ->
                    try {
                        return@map ra as RelatedArtifact?
                    } catch (e: ClassCastException) {
                        throw UnprocessableEntityException(
                            "All related artifacts must be of type " +
                                RelatedArtifact::class.java.name
                        )
                    }
                }
                .toMutableList()
        )
    }
}
