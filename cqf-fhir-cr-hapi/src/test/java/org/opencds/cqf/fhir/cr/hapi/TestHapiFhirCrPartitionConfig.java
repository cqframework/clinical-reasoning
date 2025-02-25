package org.opencds.cqf.fhir.cr.hapi;

import org.springframework.context.annotation.Bean;

public class TestHapiFhirCrPartitionConfig {
    @Bean
    public PartitionHelper partitionHelper() {
        return new PartitionHelper();
    }
}
