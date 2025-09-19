package org.opencds.cqf.fhir.cr.hapi.config;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.repository.BundleProviderUtil;
import ca.uhn.fhir.jpa.repository.HapiFhirRepository;
import ca.uhn.fhir.jpa.repository.SearchConverter;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.IRepositoryFactory;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.google.common.collect.Multimap;
import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfig {

    @Bean
    IRepositoryFactory repositoryFactory(DaoRegistry daoRegistry, RestfulServer restfulServer) {
        return rd -> new HackedRepository(daoRegistry, rd, restfulServer);
    }

    // This is a workaround for the fact that HapiFhirRepository drops
    // the _count parameter from searches, which means only partial results are
    // returned when doing synchronous searches that results in a SimpleBundleProvider.
    // Upstream, we need to fix the hapi-fhir code to allow passing _count through, particularly
    // in cases where we are using a SystemRequestDetails that results in a SimpleBundleProvider.
    class HackedRepository extends HapiFhirRepository {
        private final RequestDetails requestDetails;
        private final RestfulServer restfulServer;
        private final DaoRegistry daoRegistry;

        public HackedRepository(DaoRegistry daoRegistry, RequestDetails requestDetails, RestfulServer restfulServer) {
            super(daoRegistry, requestDetails, restfulServer);
            this.requestDetails = requestDetails;
            this.restfulServer = restfulServer;
            this.daoRegistry = daoRegistry;
        }

        @Override
        public <B extends IBaseBundle, T extends IBaseResource> B search(
                Class<B> bundleType,
                Class<T> resourceType,
                Multimap<String, List<IQueryParameterType>> searchParameters,
                Map<String, String> headers) {
            RequestDetails details = HackedDetailsCloner.startWith(requestDetails)
                    .setAction(RestOperationTypeEnum.SEARCH_TYPE)
                    .addHeaders(headers)
                    .create();
            // This adds a 10,000 count to searches that are system level and would
            // otherwise result in a SimpleBundleProvider that only returns 50 resutlts.
            if (details instanceof SystemRequestDetails) {
                details.addParameter("_count", new String[] {"10000"});
            }
            SearchConverter converter = new SearchConverter();
            converter.convertParameters(searchParameters, fhirContext());
            details.setParameters(converter.myResultParameters);
            details.setResourceName(daoRegistry.getFhirContext().getResourceType(resourceType));
            IBundleProvider bundleProvider =
                    daoRegistry.getResourceDao(resourceType).search(converter.mySearchParameterMap, details);

            if (bundleProvider == null) {
                return null;
            }

            return createBundle(details, bundleProvider, null);
        }

        private <B extends IBaseBundle> B createBundle(
                RequestDetails requestDetails, @Nonnull IBundleProvider bundleProvider, String pagingAction) {
            Integer count = RestfulServerUtils.extractCountParameter(requestDetails);
            String linkSelf = RestfulServerUtils.createLinkSelf(requestDetails.getFhirServerBase(), requestDetails);

            Set<Include> includes = new HashSet<>();
            String[] reqIncludes = requestDetails.getParameters().get(Constants.PARAM_INCLUDE);
            if (reqIncludes != null) {
                for (String nextInclude : reqIncludes) {
                    includes.add(new Include(nextInclude));
                }
            }

            Integer offset =
                    RestfulServerUtils.tryToExtractNamedParameter(requestDetails, Constants.PARAM_PAGINGOFFSET);
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
                    restfulServer,
                    requestDetails,
                    count,
                    linkSelf,
                    includes,
                    bundleProvider,
                    start,
                    bundleType,
                    pagingAction));
        }

        @SuppressWarnings("unchecked")
        private static <T> T unsafeCast(Object object) {
            return (T) object;
        }
    }

    // This class is copied from HAPI too, it's just along for the ride with the hacked repository above.
    // once the above is fixed upstream, this can go away too.
    class HackedDetailsCloner {

        private HackedDetailsCloner() {}

        static DetailsBuilder startWith(RequestDetails details) {
            RequestDetails newDetails;
            if (details instanceof ServletRequestDetails servletDetails) {
                newDetails = new ServletRequestDetails(servletDetails);
            } else {
                newDetails = new SystemRequestDetails(details);
            }
            newDetails.setRequestType(RequestTypeEnum.POST);
            newDetails.setOperation(null);
            newDetails.setResource(null);
            newDetails.setParameters(new HashMap<>());
            newDetails.setResourceName(null);
            newDetails.setCompartmentName(null);
            newDetails.setResponse(details.getResponse());

            return new DetailsBuilder(newDetails);
        }

        static class DetailsBuilder {
            private final RequestDetails details;

            DetailsBuilder(RequestDetails details) {
                this.details = details;
            }

            DetailsBuilder setAction(RestOperationTypeEnum restOperationType) {
                details.setRestOperationType(restOperationType);
                return this;
            }

            DetailsBuilder addHeaders(Map<String, String> headers) {
                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        details.addHeader(entry.getKey(), entry.getValue());
                    }
                }

                return this;
            }

            DetailsBuilder setParameters(IBaseParameters parameters) {
                IParser parser = details.getServer().getFhirContext().newJsonParser();
                details.setRequestContents(
                        parser.encodeResourceToString(parameters).getBytes());

                return this;
            }

            DetailsBuilder setParameters(Map<String, String[]> parameters) {
                details.setParameters(parameters);

                return this;
            }

            DetailsBuilder withRestOperationType(RequestTypeEnum type) {
                details.setRequestType(type);

                return this;
            }

            DetailsBuilder setOperation(String operation) {
                details.setOperation(operation);

                return this;
            }

            DetailsBuilder setResourceType(String resourceName) {
                details.setResourceName(resourceName);

                return this;
            }

            DetailsBuilder setId(IIdType id) {
                details.setId(id);

                return this;
            }

            RequestDetails create() {
                return details;
            }
        }
    }
}
