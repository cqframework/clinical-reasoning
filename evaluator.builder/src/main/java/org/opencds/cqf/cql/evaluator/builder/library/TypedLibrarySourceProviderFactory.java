package org.opencds.cqf.cql.evaluator.builder.library;

import java.util.List;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;

public interface TypedLibrarySourceProviderFactory {

    public String getType();

    public LibrarySourceProvider create(String url, List<String> headers);

}
