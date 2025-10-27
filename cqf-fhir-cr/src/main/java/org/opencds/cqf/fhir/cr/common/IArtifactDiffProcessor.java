package org.opencds.cqf.fhir.cr.common;

import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Endpoint;
import org.opencds.cqf.fhir.cr.common.ArtifactDiffProcessor.DiffCache;

public interface IArtifactDiffProcessor {

    IBaseParameters getArtifactDiff(
            IBaseResource sourceResource,
            IBaseResource targetResource,
            Boolean compareComputable,
            Boolean compareExecutable,
            DiffCache cache,
            Endpoint terminologyEndpoint);
}
