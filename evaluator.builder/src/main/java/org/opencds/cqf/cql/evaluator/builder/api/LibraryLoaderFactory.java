package org.opencds.cqf.cql.evaluator.builder.api;

import java.util.List;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.api.model.EndpointInfo;

public interface LibraryLoaderFactory {

    public LibraryLoader create(LibraryManager libraryManager, CqlTranslatorOptions translatorOptions);

    public LibraryLoader create(List<LibrarySourceProvider> librarySourceProviders, CqlTranslatorOptions translatorOptions);

    public LibraryLoader create(EndpointInfo endpointInfo, CqlTranslatorOptions translatorOptions);
}