package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.cache.IResourceChangeEvent;
import ca.uhn.fhir.jpa.cache.IResourceChangeListener;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.FhirTerser;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.runtime.Code;

/**
 * This class listens for changes to ValueSet resources and invalidates the CodeCache. The CodeCache is used in CQL evaluation to speed up terminology operations. If ValueSet changes, it's possible that the constituent codes change and therefore the cache needs to be updated.
 **/
public class CodeCacheResourceChangeListener implements IResourceChangeListener {

    private static final org.slf4j.Logger ourLog =
            org.slf4j.LoggerFactory.getLogger(CodeCacheResourceChangeListener.class);

    private final IFhirResourceDao<?> valueSetDao;
    private final Map<String, List<Code>> globalValueSetCache;
    private final FhirTerser fhirTerser;

    public CodeCacheResourceChangeListener(DaoRegistry daoRegistry, Map<String, List<Code>> globalValueSetCache) {
        this.valueSetDao = daoRegistry.getResourceDao("ValueSet");
        this.globalValueSetCache = globalValueSetCache;
        this.fhirTerser = daoRegistry.getFhirContext().newTerser();
    }

    @Override
    public void handleInit(Collection<IIdType> resourceIds) {
        // Intentionally empty. Only cache ELM on eval request
    }

    @Override
    public void handleChange(IResourceChangeEvent resourceChangeEvent) {
        if (resourceChangeEvent == null) {
            return;
        }

        this.invalidateCacheByIds(resourceChangeEvent.getDeletedResourceIds());
        this.invalidateCacheByIds(resourceChangeEvent.getUpdatedResourceIds());
    }

    private void invalidateCacheByIds(List<IIdType> ids) {
        if (ids == null) {
            return;
        }

        for (IIdType id : ids) {
            this.invalidateCacheById(id);
        }
    }

    private void invalidateCacheById(IIdType id) {
        if (!id.getResourceType().equals("ValueSet")) {
            return;
        }

        IBaseResource valueSet;
        try {
            valueSet = this.valueSetDao.read(id.toUnqualifiedVersionless(), new SystemRequestDetails());
        }
        // This happens when a Library is deleted entirely, so it's impossible to look up
        // name and version.
        catch (ResourceGoneException | ResourceNotFoundException e) {
            ourLog.debug(
                    "Failed to locate resource {} to look up url and version. Clearing all codes from cache.",
                    id.getValueAsString());
            globalValueSetCache.clear();
            return;
        }

        String url = this.fhirTerser.getSinglePrimitiveValueOrNull(valueSet, "url");

        var valuesets = globalValueSetCache.keySet();

        for (String key : valuesets) {
            if (key.contains(url)) {
                globalValueSetCache.remove(key);
                ourLog.warn("Successfully removed valueSet from ValueSetCache: {} due to updated resource", url);
            }
        }
    }
}
