package org.opencds.cqf.cql.evaluator.spring.library;

import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;

@Configuration
@ComponentScan("org.opencds.cqf.cql.evaluator.library")
public class LibraryConfiguration {

    @Bean
    FhirTypeConverter fhirTypeConverter(FhirContext fhirContext) {
        return new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
    }
    
}
