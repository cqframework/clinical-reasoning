package org.opencds.cqf.fhir.cr.hapi.repository;

import static ca.uhn.fhir.model.valueset.BundleTypeEnum.SEARCHSET;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.repository.HapiFhirRepository;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import com.google.common.collect.Multimap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is intended as a shim that extends HapiFhirRepository in order to selectively
 * override any behaviour resulting from any bugs discovered in that class in any given hapi-fhir
 * release.
 * <p/>
 * Since hapi-fhir is released quarterly, we want to maintain flexibility to change behaviour
 * within our more flexible release cadence.
 */
@SuppressWarnings("UnstableApiUsage")
public class ClinicalIntelligenceHapiFhirRepository extends HapiFhirRepository {

    private static final Logger logger = LoggerFactory.getLogger(ClinicalIntelligenceHapiFhirRepository.class);

    private final RequestDetails requestDetails;
    private final RestfulServer restfulServer;
    private final DaoRegistry daoRegistry;

    public ClinicalIntelligenceHapiFhirRepository(
            DaoRegistry daoRegistry, RequestDetails requestDetails, RestfulServer restfulServer) {
        super(daoRegistry, requestDetails, restfulServer);
        this.requestDetails = requestDetails;
        this.restfulServer = restfulServer;
        this.daoRegistry = daoRegistry;
    }

    /**
     * Override {@link HapiFhirRepository#search(Class, Class, Multimap, Map)} to ensure that the
     * _count {@link RequestDetails} parameter is passed through to the DAO layer instead of
     * dropping it.
     * <p/>
     * We need to keep the _count parameter for system-level searches that use a
     * SystemRequestDetails, which results in a SimpleBundleProvider that only returns
     * 50 results by default, which means only partial results are returned when doing synchronous
     * searches that results in a SimpleBundleProvider.
     */
    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> bundleType,
            Class<T> resourceType,
            Multimap<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {

        var details = ClinicalIntelligenceRequestDetailsCloner.startWith(requestDetails)
                .setAction(RestOperationTypeEnum.SEARCH_TYPE)
                .addHeaders(headers)
                .create();

        var converter = new SearchConverter();
        converter.convertParameters(searchParameters);
        details.setParameters(converter.resultParameters);

        details.setResourceName(daoRegistry.getFhirContext().getResourceType(resourceType));

        if (details instanceof SystemRequestDetails) {
            requestDetails.getParameters().entrySet().stream()
                    .filter(param -> Constants.PARAM_COUNT.equals(param.getKey()))
                    .map(Entry::getValue)
                    .forEach(paramValue -> details.addParameter(Constants.PARAM_COUNT, paramValue));
        }

        var resourceDao = daoRegistry.getResourceDao(resourceType);
        var bundleProvider = resourceDao.search(converter.searchParameterMap, details);

        bundleProvider = sanitizeBundleProvider(bundleProvider);

        return createBundle(details, bundleProvider);
    }

    protected IBundleProvider sanitizeBundleProvider(IBundleProvider bundleProvider) {
        return nonNull(bundleProvider) ? bundleProvider : new SimpleBundleProvider();
    }

    protected Set<Include> extractIncludesFromRequestParameters(Map<String, String[]> parameters) {
        Set<Include> includes = new HashSet<>();

        String[] reqIncludes = parameters.get(Constants.PARAM_INCLUDE);
        if (reqIncludes != null) {
            for (String nextInclude : reqIncludes) {
                includes.add(new Include(nextInclude));
            }
        }

        return includes;
    }

    protected BundleTypeEnum extractBundleTypeFromRequestParameters(Map<String, String[]> parameters) {
        BundleTypeEnum bundleType;

        String[] bundleTypeValues = parameters.get(Constants.PARAM_BUNDLETYPE);

        if (isNull(bundleTypeValues)) {
            return SEARCHSET;
        }

        bundleType = BundleTypeEnum.VALUESET_BINDER.fromCodeString(bundleTypeValues[0]);

        if (isNull(bundleType)) {
            logger.error(
                    "Could not convert value {} to a BundleTypeEnum.  Defaulting to {}",
                    bundleTypeValues[0],
                    bundleType);
            bundleType = SEARCHSET;
        }

        return bundleType;
    }

    private <B extends IBaseBundle> B createBundle(RequestDetails requestDetails, IBundleProvider bundleProvider) {

        Integer count = RestfulServerUtils.extractCountParameter(requestDetails);
        String linkSelf = RestfulServerUtils.createLinkSelf(requestDetails.getFhirServerBase(), requestDetails);

        Set<Include> includes = extractIncludesFromRequestParameters(requestDetails.getParameters());

        Integer offset = RestfulServerUtils.tryToExtractNamedParameter(requestDetails, Constants.PARAM_PAGINGOFFSET);
        if (offset == null || offset < 0) {
            offset = 0;
        }
        int start = offset;
        Integer size = bundleProvider.size();
        if (size != null) {
            start = Math.max(0, Math.min(offset, size));
        }

        BundleTypeEnum bundleType = extractBundleTypeFromRequestParameters(requestDetails.getParameters());

        return unsafeCast(ClinicalIntelligenceBundleProviderUtil.createBundleFromBundleProvider(
                restfulServer, requestDetails, count, linkSelf, includes, bundleProvider, start, bundleType, null));
    }

    @SuppressWarnings("unchecked")
    private static <T> T unsafeCast(Object object) {
        return (T) object;
    }
}
