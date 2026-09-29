package org.opencds.cqf.fhir.utility.adapter.r5

import java.util.*
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.r5.model.*
import org.opencds.cqf.fhir.utility.Canonicals
import org.opencds.cqf.fhir.utility.adapter.*

class PlanDefinitionAdapter : KnowledgeArtifactAdapter, IPlanDefinitionAdapter {
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

    override fun copy(): PlanDefinition? {
        return get().copy()
    }

    override val dependencies: MutableList<IDependencyInfo?>
        get() {
            val references = mutableListOf<IDependencyInfo?>()
            val referenceSource = this.referenceSource
            addProfileReferences(references, referenceSource)

            /*
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
            getRelatedArtifactsOfType<RelatedArtifact>(IKnowledgeArtifactAdapter.DEPENDSON)!!
                .filter { obj -> obj!!.hasResource() }
                .map { ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource) }
                .forEach { e -> references.add(e) }

            // library[]
            val libraries = this.planDefinition.library
            for (ct in libraries) {
                val dependency =
                    DependencyInfo(
                        referenceSource,
                        ct.value,
                        ct.extension,
                        { theValue -> ct.setValue(theValue) },
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
                    references.add(
                        DependencyInfo(
                            referenceSource,
                            (ext.value as CanonicalType).value,
                            ext.extension,
                            { reference -> ext.setValue(CanonicalType(reference)) },
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
        action.trigger
            .flatMap { t -> t!!.data }
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
                                { theValue -> profile.setValue(theValue) },
                            )
                        )
                    }
                // trigger[].dataRequirement[].codeFilter[].valueSet
                eventData.codeFilter
                    .filter { obj -> obj!!.hasValueSet() }
                    .forEach { cf ->
                        references.add(
                            DependencyInfo(
                                referenceSource,
                                cf!!.valueSet,
                                cf.extension,
                                { value -> cf.setValueSet(value) },
                            )
                        )
                    }
            }
        // condition[].expression.reference
        action.condition
            .filter { obj -> obj!!.hasExpression() }
            .map { obj -> obj!!.expression }
            .filter { obj -> obj!!.hasReference() }
            .forEach { expression ->
                references.add(
                    DependencyInfo(
                        referenceSource,
                        expression!!.reference,
                        expression.extension,
                        { value -> expression.setReference(value) },
                    )
                )
            }
        // dynamicValue[].expression.reference
        action.dynamicValue
            .filter { obj -> obj!!.hasExpression() }
            .map { obj -> obj!!.expression }
            .filter { obj -> obj!!.hasReference() }
            .forEach { expression ->
                references.add(
                    DependencyInfo(
                        referenceSource,
                        expression!!.reference,
                        expression.extension,
                        { value -> expression.setReference(value) },
                    )
                )
            }
        (action.input.map { obj -> obj!!.requirement } +
                action.output.map { obj -> obj!!.requirement })
            .forEach { inputOrOutput ->
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
                                { theValue -> profile.setValue(theValue) },
                            )
                        )
                    }
                // input[].codeFilter[].valueSet
                // output[].codeFilter[].valueSet
                inputOrOutput.codeFilter
                    .filter { obj -> obj!!.hasValueSet() }
                    .forEach { cf ->
                        references.add(
                            DependencyInfo(
                                referenceSource,
                                cf!!.valueSet,
                                cf.extension,
                                { value -> cf.setValueSet(value) },
                            )
                        )
                    }
            }
        // action..definitionCanonical
        val definition = action.definitionCanonicalType
        if (definition != null && definition.hasValue()) {
            references.add(
                DependencyInfo(
                    referenceSource,
                    definition.value,
                    definition.extension,
                    { theValue -> definition.setValue(theValue) },
                )
            )
        }
        action.action.forEach { nestedAction ->
            getDependenciesOfAction(nestedAction!!, references, referenceSource)
        }
    }

    override val referencedLibraries: MutableMap<String?, String?>
        get() {
            val libraries =
                this.planDefinition.library
                    .associate { l -> Canonicals.getIdPart(l) to l!!.canonical }
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
            return get().library.map { obj -> obj!!.asStringValue() }.toMutableList()
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
