package org.opencds.cqf.fhir.cql.engine.parameters

import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.fhirpath.IFhirPath
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.cql.engine.execution.EvaluationResult
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter
import org.opencds.cqf.cql.engine.fhir.fhirModelNamespaceUri
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver
import org.opencds.cqf.cql.engine.runtime.Boolean
import org.opencds.cqf.cql.engine.runtime.ClassInstance
import org.opencds.cqf.cql.engine.runtime.Date
import org.opencds.cqf.cql.engine.runtime.DateTime
import org.opencds.cqf.cql.engine.runtime.Decimal
import org.opencds.cqf.cql.engine.runtime.Integer
import org.opencds.cqf.cql.engine.runtime.List
import org.opencds.cqf.cql.engine.runtime.NamedTypeValue
import org.opencds.cqf.cql.engine.runtime.String
import org.opencds.cqf.cql.engine.runtime.Tuple
import org.opencds.cqf.cql.engine.runtime.Value
import org.opencds.cqf.fhir.utility.FhirPathCache
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Converts both CQL parameters and CQL evaluation results into FHIR Parameters resources */
class CqlFhirParametersConverter(
    protected val fhirContext: FhirContext,
    protected val adapterFactory: IAdapterFactory,
    protected val fhirTypeConverter: FhirTypeConverter,
) {
    var logger: Logger = LoggerFactory.getLogger(CqlFhirParametersConverter::class.java)

    protected val modelResolver: FhirModelResolver<*, *, *, *, *, *, *, *> =
        FhirModelResolverCache.resolverForVersion(this.fhirContext.version.version)
    protected val fhirPath: IFhirPath = FhirPathCache.cachedForContext(fhirContext)

    fun toFhirParameters(evaluationResult: EvaluationResult): IBaseParameters {
        var params: IBaseParameters?
        try {
            params =
                this.fhirContext
                    .getResourceDefinition("Parameters")
                    .implementingClass
                    .getConstructor()
                    .newInstance() as IBaseParameters
        } catch (e: Exception) {
            logger.error("Error trying to create Parameters resource", e)
            throw RuntimeException(e)
        }

        val pa = this.adapterFactory.createParameters(params)

        for (entry in evaluationResult.expressionResults.entries) {
            val name = entry.key
            val value = entry.value.value

            if (value is List) {
                if (!value.iterator().hasNext()) {
                    // Empty list
                    val emptyListValue: IBaseBooleanDatatype =
                        emptyBooleanWithExtension(
                            fhirContext,
                            EMPTY_LIST_EXT_URL,
                            booleanType(fhirContext, true),
                        )
                    addPart(pa, name, emptyListValue)
                }
                for (o in value) {
                    this.addPart(pa, name, o)
                }
            } else {
                this.addPart(pa, name, value)
            }
        }

        return params
    }

    protected fun addPart(
        pa: IParametersAdapter,
        name: kotlin.String,
    ): IParametersParameterComponentAdapter {
        val ppca = pa.addParameter()
        ppca.setName(name)

        return ppca
    }

    protected fun addPart(pa: IParametersAdapter, name: kotlin.String, value: Any?) {
        var value = value
        if (value == null) {
            value =
                emptyBooleanWithExtension(
                    fhirContext,
                    DATA_ABSENT_REASON_EXT_URL,
                    codeType(fhirContext, DATA_ABSENT_REASON_UNKNOWN_CODE),
                )
        }

        value = convertToFhirIfNeeded(value)

        if (value is Tuple) {
            val ppca = this.addPart(pa, name)
            value.elements.forEach { (k, v) -> addSubPart(ppca, k, v) }

            return
        }

        if (value is Iterable<*>) {
            val ppca = this.addPart(pa, name)
            val values = value
            for (o in values) {
                this.addSubPart(ppca, "element", o)
            }

            return
        }

        if (this.fhirTypeConverter.isCqlType(value!!)) {
            value = this.fhirTypeConverter.toFhirType(value as Value)
        }

        if (value is IBaseDatatype) {
            val ppca = this.addPart(pa, name)
            ppca.setValue(value)
        } else if (value is IBaseBackboneElement) {
            // Likely already a parameter part
            val ppca = this.adapterFactory.createParametersParameter(value)
            ppca.setName(name)
            pa.addParameter(ppca.get())
        } else if (value is IBaseResource) {
            val ppca = this.addPart(pa, name)
            ppca.setResource(value)
        } else {
            throw IllegalArgumentException(
                "unknown type when trying to convert to parameters: ${value!!.javaClass.simpleName}"
            )
        }
    }

    protected fun addSubPart(
        ppcAdapter: IParametersParameterComponentAdapter,
        name: kotlin.String,
    ): IParametersParameterComponentAdapter {
        val ppca = ppcAdapter.addPart()
        ppca.setName(name)

        return ppca
    }

    protected fun addSubPart(
        ppcAdapter: IParametersParameterComponentAdapter,
        name: kotlin.String,
        value: Any?,
    ) {
        var value = value
        val ppca = this.addSubPart(ppcAdapter, name)

        if (value == null) {
            return
        }

        value = convertToFhirIfNeeded(value)

        if (value is Iterable<*>) {
            val values = value
            for (o in values) {
                this.addSubPart(ppca, "element", o)
            }

            return
        }

        if (this.fhirTypeConverter.isCqlType(value!!)) {
            value = this.fhirTypeConverter.toFhirType(value as Value)
        }

        if (value is IBaseDatatype) {
            ppca.setValue(value)
        } else if (value is IBaseResource) {
            ppca.setResource(value)
        } else {
            throw IllegalArgumentException(
                "unknown type when trying to convert to parameters: ${value!!.javaClass.simpleName}"
            )
        }
    }

    fun toCqlParameterDefinitions(
        parameters: IBaseParameters?
    ): MutableList<CqlParameterDefinition> {
        // This list needs to be mutable so that extra parameter definitions can be added if needed.
        val cqlParameterDefinitions = mutableListOf<CqlParameterDefinition>()
        if (parameters == null) {
            return cqlParameterDefinitions
        }

        val parametersAdapter = this.adapterFactory.createParameters(parameters)

        val children =
            parametersAdapter
                .getParameter()
                .filter { x -> x!!.getName() != null }
                .groupBy { obj -> obj.name }

        for (entry in children.entries) {
            // Meta data extension, if present
            val ext =
                entry.value
                    .filter { obj -> obj!!.hasExtension() }
                    .flatMap { x -> x!!.getExtension<IBaseExtension<*, *>?>() }
                    .firstOrNull { x ->
                        x!!.url != null &&
                            (x.url ==
                                "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition")
                    }

            // Actual values. if present
            val values = entry.value.mapNotNull { ppca -> this.convertToCql(ppca!!) }

            val name = entry.key

            var isList: kotlin.Boolean? = null
            if (ext != null) {
                isList = this.isListType(ext)
            }

            // Unable to determine via the extension
            // So infer based on the values.
            if (isList == null) {
                require(values.isNotEmpty()) {
                    "Unable to determine if parameter ${entry.key} is meant to be collection. Use the http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition extension to specify metadata."
                }
                isList = values.size != 1
            }

            require(!(!isList && entry.value.size > 1)) {
                "The parameter ${entry.key} was defined as a single value but multiple values were passed"
            }

            var type: kotlin.String? = null
            if (ext != null) {
                type = this.getType(ext)
            }

            // TODO: This breaks down a bit for CQL System types because they aren't prefixed.
            if (type == null && values.isNotEmpty()) {
                val firstValue = values[0]
                if (firstValue is ClassInstance) {
                    type = "FHIR." + firstValue.type.localPart
                } else {
                    type = firstValue.javaClass.simpleName
                }
            }

            requireNotNull(type) {
                "Unable to infer type for parameter ${entry.key}. Use the http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition extension to specify metadata."
            }

            var value: Value? = null
            if (isList) {
                value = List(values)
            } else if (values.isNotEmpty()) {
                value = values[0]
            }

            cqlParameterDefinitions.add(CqlParameterDefinition(name, type, isList, value))
        }

        return cqlParameterDefinitions
    }

    fun toCqlParameters(parameters: IBaseParameters?): MutableMap<kotlin.String, Value?> {
        val parameterMap = mutableMapOf<kotlin.String, Value?>()
        val cqlParameterDefinitions = this.toCqlParameterDefinitions(parameters)
        if (cqlParameterDefinitions.isEmpty()) {
            return parameterMap
        }

        for (def in cqlParameterDefinitions) {
            parameterMap[def.name] = def.value
        }

        return parameterMap
    }

    fun toCqlParameters(
        parameters: MutableMap<kotlin.String, Any?>
    ): MutableMap<kotlin.String, Value?> {
        val parameterMap = mutableMapOf<kotlin.String, Value?>()
        parameters.forEach { (k, v) ->
            val className = v!!.javaClass.name
            val value: Value?
            if (v is MutableList<*>) {
                value = List(v.map { value -> this.convertToCqlIfNeeded(value!!) })
            } else if (className.contains("org.hl7.fhir") && className.contains("Tuple")) {
                val elements =
                    adapterFactory
                        .createTuple(v as IBase)
                        .getProperties()
                        .mapValues { entry ->
                            val listValue =
                                (entry.value as MutableList<*>).map { e ->
                                    modelResolver.toCqlValue(e, false)
                                }
                            val cqlValue =
                                if (listValue.size != 1) List(listValue) else listValue[0]
                            cqlValue
                        }
                        .toMutableMap()
                value = Tuple().withElements(elements)
            } else {
                value = convertToCqlIfNeeded(v)
            }
            parameterMap[k] = value
        }
        return parameterMap
    }

    private fun convertToCqlIfNeeded(value: Any): Value? {
        val className = value.javaClass.name
        return if (className.contains("org.hl7.fhir")) modelResolver.toCqlValue(value, false)
        else value as Value
    }

    private fun getType(parameterDefinitionExtension: IBaseExtension<*, *>): kotlin.String? {
        val type =
            this.fhirPath.evaluateFirst(
                parameterDefinitionExtension.value,
                "type",
                IPrimitiveType::class.java,
            )
        return type.map { obj -> obj.valueAsString }.orElse(null)
    }

    private fun isListType(parameterDefinitionExtension: IBaseExtension<*, *>): kotlin.Boolean {
        val max =
            this.fhirPath.evaluateFirst(
                parameterDefinitionExtension.value,
                "max",
                IPrimitiveType::class.java,
            )
        if (max.isPresent) {
            val maxString = max.get().valueAsString

            return maxString != "1"
        }

        val min =
            this.fhirPath.evaluateFirst(
                parameterDefinitionExtension.value,
                "min",
                IBaseIntegerDatatype::class.java,
            )
        return min.filter { iBaseIntegerDatatype -> iBaseIntegerDatatype.value > 1 }.isPresent
    }

    fun convertToFhirIfNeeded(value: Any?): Any? {
        return if (value is ClassInstance && value.type.namespaceURI == fhirModelNamespaceUri)
            toFhirValue(value)
        else value
    }

    fun toFhirValue(value: Value?): IBase {
        return toFhirValue(value, null)
    }

    fun toFhirValue(valueToConvert: Value?, parentName: kotlin.String?): IBase {
        var clazz: Class<*>?
        val typeName: kotlin.String
        if (valueToConvert is ClassInstance) {
            typeName = valueToConvert.type.localPart
            clazz = modelResolver.resolveType(typeName)
        } else if (valueToConvert is NamedTypeValue) {
            typeName = valueToConvert.type.localPart
            clazz = modelResolver.resolveType(typeName)
        } else {
            typeName = valueToConvert!!.typeAsString
            clazz = null
        }
        requireNotNull(clazz) { "Could not resolve FHIR type: $typeName" }
        if (
            !parentName.isNullOrBlank() &&
                !clazz.isEnum &&
                clazz.name.contains("$") &&
                (clazz.enclosingClass.simpleName != parentName)
        ) {
            val correctClassName = clazz.name.replace(clazz.enclosingClass.simpleName, parentName)
            try {
                clazz = Class.forName(correctClassName)
            } catch (e: ClassNotFoundException) {
                throw IllegalArgumentException("Could not resolve inner FHIR type: $typeName")
            }
        }

        val instance: IBase
        try {
            if (clazz.isEnum) {
                instance = modelResolver.createHapiInstance(typeName) as IBase
            } else {
                instance = clazz.getDeclaredConstructor().newInstance() as IBase
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Could not create instance of $typeName", e)
        }

        if (instance is IBaseEnumeration<*>) {
            if (valueToConvert is ClassInstance) {
                val enumValue = valueToConvert["value"]
                if (enumValue is String) {
                    instance.valueAsString = enumValue.value
                }
            }
            return instance
        }

        if (instance is IPrimitiveType<*>) {
            val primitiveValue =
                if (valueToConvert is ClassInstance) valueToConvert["value"] else valueToConvert
            if (primitiveValue is DateTime) {
                modelResolver.setPrimitiveValue(primitiveValue, instance)
            } else if (primitiveValue is Date) {
                modelResolver.setPrimitiveValue(primitiveValue, instance)
            } else if (primitiveValue is Boolean) {
                modelResolver.setPrimitiveValue(primitiveValue.value, instance)
            } else if (primitiveValue is Integer) {
                modelResolver.setPrimitiveValue(primitiveValue.value, instance)
            } else if (primitiveValue is Decimal) {
                modelResolver.setPrimitiveValue(primitiveValue.value, instance)
            } else if (primitiveValue is String) {
                modelResolver.setPrimitiveValue(primitiveValue.value, instance)
            } else if (primitiveValue != null) {
                modelResolver.setPrimitiveValue(primitiveValue.toString(), instance)
            }
            return instance
        }

        val ibaseClazz = clazz as Class<out IBase?>
        var definition =
            fhirContext.getElementDefinition(ibaseClazz)
                as BaseRuntimeElementCompositeDefinition<*>?
        if (definition == null) {
            val resourceClazz = clazz as Class<out IBaseResource?>
            definition = fhirContext.getResourceDefinition(resourceClazz)
        }

        for (child in definition.getChildren()) {
            val elementValue = (valueToConvert as ClassInstance)[child.elementName]
            if (elementValue == null) {
                continue
            }
            if (elementValue is List) {
                for (item in elementValue) {
                    child.mutator.addValue(instance, toFhirValue(item!!, typeName))
                }
            } else {
                child.mutator.addValue(instance, toFhirValue(elementValue, typeName))
            }
        }
        return instance
    }

    private fun convertToCql(ppca: IParametersParameterComponentAdapter): Value? {
        if (ppca.hasValue()) {
            return this.fhirTypeConverter.toCqlType(ppca.getValue()) as Value?
        } else if (ppca.hasResource()) {
            return modelResolver.toCqlValue(ppca.getResource(), false)
        } else if (ppca.hasPart()) {
            logger.debug("Ignored {} parameter sub-parts", ppca.getPart().size)
        }

        return null
    }

    companion object {
        // This is basically a copy and paste from R4FhirTypeConverter, but it's not exposed.
        const val EMPTY_LIST_EXT_URL: kotlin.String =
            "http://hl7.org/fhir/StructureDefinition/cqf-isEmptyList"
        const val DATA_ABSENT_REASON_EXT_URL: kotlin.String =
            "http://hl7.org/fhir/StructureDefinition/data-absent-reason"
        const val DATA_ABSENT_REASON_UNKNOWN_CODE: kotlin.String = "unknown"

        private fun booleanType(
            context: FhirContext,
            value: kotlin.Boolean?,
        ): IBaseBooleanDatatype {
            try {
                val booleanElementDef = context.getElementDefinition("Boolean")
                if (booleanElementDef == null) {
                    throw InternalErrorException("Unable to get definition for Boolean element")
                }
                return booleanElementDef.newInstance(value) as IBaseBooleanDatatype
            } catch (e: Exception) {
                throw InternalErrorException("error creating BooleanType", e)
            }
        }

        private fun codeType(context: FhirContext, value: kotlin.String?): IBaseDatatype {
            try {
                val codeElementDef = context.getElementDefinition("Code")
                if (codeElementDef == null) {
                    throw InternalErrorException("Unable to get definition for Code element")
                }
                return codeElementDef.newInstance(value) as IBaseDatatype
            } catch (e: Exception) {
                throw InternalErrorException("error creating CodeType", e)
            }
        }

        private fun emptyBooleanWithExtension(
            context: FhirContext,
            url: kotlin.String?,
            value: IBaseDatatype?,
        ): IBaseBooleanDatatype {
            val result: IBaseBooleanDatatype = booleanType(context, null)
            val ext = (result as IBaseHasExtensions).addExtension()
            ext.setUrl(url)
            ext.setValue(value)
            return result
        }
    }
}
