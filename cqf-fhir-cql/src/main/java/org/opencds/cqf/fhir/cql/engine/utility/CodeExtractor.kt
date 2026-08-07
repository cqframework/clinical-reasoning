package org.opencds.cqf.fhir.cql.engine.utility

import ca.uhn.fhir.context.BaseRuntimeChildDefinition
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.RuntimeCompositeDatatypeDefinition
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseEnumeration
import org.hl7.fhir.instance.model.api.IPrimitiveType
import org.opencds.cqf.cql.engine.runtime.Code

class CodeExtractor(fhirContext: FhirContext) {
    private val conceptDefinition =
        fhirContext.getElementDefinition("CodeableConcept") as RuntimeCompositeDatatypeDefinition
    private val codingDefinition =
        fhirContext.getElementDefinition("Coding") as RuntimeCompositeDatatypeDefinition

    private val conceptCodingChild: BaseRuntimeChildDefinition =
        conceptDefinition.getChildByName("coding")

    private val versionDefinition: BaseRuntimeChildDefinition =
        codingDefinition.getChildByName("version")
    private val codeDefinition: BaseRuntimeChildDefinition = codingDefinition.getChildByName("code")
    private val systemDefinition: BaseRuntimeChildDefinition =
        codingDefinition.getChildByName("system")
    private val displayDefinition: BaseRuntimeChildDefinition =
        codingDefinition.getChildByName("display")

    fun getElmCodesFromObject(`object`: Any?): List<Code> {
        val codes = mutableListOf<Code>()
        if (`object` is Iterable<*>) {
            for (innerObject in `object`) {
                val elmCodes = getElmCodesFromObject(innerObject)
                codes.addAll(elmCodes)
            }
        } else {
            val elmCodes = getElmCodesFromObjectInner(`object`)
            codes.addAll(elmCodes)
        }
        return codes
    }

    private fun getElmCodesFromObjectInner(`object`: Any?): List<Code> {
        val codes = mutableListOf<Code>()
        if (`object` == null) {
            return codes
        } else if (`object` is IBase) {
            val innerCodes = getCodesFromBase(`object`)
            if (innerCodes != null) {
                codes.addAll(innerCodes)
            }
        } else if (`object` is Code) {
            codes.add(`object`)
        } else {
            throw IllegalArgumentException("Unable to extract codes from object $`object`")
        }

        return codes
    }

    private fun getCodesFromBase(`object`: IBase): List<Code>? {
        if (`object` is IBaseEnumeration<*>) {
            @Suppress("UNCHECKED_CAST") val enumeration = (`object` as IBaseEnumeration<Enum<*>?>)
            return this.getCodeFromEnumeration(enumeration)
        } else if (`object`.fhirType() == "CodeableConcept") {
            return this.getCodesInConcept(`object`)
        } else if (`object`.fhirType() == "Coding") {
            return this.generateCodes(mutableListOf(`object`))
        }

        throw IllegalArgumentException(
            "Unable to extract codes from fhirType ${`object`.fhirType()}"
        )
    }

    private fun getCodeFromEnumeration(
        enumeration: IBaseEnumeration<Enum<*>?>?
    ): MutableList<Code> {
        val codes = mutableListOf<Code>()
        if (enumeration == null) {
            return codes
        }

        val enumFactory = enumeration.getEnumFactory()

        val system = enumFactory.toSystem(enumeration.getValue())
        val codeAsString = enumFactory.toCode(enumeration.getValue())
        if (!system.isNullOrEmpty() && !codeAsString.isNullOrEmpty()) {
            val code = Code()
            code.code = codeAsString
            code.system = system
            codes.add(code)
        }

        return codes
    }

    private fun getCodesInConcept(`object`: IBase?): MutableList<Code>? {
        val codingObjects = getCodingObjects(`object`) ?: return null
        return generateCodes(codingObjects)
    }

    private fun generateCodes(codingObjects: List<IBase?>): MutableList<Code> {
        val codes = mutableListOf<Code>()
        for (coding in codingObjects) {
            val code = getStringValueFromPrimitiveDefinition(this.codeDefinition, coding)
            val display = getStringValueFromPrimitiveDefinition(this.displayDefinition, coding)
            val system = getStringValueFromPrimitiveDefinition(this.systemDefinition, coding)
            val version = getStringValueFromPrimitiveDefinition(this.versionDefinition, coding)
            codes.add(
                Code().withSystem(system).withCode(code).withDisplay(display).withVersion(version)
            )
        }
        return codes
    }

    private fun getCodingObjects(`object`: IBase?): List<IBase?>? {
        var codingObject: MutableList<IBase?>? = null
        try {
            codingObject = this.conceptCodingChild.accessor.getValues(`object`)
        } catch (e: Exception) {
            // TODO: handle exception
        }
        return codingObject
    }

    private fun getStringValueFromPrimitiveDefinition(
        definition: BaseRuntimeChildDefinition,
        value: IBase?,
    ): String? {
        val accessor = definition.accessor
        if (value == null || accessor == null) {
            return null
        }

        val values = accessor.getValues(value)
        if (values.isNullOrEmpty()) {
            return null
        }

        require(values.size <= 1) {
            "More than one value returned while attempting to access primitive value."
        }

        val baseValue = values[0]

        require(baseValue is IPrimitiveType<*>) {
            "Non-primitive value encountered while trying to access primitive value."
        }
        return baseValue.valueAsString
    }
}
