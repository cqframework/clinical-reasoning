package org.opencds.cqf.cql.evaluator.spring.builder;

import java.util.function.Supplier;

import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.opencds.cqf.cql.evaluator.builder")
public class BuilderConfiguration {
    
    @Bean
    RetrieveProviderConfig retrieveProviderConfig() {
        return RetrieveProviderConfig.defaultConfig();
    }

    @Bean
    Supplier<CqlEvaluatorBuilder> cqlEvaluatorBuilderSupplier() {
        return () -> new CqlEvaluatorBuilder();
    }
}
