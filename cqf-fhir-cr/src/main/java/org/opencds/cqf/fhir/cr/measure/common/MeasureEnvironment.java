package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

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
}
