package ca.uhn.fhir.cr.repo;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.api.BundleLinks;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.IRestfulServer;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * This class pulls existing methods from the BaseResourceReturningMethodBinding class used for taking
 * the results of a BundleProvider and turning it into a Bundle.  It is intended to be used only by the
 * HapiFhirRepository.
 */
public class BundleProviderUtil {
    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(BundleProviderUtil.class);

    private BundleProviderUtil() {}

    public static IBaseResource createBundleFromBundleProvider(
            IRestfulServer<?> server,
            RequestDetails request,
            Integer limit,
            String linkSelf,
            Set<Include> includes,
            IBundleProvider result,
            int offset,
            BundleTypeEnum bundleType,
            String searchId) {
        IVersionSpecificBundleFactory bundleFactory = server.getFhirContext().newBundleFactory();
        final Integer offsetToUse;
        Integer limitToUse = limit;

        if (result.getCurrentPageOffset() != null) {
            offsetToUse = result.getCurrentPageOffset();
            limitToUse = result.getCurrentPageSize();
            Validate.notNull(
                    limitToUse, "IBundleProvider returned a non-null offset, but did not return a non-null page size");
        } else {
            offsetToUse = RestfulServerUtils.tryToExtractNamedParameter(request, Constants.PARAM_OFFSET);
        }

        int numToReturn;
        String searchIdToUse = null;
        List<IBaseResource> resourceList;
        Integer numTotalResults = result.size();

        int pageSize;
        if (offsetToUse != null || !server.canStoreSearchResults()) {
            if (limitToUse != null) {
                pageSize = limitToUse;
            } else {
                if (server.getDefaultPageSize() != null) {
                    pageSize = server.getDefaultPageSize();
                } else {
                    pageSize = numTotalResults != null ? numTotalResults : Integer.MAX_VALUE;
                }
            }
            numToReturn = pageSize;

            if (offsetToUse != null || result.getCurrentPageOffset() != null) {
                // When offset query is done result already contains correct amount (+ ir includes
                // etc.) so return everything
                resourceList = result.getResources(0, Integer.MAX_VALUE);
            } else if (numToReturn > 0) {
                resourceList = result.getResources(0, numToReturn);
            } else {
                resourceList = Collections.emptyList();
            }
            RestfulServerUtils.validateResourceListNotNull(resourceList);

        } else {
            IPagingProvider pagingProvider = server.getPagingProvider();
            if (limitToUse == null || limitToUse.equals(0)) {
                pageSize = pagingProvider.getDefaultPageSize();
            } else {
                pageSize = Math.min(pagingProvider.getMaximumPageSize(), limitToUse);
            }
            numToReturn = pageSize;

            if (numTotalResults != null) {
                numToReturn = Math.min(numToReturn, numTotalResults - offset);
            }

            if (numToReturn > 0 || result.getCurrentPageId() != null) {
                resourceList = result.getResources(offset, numToReturn + offset);
            } else {
                resourceList = Collections.emptyList();
            }
            RestfulServerUtils.validateResourceListNotNull(resourceList);

            if (numTotalResults == null) {
                numTotalResults = result.size();
            }

            if (searchId != null) {
                searchIdToUse = searchId;
            } else {
                if (numTotalResults == null || numTotalResults > numToReturn) {
                    searchIdToUse = pagingProvider.storeResultList(request, result);
                    if (isBlank(searchIdToUse)) {
                        ourLog.info(
                                "Found {} results but paging provider did not provide an ID to use for paging",
                                numTotalResults);
                        searchIdToUse = null;
                    }
                }
            }
        }

        /*
         * Remove any null entries in the list - This generally shouldn't happen but can if data has
         * been manually purged from the JPA database
         */
        boolean hasNull = false;
        for (IBaseResource next : resourceList) {
            if (next == null) {
                hasNull = true;
                break;
            }
        }
        if (hasNull) {
            resourceList.removeIf(Objects::isNull);
        }

        /*
         * Make sure all returned resources have an ID (if not, this is a bug in the user server code)
         */
        for (IBaseResource next : resourceList) {
            if ((next.getIdElement() == null || next.getIdElement().isEmpty())
                    && !(next instanceof IBaseOperationOutcome)) {
                throw new InternalErrorException(Msg.code(2311)
                        + "Server method returned resource of type["
                        + next.getClass().getSimpleName()
                        + "] with no ID specified (IResource#setId(IdDt) must be called)");
            }
        }

        BundleLinks links = new BundleLinks(
                request.getFhirServerBase(),
                includes,
                RestfulServerUtils.prettyPrintResponse(server, request),
                bundleType);
        links.setSelf(linkSelf);

        if (result.getCurrentPageOffset() != null) {

            if (isNotBlank(result.getNextPageId())) {
                links.setNext(RestfulServerUtils.createOffsetPagingLink(
                        links,
                        request.getRequestPath(),
                        request.getTenantId(),
                        offsetToUse + limitToUse,
                        limitToUse,
                        request.getParameters()));
            }
            if (isNotBlank(result.getPreviousPageId())) {
                links.setNext(RestfulServerUtils.createOffsetPagingLink(
                        links,
                        request.getRequestPath(),
                        request.getTenantId(),
                        Math.max(offsetToUse - limitToUse, 0),
                        limitToUse,
                        request.getParameters()));
            }
        }

        if (offsetToUse != null || (!server.canStoreSearchResults() && !isEverythingOperation(request))) {
            // Paging without caching
            // We're doing offset pages
            int requestedToReturn = numToReturn;
            if (server.getPagingProvider() == null && offsetToUse != null) {
                // There is no paging provider at all, so assume we're querying up to all the results we
                // need every time
                requestedToReturn += offsetToUse;
            }
            if ((numTotalResults == null || requestedToReturn < numTotalResults) && !resourceList.isEmpty()) {
                links.setNext(RestfulServerUtils.createOffsetPagingLink(
                        links,
                        request.getRequestPath(),
                        request.getTenantId(),
                        defaultIfNull(offsetToUse, 0) + numToReturn,
                        numToReturn,
                        request.getParameters()));
            }

            if (offsetToUse != null && offsetToUse > 0) {
                int start = Math.max(0, offset - pageSize);
                links.setPrev(RestfulServerUtils.createOffsetPagingLink(
                        links,
                        request.getRequestPath(),
                        request.getTenantId(),
                        start,
                        pageSize,
                        request.getParameters()));
            }
        } else if (isNotBlank(result.getCurrentPageId())) {
            // We're doing named pages
            searchIdToUse = result.getUuid();
            if (isNotBlank(result.getNextPageId())) {
                links.setNext(RestfulServerUtils.createPagingLink(
                        links, request, searchIdToUse, result.getNextPageId(), request.getParameters()));
            }
            if (isNotBlank(result.getPreviousPageId())) {
                links.setPrev(RestfulServerUtils.createPagingLink(
                        links, request, searchIdToUse, result.getPreviousPageId(), request.getParameters()));
            }
        } else if (searchIdToUse != null && !resourceList.isEmpty()) {
            if (numTotalResults == null || offset + numToReturn < numTotalResults) {
                links.setNext((RestfulServerUtils.createPagingLink(
                        links, request, searchIdToUse, offset + numToReturn, numToReturn, request.getParameters())));
            }
            if (offset > 0) {
                int start = Math.max(0, offset - pageSize);
                links.setPrev(RestfulServerUtils.createPagingLink(
                        links, request, searchIdToUse, start, pageSize, request.getParameters()));
            }
        }

        bundleFactory.addRootPropertiesToBundle(result.getUuid(), links, result.size(), result.getPublished());
        bundleFactory.addResourcesToBundle(
                new ArrayList<>(resourceList), bundleType, links.serverBase, server.getBundleInclusionRule(), includes);

        return bundleFactory.getResourceBundle();
    }

    private static boolean isEverythingOperation(RequestDetails request) {
        return (request.getRestOperationType() == RestOperationTypeEnum.EXTENDED_OPERATION_TYPE
                        || request.getRestOperationType() == RestOperationTypeEnum.EXTENDED_OPERATION_INSTANCE)
                && request.getOperation() != null
                && request.getOperation().equals("$everything");
    }
}
