package org.opencds.cqf.cql.evaluator.cql2elm.content.fhir;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.util.BundleUtil;

/**
 * This class implements the LibraryContentProvider API, using a FHIR
 * server as a source for Library resources containing CQL content.
 * 
 * This class caches the Library resources that are resolved from the remote server,
 * so it's intended to be short-lived (i.e. for the duration of a single CQL evaluation)
 */
public class FhirClientFhirLibraryContentProvider extends
    BaseFhirLibraryContentProvider {

    Logger logger = LoggerFactory.getLogger(FhirClientFhirLibraryContentProvider.class);

    private IGenericClient client;
    private AdapterFactory adapterFactory;
    private LibraryVersionSelector libraryVersionSelector;

    private Map<VersionedIdentifier, IBaseResource> cache;

    /**
     * @param client pre-configured and authorized FHIR server client
     * @param adapterFactory factory for HL7 Structure adapters
     * @param libraryVersionSelector logic for selecting a specific library
     */
    public FhirClientFhirLibraryContentProvider(IGenericClient client, AdapterFactory adapterFactory, LibraryVersionSelector libraryVersionSelector) {
        super(adapterFactory);
        this.client = client;
        this.adapterFactory = adapterFactory;
        this.libraryVersionSelector = libraryVersionSelector;
        this.cache = new HashMap<>();
    }

    protected IBaseResource getLibrary(IIdType id) {
        try {
            return this.client.read().resource("Library").withId(id).elementsSubset("name", "version", "content", "type").encodedJson().execute();
        }
        catch (Exception e) {
            logger.error(String.format("error while getting library with id %s", id), e);
        }

        return null;
    }

    @Override
    public IBaseResource getLibrary(VersionedIdentifier libraryIdentifier) {
        IBaseResource library = this.cache.get(libraryIdentifier);
        if (library != null) {
            return library;
        }

        IBaseBundle result = this.client.search().forResource("Library").elementsSubset("name", "version")
            .where(new TokenClientParam("name").exactly().code(libraryIdentifier.getId())).encodedJson().execute();

        List<? extends IBaseResource> resources = BundleUtil.toListOfResourcesOfType(this.client.getFhirContext(),
                result, this.client.getFhirContext().getResourceDefinition("Library").getImplementingClass());
                
        if (resources == null || resources.isEmpty()) {
            return null;
        }

        Collection<IBaseResource> libraries = resources.stream().map(x -> (IBaseResource)x).collect(Collectors.toList());

        library = this.libraryVersionSelector.select(libraryIdentifier, libraries);

        // This is a subsetted resource, so we get the full version here.
        if (library != null) {
            library = getLibrary(this.adapterFactory.createLibrary(library).getId());
            this.cache.put(libraryIdentifier, library);
        }

        return library;
    }
}