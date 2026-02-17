package org.opencds.cqf.fhir.cql.engine.retrieve;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.fhir.utility.iterable.BundleMappingIterable;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class RepositoryRetrieveProvider extends BaseRetrieveProvider {


    private class SearchConfig {

        /**
         * Each element of each list is OR'd
         * Each
         */
        public Multimap<String, List<IQueryParameterType>> searchParams = HashMultimap.create();
        public Predicate<IBaseResource> filter = x -> true;
    }

    private final IRepository repository;
    private final FhirContext fhirContext;

    public RepositoryRetrieveProvider(
            final IRepository repository, final TerminologyProvider terminologyProvider, RetrieveSettings settings) {
        super(repository.fhirContext(), terminologyProvider, settings);
        this.repository = requireNonNull(repository, "repository can not be null.");
        this.fhirContext = repository.fhirContext();
    }

    @Override
    public Iterable<Object> retrieve(
            final String context,
            final String contextPath,
            final Object contextValue,
            final String dataType,
            final String templateId,
            final String codePath,
            final Iterable<Code> codes,
            final String valueSet,
            final String datePath,
            final String dateLowPath,
            final String dateHighPath,
            final Interval dateRange) {
        var resourceType = fhirContext.getResourceDefinition(dataType).getImplementingClass();

        @SuppressWarnings("unchecked")
        var bt = (Class<? extends IBaseBundle>)
                this.fhirContext.getResourceDefinition("Bundle").getImplementingClass();

        var config = new SearchConfig();
        this.configureTerminology(config, dataType, codePath, codes, valueSet);
        this.configureContext(config, dataType, context, contextPath, contextValue);
        this.configureProfile(config, dataType, templateId);
        this.configureDates(config, dataType, datePath, dateLowPath, dateHighPath, dateRange);

        Map<String, String> headers = headersForContext(context, contextValue);

        var resources = this.repository.search(bt, resourceType, config.searchParams, headers);

        var iter = new BundleMappingIterable<>(repository, resources, p -> p.getResource());
        return iter.toStream().filter(config.filter).collect(Collectors.toList());
    }

    // Create headers for the FHIR compartment search (e.g. X-FHIR-Compartment: Patient/123)
    private Map<String, String> headersForContext(String context, Object contextValue) {
        if (context == null || contextValue == null) {
            return Collections.emptyMap();
        }

        return Map.of(IgRepository.FHIR_COMPARTMENT_HEADER, context + "/" + contextValue.toString());
    }

    private void configureProfile(SearchConfig config, String dataType, String templateId) {
        var mode = this.getRetrieveSettings().getSearchParameterMode();
        switch (mode) {
            case FILTER_IN_MEMORY:
            case AUTO: // TODO: Auto-detect based on CapabilityStatement
                config.filter = config.filter.and(filterByTemplateId(dataType, templateId));
                break;
            case USE_SEARCH_PARAMETERS:
                populateTemplateSearchParams(config.searchParams, dataType, templateId);
        }
    }

    private void configureContext(
            SearchConfig config, String dataType, String context, String contextPath, Object contextValue) {
        var mode = this.getRetrieveSettings().getSearchParameterMode();
        switch (mode) {
            case FILTER_IN_MEMORY:
                config.filter = config.filter.and(filterByContext(dataType, context, contextPath, contextValue));
                break;
            case AUTO: // TODO: offload detection based on CapabilityStatement
            case USE_SEARCH_PARAMETERS:
                populateContextSearchParams(config.searchParams, dataType, context, contextPath, contextValue);
                break;
        }
    }

    private void configureTerminology(
            SearchConfig config, String dataType, String codePath, Iterable<Code> codes, String valueSet) {
        var mode = this.getRetrieveSettings().getTerminologyParameterMode();
        switch (mode) {
            case FILTER_IN_MEMORY:
                config.filter = config.filter.and(filterByTerminology(dataType, codePath, codes, valueSet));
                break;
            case AUTO: // TODO: offload detection based on CapabilityStatement
            case USE_INLINE_CODES:
            case USE_VALUE_SET_URL:
                populateTerminologySearchParams(config.searchParams, dataType, codePath, codes, valueSet);
                break;
        }
    }

    private void configureDates(
            SearchConfig config,
            String dataType,
            String datePath,
            String dateLowPath,
            String dateHighPath,
            Interval dateRange) {
        var mode = this.getRetrieveSettings().getSearchParameterMode();
        switch (mode) {
            case FILTER_IN_MEMORY:
            case AUTO: // TODO: offload detection based on CapabilityStatement
//                if (datePath != null) {
//                    throw new UnsupportedOperationException("in-memory dateFilters are not supported");
//                }
//                break;
            case USE_SEARCH_PARAMETERS:
                populateDateSearchParams(config.searchParams, dataType, datePath, dateLowPath, dateHighPath, dateRange);
                break;
        }
    }

    @Override
    protected boolean inModifierSupported(String valueSet, String resourceName, String searchParamName) {
        // The IN modifier is not currently supported by the ResourceMatcher used by the InMemoryRepository
        return !(repository instanceof InMemoryFhirRepository)
                && super.inModifierSupported(valueSet, resourceName, searchParamName);
    }
}
