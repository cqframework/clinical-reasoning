package org.opencds.cqf.fhir.cql.engine.retrieve;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.util.BundleUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.Repositories;

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

        List<? extends IBaseResource> resources;
        var resourceType = fhirContext.getResourceDefinition(dataType).getImplementingClass();

        if (isFilterBySearchParam()) {
            Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();
            populateTemplateSearchParams(searchParams, templateId);
            populateContextSearchParams(searchParams, contextPath, context, contextValue);
            populateTerminologySearchParams(searchParams, codePath, codes, valueSet);
            populateDateSearchParams(searchParams, datePath, dateLowPath, dateHighPath, dateRange);
            resources = BundleUtil.toListOfResources(
                    fhirContext,
                    Repositories.searchRepositoryWithPaging(fhirContext, repository, resourceType, searchParams, null));
        } else {
            resources = BundleUtil.toListOfResources(
                            fhirContext,
                            Repositories.searchRepositoryWithPaging(fhirContext, repository, resourceType, null, null))
                    .stream()
                    .filter(filterByTemplateId(dataType, templateId))
                    .filter(filterByContext(dataType, context, contextPath, contextValue))
                    .filter(filterByTerminology(dataType, codePath, codes, valueSet))
                    .collect(Collectors.toList());
        }

        return resources.stream().map(Object.class::cast).collect(Collectors.toList());
    }
}
