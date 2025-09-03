/*-
 * #%L
 * Smile - Clinical Intelligence
 * %%
 * Copyright (C) 2024 - 2025 Smile Digital Health, Inc.
 * %%
 * All rights reserved.
 * #L%
 */
package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.measure.r4.R4MultiMeasureService;

@FunctionalInterface
public interface R4MultiMeasureServiceFactory {
    R4MultiMeasureService create(RequestDetails requestDetails);
}
