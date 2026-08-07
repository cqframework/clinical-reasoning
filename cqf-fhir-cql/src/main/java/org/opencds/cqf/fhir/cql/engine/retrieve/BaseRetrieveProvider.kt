package org.opencds.cqf.fhir.cql.engine.retrieve

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.fhirpath.IFhirPath
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.model.primitive.IdDt
import ca.uhn.fhir.rest.api.RestSearchParameterTypeEnum
import ca.uhn.fhir.rest.param.*
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException
import ca.uhn.fhir.util.ExtensionUtil
import com.google.common.collect.Multimap
import java.util.function.Predicate
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider
import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.cql.engine.runtime.Date
import org.opencds.cqf.cql.engine.runtime.DateTime
import org.opencds.cqf.cql.engine.runtime.Interval
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.PROFILE_MODE
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE
import org.opencds.cqf.fhir.cql.engine.utility.CodeExtractor
import org.opencds.cqf.fhir.utility.FhirPathCache
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class BaseRetrieveProvider
protected constructor(
    fhirContext: FhirContext,
    protected val terminologyProvider: TerminologyProvider,
    val retrieveSettings: RetrieveSettings,
) : RetrieveProvider {
    val fhirVersion = fhirContext.version.version
    protected val codeUtil = CodeExtractor(fhirContext)
    protected val fhirPath: IFhirPath = FhirPathCache.cachedForContext(fhirContext)
    private val resolver: SearchParameterResolver = SearchParameterResolver(fhirContext)

    fun filterByTemplateId(dataType: String?, templateId: String?): Predicate<IBaseResource?> {
        val profileMode = this.retrieveSettings.profileMode
        if (profileMode == PROFILE_MODE.OFF) {
            return Predicate { true }
        }

        if (
            templateId == null ||
                templateId.startsWith("http://hl7.org/fhir/StructureDefinition/$dataType")
        ) {
            logger.debug(
                "No profile-specific template id specified. Returning unfiltered resources."
            )
            return Predicate { true }
        }

        if (profileMode == PROFILE_MODE.DECLARED || profileMode == PROFILE_MODE.OPTIONAL) {
            return Predicate { res: IBaseResource? ->

                // DECLARED == require a declared profile to be there, and use it.
                // OPTIONAL == use the profile if it's there, but don't require it
                if (res!!.meta == null || res.meta.profile == null || res.meta.profile.isEmpty()) {
                    return@Predicate profileMode == PROFILE_MODE.OPTIONAL
                }

                for (profile in res.meta.profile) {
                    if (profile.hasValue() && profile.valueAsString == templateId) {
                        return@Predicate true
                    }
                }
                false
            }
        }

        // Should never see TRUST, since that should be handled by the repository.
        // ENFORCED is not yet supported.
        throw UnsupportedOperationException("$profileMode profile mode is not yet supported.")
    }

    fun filterByContext(
        dataType: String?,
        context: String?,
        contextPath: String?,
        contextValue: Any?,
    ): Predicate<IBaseResource?> {
        if (context == null || contextValue == null || contextPath == null) {
            logger.debug(
                "Unable to relate {} to {} context with contextPath: {} and contextValue: {}. Returning unfiltered resources.",
                dataType,
                context,
                contextPath,
                contextValue,
            )
            return Predicate { true }
        }

        return Predicate { res: IBaseResource? ->
            val resContextValue =
                this.fhirPath.evaluateFirst<IBase?>(res, contextPath, IBase::class.java)
            if (resContextValue.isPresent && resContextValue.get() is IIdType) {
                var id = (resContextValue.get() as IIdType).idPart

                if (id == null) {
                    logger.debug("Found null id for {} resource. Skipping.", dataType)
                    return@Predicate false
                }

                if (id.startsWith("urn:")) {
                    logger.debug("Found {} with urn: prefix. Stripping.", dataType)
                    id = stripUrnScheme(id)
                }
                if (id != contextValue) {
                    logger.debug("Found {} with id {}. Skipping.", dataType, id)
                    return@Predicate false
                }
            } else if (resContextValue.isPresent && resContextValue.get() is IBaseReference) {
                var reference = (resContextValue.get() as IBaseReference).referenceElement.value
                if (reference == null) {
                    logger.debug("Found null reference for {} resource. Skipping.", dataType)
                    return@Predicate false
                }

                if (reference.startsWith("urn:")) {
                    logger.debug(
                        "Found reference on {} resource with urn: prefix. Stripping.",
                        dataType,
                    )
                    reference = stripUrnScheme(reference)
                }

                if (reference.contains("/")) {
                    reference =
                        reference
                            .split("/".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1]
                }

                if (reference != contextValue) {
                    logger.debug("Found {} with reference {}. Skipping.", dataType, reference)
                    return@Predicate false
                }
            } else {
                val reference =
                    this.fhirPath.evaluateFirst<IBase?>(res, "reference", IBase::class.java)
                if (reference.isEmpty) {
                    logger.debug("Found {} resource unrelated to context. Skipping.", dataType)
                    return@Predicate false
                }

                var referenceString = (reference.get() as IPrimitiveType<*>).valueAsString
                if (referenceString.startsWith("urn:")) {
                    logger.debug(
                        "Found reference on {} resource with urn: prefix. Stripping.",
                        dataType,
                    )
                    referenceString = stripUrnScheme(referenceString)
                }

                if (referenceString.contains("/")) {
                    referenceString = referenceString.substring(referenceString.indexOf("/") + 1)
                }

                if (referenceString != contextValue) {
                    logger.debug(
                        "Found {} resource for context value: {} when expecting: {}. Skipping.",
                        dataType,
                        referenceString,
                        contextValue,
                    )
                    return@Predicate false
                }
            }
            true
        }
    }

    fun filterByTerminology(
        dataType: String?,
        codePath: String?,
        codes: Iterable<Code>?,
        valueSet: String?,
    ): Predicate<IBaseResource?> {
        if (codes == null && valueSet == null) {
            return Predicate { true }
        }

        if (codePath == null) {
            return Predicate { true }
        }

        return Predicate { res: IBaseResource? ->
            val values = this.fhirPath.evaluate<IBase?>(res, codePath, IBase::class.java)
            if (values != null && values.size == 1) {
                if (values[0] is IPrimitiveType<*>) {
                    return@Predicate isPrimitiveMatch(
                        dataType,
                        values[0] as IPrimitiveType<*>?,
                        codes,
                    )
                }

                if (values[0]!!.fhirType() == "CodeableConcept") {
                    val codeValueSet = getValueSetFromCode(values[0]!!)
                    if (codeValueSet != null) {
                        // TODO: If the value sets are not equal by name, test whether they have the
                        // same expansion...
                        return@Predicate codeValueSet == valueSet
                    }
                }
            }

            val resourceCodes = this.codeUtil.getElmCodesFromObject(values)
            anyCodeMatch(resourceCodes, codes) || anyCodeInValueSet(resourceCodes, valueSet)
        }
    }

    private fun stripUrnScheme(uri: String): String {
        return if (uri.startsWith("urn:uuid:")) {
            uri.substring(9)
        } else if (uri.startsWith("urn:oid:")) {
            uri.substring(8)
        } else {
            uri
        }
    }

    private fun anyCodeMatch(left: Iterable<Code>?, right: Iterable<Code>?): Boolean {
        if (left == null || right == null) {
            return false
        }

        for (code in left) {
            for (otherCode in right) {
                if (
                    code.code != null &&
                        code.code == otherCode.code &&
                        code.system != null &&
                        code.system == otherCode.system
                ) {
                    return true
                }
            }
        }

        return false
    }

    fun anyCodeInValueSet(codes: Iterable<Code>?, valueSet: String?): Boolean {
        if (codes == null || valueSet == null) {
            return false
        }

        val valueSetInfo = ValueSetInfo().withId(valueSet)
        for (code in codes) {
            if (this.terminologyProvider.`in`(code, valueSetInfo)) {
                return true
            }
        }

        return false
    }

    /**
     * Special case filtering to handle "codes" that are actually ids. This is a workaround to
     * handle filtering by Id.
     */
    private fun isPrimitiveMatch(
        dataType: String?,
        code: IPrimitiveType<*>?,
        codes: Iterable<Code>?,
    ): Boolean {
        if (code == null || codes == null) {
            return false
        }

        // This handles the case that the value is a reference such as "Medication/med-id"
        val primitiveString = code.valueAsString.replace("$dataType/", "")
        for (c in codes) {
            if (c.code == primitiveString) {
                return true
            }
        }

        return false
    }

    // Super hackery, just to get this running for connectathon
    private fun getValueSetFromCode(
        base: IBase
    ): String? { // what valuesets is it a part of, but just picking one, why not done
        // association Chris
        val ext =
            ExtensionUtil.getExtensionByUrl(
                base,
                "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-notDoneValueSet",
            )
        if (ext != null && ext.value != null && ext.value is IPrimitiveType<*>) {
            return (ext.value as IPrimitiveType<*>).valueAsString
        }
        return null
    }

    fun populateTemplateSearchParams(
        searchParams: Multimap<String?, MutableList<IQueryParameterType?>?>,
        dataType: String?,
        templateId: String?,
    ) {
        if (
            this.retrieveSettings.profileMode != PROFILE_MODE.OFF &&
                StringUtils.isNotBlank(templateId) &&
                !templateId!!.startsWith("http://hl7.org/fhir/StructureDefinition/$dataType")
        ) {
            val profileParam =
                if (this.fhirVersion!!.isOlderThan(FhirVersionEnum.R5)) UriParam(templateId)
                else ReferenceParam(templateId)
            searchParams.put("_profile", mutableListOf(profileParam))
        }
    }

    fun populateContextSearchParams(
        searchParams: Multimap<String?, MutableList<IQueryParameterType?>?>,
        dataType: String?,
        context: String?,
        contextPath: String?,
        contextValue: Any?,
    ) {
        if (contextPath.isNullOrEmpty() || contextValue == null) {
            return
        }

        val ref =
            if (StringUtils.isNotBlank(context))
                IdDt(contextValue as String).withResourceType(context)
            else IdDt(contextValue as String)
        val sp =
            this.resolver.getSearchParameterDefinition(dataType, contextPath)
                ?: throw InternalErrorException(RESOLVED_SP_DEF_NULL)

        // Self-references are token params.
        if (sp.name == "_id") {
            searchParams.put(sp.name, mutableListOf(TokenParam(contextValue)))
        } else {
            searchParams.put(sp.name, mutableListOf(ReferenceParam(ref)))
        }
    }

    fun populateTerminologySearchParams(
        searchParams: Multimap<String?, MutableList<IQueryParameterType?>?>,
        dataType: String?,
        codePath: String?,
        codes: Iterable<Code>?,
        valueSet: String?,
    ) {
        if (codePath.isNullOrEmpty()) {
            return
        }

        val sp =
            this.resolver.getSearchParameterDefinition(
                dataType,
                codePath,
                RestSearchParameterTypeEnum.TOKEN,
            ) ?: throw InternalErrorException(RESOLVED_SP_DEF_NULL)
        if (codes != null) {
            val codeList: MutableList<IQueryParameterType?> = ArrayList<IQueryParameterType?>()
            for (code in codes) {
                codeList.add(
                    TokenParam(InternalCodingDt().setSystem(code.system).setCode(code.code))
                )
            }
            searchParams.put(sp.name, codeList)
        } else if (valueSet != null) {
            val shouldUseInCodeModifier = shouldUseInCodeModifier(valueSet, dataType, sp.name)
            if (shouldUseInCodeModifier) {
                // Use the in modifier e.g. Observation?code:in=valueSetUrl
                searchParams.put(
                    sp.name,
                    mutableListOf(
                        TokenParam().setModifier(TokenParamModifier.IN).setValue(valueSet)
                    ),
                )
            } else {
                // Inline the codes into the retrieve e.g. Observation?code=system|code,system|code
                val codeList = mutableListOf<IQueryParameterType?>()
                for (code in this.terminologyProvider.expand(ValueSetInfo().withId(valueSet))) {
                    codeList.add(
                        TokenParam(InternalCodingDt().setSystem(code.system).setCode(code.code))
                    )
                }
                searchParams.put(sp.name, codeList)
            }
        }
    }

    protected fun shouldUseInCodeModifier(
        valueSet: String?,
        resourceName: String?,
        searchParamName: String?,
    ): Boolean {
        return this.retrieveSettings.terminologyParameterMode ==
            TERMINOLOGY_FILTER_MODE.USE_VALUE_SET_URL ||
            (this.retrieveSettings.terminologyParameterMode == TERMINOLOGY_FILTER_MODE.AUTO &&
                inModifierSupported(valueSet, resourceName, searchParamName))
    }

    protected open fun inModifierSupported(
        valueSet: String?,
        resourceName: String?,
        searchParamName: String?,
    ): Boolean {
        // TODO: Check valueSet in the capability statement,
        // also check that the selected search parameter supports that modifier.
        return true
    }

    fun populateDateSearchParams(
        searchParams: Multimap<String?, MutableList<IQueryParameterType?>?>,
        dataType: String?,
        dateParamName: String?,
        dateLowPath: String?,
        dateHighPath: String?,
        dateRange: Interval?,
    ) {
        if (dateParamName == null && dateHighPath == null && dateRange == null) {
            return
        }

        checkNotNull(dateRange) {
            "A date range must be provided when filtering using date parameters"
        }

        val start: java.util.Date?
        val end: java.util.Date?
        if (dateRange.start is DateTime) {
            start = (dateRange.start as DateTime).toJavaDate()
            val dateRangeEnd = dateRange.end ?: throw InternalErrorException(RESOLVED_SP_DEF_NULL)
            end = (dateRangeEnd as DateTime).toJavaDate()
        } else if (dateRange.start is Date) {
            start = (dateRange.start as Date).toJavaDate()
            val dateRangeEnd = dateRange.end ?: throw InternalErrorException(RESOLVED_SP_DEF_NULL)
            end = (dateRangeEnd as Date).toJavaDate()
        } else {
            throw UnsupportedOperationException(
                "Expected Interval of type Date or DateTime, found ${dateRange.start?.javaClass?.simpleName}"
            )
        }

        if (StringUtils.isNotBlank(dateParamName)) {
            val sp =
                this.resolver.getSearchParameterDefinition(dataType, dateParamName)
                    ?: throw InternalErrorException(RESOLVED_SP_DEF_NULL)

            // a date range is a search && condition - so we'll use a composite
            val gte = DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start)
            val lte = DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)

            searchParams.put(sp.name, mutableListOf(CompositeParam(gte, lte)))
        } else if (StringUtils.isNotBlank(dateLowPath)) {
            val dateRangeParam: MutableList<IQueryParameterType?> =
                ArrayList<IQueryParameterType?>()
            val dateParam = DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start)
            dateRangeParam.add(dateParam)
            val sp =
                this.resolver.getSearchParameterDefinition(dataType, dateLowPath)
                    ?: throw InternalErrorException(RESOLVED_SP_DEF_NULL)

            searchParams.put(sp.name, dateRangeParam)
        } else if (StringUtils.isNotBlank(dateHighPath)) {
            val dateRangeParam: MutableList<IQueryParameterType?> =
                ArrayList<IQueryParameterType?>()
            val dateParam = DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end)
            dateRangeParam.add(dateParam)
            val sp =
                this.resolver.getSearchParameterDefinition(dataType, dateHighPath)
                    ?: throw InternalErrorException(RESOLVED_SP_DEF_NULL)

            searchParams.put(sp.name, dateRangeParam)
        } else {
            error("A date path must be provided when filtering using date parameters")
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BaseRetrieveProvider::class.java)
        const val RESOLVED_SP_DEF_NULL = "resolved search parameter definition is null"
    }
}
