package org.opencds.cqf.cql.evaluator.spring.fhir;

import org.opencds.cqf.cql.evaluator.spring.fhir.adapter.AdapterConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan("org.opencds.cqf.cql.evaluator.fhir")
@Import(AdapterConfiguration.class)
public class FhirConfiguration {
    
}
