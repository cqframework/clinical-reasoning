package org.opencds.cqf.cql.evaluator.engine.execution;

import java.util.List;
import java.util.Objects;

import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;

public class PriorityLibraryLoader implements LibraryLoader {

    private List<LibraryLoader> libraryLoaders;

    public PriorityLibraryLoader(List<LibraryLoader> libraryLoaders) {
        this.libraryLoaders = Objects.requireNonNull(libraryLoaders, "libraryLoaders can not be null");
    }

    public Library load(VersionedIdentifier libraryIdentifier) {
        return this.libraryLoaders.stream().map(x -> x.load(libraryIdentifier)).findFirst().orElseGet(null);
    }
}