package org.opencds.cqf.cql.evaluator.builder.data;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.fhir.DirectoryBundler;

import ca.uhn.fhir.context.FhirContext;


@Named
public class FhirFileRetrieveProviderFactory implements TypedRetrieveProviderFactory {

    FhirContext fhirContext;
    DirectoryBundler directoryBundler;

    @Inject
    public FhirFileRetrieveProviderFactory(FhirContext fhirContext, DirectoryBundler directoryBundler){
        this.fhirContext = fhirContext;
        this.directoryBundler = directoryBundler;
    }

    @Override
    public String getType() {
        return Constants.HL7_FHIR_FILES;
    }

    @Override
    public RetrieveProvider create(String url, List<String> headers) {
        IBaseBundle bundle = this.directoryBundler.bundle(url);
        return new BundleRetrieveProvider(fhirContext, bundle);
    }
    
}
