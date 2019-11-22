package com.alphora.cql.service.factory;


import com.alphora.cql.service.Helpers;

import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;

// TODO: Auth and such.
public class DefaultTerminologyProviderFactory implements TerminologyProviderFactory {
    public TerminologyProvider create(String terminologyUri) {
        if (terminologyUri == null || terminologyUri.isEmpty()) {
            return null;
        }

        if (Helpers.isFileUri(terminologyUri)) {
            return null;
        }
        else {
            return new FhirTerminologyProvider().setEndpoint(terminologyUri, false);
        }
    }
}