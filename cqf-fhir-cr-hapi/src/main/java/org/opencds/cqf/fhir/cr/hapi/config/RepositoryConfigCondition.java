package org.opencds.cqf.fhir.cr.hapi.config;

import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class RepositoryConfigCondition implements Condition {
    private static final Logger ourLog = LoggerFactory.getLogger(RepositoryConfigCondition.class);

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        ConfigurableListableBeanFactory beanFactory = conditionContext.getBeanFactory();

        if (beanFactory == null) {
            throw new InternalErrorException("Unable to create bean CrConfigCondition: Missing bean factory");
        }

        try {
            RestfulServer bean = beanFactory.getBean(RestfulServer.class);
            if (bean == null) {
                return false;
            }
        } catch (Exception e) {
            ourLog.warn("Unable to create bean IRepositoryFactory: Missing RestfulServer");
            return false;
        }
        return true;
    }
}
