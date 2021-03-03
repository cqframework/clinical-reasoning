package org.opencds.cqf.cql.evaluator.spring.configuration;

import org.opencds.cqf.cql.evaluator.spring.EvaluatorConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

@Configuration
@Import(EvaluatorConfiguration.class)
public class TestConfigurationR4 {

    @Bean
    FhirContext fhirContext() {
        return FhirContext.forCached(FhirVersionEnum.R4);
    }
    
}
