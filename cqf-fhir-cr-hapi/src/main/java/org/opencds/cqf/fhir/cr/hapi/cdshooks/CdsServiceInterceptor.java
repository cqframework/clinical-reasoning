package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_CR_MODULE_ID;

import ca.uhn.fhir.jpa.cache.IResourceChangeEvent;
import ca.uhn.fhir.jpa.cache.IResourceChangeListener;
import ca.uhn.fhir.jpa.cache.ResourceChangeEvent;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseJson;
import ca.uhn.hapi.fhir.cdshooks.svc.CdsServiceRegistryImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery.ICrDiscoveryServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdsServiceInterceptor implements IResourceChangeListener {
    static final Logger ourLog = LoggerFactory.getLogger(CdsServiceInterceptor.class);

    private final CdsServiceRegistryImpl cdsServiceRegistry;

    private final ICrDiscoveryServiceFactory discoveryServiceFactory;

    private final ICdsCrServiceFactory crServiceFactory;

    public CdsServiceInterceptor(
            CdsServiceRegistryImpl cdsServiceRegistry,
            ICrDiscoveryServiceFactory discoveryServiceFactory,
            ICdsCrServiceFactory crServiceFactory) {
        this.cdsServiceRegistry = cdsServiceRegistry;
        this.discoveryServiceFactory = discoveryServiceFactory;
        this.crServiceFactory = crServiceFactory;
    }

    @Override
    public void handleInit(Collection<IIdType> resourceIds) {
        handleChange(ResourceChangeEvent.fromCreatedUpdatedDeletedResourceIds(
                new ArrayList<>(resourceIds), Collections.emptyList(), Collections.emptyList()));
    }

    @Override
    public void handleChange(IResourceChangeEvent resourceChangeEvent) {
        if (resourceChangeEvent == null) return;

        List<IIdType> resourceIds = getChangedResourceIds(resourceChangeEvent.getCreatedResourceIds());
        insert(resourceIds);

        resourceIds = getChangedResourceIds(resourceChangeEvent.getUpdatedResourceIds());
        update(resourceIds);

        resourceIds = getChangedResourceIds(resourceChangeEvent.getDeletedResourceIds());
        delete(resourceIds);
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
                            x -> (CdsServiceResponseJson) cdsCrServiceMethod.invoke(x, serviceId),
                            cdsServiceJson,
                            true,
                            CDS_CR_MODULE_ID);
                }

            } catch (Exception e) {
                ourLog.info("Failed to create service for %s".formatted(serviceId));
            }
        }
    }

    private void update(List<IIdType> updatedIds) {
        try {
            delete(updatedIds);
            insert(updatedIds);
        } catch (Exception e) {
            ourLog.info("Failed to update service(s) for %s".formatted(updatedIds));
        }
    }

    private void delete(List<IIdType> deletedIds) {
        for (IIdType id : deletedIds) {
            cdsServiceRegistry.unregisterService(id.getIdPart(), CDS_CR_MODULE_ID);
        }
    }

    private List<IIdType> getChangedResourceIds(List<IIdType> resourceIds) {
        return ObjectUtils.defaultIfNull(resourceIds, Collections.emptyList());
    }
}
