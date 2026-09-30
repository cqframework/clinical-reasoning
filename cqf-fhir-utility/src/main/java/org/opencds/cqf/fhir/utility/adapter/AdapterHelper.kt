package org.opencds.cqf.fhir.utility.adapter

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.opencds.cqf.fhir.utility.Resources.newBaseForVersion

object AdapterHelper {
    private val CAST_ERROR_MESSAGE = { t1: String, t2: String ->
        "Cannot cast a value of type $t1 as $t2."
    }
    private const val CANONICAL = "CanonicalType"
    private const val CODE = "Code"
    private const val CODEABLECONCEPT = "CodeableConcept"
    private const val PRIMITIVE = "IPrimitiveType"
    private const val URI = "UriType"

    fun `as`(fhirVersion: FhirVersionEnum, value: Any?, type: Class<*>): Any? {
        if (value == null) {
            return null
        }

        if (value.javaClass.simpleName == "Tuple") {
            val adapterFactory: IAdapterFactory = IAdapterFactory.forFhirVersion(fhirVersion)
            val tupleAdapter = adapterFactory.createTuple(value as IBase)
            val result = adapterFactory.createBase(newBaseForVersion(type.simpleName, fhirVersion))
            tupleAdapter.properties.forEach { (path, value) -> result.setValue(path, value) }
            return result.get()
        }

        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 -> asDstu3(value, type)
            FhirVersionEnum.R4 -> asR4(value, type)
            FhirVersionEnum.R5 -> asR5(value, type)
            else -> null
        }
    }

    private fun asDstu3(value: Any?, type: Class<*>): Any? {
        if (value == null) {
            return null
        }

        if (type.isAssignableFrom(value.javaClass)) {
            return value
        }

        if (value is org.hl7.fhir.dstu3.model.UriType) {
            when (type.simpleName) {
                "AnnotatedUuidType",
                "UuidType" ->
                    return if (value.hasPrimitiveValue() && value.value.startsWith("urn:uuid:"))
                        org.hl7.fhir.dstu3.model.UuidType(value.primitiveValue())
                    else null

                "OidType" ->
                    return if (value.hasPrimitiveValue() && value.value.startsWith("urn:oid:"))
                        org.hl7.fhir.dstu3.model.OidType(value.primitiveValue())
                    else null

                else -> {}
            }
        }

        if (value is org.hl7.fhir.dstu3.model.IntegerType) {
            when (type.simpleName) {
                "PositiveIntType" ->
                    return if (value.hasPrimitiveValue() && value.value > 0)
                        org.hl7.fhir.dstu3.model.PositiveIntType(value.primitiveValue())
                    else null

                "UnsignedIntType" ->
                    return if (value.hasPrimitiveValue() && value.value >= 0)
                        org.hl7.fhir.dstu3.model.UnsignedIntType(value.primitiveValue())
                    else null

                else -> {}
            }
        }

        if (value is org.hl7.fhir.dstu3.model.StringType) {
            when (type.simpleName) {
                "CodeType" -> return value.castToCode(value)
                "MarkdownType" -> return value.castToMarkdown(value)
                "IdType" ->
                    return if (value.hasPrimitiveValue())
                        org.hl7.fhir.dstu3.model.IdType(value.primitiveValue())
                    else null

                CODEABLECONCEPT -> return value.castToCodeableConcept(value.castToCode(value))
                URI -> return value.castToUri(value)
                else -> {}
            }
        }

        if (value is org.hl7.fhir.dstu3.model.Coding) {
            when (type.simpleName) {
                CODE,
                PRIMITIVE -> return value.codeElement
                else -> {}
            }
        }

        if (value is org.hl7.fhir.dstu3.model.Quantity) {
            when (type.simpleName) {
                "Age" -> {
                    val age = org.hl7.fhir.dstu3.model.Age()
                    age.setValue(value.value)
                    age.setCode(value.code)
                    age.setUnit(value.unit)
                    age.setSystem(value.system)
                    age.setComparator(value.comparator)
                    return age
                }

                "Distance" -> {
                    val distance = org.hl7.fhir.dstu3.model.Distance()
                    distance.setValue(value.value)
                    distance.setCode(value.code)
                    distance.setUnit(value.unit)
                    distance.setSystem(value.system)
                    distance.setComparator(value.comparator)
                    return distance
                }

                "Duration" -> {
                    val duration = org.hl7.fhir.dstu3.model.Duration()
                    duration.setValue(value.value)
                    duration.setCode(value.code)
                    duration.setUnit(value.unit)
                    duration.setSystem(value.system)
                    duration.setComparator(value.comparator)
                    return duration
                }

                "Count" -> {
                    val count = org.hl7.fhir.dstu3.model.Count()
                    count.setValue(value.value)
                    count.setCode(value.code)
                    count.setUnit(value.unit)
                    count.setSystem(value.system)
                    count.setComparator(value.comparator)
                    return count
                }

                "SimpleQuantity" -> return value.castToSimpleQuantity(value)
                else -> {}
            }
        }

        throw IllegalArgumentException(CAST_ERROR_MESSAGE(value.javaClass.name, type.name))
    }

    private fun asR4(value: Any?, type: Class<*>): Any? {
        if (value == null) {
            return null
        }

        if (type.isAssignableFrom(value.javaClass)) {
            return value
        }

        if (value is org.hl7.fhir.r4.model.UriType) {
            when (type.simpleName) {
                "UrlType" -> return value.castToUrl(value)
                "CanonicalType" -> return value.castToCanonical(value)
                "AnnotatedUuidType",
                "UuidType" ->
                    return if (value.hasPrimitiveValue() && value.value.startsWith("urn:uuid:"))
                        org.hl7.fhir.r4.model.UuidType(value.primitiveValue())
                    else null

                "OidType" ->
                    return if (value.hasPrimitiveValue() && value.value.startsWith("urn:oid:"))
                        org.hl7.fhir.r4.model.OidType(value.primitiveValue())
                    else null

                else -> {}
            }
        }

        if (value is org.hl7.fhir.r4.model.IntegerType) {
            when (type.simpleName) {
                "PositiveIntType" ->
                    return if (value.hasPrimitiveValue() && value.value > 0)
                        org.hl7.fhir.r4.model.PositiveIntType(value.primitiveValue())
                    else null

                "UnsignedIntType" ->
                    return if (value.hasPrimitiveValue() && value.value >= 0)
                        org.hl7.fhir.r4.model.UnsignedIntType(value.primitiveValue())
                    else null

                else -> {}
            }
        }

        if (value is org.hl7.fhir.r4.model.StringType) {
            when (type.simpleName) {
                "CodeType" -> return value.castToCode(value)
                "MarkdownType" -> return value.castToMarkdown(value)
                "IdType" ->
                    return if (value.hasPrimitiveValue())
                        org.hl7.fhir.r4.model.IdType(value.primitiveValue())
                    else null

                CODEABLECONCEPT -> return value.castToCodeableConcept(value.castToCode(value))
                CANONICAL -> return value.castToCanonical(value)
                URI -> return value.castToUri(value)
                else -> {}
            }
        }

        if (value is org.hl7.fhir.r4.model.Coding) {
            when (type.simpleName) {
                CODE,
                PRIMITIVE -> return value.codeElement
                else -> {}
            }
        }

        if (value is org.hl7.fhir.r4.model.Quantity) {
            when (type.simpleName) {
                "Age" -> {
                    val age = org.hl7.fhir.r4.model.Age()
                    age.setValue(value.value)
                    age.setCode(value.code)
                    age.setUnit(value.unit)
                    age.setSystem(value.system)
                    age.setComparator(value.comparator)
                    return age
                }

                "Distance" -> {
                    val distance = org.hl7.fhir.r4.model.Distance()
                    distance.setValue(value.value)
                    distance.setCode(value.code)
                    distance.setUnit(value.unit)
                    distance.setSystem(value.system)
                    distance.setComparator(value.comparator)
                    return distance
                }

                "Duration" -> {
                    val duration = org.hl7.fhir.r4.model.Duration()
                    duration.setValue(value.value)
                    duration.setCode(value.code)
                    duration.setUnit(value.unit)
                    duration.setSystem(value.system)
                    duration.setComparator(value.comparator)
                    return duration
                }

                "Count" -> {
                    val count = org.hl7.fhir.r4.model.Count()
                    count.setValue(value.value)
                    count.setCode(value.code)
                    count.setUnit(value.unit)
                    count.setSystem(value.system)
                    count.setComparator(value.comparator)
                    return count
                }

                "SimpleQuantity" -> return value.castToSimpleQuantity(value)
                "MoneyQuantity" -> {
                    val moneyQuantity = org.hl7.fhir.r4.model.MoneyQuantity()
                    moneyQuantity.setValue(value.value)
                    moneyQuantity.setCode(value.code)
                    moneyQuantity.setUnit(value.unit)
                    moneyQuantity.setSystem(value.system)
                    moneyQuantity.setComparator(value.comparator)
                    return moneyQuantity
                }

                else -> {}
            }
        }

        throw IllegalArgumentException(CAST_ERROR_MESSAGE(value.javaClass.name, type.name))
    }

    private fun asR5(value: Any?, type: Class<*>): Any? {
        if (value == null) {
            return null
        }

        if (type.isAssignableFrom(value.javaClass)) {
            return value
        }

        if (value is org.hl7.fhir.r5.model.UriType) {
            when (type.simpleName) {
                "UrlType" ->
                    return if (value.hasPrimitiveValue())
                        org.hl7.fhir.r5.model.UrlType(value.primitiveValue())
                    else null

                "CanonicalType" ->
                    return if (value.hasPrimitiveValue())
                        org.hl7.fhir.r5.model.CanonicalType(value.primitiveValue())
                    else null

                "AnnotatedUuidType",
                "UuidType" ->
                    return if (value.hasPrimitiveValue() && value.value.startsWith("urn:uuid:"))
                        org.hl7.fhir.r5.model.UuidType(value.primitiveValue())
                    else null

                "OidType" ->
                    return if (value.hasPrimitiveValue() && value.value.startsWith("urn:oid:"))
                        org.hl7.fhir.r5.model.OidType(value.primitiveValue())
                    else null

                else -> {}
            }
        }

        if (value is org.hl7.fhir.r5.model.IntegerType) {
            when (type.simpleName) {
                "PositiveIntType" ->
                    return if (value.hasPrimitiveValue() && value.value > 0)
                        org.hl7.fhir.r5.model.PositiveIntType(value.primitiveValue())
                    else null

                "UnsignedIntType" ->
                    return if (value.hasPrimitiveValue() && value.value >= 0)
                        org.hl7.fhir.r5.model.UnsignedIntType(value.primitiveValue())
                    else null

                else -> {}
            }
        }

        if (value is org.hl7.fhir.r5.model.StringType) {
            when (type.simpleName) {
                "CodeType" ->
                    return if (value.hasPrimitiveValue())
                        org.hl7.fhir.r5.model.CodeType(value.primitiveValue())
                    else null

                "MarkdownType" ->
                    return if (value.hasPrimitiveValue())
                        org.hl7.fhir.r5.model.MarkdownType(value.primitiveValue())
                    else null

                "IdType" ->
                    return if (value.hasPrimitiveValue())
                        org.hl7.fhir.r5.model.IdType(value.primitiveValue())
                    else null

                CODEABLECONCEPT ->
                    return org.hl7.fhir.r5.model.CodeableConcept(
                        org.hl7.fhir.r5.model.Coding(null, value.asStringValue(), null)
                    )

                CANONICAL -> return org.hl7.fhir.r5.model.CanonicalType(value.asStringValue())
                URI -> return org.hl7.fhir.r5.model.UriType(value.asStringValue())
                else -> {}
            }
        }

        if (value is org.hl7.fhir.r5.model.Coding) {
            when (type.simpleName) {
                CODE,
                PRIMITIVE -> return value.codeElement
                else -> {}
            }
        }

        if (value is org.hl7.fhir.r5.model.Quantity) {
            when (type.simpleName) {
                "Age" -> {
                    val age = org.hl7.fhir.r5.model.Age()
                    age.setValue(value.value)
                    age.setCode(value.code)
                    age.setUnit(value.unit)
                    age.setSystem(value.system)
                    age.setComparator(value.comparator)
                    return age
                }

                "Distance" -> {
                    val distance = org.hl7.fhir.r5.model.Distance()
                    distance.setValue(value.value)
                    distance.setCode(value.code)
                    distance.setUnit(value.unit)
                    distance.setSystem(value.system)
                    distance.setComparator(value.comparator)
                    return distance
                }

                "Duration" -> {
                    val duration = org.hl7.fhir.r5.model.Duration()
                    duration.setValue(value.value)
                    duration.setCode(value.code)
                    duration.setUnit(value.unit)
                    duration.setSystem(value.system)
                    duration.setComparator(value.comparator)
                    return duration
                }

                "Count" -> {
                    val count = org.hl7.fhir.r5.model.Count()
                    count.setValue(value.value)
                    count.setCode(value.code)
                    count.setUnit(value.unit)
                    count.setSystem(value.system)
                    count.setComparator(value.comparator)
                    return count
                }

                "SimpleQuantity" ->
                    return org.hl7.fhir.r5.model.TypeConvertor.castToSimpleQuantity(value)
                "MoneyQuantity" -> {
                    val moneyQuantity = org.hl7.fhir.r5.model.MoneyQuantity()
                    moneyQuantity.setValue(value.value)
                    moneyQuantity.setCode(value.code)
                    moneyQuantity.setUnit(value.unit)
                    moneyQuantity.setSystem(value.system)
                    moneyQuantity.setComparator(value.comparator)
                    return moneyQuantity
                }

                else -> {}
            }
        }

        throw IllegalArgumentException(CAST_ERROR_MESSAGE(value.javaClass.name, type.name))
    }
}
