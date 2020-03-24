package org.opencds.cqf.cql.service.provider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;

public class InMemoryLibrarySourceProvider implements LibrarySourceProvider {

    private List<String> libraries;

    public InMemoryLibrarySourceProvider(List<String> libraries) {
        this.libraries = libraries;
    }

    @Override
    public InputStream getLibrarySource(org.hl7.elm.r1.VersionedIdentifier libraryIdentifier) {
        String id = libraryIdentifier.getId();
        String version = libraryIdentifier.getVersion();

        String matchText = "(?s).*library\\s+" + id;
        if (version != null) {
            matchText += ("\\s+version\\s+'" + version + "'\\s+(?s).*");
        }
        else {
            matchText += "\\s+(?s).*";
        }

        for(String library : this.libraries){

            if(library.matches(matchText)){
                return new ByteArrayInputStream(library.getBytes());
            }
        }

        return null;
    }
}