package org.opencds.cqf.fhir.utility.adapter.r5

import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.r5.model.*
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IGroupAdapter
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter

class GroupAdapter : KnowledgeArtifactAdapter, IGroupAdapter {
    constructor(group: IDomainResource) : super(group) {
        require(group is Group) { "resource passed as group argument is not a Group resource" }
    }

    constructor(group: Group) : super(group)

    protected val group: Group
        get() = resource as Group

    override fun get(): Group {
        return this.group
    }

    override fun copy(): Group? {
        return get().copy()
    }

    private var checkedEffectiveDataRequirements = false
    private var effectiveDataRequirements: Library? = null
    private var effectiveDataRequirementsAdapter: LibraryAdapter? = null

    private fun getEdrReferenceString(edrExtension: Extension): String? {
        return if (edrExtension.url.contains("cqfm")) (edrExtension.value as Reference).reference
        else (edrExtension.value as UriType).value
    }

    private fun getEdrReferenceConsumer(edrExtension: Extension): (String?) -> Unit {
        return if (edrExtension.url.contains("cqfm"))
            { reference -> edrExtension.setValue(Reference(reference)) }
        else { reference -> edrExtension.setValue(CanonicalType(reference)) }
    }

    private fun findEffectiveDataRequirements() {
        if (!checkedEffectiveDataRequirements) {
            val edrExtensions =
                this.group.extension
                    .filter { ext -> ext!!.url.endsWith("-effectiveDataRequirements") }
                    .filter { obj -> obj!!.hasValue() }
                    .toMutableList()

            val edrExtension = if (edrExtensions.size == 1) edrExtensions[0] else null
            // cqfm-effectiveDataRequirements is a Reference, crmi-effectiveDataRequirements is a
            // canonical
            val maybeEdrReference =
                edrExtension?.let { edrExtension -> this.getEdrReferenceString(edrExtension) }

            if (edrExtension != null) {
                val edrReference = maybeEdrReference
                for (c in this.group.contained) {
                    if (
                        c.hasId() &&
                            (edrReference == c.id || edrReference == "#${c.id}") &&
                            c is Library
                    ) {
                        effectiveDataRequirements = c
                        effectiveDataRequirementsAdapter =
                            LibraryAdapter(effectiveDataRequirements!!)
                    }
                }
            }
            checkedEffectiveDataRequirements = true
        }
    }

    override val dependencies: MutableList<IDependencyInfo?>
        get() {
            val references = mutableListOf<IDependencyInfo?>()
            val referenceSource = this.referenceSource
            addProfileReferences(references, referenceSource)

            // If an effectiveDataRequirements library is present, use it exclusively
            findEffectiveDataRequirements()
            if (effectiveDataRequirements != null) {
                references.addAll(effectiveDataRequirementsAdapter!!.dependencies)
                return references
            }

            // Otherwise, fall back to the relatedArtifact and library

            /*
             relatedArtifact[].resource
             extension[cqf-library]
             extension[characteristicExpression].reference
             extension[cqfm-inputParameters][]
             extension[cqfm-expansionParameters][]
             extension[cqfm-effectiveDataRequirements]
             extension[cqfm-cqlOptions]
             extension[crmi-effectiveDataRequirements]
            */

            // relatedArtifact[].resource
            getRelatedArtifactsOfType<RelatedArtifact>(IKnowledgeArtifactAdapter.DEPENDSON)!!
                .filter { obj -> obj!!.hasResource() }
                .map { ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource) }
                .forEach { e -> references.add(e) }

            for (expressionExtension in
                this.group.getExtensionsByUrl(
                    "http://hl7.org/fhir/StructureDefinition/characteristicExpression"
                )) {
                val expression = expressionExtension.value
                if (expression is Expression) {
                    if (expression.hasReference()) {
                        references.add(
                            DependencyInfo(
                                referenceSource,
                                expression.reference,
                                expression.extension,
                                { reference -> expression.setReference(reference) },
                            )
                        )
                    }
                }
            }

            // extension[cqfm-effectiveDataRequirements]
            // extension[crmi-effectiveDataRequirements]
            get()
                .extension
                .filter { e -> CANONICAL_EXTENSIONS.contains(e!!.url) }
                .forEach { referenceExt ->
                    references.add(
                        DependencyInfo(
                            referenceSource,
                            getEdrReferenceString(referenceExt!!),
                            referenceExt.extension,
                            getEdrReferenceConsumer(referenceExt),
                        )
                    )
                }

            // extension[cqfm-inputParameters][]
            // extension[cqfm-expansionParameters][]
            // extension[cqfm-cqlOptions]
            get()
                .extension
                .filter { e -> REFERENCE_EXTENSIONS.contains(e!!.url) }
                .forEach { referenceExt ->
                    references.add(
                        DependencyInfo(
                            referenceSource,
                            (referenceExt!!.value as Reference).reference,
                            referenceExt.extension,
                            { reference -> referenceExt.setValue(Reference(reference)) },
                        )
                    )
                }

            // extension[cqfm-component][].resource
            get().getExtensionsByUrl(Constants.CQFM_COMPONENT).forEach { ext ->
                val ref = ext!!.value as RelatedArtifact
                if (ref.hasResource()) {
                    val dep =
                        DependencyInfo(
                            referenceSource,
                            ref.resource,
                            ref.extension,
                            { value -> ref.setResource(value) },
                        )
                    references.add(dep)
                }
            }

            return references
        }
}
