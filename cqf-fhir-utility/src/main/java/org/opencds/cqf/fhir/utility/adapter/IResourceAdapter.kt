package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.Ids
import org.opencds.cqf.fhir.utility.Ids.ensureIdType

interface IResourceAdapter : IAdapter<IBaseResource> {
    override fun get(): IBaseResource?

    val id: String?
        /**
         * Returns the id of the resource including the type. i.e. Patient/id
         *
         * @return String
         */
        get() =
            if (this.idElement == null) null
            else String.format("%s/%s", get()!!.fhirType(), this.idElement!!.idPart)

    val idPart: String?
        /**
         * Returns just the id part of the id of the resource
         *
         * @return String
         */
        get() = if (this.idElement == null) null else this.idElement!!.idPart

    val idElement: IIdType?
        get() = resolvePath(get(), "id", IIdType::class.java)

    fun setId(id: String): IAdapter<*>? {
        setId(Ids.newId(fhirContext()!!, ensureIdType(id, get()!!.fhirType())) as IIdType)
        return this
    }

    fun setId(id: IIdType?)

    @Throws(FHIRException::class) fun setProperty(name: String, value: IBase?): IBase?

    @Throws(FHIRException::class) fun addChild(name: String): IBase?

    @Throws(FHIRException::class) fun getSingleProperty(name: String): IBase?

    @Throws(FHIRException::class) fun getProperty(name: String): Array<out IBase?>?

    @Throws(FHIRException::class)
    fun getProperty(name: String, checkValid: Boolean): Array<out IBase?>?

    @Throws(FHIRException::class) fun makeProperty(name: String): IBase?

    @Throws(FHIRException::class) fun getTypesForProperty(name: String): Array<String?>?

    fun copy(): IBaseResource?

    fun copyValues(destination: IBaseResource?)

    fun equalsDeep(other: IBase?): Boolean

    fun equalsShallow(other: IBase?): Boolean

    fun <R : IBaseResource> getContained(): MutableList<R?> {
        return getContained(get())
    }

    fun hasContained(): Boolean {
        return hasContained(get())
    }

    fun <R : IBaseResource> getContained(base: IBaseResource?): MutableList<R?> {
        @Suppress("UNCHECKED_CAST")
        return resolvePathList(base, "contained", IBaseResource::class.java)
            .map { r -> r as R? }
            .toMutableList()
    }

    fun hasContained(base: IBaseResource?): Boolean {
        return getContained<IBaseResource>(base).isNotEmpty()
    }

    fun addContained(base: IBaseResource?) {
        val res = resolvePathList(get(), "contained", IBaseResource::class.java)
        res.add(base)
        setValue(get(), "contained", res)
    }

    fun hasProperty(propertyName: String?): Boolean {
        // should consider caching this?
        val propNames =
            fhirContext()!!
                .getResourceDefinition(get())
                .children
                .map { obj -> obj!!.elementName }
                .toSet()
        return propNames.contains(propertyName)
    }

    fun <T> getRelatedArtifact(): MutableList<T?> where T : ICompositeType, T : IBaseHasExtensions {
        val artifacts = mutableListOf<T?>()
        if (hasProperty("relatedArtifact")) {
            @Suppress("UNCHECKED_CAST")
            val relatedArtifacts =
                resolvePathList(get(), "relatedArtifact").map { r -> r as T? }.toList()
            artifacts.addAll(relatedArtifacts)
        } else {
            // for KnowledgeResources that do not have relatedArtifact properties,
            // we'll filter the extensions for these 2 RelatedArtifact
            @Suppress("UNCHECKED_CAST")
            val extensionArtifacts =
                getExtensionsByUrls<IBaseExtension<*, *>>(
                        get(),
                        mutableSetOf(
                            Constants.CPG_RELATED_ARTIFACT,
                            Constants.ARTIFACT_RELATED_ARTIFACT,
                        ),
                    )
                    .filter { ext ->
                        ext!!.value != null && ext.value.fhirType() == "RelatedArtifact"
                    }
                    .map { ext -> ext!!.value as T? }
                    .toList()
            artifacts.addAll(extensionArtifacts)
        }
        return artifacts
    }
}
