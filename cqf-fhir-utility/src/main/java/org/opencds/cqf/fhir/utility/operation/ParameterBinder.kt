package org.opencds.cqf.fhir.utility.operation

import ca.uhn.fhir.rest.annotation.IdParam
import ca.uhn.fhir.rest.annotation.OperationParam
import ca.uhn.fhir.util.ParametersUtil
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType
import org.opencds.cqf.fhir.utility.Resources
import java.lang.reflect.Parameter

internal interface ParameterBinder {
    enum class Type {
        ID,
        OPERATION,
        EXTRA
    }

    // Extract the value from the parameters resource that matches the external name of the parameter.
    // And removes the value from the parameters resource. This is to ensure that all parameters get consumed
    fun bind(parameters: IBaseParameters?): Any?

    fun type(): Type

    fun name(): String

    fun parameter(): Parameter

    class IdParameterBinder(private val parameter: Parameter) : ParameterBinder {
        init {
            require(
                IIdType::class.java.isAssignableFrom(parameter.type)) {
                "Parameter annotated with @IdParam must be of type IIdType"
            }
        }

        override fun type(): Type {
            return Type.ID
        }

        override fun name(): String {
            return "_id"
        }

        override fun bind(parameters: IBaseParameters?): Any? {
            throw UnsupportedOperationException("bind is not supported for @IdParam")
        }

        override fun parameter(): Parameter {
            return parameter
        }
    }

    class OperationParameterBinder(private val parameter: Parameter, private val operationParam: OperationParam) : ParameterBinder {
        private enum class ParameterClass {
            RESOURCE,
            VALUE,
            LIST
        }

        private val parameterClass: ParameterClass

        init {
            requireNotNull(operationParam.name) {"@OperationParam must have a name defined" }
            this.parameterClass = determineParameterClass(parameter)
        }

        override fun type(): Type {
            return Type.OPERATION
        }

        override fun name(): String {
            return operationParam.name
        }

        override fun bind(parameters: IBaseParameters?): Any? {
            if (operationParam.min <= 0 && parameters == null) {
                return null
            } else requireNotNull(parameters) { "Parameter " + this.name() + " is required but was not provided" }

            val context = parameters.structureFhirVersionEnum.newContextCached()
            val terser = context.newTerser()
            val params = ParametersUtil.getNamedParameters(context, parameters, this.name())
            org.opencds.cqf.fhir.utility.Parameters.removeParameter(parameters, this.name())

            require(!(params.isEmpty() && operationParam.min > 0)) { "Parameter " + this.name() + " is required but was not provided" }

            require(params.size <= 1) {
                ("Parameter " + this.name()
                        + " has more than one value. Use parameter parts for multiple values")
            }

            // Not required and not present so we can return null
            if (params.isEmpty()) {
                return null
            }

            // Here, we need to get either the part, the resource, or the value[x] from the parameter
            // For list-valued parameters, we need to the value/resource from each part

            // First, let's check for parts. For lists this is correct.
            // For single-valued parameters, this is incorrect
            val parts = terser.getValues(params[0], "part")
            require(!(parameterClass != ParameterClass.LIST && parts.isNotEmpty())) { "Parameter " + this.name() + " is not the expected type " + parameter.type }

            if (parameterClass == ParameterClass.LIST) {
                val size = parts.size
                require(operationParam.min <= size) {
                    ("Parameter " + this.name()
                            + " has fewer values than the minimum of " + operationParam.min)
                }

                require(operationParam.max !in 1..<size) {
                    ("Parameter " + this.name()
                            + " has more values than the maximum of " + operationParam.max)
                }
            }

            if (parameterClass == ParameterClass.LIST) {
                val values = mutableListOf<IBase>()
                for (part in parts) {
                    val r = terser.getSingleValueOrNull(part, "resource", IBaseResource::class.java)
                    val v = terser.getSingleValueOrNull(part, "value[x]", IBase::class.java)

                    if (v != null) {
                        values.add(v)
                    } else if (r != null) {
                        values.add(r)
                    } else {
                        throw IllegalArgumentException(
                            "Parameter " + this.name() + " has an empty part. Expected a resource or value[x]"
                        )
                    }
                }

                return values
            }

            val param = params[0]
            val valueResource = terser.getSingleValueOrNull(param, "resource", IBaseResource::class.java)
            val value = terser.getSingleValueOrNull(param, "value[x]", IBase::class.java)
            require(!(valueResource != null && value != null)) { "Parameter " + this.name() + " has both a resource and a value. Only one is allowed" }

            if (parameterClass == ParameterClass.RESOURCE) {
                require(value == null) { "Parameter " + this.name() + " is not the expected type " + parameter.type }
                require(!(valueResource == null && operationParam.min > 0)) { "Parameter " + this.name() + " is required but was not provided" }
            }

            if (parameterClass == ParameterClass.VALUE) {
                require(valueResource == null) { "Parameter " + this.name() + " is not the expected type " + parameter.type }
                require(!(value == null && operationParam.min > 0)) { "Parameter " + this.name() + " is required but was not provided" }
            }

            val returnValue = valueResource ?: value
            if (returnValue == null) {
                return null
            }

            try {
                return parameter.type.cast(returnValue)
            } catch (e: ClassCastException) {
                throw IllegalArgumentException(
                    "Parameter value '" + this.name() + "'' is not of the expected type " + parameter.type
                )
            }
        }

        override fun parameter(): Parameter {
            return parameter
        }

        companion object {
            private fun determineParameterClass(parameter: Parameter): ParameterClass {
                val type = parameter.type
                return if (IBaseResource::class.java.isAssignableFrom(type)) {
                    ParameterClass.RESOURCE
                } else if (IBaseDatatype::class.java.isAssignableFrom(type)) {
                    ParameterClass.VALUE
                } else if (MutableList::class.java.isAssignableFrom(type)) {
                    ParameterClass.LIST
                } else {
                    throw IllegalArgumentException("Parameter annotated with @Operation must be a FHIR type or a List")
                }
            }
        }
    }

    class ExtraParamBinder(private val parameter: Parameter) : ParameterBinder {
        init {
            require(
                IBaseParameters::class.java.isAssignableFrom(parameter.type)) {
                "Parameter annotated with @ExtraParams must be of type IBaseParameters"
            }
        }

        override fun type(): Type {
            return Type.EXTRA
        }

        override fun name(): String {
            return "<extra>"
        }

        override fun parameter(): Parameter {
            return this.parameter
        }

        override fun bind(parameters: IBaseParameters?): Any? {
            if (parameters == null) {
                return null
            }

            val value = Resources.clone(parameters)

            // The `bind` API "consumes" input parameters. It's expected to
            // modify the input value. So, even though we're just cloning the
            // Resource here, we want to signal that we dealt with the input
            when(parameters) {
                is org.hl7.fhir.dstu2.model.Parameters -> parameters.parameter.clear()
                is org.hl7.fhir.dstu2016may.model.Parameters -> parameters.parameter.clear()
                is org.hl7.fhir.dstu3.model.Parameters -> parameters.parameter.clear()
                is org.hl7.fhir.r4.model.Parameters -> parameters.parameter.clear()
                is org.hl7.fhir.r4b.model.Parameters -> parameters.parameter.clear()
                is org.hl7.fhir.r5.model.Parameters -> parameters.parameter.clear()
            }

            return value
        }
    }

    companion object {
        // Ensures that the parameter annotations are valid for the operation
        // Requirements specific to a given parameter are checked in the ParameterBinder
        // This method checks for cross-parameter requirements
        fun validate(parameterBinders: List<ParameterBinder>): List<ParameterBinder> {
            val idParamCount =
                    parameterBinders.count { it.type() == Type.ID }
            require(idParamCount <= 1) { "Method cannot have more than one @IdParam" }
            require(!(idParamCount > 0 && parameterBinders[0].type() != Type.ID)) { "If @IdParam is present, it must be the first parameter" }

            val extraParamCount =
                parameterBinders.count { it.type() == Type.EXTRA }
            require(extraParamCount <= 1) { "Method cannot have more than one @ExtraParams" }
            require(
                !(extraParamCount > 0
                        && parameterBinders[parameterBinders.size - 1].type() != Type.EXTRA)
            ) { "If @ExtraParams is present, it must be the last parameter" }

            return parameterBinders
        }

        fun from(parameter: Parameter): ParameterBinder {
            val idParam = parameter.getAnnotation(IdParam::class.java)
            val operationParam = parameter.getAnnotation(OperationParam::class.java)
            val extraParam = parameter.getAnnotation(ExtraParams::class.java)

            ensureExactlyOneOf(idParam, operationParam, extraParam)

            return if (idParam != null) {
                IdParameterBinder(parameter)
            } else if (operationParam != null) {
                OperationParameterBinder(parameter, operationParam)
            } else {
                ExtraParamBinder(parameter)
            }
        }

        private fun ensureExactlyOneOf(idParam: IdParam?, operationParam: OperationParam?, extraParams: ExtraParams?) {
            val count = listOfNotNull(idParam, operationParam, extraParams).count()

            require(count != 0) { "Method Parameter must be annotated with @IdParam, @OperationParam, or @ExtraParams" }
            require(count <= 1) { "Method Parameter can only be annotated with one of @IdParam, @OperationParam, or @ExtraParams" }
        }
    }
}
