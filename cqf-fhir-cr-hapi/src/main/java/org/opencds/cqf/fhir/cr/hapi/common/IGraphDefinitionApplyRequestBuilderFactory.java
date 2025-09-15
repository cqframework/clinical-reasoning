package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.graphdefinition.apply.ApplyRequestBuilder;

@FunctionalInterface
public interface IGraphDefinitionApplyRequestBuilderFactory {

    ApplyRequestBuilder createApplyRequestBuilder(RequestDetails requestDetails);
}
