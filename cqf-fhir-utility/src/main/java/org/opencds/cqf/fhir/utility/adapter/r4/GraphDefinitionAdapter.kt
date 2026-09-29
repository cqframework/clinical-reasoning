package org.opencds.cqf.fhir.utility.adapter.r4

import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IBaseHasExtensions
import org.hl7.fhir.instance.model.api.ICompositeType
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.GraphDefinition
import org.hl7.fhir.r4.model.RelatedArtifact
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.RelatedArtifactUtil.getRelatedArtifactType
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdapter

class GraphDefinitionAdapter : ResourceAdapter, IGraphDefinitionAdapter {
    constructor(graphDefinition: IDomainResource) : super(graphDefinition) {
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

    override fun copy(): GraphDefinition? {
        return get().copy()
    }

    override val dependencies: MutableList<IDependencyInfo?>
        get() {
            val references = mutableListOf<IDependencyInfo?>()
            val referenceSource = this.referenceSource
            addProfileReferences(references, referenceSource)

            /*
             * extension[cpg-relatedArtifact].resource
             */
            //        extractRelatedArtifactReferences(referenceSource, references);
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
            this.graphDefinition.setStatus(Enumerations.PublicationStatus.fromCode(status))
        }

    //    @SuppressWarnings("unchecked")
    override fun <T> getRelatedArtifactsOfType(codeString: String?): MutableList<T?> where
    T : ICompositeType,
    T : IBaseHasExtensions {
        val type =
            getRelatedArtifactType<RelatedArtifact.RelatedArtifactType>(codeString, fhirVersion()!!)
        //        return getExtensionsByUrls(get(), Set.of(Constants.CPG_RELATED_ARTIFACT,
        // Constants.ARTIFACT_RELATED_ARTIFACT))
        //                .stream()
        //                .filter(ext -> {
        //                    if (ext.value instanceof RelatedArtifact ra) {
        //                        return ra.type == type;
        //                    }
        //                    return false;
        //                })
        //                .map(ext -> (T) ext.value)
        //                .toList();
        @Suppress("UNCHECKED_CAST")
        return getRelatedArtifact<RelatedArtifact>()
            .filter { ra -> (ra as RelatedArtifact).type == type }
            .map { e -> e as T }
            .toMutableList()
    }

    override val backBoneElements: MutableList<IBaseBackboneElement?>
        get() {
            return this.graphDefinition
                .getLink()
                .map { obj -> IBaseBackboneElement::class.java.cast(obj) }
                .toMutableList()
        }

    override val node: MutableList<IBaseBackboneElement?>
        get() {
            return mutableListOf()
        }
}
