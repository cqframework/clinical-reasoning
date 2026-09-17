package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.model.primitive.IdDt
import java.util.UUID
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType

object Ids {
    /**
     * Ensures the id contains the resource type
     *
     * @param id the String representation of the id
     * @param resourceType the type of the resource
     * @return the id
     */
    @JvmStatic
    fun ensureIdType(id: String, resourceType: String): String {
        return if (id.contains("/")) id else "$resourceType/$id"
    }

    /**
     * Creates the appropriate IIdType for a given ResourceTypeClass
     *
     * @param <ResourceType> an IBase type
     * @param <IdType> an IIdType type
     * @param resourceTypeClass the type of the Resource to create an Id for
     * @param id the String representation of the Id to generate
     * @return the id </IdType></ResourceType>
     */
    @JvmStatic
    fun <ResourceType : IBaseResource, IdType : IIdType> newId(
        resourceTypeClass: Class<out ResourceType>,
        id: String,
    ): IdType {
        val versionEnum = FhirVersions.forClass(resourceTypeClass)
        return newId(versionEnum, resourceTypeClass.simpleName, id)
    }

    /**
     * Creates the appropriate IIdType for a given BaseTypeClass
     *
     * @param <BaseType> an IBase type
     * @param <IdType> an IIdType type
     * @param baseTypeClass the BaseTypeClass to use for for determining the FHIR Version
     * @param resourceName the type of the Resource to create an Id for
     * @param id the String representation of the Id to generate
     * @return the id </IdType></BaseType>
     */
    @JvmStatic
    fun <BaseType : IBase, IdType : IIdType> newId(
        baseTypeClass: Class<out BaseType>,
        resourceName: String,
        id: String,
    ): IdType {
        val versionEnum = FhirVersions.forClass(baseTypeClass)
        return newId(versionEnum, resourceName, id)
    }

    /**
     * Creates a new random Id of the appropriate IIdType for a given FhirContext
     *
     * @param <IdType> an IIdType type
     * @param fhirContext the FhirContext to use for Id generation
     * @param resourceType the type of the Resource to create an Id for
     * @return the id </IdType>
     */
    @JvmStatic
    fun <IdType : IIdType> newRandomId(fhirContext: FhirContext, resourceType: String): IdType {

        return newId(fhirContext.version.version, resourceType, UUID.randomUUID().toString())
    }

    /**
     * Creates the appropriate IIdType for a given FhirContext
     *
     * @param <IdType> an IIdType type
     * @param fhirContext the FhirContext to use for Id generation
     * @param resourceType the type of the Resource to create an Id for
     * @param id the String representation of the Id to generate
     * @return the id </IdType>
     */
    @JvmStatic
    fun <IdType : IIdType> newId(
        fhirContext: FhirContext,
        resourceType: String,
        id: String,
    ): IdType {

        return newId(fhirContext.version.version, resourceType, id)
    }

    /**
     * Creates the appropriate IIdType for a given FhirVersionEnum
     *
     * @param <IdType> an IIdType type
     * @param fhirVersionEnum the FHIR version to generate an Id for
     * @param resourceType the type of the Resource to create an Id for
     * @param idPart the String representation of the Id to generate
     * @return the id </IdType>
     */
    @JvmStatic
    fun <IdType : IIdType> newId(
        fhirVersionEnum: FhirVersionEnum,
        resourceType: String,
        idPart: String,
    ): IdType {
        return newId(fhirVersionEnum, "$resourceType/$idPart")
    }

    /**
     * Creates the appropriate IIdType for a given FhirContext
     *
     * @param <IdType> an IIdType type
     * @param fhirContext the FhirContext to use for Id generation
     * @param id the String representation of the Id to generate
     * @return the id </IdType>
     */
    @JvmStatic
    fun <IdType : IIdType> newId(fhirContext: FhirContext, id: String): IdType {

        return newId(fhirContext.version.version, id)
    }

    /**
     * The gets the "simple" Id for the Resource, without qualifiers or versions. For example,
     * "Patient/123".
     *
     * This is shorthand for resource.getIdElement().toUnqualifiedVersionless().getValue()
     *
     * @param resource the Resource to get the Id for
     * @return the simple Id
     */
    @JvmStatic
    fun simple(resource: IBaseResource): String? {
        requireNotNull(resource.idElement)

        return simple(resource.idElement)
    }

    /**
     * The gets the "simple" Id for the Id, without qualifiers or versions. For example,
     * "Patient/123".
     *
     * This is shorthand for id.toUnqualifiedVersionless().getValue()
     *
     * @param id the IIdType to get the Id for
     * @return the simple Id
     */
    @JvmStatic
    fun simple(id: IIdType): String? {
        require(id.hasResourceType())
        require(id.hasIdPart())

        return id.toUnqualifiedVersionless().value
    }

    /**
     * The gets the "simple" Id part for the Id, without qualifiers or versions or the resource
     * Prefix. For example, "123".
     *
     * This is shorthand for resource.getIdElement().toUnqualifiedVersionless().getIdPart()
     *
     * @param resource the Resource to get the Id for
     * @return the simple Id part
     */
    @JvmStatic
    fun simplePart(resource: IBaseResource): String? {
        requireNotNull(resource.idElement)

        return simplePart(resource.idElement)
    }

    /**
     * The gets the "simple" Id part for the Id, without qualifiers or versions or the resource
     * Prefix. For example, "123".
     *
     * This is shorthand for id.toUnqualifiedVersionless().getIdPart()
     *
     * @param id the IIdType to get the Id for
     * @return the simple Id part
     */
    @JvmStatic
    fun simplePart(id: IIdType): String? {
        require(id.hasResourceType())
        require(id.hasIdPart())

        return id.toUnqualifiedVersionless().idPart
    }

    /**
     * Creates the appropriate IIdType for a given FhirVersionEnum
     *
     * @param <IdType> an IIdType type
     * @param fhirVersionEnum the FHIR version to generate an Id for
     * @param id the String representation of the Id to generate
     * @return the id </IdType>
     */
    @JvmStatic
    fun <IdType : IIdType?> newId(fhirVersionEnum: FhirVersionEnum, id: String): IdType {
        @Suppress("UNCHECKED_CAST")
        return when (fhirVersionEnum) {
            FhirVersionEnum.DSTU2 -> IdDt(id)
            FhirVersionEnum.DSTU2_1 -> org.hl7.fhir.dstu2016may.model.IdType(id)
            FhirVersionEnum.DSTU2_HL7ORG -> org.hl7.fhir.dstu2.model.IdType(id)
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.IdType(id)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.IdType(id)
            FhirVersionEnum.R4B -> org.hl7.fhir.r4b.model.IdType(id)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.IdType(id)
        }
            as IdType
    }
}
