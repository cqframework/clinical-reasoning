package org.opencds.cqf.fhir.cql.engine.retrieve;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.iterable.BundleMappingIterable;

public class RepositoryRetrieveProvider extends BaseRetrieveProvider {
    private final Repository repository;
    private final FhirContext fhirContext;

    public RepositoryRetrieveProvider(
            final Repository repository, final TerminologyProvider terminologyProvider, RetrieveSettings settings) {
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
        this.configureContext(config, dataType, contextPath, context, contextValue);
        this.configureProfile(config, templateId);
        this.configureDates(config, dataType, datePath, dateLowPath, dateHighPath, dateRange);

        var resources = this.repository.search(bt, resourceType, config.searchParams);

        var iter = new BundleMappingIterable<>(repository, resources, p -> p.getResource());
        return iter.toStream().filter(config.filter).collect(Collectors.toList());
    }

    private void configureProfile(SearchConfig config, String templateId) {
        var mode = this.getRetrieveSettings().getSearchParameterMode();
        switch (mode) {
            case CQL:
            case AUTO: // TODO: Auto-detect based on CapabilityStatement
                config.filter = config.filter.and(filterByTemplateId(templateId, templateId));
                break;
            case REPOSITORY:
                populateTemplateSearchParams(config.searchParams, templateId);
        }
    }

    private void configureContext(
            SearchConfig config, String dataType, String contextPath, String context, Object contextValue) {
        var mode = this.getRetrieveSettings().getSearchParameterMode();
        switch (mode) {
            case CQL:
                config.filter = config.filter.and(filterByContext(dataType, context, contextPath, contextValue));
                break;
            case AUTO: // TODO: offload detection based on CapabilityStatement
            case REPOSITORY:
                populateContextSearchParams(config.searchParams, dataType, contextPath, context, contextValue);
                break;
        }
    }

    private void configureTerminology(
            SearchConfig config, String dataType, String codePath, Iterable<Code> codes, String valueSet) {
        var mode = this.getRetrieveSettings().getTerminologyParameterMode();
        switch (mode) {
            case CQL:
                config.filter = config.filter.and(filterByTerminology(dataType, codePath, codes, valueSet));
                break;
            case AUTO: // TODO: offload detection based on CapabilityStatement
            case INLINE:
            case REPOSITORY:
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
            case CQL:
            case AUTO:  // TODO: offload detection based on CapabilityStatement
                if (datePath != null) {
                    throw new UnsupportedOperationException("in-memory dateFilters are not supported");
                }
                break;
            case REPOSITORY:
                populateDateSearchParams(config.searchParams, dataType, datePath, dateLowPath, dateHighPath, dateRange);
        }
    }

    private class SearchConfig {
        public Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();
        public Predicate<IBaseResource> filter = x -> true;
    }
}
