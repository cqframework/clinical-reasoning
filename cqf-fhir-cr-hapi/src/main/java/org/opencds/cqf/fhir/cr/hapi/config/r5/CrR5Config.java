package org.opencds.cqf.fhir.cr.hapi.config.r5;

import org.opencds.cqf.fhir.cr.hapi.config.RepositoryConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    RepositoryConfig.class,
    CqlOperationConfig.class,
    EvaluateOperationConfig.class,
    ReleaseOperationConfig.class,
    DeleteOperationConfig.class,
    RetireOperationConfig.class,
    WithdrawOperationConfig.class,
    ReviseOperationConfig.class,
    ArtifactDiffOperationConfig.class,
    CreateChangelogOperationConfig.class
})
public class CrR5Config {}
