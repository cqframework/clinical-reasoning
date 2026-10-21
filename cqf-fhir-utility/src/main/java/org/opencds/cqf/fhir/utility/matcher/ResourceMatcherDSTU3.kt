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
import org.hl7.fhir.dstu3.model.*
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.ICompositeType
import org.opencds.cqf.fhir.utility.FhirPathCache.cachedForVersion
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher.SPPathKey

class ResourceMatcherDSTU3 : ResourceMatcher {
    override val pathCache: MutableMap<SPPathKey?, IParsedExpression?> =
        ConcurrentHashMap<SPPathKey?, IParsedExpression?>()
    private val searchParams: MutableMap<String?, RuntimeSearchParam?> =
        ConcurrentHashMap<String?, RuntimeSearchParam?>()

    override val engine: IFhirPath
        get() {
            return cachedForVersion(FhirVersionEnum.DSTU3)
        }

    override val context: FhirContext
        get() {
            return FhirContext.forDstu3Cached()
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

    override fun addCustomParameter(searchParam: RuntimeSearchParam?) {
        this.searchParams[searchParam!!.name] = searchParam
    }

    override val customParameters: MutableMap<String?, RuntimeSearchParam?>
        get() {
            return this.searchParams
        }
}
