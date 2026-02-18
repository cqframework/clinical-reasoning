package org.opencds.cqf.fhir.cr.common;

import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.ArtifactDiffProcessor.DiffCache;

public interface IArtifactDiffProcessor extends IOperationProcessor {

    IBaseParameters getArtifactDiff(
            IBaseResource sourceResource,
            IBaseResource targetResource,
            Boolean compareComputable,
            Boolean compareExecutable,
            DiffCache cache,
            IBaseResource terminologyEndpoint);
}
