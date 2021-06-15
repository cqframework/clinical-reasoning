package org.opencds.cqf.cql.evaluator.builder;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;

public interface LibraryContentProviderFactory {
    public LibraryContentProvider create(EndpointInfo endpointInfo);
    public LibraryContentProvider create(IBaseBundle contentBundle);
}