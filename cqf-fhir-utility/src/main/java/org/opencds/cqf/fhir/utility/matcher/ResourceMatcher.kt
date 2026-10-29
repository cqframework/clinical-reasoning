package org.opencds.cqf.fhir.utility.matcher

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.RuntimeSearchParam
import ca.uhn.fhir.fhirpath.IFhirPath
import ca.uhn.fhir.fhirpath.IFhirPath.IParsedExpression
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.model.primitive.DateTimeDt
import ca.uhn.fhir.rest.param.*
import java.util.*
import org.apache.commons.lang3.NotImplementedException
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.instance.model.api.*

interface ResourceMatcher {
    class SPPathKey(private val resourceType: String?, private val resourcePath: String?) {
        fun path(): String? {
            return resourcePath
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + (if (resourceType == null) 0 else resourceType.hashCode())
            result = prime * result + (if (resourcePath == null) 0 else resourcePath.hashCode())
            return result
        }

        override fun equals(obj: Any?): Boolean {
            if (this === obj) return true
            if (obj == null) return false
            if (javaClass != obj.javaClass) return false
            val or = obj as SPPathKey
            if (resourceType == null) {
                if (or.resourceType != null) return false
            } else if (resourceType != or.resourceType) return false
            if (resourcePath == null) {
                if (or.resourcePath != null) return false
            } else if (resourcePath != or.resourcePath) return false
            return true
        }
    }

    val engine: IFhirPath?

    val context: FhirContext

    val pathCache: MutableMap<SPPathKey?, IParsedExpression?>?

    fun addCustomParameter(searchParam: RuntimeSearchParam?)

    val customParameters: MutableMap<String?, RuntimeSearchParam?>?

    // The list here is an OR list. Meaning, if any element matches it's a match
    fun matches(
        name: String?,
        params: MutableList<IQueryParameterType?>?,
        resource: IBaseResource,
    ): Boolean {
        var match = true

        val context = this.context
        var s = context.getResourceDefinition(resource).getSearchParam(name)
        if (s == null) {
            s = this.customParameters!!.get(name)
        }
        if (s == null) {
            throw RuntimeException(
                "The SearchParameter $name for Resource ${resource.fhirType()} is not supported."
            )
        }

        var path = s.getPath()

        // System search parameters...
        if (path.isEmpty() && name!!.startsWith("_")) {
            path = name.substring(1)
        }

        var pathResult: MutableList<IBase>? = null
        try {
            val parsed =
                this.pathCache!!.computeIfAbsent(SPPathKey(resource.fhirType(), path)) {
                    p: SPPathKey? ->
                    try {
                        return@computeIfAbsent this.engine!!.parse(p!!.path())
                    } catch (e: java.lang.Exception) {
                        throw java.lang.RuntimeException(
                            "Parsing SearchParameter $name for Resource ${resource.fhirType()} resulted in an error.",
                            e,
                        )
                    }
                }!!
            pathResult = this.engine!!.evaluate<IBase?>(resource, parsed, IBase::class.java)
        } catch (e: Exception) {
            throw RuntimeException(
                "Evaluating SearchParameter $name for Resource ${resource.fhirType()} resulted in an error.",
                e,
            )
        }

        if (pathResult == null || pathResult.isEmpty()) {
            return false
        }

        for (param in params!!) {
            for (r in pathResult) {
                match = isParamMatchAtPath(param, r)

                if (match) {
                    return true
                }
            }
        }

        return false
    }

    private fun isParamMatchAtPath(param: IQueryParameterType?, r: IBase): Boolean {
        var match: Boolean
        if (param is ReferenceParam) {
            match = isMatchReference(param, r)
        } else if (param is DateParam) {
            match = isMatchDate(param, r)
        } else if (param is TokenParam) {
            // [parameter]=[code]: the value of [code] matches a Coding.code or Identifier.value
            // irrespective of
            // the value of the system property
            // [parameter]=[system]|[code]: the value of [code] matches a Coding.code or
            // Identifier.value, and
            // the value of [system] matches the system property of the Identifier or Coding
            // [parameter]=|[code]: the value of [code] matches a Coding.code or Identifier.value,
            // and the
            // Coding/Identifier has no system property
            // [parameter]=[system]|: any element where the value of [system] matches the system
            // property of the
            // Identifier or Coding
            match = applyModifiers(isMatchToken(param, r), param)
            if (!match) {
                val codes = getCodes(r)
                match = isMatchCoding(param, r, codes)
            }
        } else if (param is UriParam) {
            match = isMatchUri(param, r)
        } else if (param is StringParam) {
            match = isMatchString(param, r)
        } else if (param is CompositeParam<*, *>) {
            return isCompositeMatch(param, r)
        } else {
            throw NotImplementedException(
                "Resource matching not implemented for search params of type " +
                    param!!.javaClass.getSimpleName()
            )
        }
        return match
    }

    private fun isCompositeMatch(composite: CompositeParam<*, *>, r: IBase): Boolean {
        if (r !is ICompositeType) {
            /*
             * Composites are AND'd
             * r is likely a primitive (ie, single DateParam object).
             * But even if it's "something else", the isMatchDate method should handle it
             */
            return isParamMatchAtPath(composite.getLeftValue(), r) &&
                isParamMatchAtPath(composite.getRightValue(), r)
        }

        // DateRange and NumberRange do not share a lot of commonality
        // so we have to handle them like special cases here
        if (composite.getLeftValue() is DateParam) {
            // search bounds
            var searchStart = composite.getLeftValue() as DateParam
            var searchEnd = composite.getRightValue() as DateParam
            if (searchStart.getValue().after(searchEnd.getValue())) {
                // in case order is mixed
                searchStart = searchEnd
                searchEnd = composite.getLeftValue() as DateParam
            }

            val rangeParam = getDateRange(r)
            // these values we can assume are in order since we created them
            val resourceStart = rangeParam.getLowerBound()
            val resourceEnd = rangeParam.getUpperBound()

            /*
             * we have 2 ranges:
             * The Resource's period (rl -> ru)
             * The search's period (sl -> su)
             *
             * resource (r):  |rs-----------|re
             * search (s):              |ss-----------|se
             *
             * these are overlapping if:
             * * resourceStart <= searchEnd && searchStart <= resourceEnd
             *
             * NB: we write it here as:
             * searchEnd >= resourceStart && searchStart <= resourceEnd
             *
             * This is because the
             */
            return isMatchDate(searchEnd, DateTimeDt(resourceStart.getValue())) &&
                isMatchDate(searchStart, DateTimeDt(resourceEnd.getValue()))
        } else {
            throw NotImplementedException(
                "Composite matching not implemented for search params of type " +
                    composite.getLeftValue().javaClass.getSimpleName()
            )
        }
    }

    fun isMatchReference(param: IQueryParameterType, pathResult: IBase): Boolean {
        val paramValue = (param as ReferenceParam).getValue()
        if (pathResult is IBaseReference) {
            return paramValue == extractReferenceValue(pathResult)
        } else if (pathResult is IPrimitiveType<*>) {
            return paramValue == pathResult.getValueAsString()
        }
        throw UnsupportedOperationException(
            "Expected Reference element, found " + pathResult.javaClass.getSimpleName()
        )
    }

    fun isMatchDate(param: DateParam, pathResult: IBase): Boolean {
        // date, dateTime and instant are PrimitiveType<Date>
        if (pathResult is IPrimitiveType<*>) {
            val result: Any? = pathResult.getValue()
            if (result is Date) {
                return isDateMatch(param, result)
            } else {
                throw UnsupportedOperationException(
                    "Expected date, found " + pathResult.javaClass.getSimpleName()
                )
            }
        } else {
            throw UnsupportedOperationException(
                "Expected element of type date, dateTime, instant, Timing or Period, found " +
                    pathResult.javaClass.getSimpleName()
            )
        }
    }

    fun isDateMatch(param: DateParam, date: Date): Boolean {
        val compareDate = param.getValue()
        if (compareDate == null) {
            return false
        }

        when (param.getPrefix()) {
            ParamPrefixEnum.EQUAL -> {
                return date == compareDate
            }

            ParamPrefixEnum.LESSTHAN -> {
                return date.before(compareDate)
            }

            ParamPrefixEnum.GREATERTHAN -> {
                return date.after(compareDate)
            }

            ParamPrefixEnum.GREATERTHAN_OR_EQUALS -> {
                return date.after(compareDate) || date == compareDate
            }

            ParamPrefixEnum.LESSTHAN_OR_EQUALS -> {
                return date.before(compareDate) || date == compareDate
            }

            ParamPrefixEnum.NOT_EQUAL -> {
                return date != compareDate
            }

            else -> {
                // ends_before, starts_after, approximate - do not work for single date parameters
                val msg =
                    String.format(
                        "Unsupported DateTime comparison operation %s",
                        param.getPrefix().getValue(),
                    )
                throw UnsupportedOperationException(msg)
            }
        }
    }

    fun isMatchToken(param: TokenParam, pathResult: IBase?): Boolean {
        if (param.getValue() == null) {
            return true
        }

        if (pathResult is IIdType) {
            return param.getValue() == pathResult.getIdPart()
        }

        if (pathResult is IBaseEnumeration<*>) {
            return param.getValue() == pathResult.getValueAsString()
        }

        if (pathResult is IPrimitiveType<*>) {
            return param.getValue() == pathResult.getValue()
        }

        return false
    }

    fun isMatchCoding(
        param: TokenParam,
        pathResult: IBase?,
        codes: MutableList<TokenParam?>?,
    ): Boolean {
        if (codes == null || codes.isEmpty()) {
            return false
        }

        if (param.getModifier() == TokenParamModifier.IN) {
            throw UnsupportedOperationException("In modifier is unsupported")
        }

        for (c in codes) {
            val matches =
                param.getValue() == c!!.getValue() &&
                    (param.getSystem() == null || param.getSystem() == c.getSystem())
            if (matches) {
                return true
            }
        }

        return false
    }

    fun isMatchUri(param: UriParam, pathResult: IBase): Boolean {
        if (param.getValue() == null) {
            return false
        }
        if (pathResult is IPrimitiveType<*>) {
            return param.getValue() == pathResult.getValue()
        }
        throw UnsupportedOperationException(
            "Expected element of type url or uri, found " + pathResult.javaClass.getSimpleName()
        )
    }

    fun isMatchString(param: StringParam, pathResult: Any): Boolean {
        if (pathResult is IPrimitiveType<*>) {
            return param.getValue() == pathResult.getValue()
        }
        throw UnsupportedOperationException(
            "Expected element of type string, found " + pathResult.javaClass.getSimpleName()
        )
    }

    fun getDateRange(type: ICompositeType?): DateRangeParam

    fun getCodes(codeElement: IBase?): MutableList<TokenParam?>?

    companion object {
        private fun applyModifiers(input: Boolean, token: TokenParam): Boolean {
            if (token.getModifier() != null && token.getModifier() != TokenParamModifier.NOT) {
                throw NotImplementedException(
                    "Only the NOT modifier is supported on tokens at this time"
                )
            }
            if (token.getModifier() == TokenParamModifier.NOT) {
                return !input
            }
            return input
        }

        private fun extractReferenceValue(reference: IBaseReference): String? {
            if (
                reference.getReferenceElement() != null &&
                    !StringUtils.isEmpty(reference.getReferenceElement().getValue())
            ) {
                return reference.getReferenceElement().getValue()
            } else if (reference.getResource() != null) {
                return reference.getResource().getIdElement().getValue()
            }
            throw UnsupportedOperationException("No reference found")
        }
    }
}
