package org.opencds.cqf.cql.evaluator.factory.implementation;

import org.opencds.cqf.cql.Helpers;
import org.opencds.cqf.cql.evaluator.factory.ClientFactory;
import org.opencds.cqf.cql.evaluator.factory.TerminologyProviderFactory;

import org.opencds.cqf.cql.retrieve.FileBasedFhirTerminologyProvider;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.R4FhirTerminologyProvider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
public class DefaultTerminologyProviderFactory implements TerminologyProviderFactory {

    public TerminologyProvider create(FhirContext context, String terminologyUrl) {
        return this.create(context, terminologyUrl, null);
    }

    public TerminologyProvider create(FhirContext context, String terminologyUrl, ClientFactory clientFactory) {
        if (terminologyUrl == null || terminologyUrl.isEmpty()) {
            return null;
        }

        boolean isFileUri = Helpers.isFileUri(terminologyUrl);
        if (isFileUri) {
            return new FileBasedFhirTerminologyProvider(context, terminologyUrl);
        }
        
        if (clientFactory == null) {
            throw new IllegalArgumentException(String.format("Needed to access remote url %s and clientFactory was null"));
        }

        IGenericClient client = clientFactory.create(terminologyUrl);
        FhirVersionEnum version = client.getFhirContext().getVersion().getVersion();
        
        if (version.isEqualOrNewerThan(FhirVersionEnum.R5)) {
            throw new IllegalArgumentException("R5 terminology server is not yet supported.");
        }
        else if (version.isEqualOrNewerThan(FhirVersionEnum.R4)) {
            return new R4FhirTerminologyProvider(client);
        }
        else if (version.isEqualOrNewerThan(FhirVersionEnum.DSTU3)) {
            return new Dstu3FhirTerminologyProvider(client);
        }
        else {
            throw new IllegalArgumentException(String.format("Unknown FHIR terminology provider version: %s", version.toString()));
        }
    }
}