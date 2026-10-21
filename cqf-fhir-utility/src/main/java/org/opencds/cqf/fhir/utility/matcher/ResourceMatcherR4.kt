package org.opencds.cqf.fhir.utility.matcher

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.context.RuntimeSearchParam
import ca.uhn.fhir.fhirpath.IFhirPath
import ca.uhn.fhir.fhirpath.IFhirPath.IParsedExpression
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.TokenParam
import java.util.concurrent.ConcurrentHashMap
import org.apache.commons.lang3.NotImplementedException
import org.hl7.fhir.instance.model.api.*
import org.hl7.fhir.r4.model.*
import org.opencds.cqf.fhir.utility.FhirPathCache.cachedForVersion
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher.SPPathKey

class ResourceMatcherR4 : ResourceMatcher {
    override val pathCache: MutableMap<SPPathKey?, IParsedExpression?> =
        ConcurrentHashMap<SPPathKey?, IParsedExpression?>()
    private val searchParams: MutableMap<String?, RuntimeSearchParam?> =
        ConcurrentHashMap<String?, RuntimeSearchParam?>()

    override val engine: IFhirPath
        get() {
            return cachedForVersion(FhirVersionEnum.R4)
        }

    override val context: FhirContext
        get() {
            return FhirContext.forR4Cached()
        }

    override fun getDateRange(type: ICompositeType?): DateRangeParam {
        if (type is Period) {
            return DateRangeParam(type.start, type.end)
        } else if (type is Timing) {
            throw NotImplementedException("Timing resolution has not yet been implemented")
        } else {
            throw UnsupportedOperationException(
                "Expected element of type Period or Timing, found " + type!!.javaClass.simpleName
            )
        }
    }

    override fun getCodes(codeElement: IBase?): MutableList<TokenParam?> {
        var resolvedCodes = mutableListOf<TokenParam?>()
        if (codeElement is Coding) {
            resolvedCodes.add(TokenParam(codeElement.system, codeElement.code))
        } else if (codeElement is CodeType) {
            resolvedCodes.add(TokenParam(codeElement.value))
        } else if (codeElement is CodeableConcept) {
            resolvedCodes =
                codeElement
                    .getCoding()
                    .map { code -> TokenParam(code!!.system, code.code) }
                    .toMutableList()
        }

        return resolvedCodes
    }

    override fun isMatchToken(param: TokenParam, pathResult: IBase?): Boolean {
        if (param.value == null) {
            // TODO - should this be false?
            return true
        }

        if (pathResult is IIdType) {
            return param.value == pathResult.idPart
        }

        // [parameter]=[code]: the value of [code] matches a Coding.code or Identifier.value
        // irrespective of the
        // value of the system property
        // [parameter]=[system]|[code]: the value of [code] matches a Coding.code or
        // Identifier.value, and the value
        // of [system] matches the system property of the Identifier or Coding
        // [parameter]=|[code]: the value of [code] matches a Coding.code or Identifier.value, and
        // the
        // Coding/Identifier has no system property
        // [parameter]=[system]|: any
        if (pathResult is Identifier) {
            val system = pathResult.system
            val value = pathResult.value

            if (
                param.system != null &&
                    param.system == system &&
                    param.value != null &&
                    param.value == value
            ) {
                return true
            } else if (param.value != null && param.value == value) {
                return true
            } else if (param.system != null && param.system == system) {
                return true
            } else {
                // this will never be reached because of the top null check
                return false
            }
        }

        if (pathResult is IBaseEnumeration<*>) {
            return param.value == pathResult.valueAsString
        }

        if (pathResult is IPrimitiveType<*>) {
            return param.value == pathResult.value
        }

        return false
    }

    override fun addCustomParameter(searchParam: RuntimeSearchParam?) {
        this.searchParams[searchParam!!.name] = searchParam
    }

    override val customParameters: MutableMap<String?, RuntimeSearchParam?>
        get() {
            return this.searchParams
        }
}
