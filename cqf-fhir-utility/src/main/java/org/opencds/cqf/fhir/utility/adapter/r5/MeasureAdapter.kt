package org.opencds.cqf.fhir.utility.adapter.r5

import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.r5.model.*
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

    override fun copy(): Measure? {
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
        else { reference -> edrExtension.value = CanonicalType(reference) }
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
            if (edrExtension != null) {
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
             group[].population[].criteria.reference
             group[].stratifier[].criteria.reference
             group[].stratifier[].component[].criteria.reference
             supplementalData[].criteria.reference
             extension[cqfm-inputParameters][]
             extension[cqfm-expansionParameters][]
             extension[cqfm-effectiveDataRequirements]
             extension[cqfm-cqlOptions]
             extension[cqfm-component][].resource
             extension[crmi-effectiveDataRequirements]
            */

            // relatedArtifact[].resource
            getRelatedArtifactsOfType<RelatedArtifact>(IKnowledgeArtifactAdapter.DEPENDSON)!!
                .filter { obj -> obj!!.hasResource() }
                .map { ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource) }
                .forEach { e -> references.add(e) }

            // library[]
            for (library in this.measure.library) {
                val dependency =
                    DependencyInfo(
                        referenceSource,
                        library.value,
                        library.extension,
                        { theValue -> library.value = theValue },
                    )
                references.add(dependency)
            }

            for (group in this.measure.group) {
                // group[].population[].criteria.reference
                group.population
                    .filter { p -> p!!.criteria.hasReference() }
                    .forEach { p ->
                        references.add(
                            DependencyInfo(
                                referenceSource,
                                p!!.criteria.reference,
                                p.criteria.extension,
                                { reference -> p.criteria.reference = reference },
                            )
                        )
                    }
                for (stratifier in group.stratifier) {
                    // group[].stratifier[].criteria.reference
                    if (stratifier.criteria.hasReference()) {
                        references.add(
                            DependencyInfo(
                                referenceSource,
                                stratifier.criteria.reference,
                                stratifier.criteria.extension,
                                { reference -> stratifier.criteria.reference = reference },
                            )
                        )
                    }
                    // group[].stratifier[].component[].criteria.reference
                    stratifier.component
                        .filter { c -> c!!.criteria.hasReference() }
                        .forEach { component ->
                            references.add(
                                DependencyInfo(
                                    referenceSource,
                                    component!!.criteria.reference,
                                    component.criteria.extension,
                                    { reference -> component.criteria.reference = reference },
                                )
                            )
                        }
                }
            }

            // supplementalData[].criteria.reference
            this.measure.supplementalData
                .filter { s -> s!!.criteria.hasReference() }
                .forEach { supplement ->
                    references.add(
                        DependencyInfo(
                            referenceSource,
                            supplement!!.criteria.reference,
                            supplement.criteria.extension,
                            { reference -> supplement.criteria.reference = reference },
                        )
                    )
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
            get().getExtensionsByUrl(Constants.CQFM_COMPONENT).forEach { ext ->
                val ref = ext!!.value as RelatedArtifact
                if (ref.hasResource()) {
                    val dep =
                        DependencyInfo(
                            referenceSource,
                            ref.resource,
                            ref.extension,
                            { value -> ref.resource = value },
                        )
                    references.add(dep)
                }
            }

            return references
        }
}
