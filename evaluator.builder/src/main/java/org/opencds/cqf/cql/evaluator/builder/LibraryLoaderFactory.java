package org.opencds.cqf.cql.evaluator.builder;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;

public interface LibraryLoaderFactory {
    /**
     * Returns a LibraryLoader for the given endpointInfo. Loaded libraries will be
     * translated according to translatorOptions. Returns null if endpointInfo is
     * null.
     * 
     * Override in subclasses to provide "default" behavior for your platform.
     * 
     * @param endpointInfo      the EndpointInfo for the location of the Library
     *                          content
     * @param translatorOptions the options to use for CQL translation
     * @return a LibraryLoader
     */
    public LibraryLoader create(EndpointInfo endpointInfo, CqlTranslatorOptions translatorOptions);

    /**
     * Returns a LibraryLoader for the given contentBundle. Loaded libraries will be
     * translated according to translatorOptions. Returns null if contentBundle is
     * null.
     * 
     * @param contentBundle     the Bundle to use for Library content
     * @param translatorOptions the options to use for CQL translation
     * @return a LibraryLoader
     */
    public LibraryLoader create(IBaseBundle contentBundle, CqlTranslatorOptions translatorOptions);
}