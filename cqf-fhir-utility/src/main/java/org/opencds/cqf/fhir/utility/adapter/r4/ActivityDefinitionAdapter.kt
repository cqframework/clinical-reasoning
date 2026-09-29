package org.opencds.cqf.fhir.utility.adapter.r4

import java.util.*
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.r4.model.ActivityDefinition
import org.hl7.fhir.r4.model.RelatedArtifact
import org.opencds.cqf.fhir.utility.Canonicals
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IActivityDefinitionAdapter
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter

class ActivityDefinitionAdapter : KnowledgeArtifactAdapter, IActivityDefinitionAdapter {
    constructor(activityDefinition: IDomainResource) : super(activityDefinition) {
        require(activityDefinition is ActivityDefinition) {
            "resource passed as activityDefinition argument is not an ActivityDefinition resource"
        }
    }

    constructor(activityDefinition: ActivityDefinition) : super(activityDefinition)

    protected val activityDefinition: ActivityDefinition
        get() = resource as ActivityDefinition

    override fun get(): ActivityDefinition {
        return this.activityDefinition
    }

    override fun copy(): ActivityDefinition? {
        return get()!!.copy()
    }

    override val dependencies: MutableList<IDependencyInfo?>
        get() {
            val references = mutableListOf<IDependencyInfo?>()
            val referenceSource = this.referenceSource
            addProfileReferences(references, referenceSource)

            /*
            relatedArtifact[].resource
            library[]

            */

            // relatedArtifact[].resource
            getRelatedArtifactsOfType<RelatedArtifact>(IKnowledgeArtifactAdapter.DEPENDSON)!!
                .filter { obj -> obj!!.hasResource() }
                .map { ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource) }
                .forEach { e -> references.add(e) }

            // library[]
            if (hasLibrary()) {
                for (ct in this.activityDefinition.library) {
                    references.add(
                        DependencyInfo(
                            referenceSource,
                            ct.value,
                            ct.extension,
                            { theValue -> ct.setValue(theValue) },
                        )
                    )
                }
            }

            return references
        }

    override val referencedLibraries: MutableMap<String?, String?>
        get() {
            val libraries =
                this.activityDefinition.library
                    .associate { l -> Canonicals.getIdPart(l) to l.valueAsString }
                    .toMutableMap()
            libraries.putAll(resolveCqfLibraries())
            return libraries
        }

    override val description: String?
        get() {
            return get()!!.description
        }

    override fun hasLibrary(): Boolean {
        return get()!!.hasLibrary()
    }

    override val library: MutableList<String?>
        get() {
            return get()!!.library.map { obj -> obj!!.asStringValue() }.toMutableList()
        }
}
