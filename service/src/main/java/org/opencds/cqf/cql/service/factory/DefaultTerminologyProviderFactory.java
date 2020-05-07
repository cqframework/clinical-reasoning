package org.opencds.cqf.cql.service.factory;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.opencds.cqf.cql.service.Helpers;
import org.opencds.cqf.cql.service.provider.FileBasedFhirTerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.fhir.terminology.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.fhir.terminology.R4FhirTerminologyProvider;

import ca.uhn.fhir.context.FhirContext;

// TODO: Auth and such.
public class DefaultTerminologyProviderFactory implements TerminologyProviderFactory {
    public TerminologyProvider create(Map<String, Pair<String, String>> modelVersionsAndUrls, String terminologyUri) {
        if (terminologyUri == null || terminologyUri.isEmpty()) {
            return null;
        }

        // We currently only support FHIR-based terminology
        // We assume that the terminology version is the same
        // As the data version
        Pair<String, String> versionAndUrl = modelVersionsAndUrls.get("http://hl7.org/fhir");
        if (versionAndUrl == null) {
            // Assume FHIR 3.0.0
            versionAndUrl = Pair.of("3.0.0", null);
        }

        boolean isFileUri = Helpers.isFileUri(terminologyUri);

        FhirContext context;
        switch (versionAndUrl.getLeft()) {
            case "2.0.0":
                context = FhirContext.forDstu2_1();
                if (isFileUri) {
                    return new FileBasedFhirTerminologyProvider(context, terminologyUri);

                } else {
                    throw new IllegalArgumentException("Remote FHIR provider not supported for version FHIR 2.0.0");
                }
            case "3.0.0":
                context = FhirContext.forDstu3();
                if (isFileUri) {
                    return new FileBasedFhirTerminologyProvider(context, terminologyUri);

                } else {
                    return new Dstu3FhirTerminologyProvider(context.newRestfulGenericClient(terminologyUri));
                }
            case "4.0.0":
                context = FhirContext.forR4();
                if (isFileUri) {
                    return new FileBasedFhirTerminologyProvider(context, terminologyUri);

                } else {
                    return new R4FhirTerminologyProvider(context.newRestfulGenericClient(terminologyUri));
                }
            default:
                throw new IllegalArgumentException(
                        String.format("Unknown FHIR terminology provider version: %s", versionAndUrl.getLeft()));
        }
    }
}