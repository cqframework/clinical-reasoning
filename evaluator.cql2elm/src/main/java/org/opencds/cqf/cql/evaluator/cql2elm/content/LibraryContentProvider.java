package org.opencds.cqf.cql.evaluator.cql2elm.content;

import java.io.InputStream;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;


public interface LibraryContentProvider extends LibrarySourceProvider {
    
    /** 
     * Get content of a library in the format specified. If the versionedIdentifier has a version specified it returns
     * the nearest compatible version. If a version is not specified it returns the highest version. If no compatible
     * version is found, it returns null.
     * 
     * Returns null if unable to provide the type of content specified.
     * @param libraryIdentifier The identifier of the library to provide content for.
     * @param libraryContentType The format of the content to return
     * @return InputStream the library content
     */
    InputStream getLibraryContent(VersionedIdentifier libraryIdentifier, LibraryContentType libraryContentType);

    /**
     * Gets the content of a  library as CQL text. If the versionedIdentifier has a version specified it returns
     * the nearest compatible version. If a version is not specified it returns the highest version. If no compatible
     * version is found, it returns null.
     * 
     * @param libraryIdentifier The identifier of the library to provide content for.
     * @return InputStream the CQL text.
     */
    default InputStream getLibrarySource(VersionedIdentifier libraryIdentifier) {
        return getLibraryContent(libraryIdentifier, LibraryContentType.CQL);
    }
}
