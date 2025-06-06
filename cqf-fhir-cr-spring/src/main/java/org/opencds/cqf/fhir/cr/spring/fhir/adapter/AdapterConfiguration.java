package org.opencds.cqf.fhir.cr.spring.fhir.adapter;

import ca.uhn.fhir.context.FhirContext;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdapterConfiguration {

    @Bean
    IAdapterFactory adapterFactory(FhirContext fhirContext) {
        switch (fhirContext.getVersion().getVersion()) {
            case DSTU3:
                return new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();
            case R4:
                return new org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory();
            case R5:
                return new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();
            case DSTU2:
            case DSTU2_1:
            case DSTU2_HL7ORG:
            default:
                throw new UnsupportedOperationException("FHIR version %s is not supported."
                        .formatted(fhirContext.getVersion().getVersion().toString()));
        }
    }
}
