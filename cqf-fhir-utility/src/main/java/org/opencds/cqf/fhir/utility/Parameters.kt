package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.BaseRuntimeChildDefinition
import ca.uhn.fhir.context.BaseRuntimeChildDefinition.IMutator
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition
import ca.uhn.fhir.context.BaseRuntimeElementDefinition
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.util.ParametersUtil
import kotlin.jvm.optionals.getOrNull
import org.hl7.fhir.instance.model.api.*

/** A utility class for parameter creation and functions in clinical reasoning */
object Parameters {
    private fun getParameterChild(fhirContext: FhirContext): BaseRuntimeChildDefinition {
        return fhirContext.getResourceDefinition("Parameters").getChildByName("parameter")
    }

    private fun getParameterElement(fhirContext: FhirContext): BaseRuntimeElementDefinition<*>? {
        return getParameterChild(fhirContext).getChildByName("parameter")
    }

    private fun getValueMutator(fhirContext: FhirContext): IMutator {
        return getParameterElement(fhirContext)!!.getChildByName("value[x]").mutator
    }

    /**
     * Creates the appropriate parameters for a given FhirContext, IIDType, IBase Parts
     *
     * @param id an IIdType type
     * @param fhirContext the FhirContext for fhir API
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameters
     */
    @JvmStatic
    fun newParameters(
        fhirContext: FhirContext,
        id: IIdType,
        vararg parts: IBase?,
    ): IBaseParameters {
        val newParameters = ParametersUtil.newInstance(fhirContext)
        newParameters.setId(id)
        val mutator = getParameterChild(fhirContext).mutator
        for (part in parts) {
            mutator.addValue(newParameters, part)
        }
        return newParameters
    }

    /**
     * Creates the appropriate parameters for a given FhirContext, ID String, IBase Parts
     *
     * @param id String representation of the ID to generate
     * @param fhirContext the FhirContext for fhir API
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameters
     */
    @JvmStatic
    fun newParameters(fhirContext: FhirContext, id: String, vararg parts: IBase?): IBaseParameters {
        val newId = fhirContext.getElementDefinition("id")!!.newInstance() as IIdType
        newId.value = id
        return Parameters.newParameters(fhirContext, newId, *parts)
    }

    /**
     * Creates the appropriate parameters for a given FhirContext and IBase Parts
     *
     * @param fhirContext the FhirContext for fhir API
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameters
     */
    @JvmStatic
    fun newParameters(fhirContext: FhirContext, vararg parts: IBase?): IBaseParameters {
        val newParameters = ParametersUtil.newInstance(fhirContext)
        val mutator = getParameterChild(fhirContext).mutator
        for (part in parts) {
            mutator.addValue(newParameters, part)
        }
        return newParameters
    }

    /**
     * Creates new IBase parts given FhirContext, part name to get, other IBase Parts
     *
     * @param fhirContext the FhirContext for fhir API
     * @param name String representation of parts to add to parameters
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameter Part
     */
    @JvmStatic
    fun newPart(fhirContext: FhirContext, name: String, vararg parts: IBase?): IBase {
        val nameMutator = getParameterElement(fhirContext)!!.getChildByName("name").mutator
        val partMutator = getParameterElement(fhirContext)!!.getChildByName("part").mutator
        val parameterBase = getParameterElement(fhirContext)!!.newInstance()
        val theName = fhirContext.getElementDefinition("string")!!.newInstance(name)
        nameMutator.setValue(parameterBase, theName)
        for (part in parts) {
            partMutator.addValue(parameterBase, part)
        }
        return parameterBase
    }

    /**
     * Creates new IBase parts given FhirContext, fhir element type, part name to get, Object
     * instance, other IBase Parts
     *
     * @param fhirContext the FhirContext for fhir AP
     * @param type Element definition type I
     * @param name String representation of parts to add to parameters
     * @param value Object instance
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameter Part
     */
    @JvmStatic
    fun <T : IBaseDatatype?> newPart(
        fhirContext: FhirContext,
        type: Class<T>,
        name: String,
        value: Any,
        vararg parts: IBase?,
    ): IBase {
        val newPpc = newPart(fhirContext, name, *parts)
        val typeValue = fhirContext.getElementDefinition(type)!!.newInstance(value)
        getValueMutator(fhirContext).setValue(newPpc, typeValue)
        return newPpc
    }

    /**
     * Creates new IBase parts given FhirContext, fhir element type, part name to get, Object
     * instance, other IBase Parts
     *
     * @param fhirContext the FhirContext for fhir AP
     * @param typeName String representation of FhirElement definition type I
     * @param name String representation of parts to add to parameters
     * @param value Object instance
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameter Part
     */
    @JvmStatic
    fun newPart(
        fhirContext: FhirContext,
        typeName: String,
        name: String,
        value: Any,
        vararg parts: IBase?,
    ): IBase {
        val newPpc = newPart(fhirContext, name, *parts)
        val typeValue = fhirContext.getElementDefinition(typeName)!!.newInstance(value.toString())
        getValueMutator(fhirContext).setValue(newPpc, typeValue)
        return newPpc
    }

    /**
     * Creates new IBase parts given FhirContext, fhir element type, part name to get, Object
     * instance, other IBase Parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parts to add to parameters
     * @param value IBaseResource value
     * @param parts IBase types as interface marker for convergence between Hapi and HL7
     * @return new parameter Part
     */
    @JvmStatic
    fun newPart(
        fhirContext: FhirContext,
        name: String,
        value: IBaseResource,
        vararg parts: IBase?,
    ): IBase {
        val newPpc = newPart(fhirContext, name, *parts)
        getParameterElement(fhirContext)!!
            .getChildByName("resource")
            .mutator
            .setValue(newPpc, value)
        return newPpc
    }

    /**
     * method get string named parameter using FhirContext, Parameters, and name
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param parameters IBaseResource values
     * @return parameter string name
     */
    @JvmStatic
    fun getSingularStringPart(
        fhirContext: FhirContext,
        parameters: IBaseResource,
        name: String,
    ): String? {
        return ParametersUtil.getNamedParameterValueAsString(
                fhirContext,
                parameters as IBaseParameters,
                name,
            )
            .getOrNull()
    }

    /**
     * method get string named part from parameters using FhirContext, Parameters, and name
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param parameters IBaseResource values
     * @return parameter string name
     */
    @JvmStatic
    fun getPartsByName(
        fhirContext: FhirContext,
        parameters: IBaseResource,
        name: String,
    ): MutableList<IBase?> {
        return ParametersUtil.getNamedParameters(fhirContext, parameters, name)
    }

    /**
     * method create base64binary part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value Name of part value
     * @param parts IBase type parameter parts
     * @return new base64 binary part
     */
    @JvmStatic
    fun newBase64BinaryPart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "base64binary", name, value, *parts)
    }

    /**
     * method create boolean part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new boolean part
     */
    @JvmStatic
    fun newBooleanPart(
        fhirContext: FhirContext,
        name: String,
        value: Boolean,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "boolean", name, value, *parts)
    }

    /**
     * method create canonical part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new canonical part
     */
    @JvmStatic
    fun newCanonicalPart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "canonical", name, value, *parts)
    }

    /**
     * method create code part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new code part
     */
    @JvmStatic
    fun newCodePart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "code", name, value, *parts)
    }

    /**
     * method create date part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new date part
     */
    @JvmStatic
    fun newDatePart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "date", name, value, *parts)
    }

    /**
     * method create datetime part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new datetime part
     */
    @JvmStatic
    fun newDateTimePart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "datetime", name, value, *parts)
    }

    /**
     * method create decimal part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new decimal part
     */
    @JvmStatic
    fun newDecimalPart(
        fhirContext: FhirContext,
        name: String,
        value: Double,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "decimal", name, value, *parts)
    }

    /**
     * method create ID part from parameters using FhirContext, Parameter name, value of parameter,
     * parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new ID part
     */
    @JvmStatic
    fun newIdPart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "id", name, value, *parts)
    }

    /**
     * method create instant part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new instant part
     */
    fun newInstantPart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "instant", name, value, *parts)
    }

    /**
     * method create integer part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new integer part
     */
    @JvmStatic
    fun newIntegerPart(
        fhirContext: FhirContext,
        name: String,
        value: Int,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "integer", name, value, *parts)
    }

    /**
     * method create integer base 64 part from parameters using FhirContext, Parameter name, value
     * of parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new base64 integer part
     */
    fun newInteger64Part(
        fhirContext: FhirContext,
        name: String,
        value: Long,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "integer64", name, value, *parts)
    }

    /**
     * method create markdown part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new markdown part
     */
    @JvmStatic
    fun newMarkdownPart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "markdown", name, value, *parts)
    }

    /**
     * method create OID part from parameters using FhirContext, Parameter name, value of parameter,
     * parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new OID part
     */
    @JvmStatic
    fun newOidPart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "oid", name, value, *parts)
    }

    /**
     * method create positive int part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new positive int part
     */
    @JvmStatic
    fun newPositiveIntPart(
        fhirContext: FhirContext,
        name: String,
        value: Int,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "positiveint", name, value, *parts)
    }

    /**
     * method create string part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new string part
     */
    @JvmStatic
    fun newStringPart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "string", name, value, *parts)
    }

    /**
     * method create time part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new time part
     */
    @JvmStatic
    fun newTimePart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "time", name, value, *parts)
    }

    /**
     * method create unsigned int part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new unsigned int part
     */
    @JvmStatic
    fun newUnsignedIntPart(
        fhirContext: FhirContext,
        name: String,
        value: Int,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "unsignedint", name, value, *parts)
    }

    /**
     * method create uri part from parameters using FhirContext, Parameter name, value of parameter,
     * parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new uri part
     */
    @JvmStatic
    fun newUriPart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "uri", name, value, *parts)
    }

    /**
     * method create url part from parameters using FhirContext, Parameter name, value of parameter,
     * parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new url part
     */
    @JvmStatic
    fun newUrlPart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "url", name, value, *parts)
    }

    /**
     * method create uuid part from parameters using FhirContext, Parameter name, value of
     * parameter, parameter parts
     *
     * @param fhirContext the FhirContext for fhir AP I
     * @param name String representation of parameter name
     * @param value part value
     * @param parts IBase type parameter parts
     * @return new uuid part
     */
    @JvmStatic
    fun newUuidPart(
        fhirContext: FhirContext,
        name: String,
        value: String,
        vararg parts: IBase?,
    ): IBase {
        return newPart(fhirContext, "uuid", name, value, *parts)
    }

    /**
     * Removes a parameter from a Parameters object by name
     *
     * @param parameters the Parameters object to remove the parameter from
     * @param name the name of the parameter to remove
     */
    @JvmStatic
    fun removeParameter(parameters: IBaseParameters, name: String) {
        val ctx = FhirContext.forCached(parameters.structureFhirVersionEnum)
        val child = getParameterChild(ctx)

        val values = child.accessor.getValues(parameters)

        for (i in values.indices) {
            val ppc = values[i] as IBase
            val partParameterDef =
                ctx.getElementDefinition(ppc.javaClass) as BaseRuntimeElementCompositeDefinition<*>
            val nameChild = partParameterDef.getChildByName("name")
            val nameValues = nameChild.accessor.getValues(ppc)
            val ppcName = nameValues.filterIsInstance<IPrimitiveType<*>>().firstOrNull()
            if (ppcName != null && ppcName.value == name) {
                values.removeAt(i)
                return
            }
        }
    }
}
