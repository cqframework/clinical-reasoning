package org.opencds.cqf.cql.evaluator.builder;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;

public interface LibraryLoaderFactory {
    public LibraryLoader create(EndpointInfo endpointInfo, CqlTranslatorOptions translatorOptions);
    public LibraryLoader create(IBaseBundle contentBundle, CqlTranslatorOptions translatorOptions);
}