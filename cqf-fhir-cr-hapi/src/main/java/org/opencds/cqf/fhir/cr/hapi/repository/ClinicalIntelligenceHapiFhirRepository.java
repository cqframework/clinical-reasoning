package org.opencds.cqf.fhir.cr.hapi.repository;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.repository.HapiFhirRepository;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
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

    /**
     * Override {@link HapiFhirRepository#search(Class, Class, Map, Map)} to ensure that the
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

        return super.search(bundleType, resourceType, searchParameters, headers);
    }
}
