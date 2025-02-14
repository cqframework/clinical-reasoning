package ca.uhn.fhir.cr.config;

import ca.uhn.fhir.rest.server.RestfulServer;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * The purpose of this Condition is to verify that the CR dependent beans RestfulServer and EvaluationSettings exist.
 */
public class CrConfigCondition implements Condition {
    private static final Logger ourLog = LoggerFactory.getLogger(CrConfigCondition.class);

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        ConfigurableListableBeanFactory beanFactory = conditionContext.getBeanFactory();
        try {
            RestfulServer bean = beanFactory.getBean(RestfulServer.class);
            if (bean == null) {
                return false;
            }
        } catch (Exception e) {
            ourLog.warn("CrConfigCondition not met: Missing RestfulServer bean");
            return false;
        }
        try {
            EvaluationSettings bean = beanFactory.getBean(EvaluationSettings.class);
            if (bean == null) {
                return false;
            }
        } catch (Exception e) {
            ourLog.warn("CrConfigCondition not met: Missing EvaluationSettings bean");
            return false;
        }
        return true;
    }
}
