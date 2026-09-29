package org.opencds.cqf.fhir.utility.adapter.r4

import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import java.util.*
import org.hl7.fhir.instance.model.api.IBaseHasExtensions
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.ICompositeType
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.r4.model.*
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementAdapter
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter

class LibraryAdapter : KnowledgeArtifactAdapter, ILibraryAdapter {
    constructor(library: IDomainResource) : super(library) {
        require(library is Library) {
            "resource passed as library argument is not a Library resource"
        }
    }

    constructor(library: Library) : super(library)

    protected val library: Library
        get() = resource as Library

    override fun get(): Library {
        return resource as Library
    }

    override fun copy(): Library? {
        return get().copy()
    }

    override fun hasContent(): Boolean {
        return this.library.hasContent()
    }

    override fun <T : ICompositeType> getContent(): MutableList<T?>? {
        @Suppress("UNCHECKED_CAST")
        return this.library.content?.toMutableList() as MutableList<T?>?
    }

    override fun setContent(attachments: MutableList<out ICompositeType?>?) {
        val castAttachments = attachments!!.map { x -> x as Attachment? }.toMutableList()
        this.library.setContent(castAttachments)
    }

    override fun addContent(): Attachment? {
        return this.library.addContent()
    }

    override val dependencies: MutableList<IDependencyInfo?>
        get() {
            val references = mutableListOf<IDependencyInfo?>()
            val referenceSource = this.referenceSource
            addProfileReferences(references, referenceSource)

            // relatedArtifact[].resource
            val relatedArtifacts =
                getRelatedArtifactsOfType<RelatedArtifact>(IKnowledgeArtifactAdapter.DEPENDSON)!!
            for (i in relatedArtifacts.indices) {
                val ra = relatedArtifacts.get(i)!!
                if (ra.hasResource()) {
                    val dep = DependencyInfo.convertRelatedArtifact(ra, referenceSource)
                    dep.addFhirPath("relatedArtifact[" + i + "].resource")
                    references.add(dep)
                }
            }

            val dataRequirements = this.library.dataRequirement
            for (drIndex in dataRequirements.indices) {
                val dr = dataRequirements.get(drIndex)

                // dataRequirement[].profile[]
                val profiles = dr.profile
                for (profileIndex in profiles.indices) {
                    val profile = profiles.get(profileIndex)
                    if (profile.hasValue()) {
                        val dep: IDependencyInfo =
                            DependencyInfo(
                                referenceSource,
                                profile.value,
                                profile.extension,
                                { theValue -> profile.value = theValue },
                            )
                        dep.addFhirPath(
                            "dataRequirement[" + drIndex + "].profile[" + profileIndex + "]"
                        )
                        references.add(dep)
                    }
                }

                // dataRequirement[].codeFilter[].valueSet
                val codeFilters = dr.codeFilter
                for (cfIndex in codeFilters.indices) {
                    val cf = codeFilters.get(cfIndex)
                    if (cf.hasValueSet()) {
                        val dep: IDependencyInfo =
                            DependencyInfo(
                                referenceSource,
                                cf.valueSet,
                                cf.extension,
                                { value -> cf.valueSet = value },
                            )
                        dep.addFhirPath(
                            "dataRequirement[" + drIndex + "].codeFilter[" + cfIndex + "].valueSet"
                        )
                        references.add(dep)
                    }
                }
            }

            return references
        }

    override val referencedLibraries: MutableMap<String?, String?>
        get() {
            val map = HashMap<String?, String?>()
            map.put(name, canonical)
            return map
        }

    override fun retrieveReferencedLibraries(
        repository: IRepository?
    ): MutableMap<String?, ILibraryAdapter?> {
        val map = HashMap<String?, ILibraryAdapter?>()
        map.put(name, this)
        return map
    }

    override fun <T> getComponents(): MutableList<T?>? where
    T : ICompositeType,
    T : IBaseHasExtensions {
        return getRelatedArtifactsOfType("composed-of")
    }

    override val type: ICompositeType?
        get() {
            return this.library.type
        }

    override fun setType(type: String?): LibraryAdapter {
        if (LIBRARY_TYPES.contains(type)) {
            this.library.setType(
                CodeableConcept(Coding("http://hl7.org/fhir/ValueSet/library-type", type, ""))
            )
        } else {
            throw UnprocessableEntityException("Invalid type: {}", type)
        }
        return this
    }

    override fun <T : ICompositeType> getParameter(): MutableList<T?>? {
        @Suppress("UNCHECKED_CAST")
        return this.library.parameter as MutableList<T?>?
    }

    override fun hasDataRequirement(): Boolean {
        return this.library.hasDataRequirement()
    }

    override val dataRequirement: MutableList<IDataRequirementAdapter?>
        get() {
            return this.library.dataRequirement
                .map { compositeType -> DataRequirementAdapter(compositeType) }
                .toMutableList()
        }

    override fun addDataRequirement(dataRequirement: ICompositeType?): LibraryAdapter {
        this.library.addDataRequirement(dataRequirement as DataRequirement?)
        return this
    }

    override fun <T : ICompositeType> setDataRequirement(
        dataRequirement: MutableList<T?>?
    ): LibraryAdapter {
        this.library.setDataRequirement(
            dataRequirement!!.map { dr -> dr as DataRequirement? }.toMutableList()
        )
        return this
    }

    override val expansionParameters: Optional<IBaseParameters>
        get() {
            val expansionParameters =
                this.library.extension
                    .filter { ext -> ext!!.url == Constants.CQF_EXPANSION_PARAMETERS }
                    .firstOrNull()
                    ?.let { ext -> (ext.value as Reference).reference }
                    ?.let { ref ->
                        if (this.library.hasContained()) {
                            return@let this.library.contained
                                .filter { containedResource -> ref == "#${containedResource!!.id}" }
                                .filter { o -> IBaseParameters::class.java.isInstance(o) }
                                .map { obj -> IBaseParameters::class.java.cast(obj) }
                                .firstOrNull()
                        }
                        null
                    }

            if (expansionParameters != null) {
                return Optional.of(expansionParameters)
            } else {
                val id = "exp-params"
                val newExpansionParameters = Parameters()
                newExpansionParameters.setId(id)
                this.library.addContained(newExpansionParameters)
                if (this.library.getExtensionByUrl(Constants.CQF_EXPANSION_PARAMETERS) == null) {
                    val expansionParamsExt = this.library.addExtension()
                    expansionParamsExt.setUrl(Constants.CQF_EXPANSION_PARAMETERS)
                    expansionParamsExt.setValue(Reference("#$id"))
                }
                setExpansionParameters(newExpansionParameters)
                return Optional.of(newExpansionParameters)
            }
        }

    override fun setExpansionParameters(expansionParameters: IBaseParameters?) {
        if (
            expansionParameters != null && !(expansionParameters as Parameters).parameter.isEmpty()
        ) {
            val newParameters = ArrayList<Parameters.ParametersParameterComponent?>()

            for (parameter in expansionParameters.parameter) {
                val param = Parameters.ParametersParameterComponent()
                param.setName(parameter.name)
                param.setValue(parameter.value)
                newParameters.add(param)
            }

            val existingExpansionParameters = this.expansionParameters
            existingExpansionParameters.ifPresent { parameters ->
                (parameters as Parameters).setParameter(newParameters)
            }
        }
    }
}
