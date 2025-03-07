package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.model.api.IModelJson;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.hapi.fhir.cdshooks.api.ICdsMethod;
import com.fasterxml.jackson.databind.ObjectMapper;

abstract class BaseCdsCrMethod implements ICdsMethod {
    private final ICdsCrServiceFactory cdsCrServiceFactory;

    protected BaseCdsCrMethod(ICdsCrServiceFactory cdsCrServiceFactory) {
        this.cdsCrServiceFactory = cdsCrServiceFactory;
    }

    public Object invoke(ObjectMapper objectMapper, IModelJson json, String serviceId) {
        try {
            return cdsCrServiceFactory.create(serviceId).invoke(json);
        } catch (Exception e) {
            if (e.getCause() instanceof BaseServerResponseException baseServerResponseException) {
                throw baseServerResponseException;
            }
            throw new ConfigurationException(Msg.code(2434) + "Failed to invoke $apply on " + serviceId, e);
        }
    }
}
