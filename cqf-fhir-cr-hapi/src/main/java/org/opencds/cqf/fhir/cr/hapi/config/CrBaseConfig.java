package org.opencds.cqf.fhir.cr.hapi.config;

import java.time.ZoneOffset;
import java.util.Optional;
import org.opencds.cqf.fhir.cql.NpmResourceHolderGetter;
import org.opencds.cqf.fhir.cql.NpmResourceHolderGetterNoOp;
import org.opencds.cqf.fhir.cr.hapi.common.StringTimePeriodHandler;
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

    /**
     * Hackish:  Either the downstream app injected this or we default to a NO-OP implementation.
     *
     * @param optNpmResourceHolderGetter The NpmResourceHolderGetter, if injected by the downstream app, otherwise empty.
     * @return Either the downstream app's NpmResourceHolderGetter or a no-op implementation.
     */
    public static NpmResourceHolderGetter npmResourceHolderGetter(
            Optional<NpmResourceHolderGetter> optNpmResourceHolderGetter) {
        return optNpmResourceHolderGetter.orElse(NpmResourceHolderGetterNoOp.INSTANCE);
    }
}
