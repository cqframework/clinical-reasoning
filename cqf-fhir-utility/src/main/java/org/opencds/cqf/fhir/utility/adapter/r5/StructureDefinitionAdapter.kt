package org.opencds.cqf.fhir.utility.adapter.r5

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import java.util.*
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.instance.model.api.IBaseHasExtensions
import org.hl7.fhir.instance.model.api.ICompositeType
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.instance.model.api.IPrimitiveType
import org.hl7.fhir.r5.model.*
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter

class StructureDefinitionAdapter : ResourceAdapter, IStructureDefinitionAdapter {
    constructor(structureDefinition: IDomainResource) : super(structureDefinition) {
        require(structureDefinition is StructureDefinition) {
            "resource passed as planDefinition argument is not a StructureDefinition resource"
        }
    }

    constructor(structureDefinition: StructureDefinition) : super(structureDefinition)

    protected val structureDefinition: StructureDefinition
        get() = resource as StructureDefinition

    override fun <T : ICompositeType> getUseContext(): MutableList<T?>? {
        @Suppress("UNCHECKED_CAST")
        return get().useContext as MutableList<T?>?
    }

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
                .getExtensionsByUrl(Constants.CPG_ASSERTION_EXPRESSION)
                .filter { e -> e!!.value is Expression }
                .map { e -> e!!.value as Expression? }
                .filter { obj -> obj!!.hasReference() }
                .forEach { expression ->
                    references.add(
                        DependencyInfo(
                            referenceSource,
                            expression!!.reference,
                            expression.extension,
                            { value -> expression.reference = value },
                        )
                    )
                }
            get()
                .getExtensionsByUrl(Constants.CPG_FEATURE_EXPRESSION)
                .filter { e -> e!!.value is Expression }
                .map { e -> e!!.value as Expression? }
                .filter { obj -> obj!!.hasReference() }
                .forEach { expression ->
                    references.add(
                        DependencyInfo(
                            referenceSource,
                            expression!!.reference,
                            expression.extension,
                            { value -> expression.reference = value },
                        )
                    )
                }
            get()
                .getExtensionsByUrl(Constants.CPG_INFERENCE_EXPRESSION)
                .filter { e -> e!!.value is Expression }
                .map { e -> e!!.value as Expression? }
                .filter { obj -> obj!!.hasReference() }
                .forEach { expression ->
                    references.add(
                        DependencyInfo(
                            referenceSource,
                            expression!!.reference,
                            expression.extension,
                            { value -> expression.reference = value },
                        )
                    )
                }
            get().differential.element.forEach { element ->
                getDependenciesOfDifferential(element!!, references, referenceSource)
            }

            return references
        }

    private fun getDependenciesOfDifferential(
        element: ElementDefinition,
        references: MutableList<IDependencyInfo?>,
        referenceSource: String?,
    ) {
        element.type.forEach { type ->
            type!!.profile.forEach { profile ->
                references.add(
                    DependencyInfo(
                        referenceSource,
                        profile!!.valueAsString,
                        profile.extension,
                        { theValue -> profile.value = theValue },
                    )
                )
            }
            type.targetProfile.forEach { profile ->
                references.add(
                    DependencyInfo(
                        referenceSource,
                        profile!!.valueAsString,
                        profile.extension,
                        { theValue -> profile.value = theValue },
                    )
                )
            }
        }
        if (element.binding.hasValueSet()) {
            references.add(
                DependencyInfo(
                    referenceSource,
                    element.binding.valueSet,
                    element.binding.extension,
                    { reference -> element.binding.valueSet = reference },
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

    override var url: String?
        get() {
            return get().url
        }
        set(url) {
            get().setUrl(url)
        }

    override fun hasUrl(): Boolean {
        return get().hasUrl()
    }

    override var version: String?
        get() {
            return get().version
        }
        set(version) {
            get().setVersion(version)
        }

    override fun hasVersion(): Boolean {
        return get().hasVersion()
    }

    override var name: String?
        get() {
            return get().name
        }
        set(name) {
            get().setName(name)
        }

    override var approvalDate: Date?
        get() {
            return null
        }
        set(approvalDate) {
            // do nothing
        }

    override var date: Date?
        get() {
            return get().date
        }
        set(approvalDate) {
            get().setDate(approvalDate)
        }

    override fun setDateElement(date: IPrimitiveType<Date?>?) {
        if (date != null && date !is DateTimeType) {
            throw UnprocessableEntityException("Date must be " + DateTimeType::class.java.name)
        }
        get().setDateElement(date)
    }

    override var effectivePeriod: ICompositeType?
        get() {
            return null
        }
        set(effectivePeriod) {
            // do nothing
        }

    override val purpose: String?
        get() {
            return get().purpose
        }

    override fun <T> hasRelatedArtifact(): Boolean where
    T : ICompositeType,
    T : IBaseHasExtensions {
        return false
    }

    override fun <T> getRelatedArtifactsOfType(codeString: String?): MutableList<T?> where
    T : ICompositeType,
    T : IBaseHasExtensions {
        @Suppress("UNCHECKED_CAST")
        return mutableListOf<RelatedArtifact>() as MutableList<T?>
    }

    @Throws(UnprocessableEntityException::class)
    override fun <T> setRelatedArtifact(relatedArtifacts: MutableList<T?>) where
    T : ICompositeType,
    T : IBaseHasExtensions {
        // do nothing
    }

    override var status: String?
        get() {
            return if (get().status == null) null else get().status.toCode()
        }
        set(statusCodeString) {
            val status: Enumerations.PublicationStatus?
            try {
                status = Enumerations.PublicationStatus.fromCode(statusCodeString)
            } catch (e: FHIRException) {
                throw UnprocessableEntityException("Invalid status code")
            }
            get().setStatus(status)
        }

    override val experimental: Boolean
        get() {
            return get().experimental
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
