package org.opencds.cqf.fhir.cr.common;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Endpoint;

public interface ICreateChangelogProcessor extends IOperationProcessor {

    IBaseResource createChangelog(IBaseResource source, IBaseResource target, Endpoint terminologyEndpoint);
}
