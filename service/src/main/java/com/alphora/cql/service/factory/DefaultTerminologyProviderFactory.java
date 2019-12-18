package com.alphora.cql.service.factory;

import java.util.Map;

import com.alphora.cql.service.Helpers;
import com.alphora.cql.service.provider.FileBasedFhirTerminologyProvider;

import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.cql.terminology.TerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.terminology.fhir.R4FhirTerminologyProvider;

import ca.uhn.fhir.context.FhirContext;

// TODO: Auth and such.
public class DefaultTerminologyProviderFactory implements TerminologyProviderFactory {
    public TerminologyProvider create(Map<String, Pair<String, String>> modelVersionsAndUrls, String terminologyUri) {
        if (terminologyUri == null || terminologyUri.isEmpty()) {
            return null;
        }

        // We currently only support FHIR
        Pair<String, String> versionAndUrl = modelVersionsAndUrls.get("FHIR");
        if (versionAndUrl == null) {
            throw new IllegalArgumentException("Only FHIR-based terminology is supported at this time.");
        }

        boolean isFileUri = Helpers.isFileUri(terminologyUri);

        FhirContext context;
        switch (versionAndUrl.getLeft()) {
            case "2.0.0":
                context = FhirContext.forDstu2_1();
                if (isFileUri) {
                    return new FileBasedFhirTerminologyProvider(context, terminologyUri);

                }
                else {
                    throw new IllegalArgumentException("Remote FHIR provider not supported for version FHIR 2.0.0");
                }
            case "3.0.0":
                context = FhirContext.forDstu3();
                if (isFileUri) {
                    return new FileBasedFhirTerminologyProvider(context, terminologyUri);

                }
                else {
                    return new Dstu3FhirTerminologyProvider(context).setEndpoint(terminologyUri, false);
                }
            case "4.0.0":
                context = FhirContext.forR4();
                if (isFileUri) {
                    return new FileBasedFhirTerminologyProvider(context, terminologyUri);

                }
                else {
                    return new R4FhirTerminologyProvider(context).setEndpoint(terminologyUri, false);
                }
            default:
                throw new IllegalArgumentException(String.format("Unknown FHIR terminology provider version: %s", versionAndUrl.getLeft()));
        }
    }
}