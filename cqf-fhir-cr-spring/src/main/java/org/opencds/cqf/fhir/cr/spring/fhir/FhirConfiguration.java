package org.opencds.cqf.fhir.cr.spring.fhir;

import org.opencds.cqf.fhir.cr.spring.fhir.adapter.AdapterConfiguration;
import org.opencds.cqf.fhir.cr.spring.fhir.npm.NpmConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan("org.opencds.cqf.fhir.cr.fhir")
@Import({AdapterConfiguration.class, NpmConfiguration.class})
public class FhirConfiguration {

}
