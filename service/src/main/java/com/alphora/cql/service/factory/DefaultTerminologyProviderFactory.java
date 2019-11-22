package com.alphora.cql.service.factory;


import com.alphora.cql.service.Helpers;
import com.alphora.cql.service.provider.FileBasedFhirTerminologyProvider;

import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.FhirTerminologyProvider;

import ca.uhn.fhir.context.FhirContext;

// TODO: Auth and such.
public class DefaultTerminologyProviderFactory implements TerminologyProviderFactory {
    public TerminologyProvider create(String model, String version, String terminologyUri) {
        if (terminologyUri == null || terminologyUri.isEmpty()) {
            return null;
        }

        if (!model.equals("FHIR")) {
            throw new IllegalArgumentException("Only FHIR-based terminology is supported at this time.");
        }

        FhirContext context;
        switch (version) {
            case "2.0.0":
                context = FhirContext.forDstu2_1();
                break;
            case "3.0.0":
                context = FhirContext.forDstu3();
                break;
            case "4.0.0":
                context = FhirContext.forR4();
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown FHIR terminology provider version: %s", version));
        }

        if (Helpers.isFileUri(terminologyUri)) {
            return new FileBasedFhirTerminologyProvider(context, terminologyUri);
        }
        else {
            return new FhirTerminologyProvider(context).setEndpoint(terminologyUri, false);
        }
    }
}