package org.opencds.cqf.fhir.utility.dstu3

import org.hl7.fhir.dstu3.model.*
import org.hl7.fhir.dstu3.model.Parameters
import org.hl7.fhir.instance.model.api.IIdType
import org.opencds.cqf.fhir.utility.Ids

/** Utility class for getting or creating DSTU3 model parameters */
object Parameters {
    /**
     * Method to create parameters for fhir Dstu3
     *
     * @param id IdType of measure
     * @param parts Parameter dstu3 components
     * @return parameters
     */
    fun parameters(id: IdType, vararg parts: Parameters.ParametersParameterComponent?): Parameters {
        val p = parameters(*parts)
        p.setId(id)
        return p
    }

    /**
     * Method to create parameters for fhir Dstu3
     *
     * @param idPart String representation of Id parameter
     * @param parts Parameter dstu3 components
     * @return parameters
     */
    fun parameters(
        idPart: String,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters {
        return parameters(
            Ids.newId<Parameters, IIdType>(Parameters::class.java, idPart) as IdType,
            *parts,
        )
    }

    /**
     * Method to create parameters for fhir Dstu3 from component parts
     *
     * @param parts Parameter dstu3 components
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
     * Method to create parameter part for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value String representation of component value
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun part(
        name: String,
        value: String,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        val c = part(name, *parts)
        c.setValue(StringType(value))
        return c
    }

    /**
     * Method to create parameter part for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun part(
        name: String,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        val c = Parameters.ParametersParameterComponent()
        c.setName(name)
        for (p in parts) {
            c.addPart(p)
        }
        return c
    }

    /**
     * Method to create parameter part for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value String representation of component value
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun part(
        name: String,
        value: Type,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        val c = part(name, *parts)
        c.setValue(value)
        return c
    }

    /**
     * Method to create parameter part for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param resource dstu3 fhir model used
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun part(
        name: String,
        resource: Resource,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        val c = part(name, *parts)
        c.setResource(resource)
        return c
    }

    /**
     * Method to get parameter parts by parameter component name for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param parameters dstu3 model resource parameters
     * @return parameter
     */
    @JvmStatic
    fun getPartsByName(
        parameters: Parameters,
        name: String,
    ): MutableList<Parameters.ParametersParameterComponent?> {
        return parameters.getParameter().filter { x -> name == x!!.getName() }.toMutableList()
    }

    /**
     * Method to create parameter part base64binary for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value String representation of parameter component
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun base64BinaryPart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, Base64BinaryType(value), *parts)
    }

    /**
     * Method to create boolean part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value String representation of parameter component
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun booleanPart(
        name: String,
        value: Boolean,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, BooleanType(value), *parts)
    }

    /**
     * Method to create code part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value boolean representation of parameter component
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun codePart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, CodeType(value), *parts)
    }

    /**
     * Method to create date part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value String representation of parameter component
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun datePart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, DateType(value), *parts)
    }

    /**
     * Method to create dateTime part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value String representation of parameter component type
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun dateTimePart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, DateTimeType(value), *parts)
    }

    /**
     * Method to create decimal part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value double representation of parameter component type
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun decimalPart(
        name: String,
        value: Double,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, DecimalType(value), *parts)
    }

    /**
     * Method to create id part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value String representation of parameter component type
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun idPart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, IdType(value), *parts)
    }

    /**
     * Method to create instant part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value String representation of parameter component type
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun instantPart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, InstantType(value), *parts)
    }

    /**
     * Method to create integer part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value Integer representation of parameter component type
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun integerPart(
        name: String,
        value: Int,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, IntegerType(value), *parts)
    }

    /**
     * Method to create oid part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value String representation of parameter component type
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun oidPart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, OidType(value), *parts)
    }

    /**
     * Method to create postive integer part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value Integer representation of parameter component type
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun positiveIntPart(
        name: String,
        value: Int,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, PositiveIntType(value), *parts)
    }

    /**
     * Method to create code part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value String representation of parameter component type
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun stringPart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, StringType(value), *parts)
    }

    /**
     * Method to create time part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value String representation of parameter component type
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun timePart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, TimeType(value), *parts)
    }

    /**
     * Method to create unsigned integer part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value Integer representation of parameter component type
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun unsignedIntPart(
        name: String,
        value: Int,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, UnsignedIntType(value), *parts)
    }

    /**
     * Method to create uri part parameter for fhir Dstu3
     *
     * @param name String name of component parameter
     * @param value String representation of parameter component type
     * @param parts Parameter dstu3 components
     * @return parameter
     */
    @JvmStatic
    fun uriPart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, UriType(value), *parts)
    }
}
