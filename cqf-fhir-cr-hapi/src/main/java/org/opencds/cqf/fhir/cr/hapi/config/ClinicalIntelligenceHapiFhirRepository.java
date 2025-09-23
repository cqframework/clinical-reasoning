package org.opencds.cqf.fhir.cr.hapi.config;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.repository.BundleProviderUtil;
import ca.uhn.fhir.jpa.repository.HapiFhirRepository;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import com.google.common.collect.Multimap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * This class is intended as a shim that extends HapiFhirRepository in order to selectively
 * override any behaviour resulting from any bugs discovered in that class in any given hapi-fhir
 * release.
 * <p/>
 * Since hapi-fhir is released quarterly, we want to maintain flexibility to change behaviour
 * within our more flexible release cadence.
 */
public class ClinicalIntelligenceHapiFhirRepository extends HapiFhirRepository {

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

    // LUKETODO:  unit test
    /**
     * Override {@link HapiFhirRepository#search(Class, Class, Map, Map)} to ensure that _count
     * and other request parameters (such as _sort) are passed through to the DAO layer instead
     * of dropping them.
     * <p/>
     * Regarding the _count parameter, this is particularly important for system-level searches
     * that use a SystemRequestDetails, which results in a SimpleBundleProvider that only returns
     * 50 results by default, which means only partial results are returned when doing synchronous
     * searches that results in a SimpleBundleProvider.
     */
    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(
            Class<B> bundleType,
            Class<T> resourceType,
            Multimap<String, List<IQueryParameterType>> searchParameters,
            Map<String, String> headers) {

        RequestDetails details = ClinicalIntelligenceRequestDetailsCloner.startWith(requestDetails)
                .setAction(RestOperationTypeEnum.SEARCH_TYPE)
                .addHeaders(headers)
                .create();

        ClinicalIntelligenceSearchConverter converter = new ClinicalIntelligenceSearchConverter();
        converter.convertParameters(searchParameters, fhirContext());
        details.setParameters(converter.myResultParameters);
        requestDetails.getParameters()
            .forEach(details::addParameter);

        details.setResourceName(daoRegistry.getFhirContext().getResourceType(resourceType));

        //        // LUKETODO: change this:
        //        // This adds a 10,000 count to searches that are system level and would
        //        // otherwise result in a SimpleBundleProvider that only returns 50 resutlts.
        //        if (details instanceof SystemRequestDetails) {
        //            details.addParameter("_count", new String[]{"10000"});
        //        }

        final IFhirResourceDao<T> resourceDao = daoRegistry.getResourceDao(resourceType);
        final IBundleProvider bundleProvider =
            resourceDao.search(converter.mySearchParameterMap, details);

        if (bundleProvider == null) {
            return null;
        }

        return createBundle(details, bundleProvider);
    }

    private <B extends IBaseBundle> B createBundle(RequestDetails requestDetails, IBundleProvider bundleProvider) {

        Integer count = RestfulServerUtils.extractCountParameter(requestDetails);
        String linkSelf = RestfulServerUtils.createLinkSelf(requestDetails.getFhirServerBase(), requestDetails);

        Set<Include> includes = new HashSet<>();
        String[] reqIncludes = requestDetails.getParameters().get(Constants.PARAM_INCLUDE);
        if (reqIncludes != null) {
            for (String nextInclude : reqIncludes) {
                includes.add(new Include(nextInclude));
            }
        }

        Integer offset = RestfulServerUtils.tryToExtractNamedParameter(requestDetails, Constants.PARAM_PAGINGOFFSET);
        if (offset == null || offset < 0) {
            offset = 0;
        }
        int start = offset;
        Integer size = bundleProvider.size();
        if (size != null) {
            start = Math.max(0, Math.min(offset, size));
        }

        BundleTypeEnum bundleType;
        String[] bundleTypeValues = requestDetails.getParameters().get(Constants.PARAM_BUNDLETYPE);
        if (bundleTypeValues != null) {
            bundleType = BundleTypeEnum.VALUESET_BINDER.fromCodeString(bundleTypeValues[0]);
        } else {
            bundleType = BundleTypeEnum.SEARCHSET;
        }

        return unsafeCast(BundleProviderUtil.createBundleFromBundleProvider(
                restfulServer, requestDetails, count, linkSelf, includes, bundleProvider, start, bundleType, null));
    }

    @SuppressWarnings("unchecked")
    private static <T> T unsafeCast(Object object) {
        return (T) object;
    }
}
