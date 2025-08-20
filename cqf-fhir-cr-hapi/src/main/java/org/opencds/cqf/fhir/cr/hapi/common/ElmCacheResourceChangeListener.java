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
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

/**
 * This class listens for changes to Library resources and invalidates the CodeCache. The CodeCache is used in CQL evaluatuon to speed up the measure operations. If underlying values change in the library then cache requires updating.
 **/
public class ElmCacheResourceChangeListener implements IResourceChangeListener {

    private static final org.slf4j.Logger ourLog =
            org.slf4j.LoggerFactory.getLogger(ElmCacheResourceChangeListener.class);

    private final IFhirResourceDao<?> libraryDao;
    private final Map<VersionedIdentifier, CompiledLibrary> globalLibraryCache;
    private final FhirTerser fhirTerser;

    public ElmCacheResourceChangeListener(
            DaoRegistry daoRegistry, Map<VersionedIdentifier, CompiledLibrary> globalLibraryCache) {

        this.libraryDao = daoRegistry.getResourceDao("Library");
        this.globalLibraryCache = globalLibraryCache;
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
        if (!id.getResourceType().equals("Library")) {
            return;
        }

        IBaseResource library;
        try {
            library = this.libraryDao.read(id, new SystemRequestDetails());
        } catch (ResourceGoneException | ResourceNotFoundException e) {
            // TODO: This needs to be smarter... the issue is that ELM is cached with
            // library name and version as the key since
            // that's the access path the CQL engine uses, but change notifications occur
            // with the resource Id, which is not
            // necessarily tied to the resource name. In any event, if a unknown resource is
            // deleted, clear all libraries as a workaround.
            // One option is to maintain a cache with multiple indices.
            ourLog.debug(
                    "Failed to locate resource {} to look up name and version. Clearing all libraries from cache.",
                    id.getValueAsString());
            this.globalLibraryCache.clear();
            return;
        }

        String name = this.fhirTerser.getSinglePrimitiveValueOrNull(library, "name");
        String version = this.fhirTerser.getSinglePrimitiveValueOrNull(library, "version");

        this.globalLibraryCache.remove(new VersionedIdentifier().withId(name).withVersion(version));
    }
}
