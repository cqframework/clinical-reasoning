package org.opencds.cqf.fhir.utility.adapter.dstu3

import org.hl7.fhir.dstu3.model.*
import org.hl7.fhir.instance.model.api.IDomainResource
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter
import org.opencds.cqf.fhir.utility.adapter.IMeasureAdapter

class MeasureAdapter : KnowledgeArtifactAdapter, IMeasureAdapter {
    constructor(measure: IDomainResource) : super(measure) {
        require(measure is Measure) {
            "resource passed as measure argument is not a Measure resource"
        }
    }

    constructor(measure: Measure) : super(measure)

    protected val measure: Measure
        get() = resource as Measure

    override fun get(): Measure {
        return this.measure
    }

    override fun copy(): Measure {
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
            { reference -> edrExtension.value = Reference(reference) }
        else { reference -> edrExtension.value = UriType(reference) }
    }

    private fun findEffectiveDataRequirements() {
        if (!checkedEffectiveDataRequirements) {
            val edrExtensions =
                this.measure.extension
                    .filter { ext -> ext!!.url.endsWith("-effectiveDataRequirements") }
                    .filter { obj -> obj!!.hasValue() }
                    .toMutableList()

            val edrExtension = if (edrExtensions.size == 1) edrExtensions[0] else null
            // cqfm-effectiveDataRequirements is a Reference, crmi-effectiveDataRequirements is a
            // canonical
            val maybeEdrReference =
                edrExtension?.let { edrExtension -> this.getEdrReferenceString(edrExtension) }
            if (maybeEdrReference != null) {
                val edrReference = maybeEdrReference
                for (c in this.measure.contained) {
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
             library[]
             group[].population[].criteria.reference - no path on dstu3
             group[].stratifier[].criteria.reference - no path on dstu3
             group[].stratifier[].component[].criteria.reference - no path on dstu3
             supplementalData[].criteria.reference - no path on dstu3
             extension[cqfm-inputParameters][]
             extension[cqfm-expansionParameters][]
             extension[cqfm-effectiveDataRequirements]
             extension[cqfm-cqlOptions]
             extension[cqfm-component][].resource
             extension[crmi-effectiveDataRequirements]
            */

            // relatedArtifact[].resource
            getRelatedArtifactsOfType<RelatedArtifact>(IKnowledgeArtifactAdapter.DEPENDSON)
                .filter { obj -> obj!!.hasResource() }
                .map { ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource) }
                .forEach { e -> references.add(e) }

            // library[]
            for (library in this.measure.library) {
                val dependency =
                    DependencyInfo(
                        referenceSource,
                        library.reference,
                        library.extension,
                        { value -> library.reference = value },
                    )
                references.add(dependency)
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
                            { reference -> referenceExt.value = Reference(reference) },
                        )
                    )
                }

            // extension[cqfm-component][].resource
            get()
                .getExtensionsByUrl(Constants.CQFM_COMPONENT)
                .forEach({ ext ->
                    val ref = ext!!.value as RelatedArtifact
                    if (ref.hasResource() && ref.resource.hasReference()) {
                        val dep =
                            DependencyInfo(
                                referenceSource,
                                ref.resource.reference,
                                ref.extension,
                                { reference -> ref.resource.reference = reference },
                            )
                        references.add(dep)
                    }
                })

            return references
        }
}
