package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.context.RuntimeSearchParam
import ca.uhn.fhir.context.RuntimeSearchParam.RuntimeSearchParamStatusEnum
import ca.uhn.fhir.rest.api.RestSearchParameterTypeEnum
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType

object BundleHelper {
    private val UNSUPPORTED_VERSION_OF_FHIR = { v: String -> "Unsupported version of FHIR: $v" }

    /**
     * Returns the resource of the first entry in a Bundle
     *
     * @param bundle IBaseBundle type
     * @return IBaseBackboneElement
     */
    @JvmStatic
    fun getEntryFirstRep(bundle: IBaseBundle): IBaseBackboneElement? {
        return when (val fhirVersion = bundle.structureFhirVersionEnum) {
            FhirVersionEnum.DSTU3 -> (bundle as org.hl7.fhir.dstu3.model.Bundle).entryFirstRep
            FhirVersionEnum.R4 -> (bundle as org.hl7.fhir.r4.model.Bundle).entryFirstRep
            FhirVersionEnum.R5 -> (bundle as org.hl7.fhir.r5.model.Bundle).entryFirstRep
            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Returns the resource of the first entry in a Bundle
     *
     * @param bundle IBaseBundle type
     * @return IBaseResource
     */
    @JvmStatic
    fun getEntryResourceFirstRep(bundle: IBaseBundle): IBaseResource? {
        return when (val fhirVersion = bundle.structureFhirVersionEnum) {
            FhirVersionEnum.DSTU3 -> {
                val dstu3Entry = (bundle as org.hl7.fhir.dstu3.model.Bundle).entryFirstRep
                if (dstu3Entry != null && dstu3Entry.hasResource()) dstu3Entry.resource else null
            }

            FhirVersionEnum.R4 -> {
                val r4Entry = (bundle as org.hl7.fhir.r4.model.Bundle).entryFirstRep
                if (r4Entry != null && r4Entry.hasResource()) r4Entry.resource else null
            }

            FhirVersionEnum.R5 -> {
                val r5Entry = (bundle as org.hl7.fhir.r5.model.Bundle).entryFirstRep
                if (r5Entry != null && r5Entry.hasResource()) r5Entry.resource else null
            }

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Returns a list of resources from the Bundle entries
     *
     * @param bundle IBaseBundle type
     * @return List of IBaseResource
     */
    @JvmStatic
    fun getEntryResources(bundle: IBaseBundle): MutableList<IBaseResource> {
        return when (val fhirVersion = bundle.structureFhirVersionEnum) {
            FhirVersionEnum.DSTU3 -> {
                (bundle as org.hl7.fhir.dstu3.model.Bundle)
                    .entry
                    .filter { it.hasResource() }
                    .map { it.resource }
                    .toMutableList()
            }

            FhirVersionEnum.R4 -> {
                (bundle as org.hl7.fhir.r4.model.Bundle)
                    .entry
                    .filter { it.hasResource() }
                    .map { it.resource }
                    .toMutableList()
            }

            FhirVersionEnum.R5 -> {
                (bundle as org.hl7.fhir.r5.model.Bundle)
                    .entry
                    .filter { it.hasResource() }
                    .map { it.resource }
                    .toMutableList()
            }

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Returns the Resource for a given entry
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return IBaseResource
     */
    @JvmStatic
    fun getEntryResource(
        fhirVersion: FhirVersionEnum,
        entry: IBaseBackboneElement,
    ): IBaseResource? {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                (entry as org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent).resource
            FhirVersionEnum.R4 ->
                (entry as org.hl7.fhir.r4.model.Bundle.BundleEntryComponent).resource
            FhirVersionEnum.R5 ->
                (entry as org.hl7.fhir.r5.model.Bundle.BundleEntryComponent).resource
            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Checks if an entry has a request type of PUT
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return boolean
     */
    @JvmStatic
    fun isEntryRequestPut(fhirVersion: FhirVersionEnum, entry: IBaseBackboneElement): Boolean {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                (entry as org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent).request.method ==
                    org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.PUT

            FhirVersionEnum.R4 ->
                (entry as org.hl7.fhir.r4.model.Bundle.BundleEntryComponent).request.method ==
                    org.hl7.fhir.r4.model.Bundle.HTTPVerb.PUT

            FhirVersionEnum.R5 ->
                (entry as org.hl7.fhir.r5.model.Bundle.BundleEntryComponent).request.method ==
                    org.hl7.fhir.r5.model.Bundle.HTTPVerb.PUT

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Checks if an entry has a request type of POST
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return boolean
     */
    @JvmStatic
    fun isEntryRequestPost(fhirVersion: FhirVersionEnum, entry: IBaseBackboneElement): Boolean {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                (entry as org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent).request.method ==
                    org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.POST

            FhirVersionEnum.R4 ->
                (entry as org.hl7.fhir.r4.model.Bundle.BundleEntryComponent).request.method ==
                    org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST

            FhirVersionEnum.R5 ->
                (entry as org.hl7.fhir.r5.model.Bundle.BundleEntryComponent).request.method ==
                    org.hl7.fhir.r5.model.Bundle.HTTPVerb.POST

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Checks if an entry has a request type of DELETE
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return boolean
     */
    @JvmStatic
    fun isEntryRequestDelete(fhirVersion: FhirVersionEnum, entry: IBaseBackboneElement): Boolean {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                (entry as org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent).request.method ==
                    org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.DELETE

            FhirVersionEnum.R4 ->
                (entry as org.hl7.fhir.r4.model.Bundle.BundleEntryComponent).request.method ==
                    org.hl7.fhir.r4.model.Bundle.HTTPVerb.DELETE

            FhirVersionEnum.R5 ->
                (entry as org.hl7.fhir.r5.model.Bundle.BundleEntryComponent).request.method ==
                    org.hl7.fhir.r5.model.Bundle.HTTPVerb.DELETE

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Checks if an entry has a request type of GET
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return boolean
     */
    @JvmStatic
    fun isEntryRequestGet(fhirVersion: FhirVersionEnum, entry: IBaseBackboneElement): Boolean {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                (entry as org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent).request.method ==
                    org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.GET

            FhirVersionEnum.R4 ->
                (entry as org.hl7.fhir.r4.model.Bundle.BundleEntryComponent).request.method ==
                    org.hl7.fhir.r4.model.Bundle.HTTPVerb.GET

            FhirVersionEnum.R5 ->
                (entry as org.hl7.fhir.r5.model.Bundle.BundleEntryComponent).request.method ==
                    org.hl7.fhir.r5.model.Bundle.HTTPVerb.GET

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Returns the list of entries from the Bundle
     *
     * @param bundle IBaseBundle type
     * @return IBaseBackboneElement
     */
    @JvmStatic
    fun <T : IBaseBackboneElement> getEntry(bundle: IBaseBundle): MutableList<T?> {
        @Suppress("UNCHECKED_CAST")
        return when (val fhirVersion = bundle.structureFhirVersionEnum) {
            FhirVersionEnum.DSTU3 -> (bundle as org.hl7.fhir.dstu3.model.Bundle).entry
            FhirVersionEnum.R4 -> (bundle as org.hl7.fhir.r4.model.Bundle).entry
            FhirVersionEnum.R5 -> (bundle as org.hl7.fhir.r5.model.Bundle).entry
            else ->
                throw java.lang.IllegalArgumentException(
                    BundleHelper.UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
            as MutableList<T?>
    }

    /**
     * Gets request id if present
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return Optional IIdType
     */
    @JvmStatic
    fun getEntryRequestId(fhirVersion: FhirVersionEnum, entry: IBaseBackboneElement): IIdType? {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                (entry as org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent)
                    .request
                    .url
                    .let { Canonicals.getIdPart(it) }
                    .let { org.hl7.fhir.dstu3.model.IdType(it) }

            FhirVersionEnum.R4 ->
                (entry as org.hl7.fhir.r4.model.Bundle.BundleEntryComponent)
                    .request
                    .url
                    .let { Canonicals.getIdPart(it) }
                    .let { org.hl7.fhir.r4.model.IdType(it) }

            FhirVersionEnum.R5 ->
                (entry as org.hl7.fhir.r5.model.Bundle.BundleEntryComponent)
                    .request
                    .url
                    .let { Canonicals.getIdPart(it) }
                    .let { org.hl7.fhir.r5.model.IdType(it) }

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    @JvmStatic
    fun getBundleEntryResourceIds(
        fhirContext: FhirContext,
        bundle: IBaseBundle,
    ): MutableList<IIdType?> {
        return getBundleEntryResourceIds(fhirContext.version.version, bundle)
    }

    @JvmStatic
    fun getBundleEntryResourceIds(
        fhirVersion: FhirVersionEnum,
        bundle: IBaseBundle,
    ): MutableList<IIdType?> {
        val entry = getEntry<IBaseBackboneElement>(bundle)

        return entry
            .map { BundleHelper.getEntryResource(fhirVersion, it!!)!!.idElement }
            .toMutableList()
    }

    /**
     * Gets request url if present
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement type
     * @return url of request
     */
    @JvmStatic
    fun getEntryRequestUrl(fhirVersion: FhirVersionEnum, entry: IBaseBackboneElement): String? {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                (entry as org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent).request.url
            FhirVersionEnum.R4 ->
                (entry as org.hl7.fhir.r4.model.Bundle.BundleEntryComponent).request.url

            FhirVersionEnum.R5 ->
                (entry as org.hl7.fhir.r5.model.Bundle.BundleEntryComponent).request.url

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Sets the list of entries of the Bundle
     *
     * @param bundle IBaseBundle type
     * @param entries List of IBaseBackboneElement type
     */
    @JvmStatic
    fun setEntry(bundle: IBaseBundle, entries: MutableList<out IBaseBackboneElement>) {
        @Suppress("UNCHECKED_CAST")
        when (val fhirVersion = bundle.structureFhirVersionEnum) {
            FhirVersionEnum.DSTU3 ->
                (bundle as org.hl7.fhir.dstu3.model.Bundle).setEntry(
                    entries as MutableList<org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent>
                )

            FhirVersionEnum.R4 ->
                (bundle as org.hl7.fhir.r4.model.Bundle).setEntry(
                    entries as MutableList<org.hl7.fhir.r4.model.Bundle.BundleEntryComponent>
                )

            FhirVersionEnum.R5 ->
                (bundle as org.hl7.fhir.r5.model.Bundle).setEntry(
                    entries as MutableList<org.hl7.fhir.r5.model.Bundle.BundleEntryComponent>
                )

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Adds the entry to the Bundle and returns the Bundle
     *
     * @param bundle IBaseBundle type
     * @param entry IBaseBackboneElement type
     * @return IBaseBundle
     */
    @JvmStatic
    fun addEntry(bundle: IBaseBundle, entry: IBaseBackboneElement?): IBaseBundle {
        when (val fhirVersion = bundle.structureFhirVersionEnum) {
            FhirVersionEnum.DSTU3 ->
                (bundle as org.hl7.fhir.dstu3.model.Bundle).addEntry(
                    entry as org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent?
                )

            FhirVersionEnum.R4 ->
                (bundle as org.hl7.fhir.r4.model.Bundle).addEntry(
                    entry as org.hl7.fhir.r4.model.Bundle.BundleEntryComponent?
                )

            FhirVersionEnum.R5 ->
                (bundle as org.hl7.fhir.r5.model.Bundle).addEntry(
                    entry as org.hl7.fhir.r5.model.Bundle.BundleEntryComponent?
                )

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }

        return bundle
    }

    /**
     * Returns a new Bundle for the given version of FHIR and type
     *
     * @param fhirVersion FhirVersionEnum
     * @param type The type of Bundle to return, defaults to COLLECTION
     * @return IBaseBundle
     */
    @JvmStatic
    fun newBundle(fhirVersion: FhirVersionEnum, type: String?): IBaseBundle {
        return newBundle(fhirVersion, null, type)
    }

    /**
     * Returns a new Bundle for the given version of FHIR
     *
     * @param fhirVersion FhirVersionEnum
     * @param id Id to set on the Bundle, will ignore if null
     * @param type The type of Bundle to return, defaults to COLLECTION
     * @return IBaseBundle
     */
    @JvmStatic
    fun newBundle(fhirVersion: FhirVersionEnum, id: String?, type: String?): IBaseBundle {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 -> newDstu3Bundle(id, type)
            FhirVersionEnum.R4 -> newR4Bundle(id, type)
            FhirVersionEnum.R5 -> newR5Bundle(id, type)
            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Returns a new Bundle for the given version of FHIR
     *
     * @param fhirVersion FhirVersionEnum
     * @return IBaseBundle
     */
    @JvmStatic
    fun newBundle(fhirVersion: FhirVersionEnum): IBaseBundle {
        return newBundle(fhirVersion, null, null)
    }

    private fun newR5Bundle(id: String?, type: String?): org.hl7.fhir.r5.model.Bundle {
        val r5Bundle = org.hl7.fhir.r5.model.Bundle()
        if (!id.isNullOrEmpty()) {
            r5Bundle.setId(id)
        }
        r5Bundle.setType(
            if (type.isNullOrEmpty()) org.hl7.fhir.r5.model.Bundle.BundleType.COLLECTION
            else org.hl7.fhir.r5.model.Bundle.BundleType.fromCode(type)
        )
        return r5Bundle
    }

    private fun newR4Bundle(id: String?, type: String?): org.hl7.fhir.r4.model.Bundle {
        val r4Bundle = org.hl7.fhir.r4.model.Bundle()
        if (!id.isNullOrEmpty()) {
            r4Bundle.setId(id)
        }
        r4Bundle.setType(
            if (type.isNullOrEmpty()) org.hl7.fhir.r4.model.Bundle.BundleType.COLLECTION
            else org.hl7.fhir.r4.model.Bundle.BundleType.fromCode(type)
        )
        return r4Bundle
    }

    private fun newDstu3Bundle(id: String?, type: String?): org.hl7.fhir.dstu3.model.Bundle {
        val dstu3Bundle = org.hl7.fhir.dstu3.model.Bundle()
        if (!id.isNullOrEmpty()) {
            dstu3Bundle.setId(id)
        }
        dstu3Bundle.setType(
            if (type.isNullOrEmpty()) org.hl7.fhir.dstu3.model.Bundle.BundleType.COLLECTION
            else org.hl7.fhir.dstu3.model.Bundle.BundleType.fromCode(type)
        )
        return dstu3Bundle
    }

    /**
     * Sets the BundleType of the Bundle and returns the Bundle
     *
     * @param bundle IBaseBundle
     * @param bundleType String
     * @return IBaseBundle
     */
    @JvmStatic
    fun setBundleType(bundle: IBaseBundle, bundleType: String?): IBaseBundle? {
        return when (val fhirVersion = bundle.structureFhirVersionEnum) {
            FhirVersionEnum.DSTU3 ->
                (bundle as org.hl7.fhir.dstu3.model.Bundle).setType(
                    org.hl7.fhir.dstu3.model.Bundle.BundleType.fromCode(bundleType)
                )

            FhirVersionEnum.R4 ->
                (bundle as org.hl7.fhir.r4.model.Bundle).setType(
                    org.hl7.fhir.r4.model.Bundle.BundleType.fromCode(bundleType)
                )

            FhirVersionEnum.R5 ->
                (bundle as org.hl7.fhir.r5.model.Bundle).setType(
                    org.hl7.fhir.r5.model.Bundle.BundleType.fromCode(bundleType)
                )

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Sets the total property of the Bundle and returns the Bundle
     *
     * @param bundle IBaseBundle
     * @param total int
     * @return IBaseBundle
     */
    @JvmStatic
    fun setBundleTotal(bundle: IBaseBundle, total: Int): IBaseBundle? {
        return when (val fhirVersion = bundle.structureFhirVersionEnum) {
            FhirVersionEnum.DSTU3 -> (bundle as org.hl7.fhir.dstu3.model.Bundle).setTotal(total)
            FhirVersionEnum.R4 -> (bundle as org.hl7.fhir.r4.model.Bundle).setTotal(total)
            FhirVersionEnum.R5 -> (bundle as org.hl7.fhir.r5.model.Bundle).setTotal(total)
            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Returns a new entry element
     *
     * @param fhirVersion FhirVersionEnum
     * @return IBaseBackboneElement
     */
    @JvmStatic
    fun newEntry(fhirVersion: FhirVersionEnum): IBaseBackboneElement {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent()
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.Bundle.BundleEntryComponent()
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.Bundle.BundleEntryComponent()
            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Returns a new entry element with the Resource
     *
     * @param resource IBaseResource
     * @return IBaseBackboneElement
     */
    @JvmStatic
    fun newEntryWithResource(resource: IBaseResource): IBaseBackboneElement {
        return when (val fhirVersion = resource.structureFhirVersionEnum) {
            FhirVersionEnum.DSTU3 ->
                org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent()
                    .setResource(resource as org.hl7.fhir.dstu3.model.Resource)

            FhirVersionEnum.R4 ->
                org.hl7.fhir.r4.model.Bundle.BundleEntryComponent()
                    .setResource(resource as org.hl7.fhir.r4.model.Resource)

            FhirVersionEnum.R5 ->
                org.hl7.fhir.r5.model.Bundle.BundleEntryComponent()
                    .setResource(resource as org.hl7.fhir.r5.model.Resource)

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Returns a new entry element with the Response
     *
     * @param fhirVersion FhirVersionEnum
     * @param response IBaseBackboneElement
     * @return IBaseBackboneElement
     */
    @JvmStatic
    fun newEntryWithResponse(
        fhirVersion: FhirVersionEnum,
        response: IBaseBackboneElement?,
    ): IBaseBackboneElement? {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent()
                    .setResponse(
                        response as org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent?
                    )

            FhirVersionEnum.R4 ->
                org.hl7.fhir.r4.model.Bundle.BundleEntryComponent()
                    .setResponse(
                        response as org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent?
                    )

            FhirVersionEnum.R5 ->
                org.hl7.fhir.r5.model.Bundle.BundleEntryComponent()
                    .setResponse(
                        response as org.hl7.fhir.r5.model.Bundle.BundleEntryResponseComponent?
                    )

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Returns a new BundleEntryResponse element with the location
     *
     * @param fhirVersion FhirVersionEnum
     * @param location String
     * @return IBaseBackboneElement
     */
    @JvmStatic
    fun newResponseWithLocation(
        fhirVersion: FhirVersionEnum,
        location: String?,
    ): IBaseBackboneElement? {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                org.hl7.fhir.dstu3.model.Bundle.BundleEntryResponseComponent().setLocation(location)
            FhirVersionEnum.R4 ->
                org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent().setLocation(location)
            FhirVersionEnum.R5 ->
                org.hl7.fhir.r5.model.Bundle.BundleEntryResponseComponent().setLocation(location)
            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Returns a new BundleEntryRequest element with the method and url
     *
     * @param fhirVersion FhirVersionEnum
     * @param method String
     * @param url String
     * @return IBaseBackboneElement
     */
    @JvmStatic
    fun newRequest(
        fhirVersion: FhirVersionEnum,
        method: String?,
        url: String?,
    ): IBaseBackboneElement {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent()
                    .setMethod(org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.fromCode(method))
                    .setUrl(url)

            FhirVersionEnum.R4 ->
                org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent()
                    .setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.fromCode(method))
                    .setUrl(url)

            FhirVersionEnum.R5 ->
                org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent()
                    .setMethod(org.hl7.fhir.r5.model.Bundle.HTTPVerb.fromCode(method))
                    .setUrl(url)

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Returns a new BundleEntryRequest element with the method
     *
     * @param fhirVersion FhirVersionEnum
     * @param method String
     * @return IBaseBackboneElement
     */
    @JvmStatic
    fun newRequest(fhirVersion: FhirVersionEnum, method: String?): IBaseBackboneElement? {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent()
                    .setMethod(org.hl7.fhir.dstu3.model.Bundle.HTTPVerb.fromCode(method))

            FhirVersionEnum.R4 ->
                org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent()
                    .setMethod(org.hl7.fhir.r4.model.Bundle.HTTPVerb.fromCode(method))

            FhirVersionEnum.R5 ->
                org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent()
                    .setMethod(org.hl7.fhir.r5.model.Bundle.HTTPVerb.fromCode(method))

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Sets the BundleEntryRequest url
     *
     * @param fhirVersion FhirVersionEnum
     * @param request IBaseBackboneElement
     * @param url String
     * @return IBaseBackboneElement
     */
    @JvmStatic
    fun setRequestUrl(
        fhirVersion: FhirVersionEnum,
        request: IBaseBackboneElement,
        url: String?,
    ): IBaseBackboneElement? {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                (request as org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent).setUrl(url)
            FhirVersionEnum.R4 ->
                (request as org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent).setUrl(url)
            FhirVersionEnum.R5 ->
                (request as org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent).setUrl(url)
            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Sets the BundleEntryRequest ifNoneExist property
     *
     * @param fhirVersion FhirVersionEnum
     * @param request IBaseBackboneElement
     * @param ifNoneExist String
     * @return IBaseBackboneElement
     */
    @JvmStatic
    fun setRequestIfNoneExist(
        fhirVersion: FhirVersionEnum,
        request: IBaseBackboneElement,
        ifNoneExist: String?,
    ): IBaseBackboneElement? {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                (request as org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent)
                    .setIfNoneExist(ifNoneExist)

            FhirVersionEnum.R4 ->
                (request as org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent)
                    .setIfNoneExist(ifNoneExist)

            FhirVersionEnum.R5 ->
                (request as org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent)
                    .setIfNoneExist(ifNoneExist)

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Sets the BundleEntry fullUrl property
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement
     * @param fullUrl String
     * @return IBaseBackboneElement
     */
    @JvmStatic
    fun setEntryFullUrl(
        fhirVersion: FhirVersionEnum,
        entry: IBaseBackboneElement,
        fullUrl: String?,
    ): IBaseBackboneElement? {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                (entry as org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent).setFullUrl(fullUrl)
            FhirVersionEnum.R4 ->
                (entry as org.hl7.fhir.r4.model.Bundle.BundleEntryComponent).setFullUrl(fullUrl)
            FhirVersionEnum.R5 ->
                (entry as org.hl7.fhir.r5.model.Bundle.BundleEntryComponent).setFullUrl(fullUrl)
            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    /**
     * Sets the BundleEntry request property
     *
     * @param fhirVersion FhirVersionEnum
     * @param entry IBaseBackboneElement
     * @param request IBaseBackboneElement
     * @return IBaseBackboneElement
     */
    @JvmStatic
    fun setEntryRequest(
        fhirVersion: FhirVersionEnum,
        entry: IBaseBackboneElement,
        request: IBaseBackboneElement?,
    ): IBaseBackboneElement? {
        val requestTypeError = { t: String -> "Request should be of type: $t" }
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 -> {
                require(
                    !(request != null &&
                        request !is org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent)
                ) {
                    requestTypeError("org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent")
                }
                (entry as org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent).setRequest(request)
            }

            FhirVersionEnum.R4 -> {
                require(
                    !(request != null &&
                        request !is org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent)
                ) {
                    requestTypeError("org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent")
                }
                (entry as org.hl7.fhir.r4.model.Bundle.BundleEntryComponent).setRequest(request)
            }

            FhirVersionEnum.R5 -> {
                require(
                    !(request != null &&
                        request !is org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent)
                ) {
                    requestTypeError("org.hl7.fhir.r5.model.Bundle.BundleEntryRequestComponent")
                }
                (entry as org.hl7.fhir.r5.model.Bundle.BundleEntryComponent).setRequest(request)
            }

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }

    @JvmStatic
    fun resourceToRuntimeSearchParam(resource: IBaseResource): RuntimeSearchParam {
        return when (val fhirVersion = resource.structureFhirVersionEnum) {
            FhirVersionEnum.DSTU3 -> {
                val res = resource as org.hl7.fhir.dstu3.model.SearchParameter
                RuntimeSearchParam(
                    res.idElement,
                    res.url,
                    res.code,
                    res.description,
                    res.expression,
                    RestSearchParameterTypeEnum.REFERENCE,
                    null,
                    res.target.map { it.toString() }.toSet(),
                    RuntimeSearchParamStatusEnum.ACTIVE,
                    res.base.map { it.toString() }.toList(),
                )
            }

            FhirVersionEnum.R4 -> {
                val resR4 = resource as org.hl7.fhir.r4.model.SearchParameter
                RuntimeSearchParam(
                    resR4.idElement,
                    resR4.url,
                    resR4.code,
                    resR4.description,
                    resR4.expression,
                    RestSearchParameterTypeEnum.REFERENCE,
                    null,
                    resR4.target.map { it.toString() }.toSet(),
                    RuntimeSearchParamStatusEnum.ACTIVE,
                    resR4.base.map { it.toString() }.toList(),
                )
            }

            FhirVersionEnum.R5 -> {
                val resR5 = resource as org.hl7.fhir.r5.model.SearchParameter
                RuntimeSearchParam(
                    resR5.idElement,
                    resR5.url,
                    resR5.code,
                    resR5.description,
                    resR5.expression,
                    RestSearchParameterTypeEnum.REFERENCE,
                    null,
                    resR5.target.map { it.toString() }.toSet(),
                    RuntimeSearchParamStatusEnum.ACTIVE,
                    resR5.base.map { it.toString() }.toList(),
                )
            }

            else ->
                throw IllegalArgumentException(
                    UNSUPPORTED_VERSION_OF_FHIR(fhirVersion.fhirVersionString)
                )
        }
    }
}
