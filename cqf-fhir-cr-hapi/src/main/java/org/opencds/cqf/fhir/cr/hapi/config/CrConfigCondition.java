package org.opencds.cqf.fhir.cr.hapi.config;

import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
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

        if (beanFactory == null) {
            throw new InternalErrorException("Unable to create bean CrConfigCondition: Missing bean factory");
        }

        try {
            beanFactory.getBean(RestfulServer.class);
        } catch (Exception e) {
            ourLog.warn("CrConfigCondition not met: Missing RestfulServer bean");
            return false;
        }
        try {
            beanFactory.getBean(EvaluationSettings.class);
        } catch (Exception e) {
            ourLog.warn("CrConfigCondition not met: Missing EvaluationSettings bean");
            return false;
        }

        try{
            beanFactory.getBean(TerminologyServerClientSettings.class);
        } catch (Exception e) {
            ourLog.warn("CrConfigCondition not met: Missing TerminologyServerClientSettings bean");
            return false;
        }
        return true;
    }
}
