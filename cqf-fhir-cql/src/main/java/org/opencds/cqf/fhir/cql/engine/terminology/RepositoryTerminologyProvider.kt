package org.opencds.cqf.fhir.cql.engine.terminology

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.fhirpath.IFhirPath
import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.util.BundleUtil
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_PRE_EXPANSION_MODE
import org.opencds.cqf.fhir.utility.FhirPathCache
import org.opencds.cqf.fhir.utility.ValueSets
import org.opencds.cqf.fhir.utility.search.Searches
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/*
 * The implementation of this class uses a sorted list to perform terminology membership operations.
 * I tried changing the implementation to use a TreeSet rather than a sorted list and a binary
 * search, but found that the performance was reduced by ~33%. Please run the benchmarks to verify
 * that changes to this class do not result in significant performance degradation.
 */
class RepositoryTerminologyProvider
@JvmOverloads
constructor(
    private val repository: IRepository,
    private val valueSetIndex: MutableMap<String, MutableList<Code>> =
        HashMap<String, MutableList<Code>>(),
    private val terminologySettings: TerminologySettings = TerminologySettings(),
) : TerminologyProvider {
    private val fhirContext: FhirContext = repository.fhirContext()
    private val fhirPath: IFhirPath = FhirPathCache.cachedForContext(fhirContext)

    // The cached expansions are sorted by code order
    // This is used determine the range of codes to check
    private class Range(val start: Int, val end: Int) {
        companion object {
            val EMPTY: Range = Range(-1, -1)
        }
    }

    constructor(
        repository: IRepository,
        terminologySettings: TerminologySettings,
    ) : this(repository, HashMap<String, MutableList<Code>>(), terminologySettings)

    /**
     * This method checks for membership of a Code in a ValueSet
     *
     * @param code The Code to check.
     * @param valueSet The ValueSetInfo for the ValueSet to check membership of. Can not be null.
     * @return True if code is in the ValueSet.
     */
    override fun `in`(code: Code, valueSet: ValueSetInfo): Boolean {
        // Implementation note: This function should be considered inner loop
        // code. It's called thousands or millions of times by the CQL engine
        // during evaluation

        val codes = this.expand(valueSet)

        if (code.system == null) {
            // If the system is not provided and the resolved value set contains codes from multiple
            // code systems, a
            // run-time error is thrown because the operation is ambiguous
            val distinctSystems = codes.map { it.system }.distinct().count()
            require(distinctSystems <= 1) {
                "The 'in' operation is ambiguous because the code system is not provided and the resolved value set contains codes from multiple code systems"
            }
        }

        // This range includes all codes that have an equivalent code value,
        // So we only need to check the code system.
        val range = this.getSearchRange(code, codes)
        for (i in range.start..<range.end) {
            val c = codes[i]
            if (code.system == null || c.system == code.system) {
                return true
            }
        }

        return false
    }

    /**
     * This method expands a ValueSet into a list of Codes. It will use the "expansion" element of
     * the ValueSet if present. It will fall back the to "compose" element if not present. **NOTE:**
     * This provider does not provide a full expansion of the "compose" element. If only lists the
     * codes present in the "compose".
     *
     * @param valueSet The ValueSetInfo of the ValueSet to expand
     * @return The Codes in the ValueSet. **NOTE:** This method never returns null.
     */
    override fun expand(valueSet: ValueSetInfo): MutableList<Code> {

        // create a url|version canonical url from the info
        val url = valueSet.id + (if (valueSet.version != null) ("|" + valueSet.version) else "")

        val expansion = this.valueSetIndex.computeIfAbsent(url) { tryExpand(valueSet) }
        requireNotNull(expansion) { "Unable to get expansion for ValueSet ${valueSet.id}" }

        return expansion
    }

    private fun classFor(resourceType: String?): Class<out IBaseResource?>? {
        return this.fhirContext.getResourceDefinition(resourceType).implementingClass
    }

    // Attempts to perform expansion of the referenced ValueSet. It will first use an
    // expansion if one is available, and then ask the underlying repository to perform
    // an expansion if possible, and then fall back to doing a "naive" expansion where
    // possible. A "naive" expansion includes only codes directly referenced in the ValueSet
    // It's not possible to run expansion filters without the support of a terminology server.
    private fun tryExpand(valueSet: ValueSetInfo): MutableList<Code> {
        var codes = performExpansion(valueSet)
        // Filter out invalid codes that are missing a code or system
        codes =
            codes
                .filter { x -> x.code != null && x.system != null }
                .sortedWith(CODE_COMPARATOR)
                .toMutableList()
        return codes
    }

    private fun tryExpandOperation(vs: IBaseResource, valueSet: ValueSetInfo): MutableList<Code>? {
        var vs = vs
        val useExpandOperation =
            ((this.terminologySettings.valuesetExpansionMode == VALUESET_EXPANSION_MODE.AUTO) &&
                canRepositoryExpand(valueSet)) ||
                this.terminologySettings.valuesetExpansionMode ==
                    VALUESET_EXPANSION_MODE.USE_EXPAND_OPERATION

        var codes: MutableList<Code>? = null
        if (useExpandOperation) {
            vs =
                this.repository
                    .invoke<IBaseParameters?, IIdType?>(vs.idElement, "\$expand", null)
                    .resource
            codes = ValueSets.getCodesInExpansion(this.fhirContext, vs)
        }

        require(
            !(codes == null &&
                (this.terminologySettings.valuesetExpansionMode ==
                    VALUESET_EXPANSION_MODE.USE_EXPAND_OPERATION))
        ) {
            "Failed to expand ValueSet ${valueSet.id}"
        }

        return codes
    }

    private fun tryNaiveExpansion(vs: IBaseResource?, valueSet: ValueSetInfo): MutableList<Code> {
        require(!containsExpansionLogic(vs)) {
            "ValueSet ${valueSet.id} requires \$expand to support correctly, and \$expand is not available"
        }

        logger.warn(
            "ValueSet {} is not expanded. Falling back to compose definition. This will potentially produce incorrect results. ",
            valueSet.id,
        )

        val codes = ValueSets.getCodesInCompose(fhirContext, vs)

        requireNotNull(codes) { "Failed to expand ValueSet ${valueSet.id}" }

        return codes
    }

    private fun performExpansion(valueSet: ValueSetInfo): MutableList<Code> {
        val search =
            if (valueSet.version != null) Searches.byUrlAndVersion(valueSet.id, valueSet.version)
            else Searches.byUrl(valueSet.id)

        @Suppress("UNCHECKED_CAST")
        val results: IBaseBundle? =
            this.repository.search(
                classFor("Bundle") as Class<out IBaseBundle?>?,
                classFor("ValueSet"),
                search,
                null,
            )

        val resources = BundleUtil.toListOfResources(fhirContext, results)

        require(!resources.isEmpty()) { "Unable to locate ValueSet ${valueSet.id}" }

        require(resources.size <= 1) { "Multiple ValueSets resolved for ${valueSet.id}" }

        val vs = resources[0]
        var codes = tryPreExpansion(vs, valueSet)
        if (codes != null) {
            return codes
        }

        codes = tryExpandOperation(vs, valueSet)
        if (codes != null) {
            return codes
        }

        return tryNaiveExpansion(vs, valueSet)
    }

    private fun tryPreExpansion(vs: IBaseResource?, valueSet: ValueSetInfo): MutableList<Code>? {
        if (
            this.terminologySettings.valuesetPreExpansionMode == VALUESET_PRE_EXPANSION_MODE.IGNORE
        ) {
            return null
        }

        val codes = ValueSets.getCodesInExpansion(this.fhirContext, vs)
        require(
            !(codes == null &&
                this.terminologySettings.valuesetPreExpansionMode ==
                    VALUESET_PRE_EXPANSION_MODE.REQUIRE)
        ) {
            "ValueSet PreExpansion mode was set to REQUIRE, and valueSet ${valueSet.id} did not have an expansion"
        }

        if (codes != null && true == isNaiveExpansion(vs)) {
            logger.warn(
                "Codes for ValueSet {} expanded without a terminology server, some results may not be correct.",
                valueSet.id,
            )
        }

        return codes
    }

    // Given a set of Codes sorted by ".code", find the range that matching codes
    // occur in. This function first performs a binary search to find a matching element
    // and then iterates backwards and forwards from there to get the set of candidate codes
    private fun getSearchRange(code: Code, expansion: MutableList<Code>): Range {
        // Can't match on a null code
        if (code.code == null) {
            return Range.EMPTY
        }

        val index = expansion.binarySearch(code, CODE_COMPARATOR)

        if (index < 0) {
            return Range.EMPTY
        }

        var first = index
        var last = index + 1

        val value: String = code.code!!

        while (first > 0 && expansion[first - 1].code == value) {
            first--
        }

        while (last < expansion.size && expansion[last].code == value) {
            last++
        }

        return Range(first, last)
    }

    /**
     * Lookup is only partially implemented for this TerminologyProvider. Full implementation
     * requires the ability to access the full CodeSystem. This implementation only checks the code
     * system of the code matches the CodeSystemInfo url, and verifies the version if present.
     *
     * @param code The Code to lookup
     * @param codeSystem The CodeSystemInfo of the CodeSystem to check.
     * @return The Code if the system of the Code (and version if specified) matches the
     *   CodeSystemInfo url (and version)
     */
    override fun lookup(code: Code, codeSystem: CodeSystemInfo): Code? {
        if (code.system == null) {
            return null
        }

        if (
            code.system == codeSystem.id &&
                (code.version == null || code.version == codeSystem.version)
        ) {
            logger.warn("Unvalidated CodeSystem lookup: {} in {}", code, codeSystem.id)
            return code
        }

        return null
    }

    private fun isNaiveExpansion(resource: IBaseResource?): Boolean? {
        val expansion = ValueSets.getExpansion(this.fhirContext, resource)
        if (expansion != null) {
            val `object`: Any? =
                ValueSets.getExpansionParameters<IBase?>(
                    expansion,
                    fhirPath,
                    ".where(name = 'naive').value",
                )
            if (`object` is IBase) {
                return resolveNaiveBoolean(`object`)
            } else if (`object` is Iterable<*>) {
                @Suppress("UNCHECKED_CAST") val naiveParameters = `object` as MutableList<IBase>
                for (param in naiveParameters) {
                    return resolveNaiveBoolean(param)
                }
            }
        }
        return null
    }

    private fun resolveNaiveBoolean(param: IBase): Boolean? {
        if (param.fhirType() == "boolean") {
            return (param as IPrimitiveType<*>).getValue() as Boolean?
        } else {
            return null
        }
    }

    private fun containsExpansionLogic(resource: IBaseResource?): Boolean {
        val includeFilters = ValueSets.getIncludeFilters(this.fhirContext, resource)
        if (includeFilters != null && !includeFilters.isEmpty()) {
            return true
        }
        val excludeFilters = ValueSets.getExcludeFilters(this.fhirContext, resource)
        if (excludeFilters != null && !excludeFilters.isEmpty()) {
            return true
        }
        return false
    }

    private fun canRepositoryExpand(valueSetInfo: ValueSetInfo?): Boolean {
        // TODO: check capabilities statement to see if expansion is supported.
        return true
    }

    companion object {
        private val logger: Logger =
            LoggerFactory.getLogger(RepositoryTerminologyProvider::class.java)

        private val CODE_COMPARATOR = Comparator { x: Code?, y: Code? ->
            x!!.code!!.compareTo(y!!.code!!)
        }
    }
}
