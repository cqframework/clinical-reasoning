package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Version-agnostic environment configuration for measure evaluation.
 *
 * <p>Holds the external infrastructure inputs that describe <em>where</em> data, content,
 * and terminology come from, plus any supplemental data bundle. The service layer resolves
 * these into configured repositories and engines.</p>
 *
 * <p>Endpoint resources are typed as {@link IBaseResource} so both R4 and DSTU3 Endpoint
 * instances can be carried without version coupling. {@code Repositories.proxy()} already
 * accepts {@code IBaseResource} endpoints.</p>
 *
 * @param contentEndpoint     endpoint for library/content resolution (nullable)
 * @param terminologyEndpoint endpoint for terminology resolution (nullable)
 * @param dataEndpoint        endpoint for clinical data retrieval (nullable)
 * @param additionalData      supplemental data bundle for repository federation and engine
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
