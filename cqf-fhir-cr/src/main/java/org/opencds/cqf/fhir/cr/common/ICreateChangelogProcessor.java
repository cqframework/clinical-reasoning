package org.opencds.cqf.fhir.cr.common;

import org.hl7.fhir.instance.model.api.IBaseResource;

public interface ICreateChangelogProcessor extends IOperationProcessor {

    IBaseResource createChangelog(IBaseResource source, IBaseResource target, IBaseResource terminologyEndpoint);
}
