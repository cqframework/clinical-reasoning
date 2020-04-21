package org.opencds.cqf.cql.evaluator.translation.provider.stu3;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.cql.evaluator.translation.provider.VersionComparingLibrarySourceProvider;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class Stu3ServerLibrarySourceProvider extends VersionComparingLibrarySourceProvider {

    private IGenericClient client;

    public Stu3ServerLibrarySourceProvider(IGenericClient client) {
        this.client = client;
        if (!client.getFhirContext().getVersion().getVersion().isEquivalentTo(FhirVersionEnum.DSTU3)) {
            throw new IllegalArgumentException("This library source provider requires an STU3 server.");
        }
    }

    @Override
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {
        Objects.requireNonNull(versionedIdentifier, "versionedIdentifier can not be null.");

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

}