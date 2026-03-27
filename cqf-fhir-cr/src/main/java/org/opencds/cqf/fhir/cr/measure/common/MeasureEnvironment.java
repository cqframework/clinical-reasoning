package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;

/**
 * Version-agnostic environment configuration for measure evaluation.
 *
 * <p>Separates the <em>infrastructure inputs</em> (where data, content, and terminology come from)
 * from the <em>operation parameters</em> (what to evaluate). Per the pipeline architecture,
 * environment resolution happens before domain logic: the service layer composes a
 * {@code ProxyRepository} from these endpoints and passes it to the evaluator.
 *
 * <p>Endpoint resources are typed as {@link IBaseResource} so both R4 and DSTU3
 * {@code Endpoint} instances can be carried without version coupling.
 * {@code Repositories.proxy()} already accepts {@code IBaseResource} endpoints directly.
 *
 * @param contentEndpoint     endpoint for library/content resolution (nullable)
 * @param terminologyEndpoint endpoint for terminology resolution (nullable)
 * @param dataEndpoint        endpoint for clinical data retrieval (nullable)
 * @param additionalData      supplemental data bundle for repository federation and CQL engine
 *                            configuration (nullable)
 */
public record MeasureEnvironment(
        @Nullable IBaseResource contentEndpoint,
        @Nullable IBaseResource terminologyEndpoint,
        @Nullable IBaseResource dataEndpoint,
        @Nullable IBaseBundle additionalData) {

    /** Empty environment — no endpoints, no additional data. */
    public static final MeasureEnvironment EMPTY = new MeasureEnvironment(null, null, null, null);

    /**
     * Resolves this environment against a base repository.
     *
     * <p>If any endpoint is present, wraps {@code base} in a {@code ProxyRepository}; null
     * endpoints fall back to {@code base}. If {@code additionalData} is present, federates
     * the result with an in-memory repository seeded from that bundle.
     *
     * @param base the base repository to build on top of
     * @return the resolved repository, possibly wrapped
     */
    public IRepository resolve(IRepository base) {
        IRepository repo = base;
        if (dataEndpoint() != null || contentEndpoint() != null || terminologyEndpoint() != null) {
            repo = Repositories.proxy(repo, true, dataEndpoint(), contentEndpoint(), terminologyEndpoint());
        }
        if (additionalData() != null) {
            repo = new FederatedRepository(repo, new InMemoryFhirRepository(repo.fhirContext(), additionalData()));
        }
        return repo;
    }
}
