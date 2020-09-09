package org.opencds.cqf.cql.evaluator.builder.terminology;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.fhir.DirectoryBundler;

import ca.uhn.fhir.context.FhirContext;

public class BundleTerminologyProviderFactory implements TypedTerminologyProviderFactory {

    private FhirContext fhirContext;
    private DirectoryBundler directoryBundler;

    @Inject
    public BundleTerminologyProviderFactory(FhirContext fhirContent, DirectoryBundler directoryBundler) {
        this.fhirContext = Objects.requireNonNull(fhirContent, "fhirContext can not be null");
        this.directoryBundler = Objects.requireNonNull(directoryBundler, "directoryBundler can not be null");
    }

    @Override
    public String getType() {
        return Constants.HL7_FHIR_FILES;
    }

    @Override
    public TerminologyProvider create(String url, List<String> headers) {
        IBaseBundle bundle = this.directoryBundler.bundle(url);
        return new BundleTerminologyProvider(this.fhirContext, bundle);
    }
    
}
