package org.opencds.cqf.fhir.cql.engine.retrieve

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.util.bundle.BundleEntryParts
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import java.util.function.Predicate
import java.util.stream.Collectors
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.cql.engine.runtime.ClassInstance
import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.cql.engine.runtime.Interval
import org.opencds.cqf.cql.engine.runtime.Value
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE
import org.opencds.cqf.fhir.utility.iterable.BundleMappingIterable
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository

class RepositoryRetrieveProvider(
    private val repository: IRepository,
    terminologyProvider: TerminologyProvider,
    settings: RetrieveSettings,
) : BaseRetrieveProvider(repository.fhirContext(), terminologyProvider, settings) {
    private class SearchConfig {
        /** Each element of each list is OR'd Each */
        var searchParams: Multimap<String?, MutableList<IQueryParameterType?>?> =
            HashMultimap.create<String?, MutableList<IQueryParameterType?>?>()

        var filter: Predicate<IBaseResource?> = Predicate { true }
    }

    private val fhirContext: FhirContext = repository.fhirContext()

    override fun retrieve(
        context: String?,
        contextPath: String?,
        contextValue: String?,
        dataType: String,
        templateId: String?,
        codePath: String?,
        codes: Iterable<Code>?,
        valueSet: String?,
        datePath: String?,
        dateLowPath: String?,
        dateHighPath: String?,
        dateRange: Interval?,
    ): Iterable<Value?>? {
        val resourceType = fhirContext.getResourceDefinition(dataType).implementingClass

        @Suppress("UNCHECKED_CAST")
        val bt =
            this.fhirContext.getResourceDefinition("Bundle").implementingClass
                as Class<out IBaseBundle?>?

        val config = SearchConfig()
        this.configureTerminology(config, dataType, codePath, codes, valueSet)
        this.configureContext(config, dataType, context, contextPath, contextValue)
        this.configureProfile(config, dataType, templateId)
        this.configureDates(config, dataType, datePath, dateLowPath, dateHighPath, dateRange)

        val headers = headersForContext(context, contextValue)

        val resources: IBaseBundle? =
            this.repository.search(bt, resourceType, config.searchParams, headers)

        val modelResolver = FhirModelResolverCache.resolverForVersion(fhirContext.version.version)
        val iter =
            BundleMappingIterable<IBaseBundle?, IBaseResource?>(
                repository,
                resources,
                { obj: BundleEntryParts? -> obj!!.resource },
            )
        return iter
            .toStream()
            .filter(config.filter)
            .map<ClassInstance?> { r: IBaseResource? -> modelResolver.toCqlValue(r, false) }
            .collect(Collectors.toList())
    }

    // Create headers for the FHIR compartment search (e.g. X-FHIR-Compartment: Patient/123)
    private fun headersForContext(
        context: String?,
        contextValue: Any?,
    ): MutableMap<String?, String?> {
        if (context == null || contextValue == null) {
            return mutableMapOf()
        }

        return mutableMapOf(IgRepository.FHIR_COMPARTMENT_HEADER to "$context/$contextValue")
    }

    private fun configureProfile(config: SearchConfig, dataType: String?, templateId: String?) {
        val mode = this.retrieveSettings.searchParameterMode
        when (mode) {
            SEARCH_FILTER_MODE.FILTER_IN_MEMORY,
            SEARCH_FILTER_MODE.AUTO ->
                config.filter = config.filter.and(filterByTemplateId(dataType, templateId))

            SEARCH_FILTER_MODE.USE_SEARCH_PARAMETERS ->
                populateTemplateSearchParams(config.searchParams, dataType, templateId)
        }
    }

    private fun configureContext(
        config: SearchConfig,
        dataType: String?,
        context: String?,
        contextPath: String?,
        contextValue: Any?,
    ) {
        val mode = this.retrieveSettings.searchParameterMode
        when (mode) {
            SEARCH_FILTER_MODE.FILTER_IN_MEMORY ->
                config.filter =
                    config.filter.and(filterByContext(dataType, context, contextPath, contextValue))

            SEARCH_FILTER_MODE.AUTO,
            SEARCH_FILTER_MODE.USE_SEARCH_PARAMETERS ->
                populateContextSearchParams(
                    config.searchParams,
                    dataType,
                    context,
                    contextPath,
                    contextValue,
                )
        }
    }

    private fun configureTerminology(
        config: SearchConfig,
        dataType: String?,
        codePath: String?,
        codes: Iterable<Code>?,
        valueSet: String?,
    ) {
        val mode = this.retrieveSettings.terminologyParameterMode
        when (mode) {
            TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY ->
                config.filter =
                    config.filter.and(filterByTerminology(dataType, codePath, codes, valueSet))

            TERMINOLOGY_FILTER_MODE.AUTO,
            TERMINOLOGY_FILTER_MODE.USE_INLINE_CODES,
            TERMINOLOGY_FILTER_MODE.USE_VALUE_SET_URL ->
                populateTerminologySearchParams(
                    config.searchParams,
                    dataType,
                    codePath,
                    codes,
                    valueSet,
                )
        }
    }

    private fun configureDates(
        config: SearchConfig,
        dataType: String?,
        datePath: String?,
        dateLowPath: String?,
        dateHighPath: String?,
        dateRange: Interval?,
    ) {
        val mode = this.retrieveSettings.searchParameterMode
        when (mode) {
            SEARCH_FILTER_MODE.FILTER_IN_MEMORY,
            SEARCH_FILTER_MODE.AUTO,
            SEARCH_FILTER_MODE.USE_SEARCH_PARAMETERS ->
                populateDateSearchParams(
                    config.searchParams,
                    dataType,
                    datePath,
                    dateLowPath,
                    dateHighPath,
                    dateRange,
                )
        }
    }

    override fun inModifierSupported(
        valueSet: String?,
        resourceName: String?,
        searchParamName: String?,
    ): Boolean {
        // The IN modifier is not currently supported by the ResourceMatcher used by the
        // InMemoryRepository
        return repository !is InMemoryFhirRepository &&
            super.inModifierSupported(valueSet, resourceName, searchParamName)
    }
}
