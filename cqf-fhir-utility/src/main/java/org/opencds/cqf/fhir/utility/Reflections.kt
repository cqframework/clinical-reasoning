package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.BaseRuntimeChildDefinition.IAccessor
import ca.uhn.fhir.context.FhirContext
import java.util.function.Function
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IPrimitiveType

/**
 * This class provides utility methods for doing reflection on FHIR resources. It's specifically
 * focused on knowledge artifact resources since there's not a common interface for those across
 * different Resources (and FHIR versions)
 */
object Reflections {
    /**
     * Gets the IAccessor for the given BaseType and child
     *
     * @param <BaseType> an IBase type
     * @param baseTypeClass the class of the IBase type
     * @param childName the name of the child property of the BaseType to generate an accessor for
     * @return an IAccessor for the given child and the BaseType </BaseType>
     */
    @JvmStatic
    fun <BaseType : IBase> getAccessor(
        baseTypeClass: Class<out BaseType>,
        childName: String,
    ): IAccessor {
        val fhirContext = FhirContext.forCached(FhirVersions.forClass(baseTypeClass))
        val elementDefinition = fhirContext.getElementDefinition(baseTypeClass)
        return elementDefinition.getChildByName(childName).accessor
    }

    /**
     * Generates a function to access a primitive property of the given BaseType.
     *
     * @param <BaseType> an IBase type
     * @param <ReturnType> a return type for the Functions
     * @param baseTypeClass the class of a the IBase type
     * @param childName to create a function for
     * @return a function for accessing the "childName" property of the BaseType
     *   </ReturnType></BaseType>
     */
    fun <BaseType : IBase, ReturnType> getPrimitiveFunction(
        baseTypeClass: Class<out BaseType>,
        childName: String,
    ): java.util.function.Function<BaseType, ReturnType?> {

        val accessor = getAccessor(baseTypeClass, childName)
        return Function { r: BaseType ->
            val value = accessor.getFirstValueOrNull<IBase?>(r)
            if (value.isEmpty) {
                return@Function null
            } else {
                val x = (value.get() as IPrimitiveType<ReturnType?>).value
                return@Function x
            }
        }
    }

    /**
     * Generates a function to access a primitive property of the given BaseType.
     *
     * @param <BaseType> an IBase type
     * @param <ReturnType> a return type for the Functions
     * @param baseTypeClass the class of a the IBase type
     * @param childName to create a function for
     * @return a function for accessing the "childName" property of the BaseType
     *   </ReturnType></BaseType>
     */
    fun <BaseType : IBase, ReturnType : MutableList<out IBase>?> getFunction(
        baseTypeClass: Class<out BaseType>,
        childName: String,
    ): Function<BaseType, ReturnType?> {

        val accessor = getAccessor(baseTypeClass, childName)
        return Function { r: BaseType ->
            val x = accessor.getValues(r) as ReturnType?
            x
        }
    }

    /**
     * Generates a function to access the "version" property of the given BaseType.
     *
     * @param <BaseType> an IBase type
     * @param baseTypeClass the class of a the IBase type
     * @return a function for accessing the "version" property of the BaseType </BaseType>
     */
    @JvmStatic
    fun <BaseType : IBase> getVersionFunction(
        baseTypeClass: Class<out BaseType>
    ): Function<BaseType, String?> {
        return getPrimitiveFunction(baseTypeClass, "version")
    }

    /**
     * Generates a function to access the "url" property of the given BaseType.
     *
     * @param <BaseType> an IBase type
     * @param baseTypeClass the class of a the IBase type
     * @return a function for accessing the "url" property of the BaseType </BaseType>
     */
    @JvmStatic
    fun <BaseType : IBase> getUrlFunction(
        baseTypeClass: Class<out BaseType>
    ): Function<BaseType, String?> {
        return getPrimitiveFunction(baseTypeClass, "url")
    }

    /**
     * Generates a function to access the "name" property of the given BaseType.
     *
     * @param <BaseType> an IBase type
     * @param baseTypeClass the class of a the IBase type
     * @return a function for accessing the "name" property of the BaseType </BaseType>
     */
    @JvmStatic
    fun <BaseType : IBase> getNameFunction(
        baseTypeClass: Class<out BaseType>
    ): Function<BaseType, String?> {
        return getPrimitiveFunction(baseTypeClass, "name")
    }
}
