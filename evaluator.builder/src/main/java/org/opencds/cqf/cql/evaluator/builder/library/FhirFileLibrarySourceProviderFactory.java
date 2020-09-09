package org.opencds.cqf.cql.evaluator.builder.library;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.cql2elm.BundleLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.fhir.DirectoryBundler;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;

import ca.uhn.fhir.context.FhirContext;

public class FhirFileLibrarySourceProviderFactory implements TypedLibrarySourceProviderFactory {

    FhirContext fhirContext;
    DirectoryBundler directoryBundler;
    AdapterFactory adapterFactory;

    @Inject
    public FhirFileLibrarySourceProviderFactory(FhirContext fhirContext, DirectoryBundler directoryBundler, AdapterFactory adapterFactory) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null");
        this.directoryBundler = Objects.requireNonNull(directoryBundler, "directoryBundler can not be null");
        this.adapterFactory = Objects.requireNonNull(adapterFactory, "adapterFactory can not be null");
    }

    @Override
    public String getType() {
        return Constants.HL7_FHIR_FILES;
    }

    @Override
    public LibrarySourceProvider create(String url, List<String> headers) {
        IBaseBundle bundle = this.directoryBundler.bundle(url);
        return new BundleLibrarySourceProvider(fhirContext, bundle, adapterFactory);
    }
}
