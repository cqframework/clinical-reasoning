package org.opencds.cqf.fhir.utility.adapter.dstu3

import org.hl7.fhir.dstu3.model.ElementDefinition
import org.hl7.fhir.dstu3.model.StructureDefinition
import org.hl7.fhir.dstu3.model.UriType
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.instance.model.api.IPrimitiveType
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter

class StructureDefinitionAdapter : KnowledgeArtifactAdapter, IStructureDefinitionAdapter {
    constructor(structureDefinition: IDomainResource) : super(structureDefinition) {
        require(structureDefinition is StructureDefinition) {
            "resource passed as planDefinition argument is not a StructureDefinition resource"
        }
    }

    constructor(structureDefinition: StructureDefinition) : super(structureDefinition)

    protected val structureDefinition: StructureDefinition
        get() = resource as StructureDefinition

    override val dependencies: MutableList<IDependencyInfo?>
        get() {
            val references = mutableListOf<IDependencyInfo?>()
            val referenceSource = this.referenceSource
            addProfileReferences(references, referenceSource)

            /*
               extension[].url
               modifierExtension[].url
               baseDefinition
               differential.element[].type.code
               differential.element[].type.profile[]
               differential.element[].type.targetProfile[]
               differential.element[].binding.valueSet
               differential.element[].extension[].url
               differential.element[].modifierExtension[].url
               extension[cpg-inferenceExpression].reference
               extension[cpg-assertionExpression].reference
               extension[cpg-featureExpression].reference
            */
            if (get().hasBaseDefinition()) {
                references.add(
                    DependencyInfo(
                        referenceSource,
                        get().baseDefinition,
                        get().baseDefinitionElement.extension,
                        { reference -> get().baseDefinition = reference },
                    )
                )
            }

            get()
                .differential
                .element
                .forEach({ element ->
                    getDependenciesOfDifferential(element!!, references, referenceSource)
                })

            return references
        }

    private fun getDependenciesOfDifferential(
        element: ElementDefinition,
        references: MutableList<IDependencyInfo?>,
        referenceSource: String?,
    ) {
        element.type.forEach({ type ->
            if (type!!.hasProfile()) {
                references.add(
                    DependencyInfo(
                        referenceSource,
                        type.profile,
                        type.getProfileElement().extension,
                        { value -> type.setProfile(value) },
                    )
                )
            }
            if (type.hasTargetProfile()) {
                references.add(
                    DependencyInfo(
                        referenceSource,
                        type.targetProfile,
                        type.getTargetProfileElement().extension,
                        { value -> type.setTargetProfile(value) },
                    )
                )
            }
        })
        if (element.binding.hasValueSet()) {
            references.add(
                DependencyInfo(
                    referenceSource,
                    element.binding.valueSet.primitiveValue(),
                    element.binding.extension,
                    { reference -> element.binding.setValueSet(UriType(reference)) },
                )
            )
        }
    }

    override fun get(): StructureDefinition {
        return this.structureDefinition
    }

    override fun copy(): StructureDefinition? {
        return get().copy()
    }

    override val derivation: String?
        get() {
            return if (get().hasDerivation()) get().derivation.toCode() else null
        }

    override val baseDefinition: IPrimitiveType<String?>?
        get() {
            return get().baseDefinitionElement
        }

    override fun hasSnapshot(): Boolean {
        return get().hasSnapshot()
    }

    override val snapshotElements: MutableList<IElementDefinitionAdapter?>
        get() {
            return get()
                .snapshot
                .element
                .filter { obj -> obj!!.hasPath() }
                .filter { e -> e!!.path.split(".").size > 1 }
                .map { element -> adapterFactory.createElementDefinition(element) }
                .toMutableList()
        }

    override val allSnapshotElements: MutableList<IElementDefinitionAdapter?>
        get() {
            return get()
                .snapshot
                .element
                .filter { obj -> obj!!.hasPath() }
                .map { element -> adapterFactory.createElementDefinition(element) }
                .toMutableList()
        }

    override val allDifferentialElements: MutableList<IElementDefinitionAdapter?>
        get() {
            return get()
                .differential
                .element
                .filter { obj -> obj!!.hasPath() }
                .map { element -> adapterFactory.createElementDefinition(element) }
                .toMutableList()
        }

    override val differentialElements: MutableList<IElementDefinitionAdapter?>
        get() {
            return get()
                .differential
                .element
                .filter { obj -> obj!!.hasPath() }
                .filter { e -> e!!.path.split(".").size > 1 }
                .map { element -> adapterFactory.createElementDefinition(element) }
                .toMutableList()
        }
}
