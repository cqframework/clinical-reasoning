package org.opencds.cqf.fhir.cr.hapi.config;

import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

public class ProviderLoader {
    private static final Logger logger = LoggerFactory.getLogger(ProviderLoader.class);
    private final ApplicationContext applicationContext;
    private final ProviderSelector providerSelector;
    private final RestfulServer restfulServer;

    public ProviderLoader(
            RestfulServer restfulServer, ApplicationContext applicationContext, ProviderSelector providerSelector) {
        this.applicationContext = applicationContext;
        this.providerSelector = providerSelector;
        this.restfulServer = restfulServer;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void loadProviders() {
        var type = providerSelector.getProviderType();
        if (type == null) {
            throw new InternalErrorException("Provider not supported for the current FHIR version");
        }
        for (Class<?> op : type) {
            logger.info("loading provider: {}", op);
            restfulServer.registerProvider(applicationContext.getBean(op));
        }
    }
}
