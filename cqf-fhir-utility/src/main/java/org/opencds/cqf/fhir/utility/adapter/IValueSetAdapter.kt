package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBaseBackboneElement

/** This interface exposes common functionality across all FHIR ValueSet versions. */
interface IValueSetAdapter : IKnowledgeArtifactAdapter {
    fun addUseContext(usageContext: IUsageContextAdapter?): IValueSetAdapter?

    fun <T : IBaseBackboneElement> setExpansion(expansion: T?)

    fun <T : IBaseBackboneElement> getExpansion(): T?

    fun hasExpansion(): Boolean

    fun hasExpansionContains(): Boolean

    val expansionTotal: Int

    val expansionContains: MutableList<IValueSetExpansionContainsAdapter?>?

    fun appendExpansionContains(expansionContains: MutableList<IValueSetExpansionContainsAdapter?>?)

    fun <T : IBaseBackboneElement> newExpansion(): T?

    fun addExpansionStringParameter(name: String?, value: String?)

    fun hasExpansionStringParameter(name: String?, value: String?): Boolean

    val composeInclude: MutableList<IValueSetConceptSetAdapter?>?

    val valueSetIncludes: MutableList<String?>?

    fun hasCompose(): Boolean

    fun hasComposeInclude(): Boolean

    fun hasComposeExclude(): Boolean

    fun hasComposeFilters(): Boolean

    /**
     * A simple compose element of a ValueSet must have a compose without an exclude element. Each
     * element of the include cannot have a filter or reference a ValueSet and must have a system
     * and enumerate concepts.
     *
     * @return boolean
     */
    fun hasSimpleCompose(): Boolean

    /**
     * A grouping compose element of a ValueSet must have a compose without an exclude element and
     * each element of the include must reference a ValueSet.
     *
     * @return boolean
     */
    fun hasGroupingCompose(): Boolean

    /**
     * Indicates whether this ValueSet's compose element contains explicitly enumerated concepts.
     *
     * This method returns `true` if any `compose.include` component defines one or more `concept`
     * entries. Explicit concepts represent directly specified codes that may be expanded locally
     * using naive expansion, without requiring a terminology service.
     *
     * This method is used by expansion logic to determine whether local expansion should include
     * explicitly defined codes. A ValueSet may contain both explicit concepts and referenced
     * ValueSets (a hybrid compose), in which case both sources must be included in the expansion.
     *
     * This method does not evaluate exclude elements or filters. Presence of filters may still
     * require terminology service expansion depending on implementation capabilities.
     *
     * @return `true` if the ValueSet compose includes explicitly enumerated concepts; `false`
     *   otherwise
     */
    fun hasExplicitConcepts(): Boolean

    /**
     * Indicates whether this ValueSet's compose element includes references to other ValueSets.
     *
     * This method returns `true` if any `compose.include` component specifies one or more
     * `valueSet` canonical references. Referenced ValueSets must be expanded and their resulting
     * codes incorporated into this ValueSet's expansion.
     *
     * This method is used by expansion logic to determine whether recursive expansion of dependent
     * ValueSets is required. A ValueSet may contain both referenced ValueSets and explicitly
     * enumerated concepts (a hybrid compose), in which case both must be expanded and merged.
     *
     * This method does not evaluate exclude elements or filters. Referenced ValueSets that require
     * terminology service expansion may still be delegated depending on implementation
     * capabilities.
     *
     * @return `true` if the ValueSet compose includes references to other ValueSets; `false`
     *   otherwise
     */
    fun hasValueSetReferences(): Boolean

    /**
     * Performs a naive expansion on the ValueSet by collecting all codes within the compose. Can
     * only be performed on a ValueSet with a simple compose.
     */
    fun naiveExpand()

    fun hasNaiveParameter(): Boolean

    fun <T : IBaseBackboneElement> createNaiveParameter(): T?
}
