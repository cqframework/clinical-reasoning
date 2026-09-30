package org.opencds.cqf.fhir.utility.adapter.dstu3

import org.hl7.fhir.dstu3.model.Enumerations
import org.hl7.fhir.dstu3.model.GraphDefinition
import org.hl7.fhir.dstu3.model.RelatedArtifact
import org.hl7.fhir.dstu3.model.Resource
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.RelatedArtifactUtil.getRelatedArtifactType
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdapter

class GraphDefinitionAdapter : ResourceAdapter, IGraphDefinitionAdapter {
    constructor(graphDefinition: IDomainResource) : super(graphDefinition as Resource) {
        require(graphDefinition is GraphDefinition) {
            "resource passed as graphDefinition argument is not a GraphDefinition resource"
        }
    }

    constructor(graphDefinition: GraphDefinition) : super(graphDefinition)

    protected val graphDefinition: GraphDefinition
        get() = resource as GraphDefinition

    override fun get(): GraphDefinition {
        return this.graphDefinition
    }

    override fun copy(): GraphDefinition {
        return get().copy()
    }

    override val dependencies: MutableList<IDependencyInfo?>
        get() {
            val references = mutableListOf<IDependencyInfo?>()
            val referenceSource = this.referenceSource
            addProfileReferences(references, referenceSource)

            /*
             *  extension[cpg-relatedArtifact].resource
             */
            getRelatedArtifactsOfType<RelatedArtifact>(Constants.RELATEDARTIFACT_TYPE_DEPENDSON)
                .filter { ra -> (ra as RelatedArtifact).hasResource() }
                .map { ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource) }
                .forEach { e -> references.add(e) }

            return references
        }

    override fun <T : ICompositeType> getUseContext(): MutableList<T?>? {
        @Suppress("UNCHECKED_CAST")
        return this.graphDefinition.useContext as MutableList<T?>?
    }

    override var status: String?
        get() {
            return this.graphDefinition.status.toCode()
        }
        set(status) {
            this.graphDefinition.status = Enumerations.PublicationStatus.fromCode(status)
        }

    override fun <T> getRelatedArtifactsOfType(codeString: String?): MutableList<T?> where
    T : ICompositeType,
    T : IBaseHasExtensions {
        val type =
            getRelatedArtifactType<RelatedArtifact.RelatedArtifactType>(codeString, fhirVersion()!!)
        @Suppress("UNCHECKED_CAST")
        return getExtensionsByUrls<IBaseExtension<*, *>>(
                get(),
                setOf(Constants.CPG_RELATED_ARTIFACT, Constants.ARTIFACT_RELATED_ARTIFACT),
            )
            .filter { ext ->
                if (ext!!.value is RelatedArtifact) {
                    return@filter (ext.value as RelatedArtifact).type == type
                }
                false
            }
            .map { ext -> ext!!.value as T? }
            .toMutableList()
    }

    override val backBoneElements: MutableList<IBaseBackboneElement?>
        get() {
            return mutableListOf()
        }

    override val node: MutableList<IBaseBackboneElement?>
        get() {
            return mutableListOf()
        }
}
