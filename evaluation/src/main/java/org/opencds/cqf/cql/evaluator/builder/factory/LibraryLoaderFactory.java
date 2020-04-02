package org.opencds.cqf.cql.evaluator.factory;

import java.util.EnumSet;
import java.util.List;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.opencds.cqf.cql.execution.LibraryLoader;

public interface LibraryLoaderFactory {
    LibraryLoader create(List<String> libraries, EnumSet<CqlTranslator.Options> translatorOptions);
    LibraryLoader create(String libraryPath, EnumSet<CqlTranslator.Options> translatorOptions);
    LibraryLoader create(String libraryPath, EnumSet<CqlTranslator.Options> translatorOptions, ClientFactory clientFactory);
}