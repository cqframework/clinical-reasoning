package org.opencds.cqf.fhir.utility.adapter.dstu3

import java.util.*
import org.hl7.fhir.dstu3.model.*
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IDomainResource
import org.opencds.cqf.fhir.utility.Canonicals.getIdPart
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionAdapter

internal class PlanDefinitionAdapter : KnowledgeArtifactAdapter, IPlanDefinitionAdapter {
    constructor(planDefinition: IDomainResource) : super(planDefinition) {
        require(planDefinition is PlanDefinition) {
            "resource passed as planDefinition argument is not a PlanDefinition resource"
        }
    }

    constructor(planDefinition: PlanDefinition) : super(planDefinition)

    protected val planDefinition: PlanDefinition
        get() = resource as PlanDefinition

    override fun get(): PlanDefinition {
        return this.planDefinition
    }

    override fun copy(): PlanDefinition {
        return get().copy()
    }

    override val dependencies: MutableList<IDependencyInfo?>
        get() {
            val references = mutableListOf<IDependencyInfo?>()
            val referenceSource = this.referenceSource
            addProfileReferences(references, referenceSource)

            /*
             https://build.fhir.org/ig/HL7/crmi-ig/distribution.html#package-and-data-requirements
             relatedArtifact[].resource
             library[]
             action[]..trigger[].dataRequirement[].profile[]
             action[]..trigger[].dataRequirement[].codeFilter[].valueSet
             action[]..condition[].expression.reference
             action[]..input[].profile[]
             action[]..input[].codeFilter[].valueSet
             action[]..output[].profile[]
             action[]..output[].codeFilter[].valueSet
             action[]..definitionCanonical
             action[]..dynamicValue[].expression.reference
             extension[cpg-partOf]
            */

            // relatedArtifact[].resource
            getRelatedArtifactsOfType<RelatedArtifact>(IKnowledgeArtifactAdapter.DEPENDSON)
                .filter { obj -> obj!!.hasResource() }
                .map { ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource) }
                .forEach { e -> references.add(e) }

            // library[]
            val libraries = this.planDefinition.library
            for (ref in libraries) {
                val dependency =
                    DependencyInfo(
                        referenceSource,
                        ref.reference,
                        ref.extension,
                        { value -> ref.reference = value },
                    )
                references.add(dependency)
            }
            // action[]
            this.planDefinition.action.forEach { action ->
                getDependenciesOfAction(action!!, references, referenceSource)
            }
            this.planDefinition.extension
                .filter { ext -> ext!!.url.contains("cpg-partOf") }
                .filter { obj -> obj!!.hasValue() }
                .firstOrNull()
                ?.let { ext ->
                    val reference = ext.value as UriType
                    references.add(
                        DependencyInfo(
                            referenceSource,
                            reference.value,
                            ext.extension,
                            { theValue -> reference.value = theValue },
                        )
                    )
                }
            return references
        }

    private fun getDependenciesOfAction(
        action: PlanDefinition.PlanDefinitionActionComponent,
        references: MutableList<IDependencyInfo?>,
        referenceSource: String?,
    ) {
        action.triggerDefinition
            .map { obj -> obj!!.getEventData() }
            .forEach { eventData ->
                // trigger[].dataRequirement[].profile[]
                eventData!!
                    .profile
                    .filter { obj -> obj!!.hasValue() }
                    .forEach { profile ->
                        references.add(
                            DependencyInfo(
                                referenceSource,
                                profile!!.value,
                                profile.extension,
                                { theValue -> profile.value = theValue },
                            )
                        )
                    }
                // trigger[].dataRequirement[].codeFilter[].valueSet
                eventData.codeFilter
                    .filter { obj -> obj!!.hasValueSet() }
                    .forEach { cf -> references.add(dependencyFromDataRequirementCodeFilter(cf!!)) }
            }
        (action.input + action.output).forEach { inputOrOutput ->
            // ..input[].profile[]
            // ..output[].profile[]
            inputOrOutput!!
                .profile
                .filter { obj -> obj!!.hasValue() }
                .forEach { profile ->
                    references.add(
                        DependencyInfo(
                            referenceSource,
                            profile!!.value,
                            profile.extension,
                            { theValue -> profile.value = theValue },
                        )
                    )
                }
            // input[].codeFilter[].valueSet
            // output[].codeFilter[].valueSet
            inputOrOutput.codeFilter
                .filter { obj -> obj!!.hasValueSet() }
                .forEach { cf -> references.add(dependencyFromDataRequirementCodeFilter(cf!!)) }
        }
        // action..definition
        val definition = action.definition
        if (definition != null && definition.hasReference()) {
            references.add(
                DependencyInfo(
                    referenceSource,
                    definition.reference,
                    definition.extension,
                    { value -> definition.reference = value },
                )
            )
        }
        action.action.forEach { nestedAction ->
            getDependenciesOfAction(nestedAction!!, references, referenceSource)
        }
    }

    private fun dependencyFromDataRequirementCodeFilter(
        cf: DataRequirement.DataRequirementCodeFilterComponent
    ): DependencyInfo? {
        val vs = cf.valueSet
        if (vs is StringType) {
            return DependencyInfo(
                this.planDefinition.url,
                vs.value,
                vs.extension,
                { theValue -> vs.value = theValue },
            )
        } else if (vs is Reference) {
            return DependencyInfo(
                this.planDefinition.url,
                vs.reference,
                vs.extension,
                { value -> vs.reference = value },
            )
        }
        return null
    }

    override val referencedLibraries: MutableMap<String?, String?>
        get() {
            val libraries =
                this.planDefinition.library
                    .associate { l: Reference? -> getIdPart(l!!.reference) to l.reference }
                    .toMutableMap()
            libraries.putAll(resolveCqfLibraries())
            return libraries
        }

    override val description: String?
        get() {
            return get().description
        }

    override fun hasLibrary(): Boolean {
        return get().hasLibrary()
    }

    override val library: MutableList<String?>
        get() {
            return get().library.map { obj -> obj!!.reference }.toMutableList()
        }

    override fun hasGoal(): Boolean {
        return get().hasGoal()
    }

    override val goal: MutableList<IBaseBackboneElement?>
        get() {
            return get()
                .goal
                .map { obj -> IBaseBackboneElement::class.java.cast(obj) }
                .toMutableList()
        }

    override fun hasAction(): Boolean {
        return get().hasAction()
    }

    override val action: MutableList<IPlanDefinitionActionAdapter?>
        get() {
            return get()
                .action
                .map { action -> PlanDefinitionActionAdapter(action) }
                .toMutableList()
        }
}
