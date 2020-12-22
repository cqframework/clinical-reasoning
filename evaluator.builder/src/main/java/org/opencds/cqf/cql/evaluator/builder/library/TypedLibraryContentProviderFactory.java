package org.opencds.cqf.cql.evaluator.builder.library;

import java.util.List;

import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;

public interface TypedLibraryContentProviderFactory {

    public String getType();

    public LibraryContentProvider create(String url, List<String> headers);
    
}