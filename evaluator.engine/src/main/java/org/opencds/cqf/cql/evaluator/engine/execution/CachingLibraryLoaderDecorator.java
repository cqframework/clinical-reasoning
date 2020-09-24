package org.opencds.cqf.cql.evaluator.engine.execution;

import java.util.HashMap;
import java.util.Map;

import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;

public class CachingLibraryLoaderDecorator implements LibraryLoader {

    
    private final LibraryLoader innerLoader;

    private Map<VersionedIdentifier, Library> libraries = new HashMap<>();

    public CachingLibraryLoaderDecorator( final LibraryLoader libraryLoader) {
        this.innerLoader = libraryLoader;
    }

    public Library load(final VersionedIdentifier libraryIdentifier) {
        try {
            if (!libraries.containsKey(libraryIdentifier)) {
                libraries.put(libraryIdentifier, this.innerLoader.load(libraryIdentifier));
            }

            return libraries.get(libraryIdentifier);
        }
        catch (final Exception e) {
            return null;
        }
    }
}