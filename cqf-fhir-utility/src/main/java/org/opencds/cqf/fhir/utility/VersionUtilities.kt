package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirVersionEnum
import java.math.BigDecimal
import java.util.Date
import org.hl7.fhir.instance.model.api.IAnyResource
import org.hl7.fhir.instance.model.api.IBaseReference
import org.hl7.fhir.instance.model.api.IPrimitiveType

object VersionUtilities {
    private const val UNSUPPORTED = "unknown or unsupported FHIR version"

    /**
     * Returns a FhirVersionEnum for the supplied version string. Supports partial versions (e.g.
     * "3.0") and named versions (e.g. "R4")
     *
     * @param fhirVersion the FHIR version to get an enum for.
     * @return the FhirVersionEnum
     */
    @JvmStatic
    fun enumForVersion(fhirVersion: String): FhirVersionEnum {
        require(fhirVersion.isNotEmpty()) { "fhirVersion can not be empty" }

        // This matches "R4", "dstu3", etc.
        try {
            return FhirVersionEnum.valueOf(fhirVersion.uppercase())
        } catch (e: Exception) {
            // intentionally empty, we want to fall through
        }

        // This matches specific FHIR versions that match the structure versions
        // e.g. 4.0.1, 3.0.2, etc, including partials.
        val version = FhirVersionEnum.forVersionString(fhirVersion)
        if (version != null) {
            return version
        }

        // This returns the closest matching major version
        return when (fhirVersion.substring(0, 1)) {
            "2" -> FhirVersionEnum.DSTU2
            "3" -> FhirVersionEnum.DSTU3
            "4" -> FhirVersionEnum.R4
            "5" -> FhirVersionEnum.R5
            else -> throw IllegalArgumentException(UNSUPPORTED)
        }
    }

    /**
     * Returns a StringType for the supplied FHIR version with a value of the supplied string.
     *
     * @param fhirVersion the FHIR version to create a StringType for
     * @param string the string value of the StringType
     * @return the new StringType
     */
    @JvmStatic
    @JvmOverloads
    fun stringTypeForVersion(
        fhirVersion: FhirVersionEnum,
        string: String? = null,
    ): IPrimitiveType<String?> {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU2 -> org.hl7.fhir.dstu2.model.StringType(string)
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.StringType(string)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.StringType(string)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.StringType(string)
            else -> throw IllegalArgumentException(UNSUPPORTED)
        }
    }

    /**
     * Returns a UriType for the supplied FHIR version with a value of the supplied uri.
     *
     * @param fhirVersion the FHIR version to create a UriType for
     * @param uri the string value of the UriType
     * @return the new UriType
     */
    @JvmStatic
    @JvmOverloads
    fun uriTypeForVersion(
        fhirVersion: FhirVersionEnum,
        uri: String? = null,
    ): IPrimitiveType<String?> {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU2 -> org.hl7.fhir.dstu2.model.UriType(uri)
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.UriType(uri)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.UriType(uri)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.UriType(uri)
            else -> throw IllegalArgumentException(UNSUPPORTED)
        }
    }

    /**
     * Returns a CanonicalType for the supplied version with a value of the supplied value. A
     * UriType will be returned for FHIR versions before R4.
     *
     * @param fhirVersion the FHIR version to create a CanonicalType for
     * @param value the string value of the CanonicalType
     * @return the new CanonicalType
     */
    @JvmStatic
    @JvmOverloads
    fun canonicalTypeForVersion(
        fhirVersion: FhirVersionEnum,
        value: String? = null,
    ): IPrimitiveType<String?> {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU2 -> org.hl7.fhir.dstu2.model.UriType(value)
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.UriType(value)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.CanonicalType(value)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.CanonicalType(value)
            else -> throw IllegalArgumentException(UNSUPPORTED)
        }
    }

    /**
     * Returns a BooleanType for the supplied version with a value of the supplied value.
     *
     * @param fhirVersion the FHIR version to create a BooleanType for
     * @param value the value of the BooleanType
     * @return the new BooleanType
     */
    @JvmStatic
    fun booleanTypeForVersion(
        fhirVersion: FhirVersionEnum,
        value: Boolean,
    ): IPrimitiveType<Boolean> {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU2 -> org.hl7.fhir.dstu2.model.BooleanType(value)
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.BooleanType(value)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.BooleanType(value)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.BooleanType(value)
            else -> throw IllegalArgumentException(UNSUPPORTED)
        }
    }

    /**
     * Returns a CodeType for the supplied version with a value of the supplied code.
     *
     * @param fhirVersion the FHIR version to create a CodeType for
     * @param code the string value of the CodeType
     * @return the new CodeType
     */
    @JvmStatic
    fun codeTypeForVersion(fhirVersion: FhirVersionEnum, code: String?): IPrimitiveType<String?> {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU2 -> org.hl7.fhir.dstu2.model.CodeType(code)
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.CodeType(code)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.CodeType(code)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.CodeType(code)
            else -> throw IllegalArgumentException(UNSUPPORTED)
        }
    }

    /**
     * Returns a Reference for the supplied version with a value of the supplied value.
     *
     * @param fhirVersion the FHIR version to create a Reference for
     * @param value the string value of the Reference
     * @return the new Reference
     */
    @JvmStatic
    fun referenceTypeForVersion(fhirVersion: FhirVersionEnum, value: String?): IBaseReference {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU2 -> org.hl7.fhir.dstu2.model.Reference().setReference(value)
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.Reference().setReference(value)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.Reference().setReference(value)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.Reference().setReference(value)
            else -> throw IllegalArgumentException(UNSUPPORTED)
        }
    }

    /**
     * Returns a Reference for the supplied version with a value of the supplied value.
     *
     * @param fhirVersion the FHIR version to create a Reference for
     * @param value the resource value of the Reference
     * @return the new Reference
     */
    @JvmStatic
    fun referenceTypeForVersion(
        fhirVersion: FhirVersionEnum,
        value: IAnyResource?,
    ): IBaseReference {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU2 -> org.hl7.fhir.dstu2.model.Reference(value)
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.Reference(value)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.Reference(value)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.Reference(value)
            else -> throw java.lang.IllegalArgumentException(UNSUPPORTED)
        }
    }

    /**
     * Returns an IntegerType for the supplied version with a value of the supplied value.
     *
     * @param fhirVersion the FHIR version to create a IntegerType for
     * @param value the integer value of the IntegerType
     * @return the new IntegerType
     */
    @JvmStatic
    fun integerTypeForVersion(fhirVersion: FhirVersionEnum, value: Int): IPrimitiveType<Int?> {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU2 -> org.hl7.fhir.dstu2.model.IntegerType(value)
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.IntegerType(value)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.IntegerType(value)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.IntegerType(value)
            else -> throw java.lang.IllegalArgumentException(UNSUPPORTED)
        }
    }

    /**
     * Returns a DecimalType for the supplied version with a value of the supplied value.
     *
     * @param fhirVersion the FHIR version to create a DecimalType for
     * @param value the decimal value of the DecimalType
     * @return the new DecimalType
     */
    @JvmStatic
    fun decimalTypeForVersion(
        fhirVersion: FhirVersionEnum,
        value: BigDecimal?,
    ): IPrimitiveType<BigDecimal?> {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU2 -> org.hl7.fhir.dstu2.model.DecimalType(value)
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.DecimalType(value)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.DecimalType(value)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.DecimalType(value)
            else -> throw java.lang.IllegalArgumentException(UNSUPPORTED)
        }
    }

    /**
     * Returns a DateTimeType for the supplied version with a value of the supplied value.
     *
     * @param fhirVersion the FHIR version to create a DateTimeType for
     * @param value the string value of the DateTimeType
     * @return the new DateTimeType
     */
    @JvmStatic
    fun dateTimeTypeForVersion(
        fhirVersion: FhirVersionEnum,
        value: String?,
    ): IPrimitiveType<Date?> {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU2 -> org.hl7.fhir.dstu2.model.DateTimeType(value)
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.DateTimeType(value)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.DateTimeType(value)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.DateTimeType(value)
            else -> throw java.lang.IllegalArgumentException(UNSUPPORTED)
        }
    }

    /**
     * Returns an InstantType for the supplied version with a value of the supplied value.
     *
     * @param fhirVersion the FHIR version to create a InstantType for
     * @param value the string value of the InstantType
     * @return the new InstantType
     */
    @JvmStatic
    fun instantTypeForVersion(fhirVersion: FhirVersionEnum, value: String?): IPrimitiveType<Date?> {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU2 -> org.hl7.fhir.dstu2.model.InstantType(value)
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.InstantType(value)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.InstantType(value)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.InstantType(value)
            else -> throw java.lang.IllegalArgumentException(UNSUPPORTED)
        }
    }
}
