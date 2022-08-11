package org.opencds.cqf.cql.evaluator.builder;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.fhir.instance.model.api.IBaseBundle;

public interface LibrarySourceProviderFactory {
    public LibrarySourceProvider create(EndpointInfo endpointInfo);
    public LibrarySourceProvider create(IBaseBundle contentBundle);
}