package org.opencds.cqf.fhir.utility.r4

import com.google.common.base.Preconditions
import java.util.stream.Collectors
import org.hl7.fhir.instance.model.api.IIdType
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.r4.model.Parameters
import org.opencds.cqf.fhir.utility.Ids

/** Utility class for getting or creating r4 parameters */
object Parameters {
    /**
     * Method to create parameters for fhir r4
     *
     * @param id IdType of measure
     * @param parts Parameter r4 components
     * @return parameters
     */
    @JvmStatic
    fun parameters(
        id: IdType?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters {
        Preconditions.checkNotNull<IdType?>(id)
        val p = parameters(*parts)
        p.setId(id)
        return p
    }

    /**
     * Method to create parameters for fhir r4 model via submission of IdPart
     *
     * @param idPart String representation of the Id used to create IDtype
     * @param parts Parameter r4 components
     * @return parameters
     */
    @JvmStatic
    fun parameters(
        idPart: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters {
        Preconditions.checkNotNull<String?>(idPart)
        return parameters(
            Ids.newId<Parameters, IIdType>(Parameters::class.java, idPart!!) as IdType,
            *parts,
        )
    }

    /**
     * Method to create parameters for fhir r4 model via submission of parameter component
     *
     * @param parts Parameter r4 components
     * @return parameters
     */
    @JvmStatic
    fun parameters(vararg parts: Parameters.ParametersParameterComponent?): Parameters {
        val p = Parameters()
        for (c in parts) {
            p.addParameter(c)
        }
        return p
    }

    /**
     * Method to create parameters part for fhir r4 model via submission of parameter component
     *
     * @param name String of Parameter component name
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun part(
        name: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        Preconditions.checkNotNull<String?>(name)
        val c = Parameters.ParametersParameterComponent()
        c.setName(name)
        for (p in parts) {
            c.addPart(p)
        }
        return c
    }

    /**
     * Method to create parameters part for fhir r4 model via submission of parameter component
     *
     * @param name String of parameter component name
     * @param value String representation of parameter component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun part(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        Preconditions.checkNotNull<String?>(name)
        Preconditions.checkNotNull<String?>(value)
        val c = part(name, *parts)
        c.setValue(StringType(value))
        return c
    }

    /**
     * Method to create parameters part for fhir r4 model via submission of parameter component
     *
     * @param name String of parameter component name
     * @param value String representation of parameter component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun part(
        name: String?,
        value: Type?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        Preconditions.checkNotNull<String?>(name)
        Preconditions.checkNotNull<Type?>(value)
        val c = part(name, *parts)
        c.setValue(value)
        return c
    }

    /**
     * Method to create parameters part for fhir r4 model via submission of parameter component
     *
     * @param name String of parameter component name
     * @param resource Model resource type
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun part(
        name: String?,
        resource: Resource?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        Preconditions.checkNotNull<String?>(name)
        Preconditions.checkNotNull<Resource?>(resource)
        val c = part(name, *parts)
        c.setResource(resource)
        return c
    }

    /**
     * Method to get parameter component part with component part name
     *
     * @param name String of parameter component name
     * @param parameters fhir model parameters
     * @return parameter component
     */
    @JvmStatic
    fun getPartsByName(
        parameters: Parameters?,
        name: String?,
    ): MutableList<Parameters.ParametersParameterComponent?> {
        Preconditions.checkNotNull<Parameters?>(parameters)
        Preconditions.checkNotNull<String?>(name)
        return parameters!!
            .getParameter()
            .stream()
            .filter { x: Parameters.ParametersParameterComponent? -> name == x!!.getName() }
            .collect(Collectors.toList())
    }

    /**
     * Method to create parameter component with base64binary part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun base64BinaryPart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, Base64BinaryType(value), *parts)
    }

    /**
     * Method to create parameter component with boolean part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun booleanPart(
        name: String?,
        value: Boolean,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, BooleanType(value), *parts)
    }

    /**
     * Method to create parameter component with canonical part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun canonicalPart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, CanonicalType(value), *parts)
    }

    /**
     * Method to create parameter component with code part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun codePart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, CodeType(value), *parts)
    }

    /**
     * Method to create parameter component with date part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun datePart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, DateType(value), *parts)
    }

    /**
     * Method to create parameter component with dateTime part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun dateTimePart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, DateTimeType(value), *parts)
    }

    /**
     * Method to create parameter component with decimal part
     *
     * @param name String of parameter component name
     * @param value double representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun decimalPart(
        name: String?,
        value: Double,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, DecimalType(value), *parts)
    }

    /**
     * Method to create parameter component with id part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun idPart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, IdType(value), *parts)
    }

    /**
     * Method to create parameter component with instant part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun instantPart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, InstantType(value), *parts)
    }

    /**
     * Method to create parameter component with integer part
     *
     * @param name String of parameter component name
     * @param value Int representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun integerPart(
        name: String?,
        value: Int,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, IntegerType(value), *parts)
    }

    /**
     * Method to create parameter component with markdown part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun markdownPart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, MarkdownType(value), *parts)
    }

    /**
     * Method to create parameter component with oid part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun oidPart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, OidType(value), *parts)
    }

    /**
     * Method to create parameter component with postivie integer part
     *
     * @param name String of parameter component name
     * @param value Integer representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun positiveIntPart(
        name: String?,
        value: Int,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, PositiveIntType(value), *parts)
    }

    /**
     * Method to create parameter component with String part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun stringPart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, StringType(value), *parts)
    }

    /**
     * Method to create parameter component with time part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun timePart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, TimeType(value), *parts)
    }

    /**
     * Method to create parameter component with unsigned int part
     *
     * @param name String of parameter component name
     * @param value Integer representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun unsignedIntPart(
        name: String?,
        value: Int,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, UnsignedIntType(value), *parts)
    }

    /**
     * Method to create parameter component with uri part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun uriPart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, UriType(value), *parts)
    }

    /**
     * Method to create parameter component with url part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun urlPart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, UrlType(value), *parts)
    }

    /**
     * Method to create parameter component with uuid part
     *
     * @param name String of parameter component name
     * @param value String representation of component value
     * @param parts Parameter r4 components
     * @return parameter component
     */
    @JvmStatic
    fun uuidPart(
        name: String?,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, UuidType(value), *parts)
    }
}
