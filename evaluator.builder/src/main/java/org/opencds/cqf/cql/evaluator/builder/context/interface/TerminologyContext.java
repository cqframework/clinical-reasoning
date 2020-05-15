package org.opencds.cqf.cql.evaluator.builder.context.interface;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

import ca.uhn.fhir.context.FhirContext;

public interface TerminologyContext {
    public BuilderDataContext withTerminologyProvider(TerminologyProvider terminologyProvider);
    public BuilderDataContext withTerminologyProvider(List<String> terminologyBundles);
    public BuilderDataContext withTerminologyProvider(FhirContext fhirContext, IBaseBundle bundles);
    public BuilderDataContext withTerminologyProvider(FhirContext fhirContext, String fileUri);
}
