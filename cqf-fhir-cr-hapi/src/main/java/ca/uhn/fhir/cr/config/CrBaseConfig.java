package ca.uhn.fhir.cr.config;

import ca.uhn.fhir.cr.common.StringTimePeriodHandler;
import java.time.ZoneOffset;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrBaseConfig {

    @Bean
    StringTimePeriodHandler stringTimePeriodHandler() {
        return new StringTimePeriodHandler(ZoneOffset.UTC);
    }

    @Bean
    MeasurePeriodValidator measurePeriodValidator() {
        return new MeasurePeriodValidator();
    }
}
