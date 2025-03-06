package org.opencds.cqf.fhir.cr.hapi.config;

import org.opencds.cqf.fhir.cr.hapi.config.r4.ApplyOperationConfig;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Conditional(CrConfigCondition.class)
@Import({RepositoryConfig.class, ApplyOperationConfig.class})
public class CrCdsConfig {}
