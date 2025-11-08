package org.opencds.cqf.fhir.cr.hapi.config;

import java.time.ZoneOffset;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.hapi.common.StringTimePeriodHandler;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrBaseConfig {
    @Bean
    CrSettings settings(
            EvaluationSettings evaluationSettings, TerminologyServerClientSettings terminologyServerClientSettings) {
        return new CrSettings()
                .withEvaluationSettings(evaluationSettings)
                .withTerminologyServerClientSettings(terminologyServerClientSettings);
    }

    @Bean
    StringTimePeriodHandler stringTimePeriodHandler() {
        return new StringTimePeriodHandler(ZoneOffset.UTC);
    }

    @Bean
    MeasurePeriodValidator measurePeriodValidator() {
        return new MeasurePeriodValidator();
    }
}
