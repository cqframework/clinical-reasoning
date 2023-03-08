package org.opencds.cqf.cql.evaluator.builder.library;

import static java.util.Objects.requireNonNull;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.DirectoryBundler;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;

import ca.uhn.fhir.context.FhirContext;

@Named
public class FhirFileLibrarySourceProviderFactory implements TypedLibrarySourceProviderFactory {

  FhirContext fhirContext;
  DirectoryBundler directoryBundler;
  AdapterFactory adapterFactory;
  LibraryVersionSelector libraryVersionSelector;

  @Inject
  public FhirFileLibrarySourceProviderFactory(FhirContext fhirContext,
      DirectoryBundler directoryBundler, AdapterFactory adapterFactory,
      LibraryVersionSelector libraryVersionSelector) {
    this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null");
    this.directoryBundler = requireNonNull(directoryBundler, "directoryBundler can not be null");
    this.adapterFactory = requireNonNull(adapterFactory, "adapterFactory can not be null");
    this.libraryVersionSelector =
        requireNonNull(libraryVersionSelector, "libraryVersionSelector can not be null");
  }

  @Override
  public String getType() {
    return Constants.HL7_FHIR_FILES;
  }

  @Override
  public LibrarySourceProvider create(String url, List<String> headers) {
    IBaseBundle bundle = this.directoryBundler.bundle(url);
    return new BundleFhirLibrarySourceProvider(fhirContext, bundle, adapterFactory,
        this.libraryVersionSelector);
  }
}
