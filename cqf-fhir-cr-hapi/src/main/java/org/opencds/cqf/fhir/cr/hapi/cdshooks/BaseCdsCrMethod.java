package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.model.api.IModelJson;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.hapi.fhir.cdshooks.api.ICdsMethod;
import com.fasterxml.jackson.databind.ObjectMapper;

abstract class BaseCdsCrMethod implements ICdsMethod {
    private final ICdsCrServiceFactory cdsCrServiceFactory;

    protected BaseCdsCrMethod(ICdsCrServiceFactory cdsCrServiceFactory) {
        this.cdsCrServiceFactory = cdsCrServiceFactory;
    }

    protected ICdsCrService createCdsCrService(String serviceId) {
        return cdsCrServiceFactory.create(serviceId);
    }

    @Override
    public Object invoke(ObjectMapper objectMapper, IModelJson json, String serviceId) {
        try {
            return createCdsCrService(serviceId).invoke(json);
        } catch (Exception e) {
            if (e.getCause() instanceof BaseServerResponseException baseServerResponseException) {
                throw baseServerResponseException;
            }
            throw new InternalErrorException("Failed to invoke $apply on " + serviceId, e);
        }
    }
}
