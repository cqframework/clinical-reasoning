package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_CR_MODULE_ID;

import ca.uhn.fhir.jpa.cache.IResourceChangeEvent;
import ca.uhn.fhir.jpa.cache.IResourceChangeListener;
import ca.uhn.fhir.jpa.cache.ResourceChangeEvent;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseJson;
import ca.uhn.hapi.fhir.cdshooks.svc.CdsServiceRegistryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery.ICrDiscoveryServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdsServiceInterceptor implements IResourceChangeListener {
    static final Logger ourLog = LoggerFactory.getLogger(CdsServiceInterceptor.class);

    private final CdsServiceRegistryImpl cdsServiceRegistry;

    private final ICrDiscoveryServiceFactory discoveryServiceFactory;

    private final ICdsCrServiceFactory crServiceFactory;

    private final ObjectMapper om;

    public CdsServiceInterceptor(
            CdsServiceRegistryImpl cdsServiceRegistry,
            ICrDiscoveryServiceFactory discoveryServiceFactory,
            ICdsCrServiceFactory crServiceFactory,
            ObjectMapper om) {
        this.cdsServiceRegistry = cdsServiceRegistry;
        this.discoveryServiceFactory = discoveryServiceFactory;
        this.crServiceFactory = crServiceFactory;
        this.om = om;
    }

    @Override
    public void handleInit(Collection<IIdType> resourceIds) {
        handleChange(ResourceChangeEvent.fromCreatedUpdatedDeletedResourceIds(
                new ArrayList<>(resourceIds), Collections.emptyList(), Collections.emptyList()));
    }

    @Override
    public void handleChange(IResourceChangeEvent resourceChangeEvent) {
        if (resourceChangeEvent == null) return;
        if (resourceChangeEvent.getCreatedResourceIds() != null
                && !resourceChangeEvent.getCreatedResourceIds().isEmpty()) {
            insert(resourceChangeEvent.getCreatedResourceIds());
        }
        if (resourceChangeEvent.getUpdatedResourceIds() != null
                && !resourceChangeEvent.getUpdatedResourceIds().isEmpty()) {
            update(resourceChangeEvent.getUpdatedResourceIds());
        }
        if (resourceChangeEvent.getDeletedResourceIds() != null
                && !resourceChangeEvent.getDeletedResourceIds().isEmpty()) {
            delete(resourceChangeEvent.getDeletedResourceIds());
        }
    }

    private void insert(List<IIdType> createdIds) {
        for (IIdType id : createdIds) {
            var serviceId = id.getIdPart();
            try {
                CdsServiceJson cdsServiceJson =
                        discoveryServiceFactory.create(serviceId).resolveService();
                if (cdsServiceJson != null) {
                    final CdsCrServiceMethod cdsCrServiceMethod =
                            new CdsCrServiceMethod(cdsServiceJson, crServiceFactory);

                    cdsServiceRegistry.registerService(
                            serviceId,
                            x -> (CdsServiceResponseJson) cdsCrServiceMethod.invoke(om, x, serviceId),
                            cdsServiceJson,
                            true,
                            CDS_CR_MODULE_ID);
                }

            } catch (Exception e) {
                ourLog.info(String.format("Failed to create service for %s", serviceId));
            }
        }
    }

    private void update(List<IIdType> updatedIds) {
        try {
            delete(updatedIds);
            insert(updatedIds);
        } catch (Exception e) {
            ourLog.info(String.format("Failed to update service(s) for %s", updatedIds));
        }
    }

    private void delete(List<IIdType> deletedIds) {
        for (IIdType id : deletedIds) {
            cdsServiceRegistry.unregisterService(id.getIdPart(), CDS_CR_MODULE_ID);
        }
    }
}
