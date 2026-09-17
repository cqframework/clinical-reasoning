package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType

object Resources {
    @JvmStatic
    fun <T : IBase> castOrThrow(obj: IBase?, type: Class<T>, errorMessage: String?): T? {
        if (obj == null) return null
        if (type.isInstance(obj)) {
            return type.cast(obj)
        }
        throw IllegalArgumentException(errorMessage)
    }

    @JvmStatic
    fun <T : IBaseResource, I : IIdType> newResource(resourceClass: Class<T>, idPart: String): T {
        require(!idPart.contains("/")) {
            "idPart must be a simple id. Do not include resourceType or history"
        }
        val resource = newResource(resourceClass)

        @Suppress("UNCHECKED_CAST") val id = Ids.newId<T, IIdType>(resourceClass, idPart) as I
        resource.setId(id)
        return resource
    }

    @JvmStatic
    fun <T : IBaseResource> newResource(resourceClass: Class<T>): T {
        try {
            return resourceClass.getConstructor().newInstance()
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "resourceClass must be a type with an empty default constructor to use this function"
            )
        }
    }

    @JvmStatic
    fun <T : IBaseBackboneElement> newBackboneElement(backboneElementClass: Class<T>): T {
        try {
            return backboneElementClass.getConstructor().newInstance()
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "backboneElementClass must be a type with an empty default constructor to use this function"
            )
        }
    }

    @JvmStatic
    fun newBaseForVersion(type: String, fhirVersion: FhirVersionEnum): IBase {
        return newBase(getClassForTypeAndVersion(type, fhirVersion))
    }

    @JvmStatic
    fun <T : IBase> newBase(baseClass: Class<T>): T {
        try {
            return baseClass.getConstructor().newInstance()
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "bClass must be a type with an empty default constructor to use this function"
            )
        }
    }

    @JvmStatic
    fun <T : IBase> getClassForTypeAndVersion(
        type: String,
        fhirVersion: FhirVersionEnum,
    ): Class<T> {
        try {
            @Suppress("UNCHECKED_CAST")
            return Class.forName("org.hl7.fhir.${fhirVersion.toString().lowercase()}.model.$type")
                as Class<T>
        } catch (e: Exception) {
            throw IllegalArgumentException(e)
        }
    }

    /**
     * Deep clone a resource with the FhirTerser
     *
     * @param <T> the resource type
     * @param resource the resource to clone
     * @return the cloned resource </T>
     */
    @JvmStatic
    fun <T : IBaseResource> clone(resource: T): T {
        val terser = FhirContext.forCached(resource.structureFhirVersionEnum).newTerser()
        return terser.clone(resource)
    }

    /**
     * Convert a resource to a prettified JSON string
     *
     * @param resource the resource to convert
     * @return the JSON string
     */
    @JvmStatic
    fun stringify(resource: IBaseResource): String {
        return FhirContext.forCached(resource.structureFhirVersionEnum)
            .newJsonParser()
            .setPrettyPrint(true)
            .encodeResourceToString(resource)
    }
}
