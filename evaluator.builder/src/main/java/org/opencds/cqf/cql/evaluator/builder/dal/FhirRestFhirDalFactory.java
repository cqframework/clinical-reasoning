package org.opencds.cqf.cql.evaluator.builder.dal;

import static java.util.Objects.requireNonNull;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.fhir.ClientFactory;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.fhir.dal.RestFhirDal;

import ca.uhn.fhir.rest.client.api.IGenericClient;


@Named
public class FhirRestFhirDalFactory implements TypedFhirDalFactory {

    private ClientFactory clientFactory;

    @Inject
    public FhirRestFhirDalFactory(ClientFactory clientFactory) {
        this.clientFactory = requireNonNull(clientFactory, "clientFactory can not be null");
    }

    @Override
    public String getType() {
        return Constants.HL7_FHIR_REST;
    }

    @Override
    public FhirDal create(String url, List<String> headers) {
        IGenericClient client = this.clientFactory.create(url, headers);

        return new RestFhirDal(client);
    }  
}
