package org.opencds.cqf.fhir.cql.engine.retrieve;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.iterable.BundleIterable;
import org.opencds.cqf.fhir.utility.iterable.BundleMappingIterable;
import org.opencds.cqf.fhir.utility.search.Searches;

public class RepositoryRetrieveProvider extends RetrieveProvider {
    private final Repository repository;
    private final FhirContext fhirContext;

    public RepositoryRetrieveProvider(final Repository repository, RetrieveSettings settings) {
        super(repository.fhirContext());
        this.repository = requireNonNull(repository, "repository can not be null.");
        this.fhirContext = repository.fhirContext();
        this.setSearchByTemplate(settings.getSearchByTemplate());
        this.setFilterBySearchParam(settings.getFilterBySearchParam());
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

        if (isFilterBySearchParam()) {
            Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();
            populateTemplateSearchParams(searchParams, templateId);
            populateContextSearchParams(searchParams, dataType, contextPath, context, contextValue);
            populateTerminologySearchParams(searchParams, dataType, codePath, codes, valueSet);
            populateDateSearchParams(searchParams, dataType, datePath, dateLowPath, dateHighPath, dateRange);
            var resources = this.repository.search(bt, resourceType, searchParams);

            return new BundleMappingIterable<>(repository, resources, p -> p.getResource());
        } else {
            var resources = this.repository.search(bt, resourceType, Searches.ALL);
            var iter = new BundleIterable<IBaseBundle>(repository, resources);
            return iter.toStream()
                    .map(x -> x.getResource())
                    .filter(filterByTemplateId(dataType, templateId))
                    .filter(filterByContext(dataType, context, contextPath, contextValue))
                    .filter(filterByTerminology(dataType, codePath, codes, valueSet))
                    // TODO: filter by date search
                    .collect(Collectors.toList());
        }
    }
}
