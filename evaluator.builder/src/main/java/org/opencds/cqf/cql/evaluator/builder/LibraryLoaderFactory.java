package org.opencds.cqf.cql.evaluator.builder;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;

public interface LibraryLoaderFactory {
    public LibraryLoader create(EndpointInfo endpointInfo, CqlTranslatorOptions translatorOptions);
}