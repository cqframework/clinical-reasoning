package com.alphora.cql.service.factory;


import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;

// TODO: Auth and such.
public class DefaultTerminologyProviderFactory implements TerminologyProviderFactory {
    public TerminologyProvider create(String terminologyUri) {
        if (terminologyUri == null || terminologyUri.isEmpty()) {
            return null;
        }
        
        return new FhirTerminologyProvider().setEndpoint(terminologyUri, false);
    }
}