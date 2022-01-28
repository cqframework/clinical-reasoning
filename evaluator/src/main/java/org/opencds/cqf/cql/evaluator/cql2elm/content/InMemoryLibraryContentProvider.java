package org.opencds.cqf.cql.evaluator.cql2elm.content;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.hl7.elm.r1.VersionedIdentifier;

/**
 * This class implements the LibraryContentProvider API, using a
 * set of strings representing CQL library content as a source.
 */
public class InMemoryLibraryContentProvider implements LibraryContentProvider {

    private List<String> libraries;

    public InMemoryLibraryContentProvider(List<String> libraries) {
        this.libraries = libraries;
    }

    @Override
    public InputStream getLibrarySource(org.hl7.elm.r1.VersionedIdentifier libraryIdentifier) {
        String id = libraryIdentifier.getId();
        String version = libraryIdentifier.getVersion();

        String matchText = "(?s).*library\\s+" + id;
        if (version != null) {
            matchText += ("\\s+version\\s+'" + version + "'\\s+(?s).*");
        } else {
            matchText += "\\s+(?s).*";
        }

        for (String library : this.libraries) {

            if (library.matches(matchText)) {
                return new ByteArrayInputStream(library.getBytes());
            }
        }

        return null;
    }

    @Override
    public InputStream getLibraryContent(VersionedIdentifier libraryIdentifier, LibraryContentType libraryContentType) {
        if (libraryContentType == LibraryContentType.CQL) {
            return this.getLibrarySource(libraryIdentifier);
        }

        return null;
    }
}