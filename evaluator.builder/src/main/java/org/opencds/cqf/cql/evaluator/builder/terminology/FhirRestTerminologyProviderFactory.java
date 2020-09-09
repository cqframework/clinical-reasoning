package org.opencds.cqf.cql.evaluator.builder.terminology;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.opencds.cqf.cql.engine.fhir.terminology.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.fhir.terminology.R4FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.fhir.ClientFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class FhirRestTerminologyProviderFactory implements TypedTerminologyProviderFactory {

    private FhirContext fhirContext;
    private ClientFactory clientFactory;

    @Inject
    public FhirRestTerminologyProviderFactory(FhirContext fhirContent, ClientFactory clientFactory) {
        this.fhirContext = Objects.requireNonNull(fhirContent, "fhirContext can not be null");
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory can not be null");
    }

    @Override
    public String getType() {
        return Constants.HL7_FHIR_REST;
    }

    @Override
    public TerminologyProvider create(String url, List<String> headers) {
        IGenericClient client = this.clientFactory.create(url, headers);

        switch (this.fhirContext.getVersion().getVersion()) {
            case DSTU3:
                return new Dstu3FhirTerminologyProvider(client);
            case R4:
                return new R4FhirTerminologyProvider(client);
            default:
                throw new IllegalArgumentException(String.format("unsupported FHIR version: %s", fhirContext));
        }
    }  
}
