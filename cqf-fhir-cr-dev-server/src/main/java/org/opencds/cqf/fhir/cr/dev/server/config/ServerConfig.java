package org.opencds.cqf.fhir.cr.dev.server.config;

import org.opencds.cqf.fhir.cr.hapi.config.CrProcessorConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({CrProcessorConfig.class, ServerR4Config.class, ServerR5Config.class})
public class ServerConfig {}
