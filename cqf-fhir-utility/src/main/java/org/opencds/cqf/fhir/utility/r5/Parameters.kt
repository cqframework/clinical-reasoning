package org.opencds.cqf.fhir.utility.r5

import org.hl7.fhir.instance.model.api.IIdType
import org.hl7.fhir.r5.model.*
import org.hl7.fhir.r5.model.Parameters
import org.opencds.cqf.fhir.utility.Ids

/** Utility class for R5 parameters */
object Parameters {
    /**
     * Method to create parameters for r5
     *
     * @param id IdType of measure
     * @param parts Parameter r5 components
     * @return parameters
     */
    @JvmStatic
    fun parameters(id: IdType, vararg parts: Parameters.ParametersParameterComponent?): Parameters {
        val p = parameters(*parts)
        p.setId(id)
        return p
    }

    /**
     * Method to create a parameter for r5
     *
     * @param idPart String representing the Id of measure
     * @param parts Parameter r5 components
     * @return parameters
     */
    @JvmStatic
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
     * Method to create a parameter for r5
     *
     * @param parts Parameter r5 components
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
     * Method to create a parameter for r5
     *
     * @param parts Parameter r5 components
     * @param name String representing parameter name
     * @return parameters
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
     * Method to create a parameter for r5
     *
     * @param parts Parameter r5 components
     * @param name String representing parameter name
     * @param value String representing value of parameter
     * @return parameters
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
     * Method to create a parameter for r5
     *
     * @param parts Parameter r5 components
     * @param name String representing parameter name
     * @param value Datatype representing value of parameter
     * @return parameters
     */
    @JvmStatic
    fun part(
        name: String,
        value: DataType,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        val c = part(name, *parts)
        c.setValue(value)
        return c
    }

    /**
     * Method to create a parameter for r5
     *
     * @param parts Parameter r5 components
     * @param name String representing parameter name
     * @return parameters
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
     * Method to get a parameter parts for r5
     *
     * @param parameters Parameter r5 components
     * @param name String representing parameter name
     * @return parameters
     */
    @JvmStatic
    fun getPartsByName(
        parameters: Parameters,
        name: String,
    ): MutableList<Parameters.ParametersParameterComponent?> {
        return parameters.parameter.filter { x -> name == x!!.getName() }.toMutableList()
    }

    /**
     * Method to get base64 parameter part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @return parameters
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
     * Method to get boolean parameter part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @return parameters
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
     * Method to get canonical parameter part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    @JvmStatic
    fun canonicalPart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, CanonicalType(value), *parts)
    }

    /**
     * Method to get code part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
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
     * Method to get datepart for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
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
     * Method to get dateTime part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
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
     * Method to get decimal part for r5
     *
     * @param name Parameter part name
     * @param value double representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
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
     * Method to get decimal idpart for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
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
     * Method to get instant part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
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
     * Method to get integer part for r5
     *
     * @param name Parameter part name
     * @param value int representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
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
     * Method to get integer part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    @JvmStatic
    fun integer64Part(
        name: String,
        value: Long,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, Integer64Type(value), *parts)
    }

    /**
     * Method to get markdown part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    @JvmStatic
    fun markdownPart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, MarkdownType(value), *parts)
    }

    /**
     * Method to get OID part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
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
     * Method to get positive int part for r5
     *
     * @param name Parameter part name
     * @param value Int representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
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
     * Method to get String part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
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
     * Method to get time part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
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
     * Method to get unsigned int part for r5
     *
     * @param name Parameter part name
     * @param value Int representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
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
     * Method to get unsigned uri part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    @JvmStatic
    fun uriPart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, UriType(value), *parts)
    }

    /**
     * Method to get unsigned url part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    @JvmStatic
    fun urlPart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, UrlType(value), *parts)
    }

    /**
     * Method to get unsigned uuid part for r5
     *
     * @param name Parameter part name
     * @param value String representation of parameter value
     * @param parts representing component parts of parameter
     * @return parameters
     */
    @JvmStatic
    fun uuidPart(
        name: String,
        value: String?,
        vararg parts: Parameters.ParametersParameterComponent?,
    ): Parameters.ParametersParameterComponent {
        return part(name, UuidType(value), *parts)
    }
}
