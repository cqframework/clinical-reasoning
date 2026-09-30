package org.opencds.cqf.fhir.utility.adapter.r5

import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import java.util.*
import org.hl7.fhir.instance.model.api.IBaseHasExtensions
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.ICompositeType
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.r5.model.*
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

    override fun copy(): Library {
        return get().copy()
    }

    override fun hasContent(): Boolean {
        return this.library.hasContent()
    }

    override fun <T : ICompositeType> getContent(): MutableList<T?>? {
        @Suppress("UNCHECKED_CAST")
        return this.library.content as MutableList<T?>?
    }

    override fun setContent(attachments: MutableList<out ICompositeType?>?) {
        val castAttachments = attachments!!.map { x -> x as Attachment? }.toMutableList()
        this.library.content = castAttachments
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
            getRelatedArtifactsOfType<RelatedArtifact>(IKnowledgeArtifactAdapter.DEPENDSON)!!
                .filter { obj -> obj!!.hasResource() }
                .map { ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource) }
                .forEach { e -> references.add(e) }

            this.library.dataRequirement.forEach { dr ->
                dr!!
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
                dr.codeFilter
                    .filter { obj -> obj!!.hasValueSet() }
                    .forEach { cf ->
                        references.add(
                            DependencyInfo(
                                referenceSource,
                                cf!!.valueSet,
                                cf.extension,
                                { value -> cf.valueSet = value },
                            )
                        )
                    }
            }
            return references
        }

    override val referencedLibraries: MutableMap<String?, String?>
        get() {
            val map = mutableMapOf<String?, String?>()
            map[name] = canonical
            return map
        }

    override fun retrieveReferencedLibraries(
        repository: IRepository?
    ): MutableMap<String?, ILibraryAdapter?> {
        val map = mutableMapOf<String?, ILibraryAdapter?>()
        map[name] = this
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
            this.library.type =
                CodeableConcept(Coding("http://hl7.org/fhir/ValueSet/library-type", type, ""))
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
        this.library.dataRequirement =
            dataRequirement!!.map { dr -> dr as DataRequirement? }.toMutableList()

        return this
    }

    override fun <T : ICompositeType> getUseContext(): MutableList<T?>? {
        @Suppress("UNCHECKED_CAST")
        return this.library.useContext as MutableList<T?>?
    }

    override val expansionParameters: Optional<IBaseParameters>
        get() {
            val expansionParameters =
                this.library.extension
                    .filter { ext -> ext!!.url == Constants.CQF_EXPANSION_PARAMETERS }
                    .firstOrNull()
                    ?.let { ext -> (ext.value as Reference).reference }
                    ?.let { ref: String? ->
                        if (this.library.hasContained()) {
                            return@let this.library.contained
                                .filter { containedResource -> ref == "#${containedResource!!.id}" }
                                .filterIsInstance<IBaseParameters>()
                                .firstOrNull()
                        }
                        null
                    }

            if (expansionParameters != null) {
                return Optional.of(expansionParameters)
            } else {
                val id = "exp-params"
                val newExpansionParameters = Parameters()
                newExpansionParameters.id = id
                this.library.addContained(newExpansionParameters)
                if (this.library.getExtensionByUrl(Constants.CQF_EXPANSION_PARAMETERS) == null) {
                    val expansionParamsExt = this.library.addExtension()
                    expansionParamsExt.url = Constants.CQF_EXPANSION_PARAMETERS
                    expansionParamsExt.value = Reference("#$id")
                }
                setExpansionParameters(newExpansionParameters)
                return Optional.of(newExpansionParameters)
            }
        }

    override fun setExpansionParameters(expansionParameters: IBaseParameters?) {
        if (
            expansionParameters != null &&
                (expansionParameters as Parameters).parameter.isNotEmpty()
        ) {
            val newParameters = mutableListOf<Parameters.ParametersParameterComponent?>()

            for (parameter in expansionParameters.parameter) {
                val param = Parameters.ParametersParameterComponent()
                param.name = parameter.name
                param.value = parameter.value
                newParameters.add(param)
            }

            val existingExpansionParameters = this.expansionParameters
            existingExpansionParameters.ifPresent { parameters ->
                (parameters as Parameters).parameter = newParameters
            }
        }
    }
}
