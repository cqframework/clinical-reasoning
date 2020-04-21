package org.opencds.cqf.cql.evaluator.factory;
import org.opencds.cqf.cql.terminology.TerminologyProvider;

import ca.uhn.fhir.context.FhirContext;

public interface TerminologyProviderFactory {
    // Passing FHIR context here is a workaround for the File-based terminology provider not yet
    // knowing how to detect resource versions. This will be removed in the future.
    TerminologyProvider create(FhirContext context, String terminologyUrl);
    TerminologyProvider create(FhirContext context, String terminologyUrl, ClientFactory clientFactory);
}