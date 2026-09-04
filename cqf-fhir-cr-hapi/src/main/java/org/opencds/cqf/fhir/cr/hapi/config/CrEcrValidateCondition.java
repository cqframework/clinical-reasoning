package org.opencds.cqf.fhir.cr.hapi.config;

import ca.uhn.fhir.validation.FhirValidator;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that gates the creation of a {@link FhirValidator} configured to load NPM packages provided by {@link CrSettings}. Based on the {@code cr.ecr.validate.enabled}
 * property (or the equivalent {@code CR_ECR_VALIDATE_ENABLED} environment variable).
 *
 * <p>Set {@code cr.ecr.validate.enabled=true} (or export {@code CR_ECR_VALIDATE_ENABLED=true}) to
 * enable the creation of this {@link FhirValidator}.
 */
public class CrEcrValidateCondition implements Condition {
    private static final Logger ourLog = LoggerFactory.getLogger(CrEcrValidateCondition.class);

    static final String PROPERTY_NAME = "cr.ecr.validate.enabled";

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        var environment = conditionContext.getEnvironment();
        var enabled = environment.getProperty(PROPERTY_NAME, Boolean.class, false);
        if (!enabled) {
            ourLog.info(
                    "CrEcrValidateCondition not met: '{}' is not set to true. "
                            + "Custom FhirValidator will not be created.",
                    PROPERTY_NAME);
        }
        return enabled;
    }
}
