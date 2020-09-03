package org.opencds.cqf.cql.evaluator.builder;

import java.util.List;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;

public interface LibraryLoaderFactory {

    public LibraryLoader create(LibraryManager libraryManager, CqlTranslatorOptions translatorOptions);

    public LibraryLoader create(List<LibrarySourceProvider> librarySourceProviders, CqlTranslatorOptions translatorOptions);

    public LibraryLoader create(EndpointInfo endpointInfo, CqlTranslatorOptions translatorOptions);
}