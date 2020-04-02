package org.opencds.cqf.cql.evaluator.provider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.*;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;

// TODO: Genericize this
public class Dstu3FhirServerLibrarySourceProvider implements LibrarySourceProvider {

    private IGenericClient client;

    public Dstu3FhirServerLibrarySourceProvider(IGenericClient client) {
        this.client = client;
        if (!client.getFhirContext().getVersion().getVersion().isEquivalentTo(FhirVersionEnum.DSTU3)) {
            throw new IllegalArgumentException("Only Dstu3 servers supported for loading cql content at this time.");
        }
    }

    @Override
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {
        if (versionedIdentifier == null) {
            return null;
        }

        Library library = this.getLibrary(versionedIdentifier.getId(), versionedIdentifier.getVersion());
        if (library == null ){
            return null;
        }

        return this.getCqlStream(library);
    }

    public Library getLibrary(String url) {
        try {
            return this.client.read().resource(Library.class).withUrl(url).elementsSubset("name", "version", "content", "type").encodedJson().execute();
        }
        catch (Exception e) {
            // TODO: Logging
        }

        return null;
    }

    public Library getLibrary(String name, String version) {
        try {
            Bundle result = this.client.search().forResource(Library.class).elementsSubset("name", "version").where(Library.NAME.matchesExactly().value(name))
                    .returnBundle(Bundle.class).encodedJson().execute();

            Library library = null;
            String libraryUrl = null;
            Library maxVersion = null;
            String maxUrl = null;
            if (result.hasEntry() && result.getEntry().size() > 0){
                for (Bundle.BundleEntryComponent bec : result.getEntry()) {
                    Library l = (Library)bec.getResource();
                    if ((version != null && l.getVersion().equals(version)) ||
                        (version == null && !l.hasVersion()))
                    {
                        library = l;
                        libraryUrl = bec.getFullUrl();
                    }
        
                    if (maxVersion == null || compareVersions(maxVersion.getVersion(), l.getVersion()) < 0){
                        maxVersion = l;
                        maxUrl = bec.getFullUrl();
                    }
                }
            }

            if (version == null && maxVersion != null) {
                library = maxVersion;
                libraryUrl = maxUrl;
            }

            // This is a subsetted resource, so we get the full version here.
            if (library != null) {
                return getLibrary(libraryUrl);
            }

        }
        catch (Exception e) {
            // TODO: Logging
        }

        return null;
    }

    private InputStream getCqlStream(Library library) {
        if (library.getType().getCoding().get(0).getCode().equals("logic-library")) {
            for (Attachment content : library.getContent()) {
                // TODO: Could use this for any content type, would require a mapping from content type to LanguageServer LanguageId
                if (content.getContentType().equals("text/cql")) {
                    return new ByteArrayInputStream(content.getData());
                }
                // TODO: Decompile ELM if no CQL is available?
            }
        }

        return null;
    }

    public static int compareVersions(String version1, String version2)
    {
        // Treat null as MAX VERSION
        if (version1 == null && version2 == null) {
            return 0;
        }

        if (version1 != null && version2 == null) {
            return -1;
        }

        if (version1 == null && version2 != null) {
            return 1;
        }

        String[] string1Vals = version1.split("\\.");
        String[] string2Vals = version2.split("\\.");
    
        int length = Math.max(string1Vals.length, string2Vals.length);
    
        for (int i = 0; i < length; i++)
        {
            Integer v1 = (i < string1Vals.length)?Integer.parseInt(string1Vals[i]):0;
            Integer v2 = (i < string2Vals.length)?Integer.parseInt(string2Vals[i]):0;
    
            //Making sure Version1 bigger than version2
            if (v1 > v2)
            {
                return 1;
            }
            //Making sure Version1 smaller than version2
            else if(v1 < v2)
            {
                return -1;
            }
        }
    
        //Both are equal
        return 0;
    }
}