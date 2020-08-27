package org.opencds.cqf.cql.evaluator.cql2elm;

import static org.opencds.cqf.cql.evaluator.fhir.AdapterFactory.libraryAdapterFor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.util.BundleUtil;

/**
 * This class implements the cql-translator LibrarySourceProvider API, using a FHIR
 * server as a source for Library resources containing CQL content.
 */
public class FhirServerLibrarySourceProvider extends
    VersionComparingLibrarySourceProvider {

    private IGenericClient client;

    /**
     * @param client pre-configured and authorized FHIR server client
     */
    public FhirServerLibrarySourceProvider(IGenericClient client) {
        this.client = client;
    }

    protected IBaseResource getLibrary(String url) {
        try {
            return (IBaseResource)this.client.read().resource("Library").withUrl(url).elementsSubset("name", "version", "content", "type").encodedJson().execute();
        }
        catch (Exception e) {
            // TODO: Logging
        }

        return null;
    }

    @Override
    public IBaseResource getLibrary(VersionedIdentifier libraryIdentifier) {
        IBaseBundle result = this.client.search().forResource("Library").elementsSubset("name", "version")
            .where(new TokenClientParam("name").exactly().code(libraryIdentifier.getId())).encodedJson().execute();

        List<? extends IBaseResource> resources = BundleUtil.toListOfResourcesOfType(this.client.getFhirContext(),
                result, this.client.getFhirContext().getResourceDefinition("Library").getImplementingClass());
                
        if (resources == null || resources.isEmpty()) {
            return null;
        }

        Collection<IBaseResource> libraries = resources.stream().map(x -> (IBaseResource)x).collect(Collectors.toList());

        IBaseResource library = this.select(libraries, libraryIdentifier);

        // This is a subsetted resource, so we get the full version here.
        if (library != null) {
            return getLibrary(libraryAdapterFor(library).getUrl());
        }

        return null;
    }
}