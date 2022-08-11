package org.opencds.cqf.cql.evaluator.builder.library;

import static java.util.Objects.requireNonNull;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.FhirClientFhirLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.ClientFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;

import ca.uhn.fhir.rest.client.api.IGenericClient;

@Named
public class FhirRestLibrarySourceProviderFactory implements TypedLibrarySourceProviderFactory {

    private ClientFactory clientFactory;
    private AdapterFactory adapterFactory;
    private LibraryVersionSelector libraryVersionSelector;

    @Inject
    public FhirRestLibrarySourceProviderFactory(ClientFactory clientFactory, AdapterFactory adapterFactory, LibraryVersionSelector libraryVersionSelector) {
        this.clientFactory = requireNonNull(clientFactory, "clientFactory can not be null");
        this.adapterFactory = requireNonNull(adapterFactory, "adapterFactory can not be null");
        this.libraryVersionSelector = requireNonNull(libraryVersionSelector, "libraryVersionSelector can not be null");
    }

    @Override
    public String getType() {
        return Constants.HL7_FHIR_REST;
    }

    @Override
    public LibrarySourceProvider create(String url, List<String> headers) {
        IGenericClient client = this.clientFactory.create(url, headers);
        return new FhirClientFhirLibrarySourceProvider(client, this.adapterFactory, this.libraryVersionSelector);
    }
}
