package org.opencds.cqf.cql.evaluator.builder.library;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.Uris.isFileUri;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;

import ca.uhn.fhir.context.FhirContext;

@Named
public class LibrarySourceProviderFactory
    implements org.opencds.cqf.cql.evaluator.builder.LibrarySourceProviderFactory {

  protected Set<TypedLibrarySourceProviderFactory> librarySourceProviderFactories;
  protected FhirContext fhirContext;
  protected AdapterFactory adapterFactory;
  protected LibraryVersionSelector libraryVersionSelector;

  protected Map<VersionedIdentifier, Model> globalModelCache = new ConcurrentHashMap<>();

  @Inject
  public LibrarySourceProviderFactory(FhirContext fhirContext, AdapterFactory adapterFactory,
      Set<TypedLibrarySourceProviderFactory> librarySourceProviderFactories,
      LibraryVersionSelector libraryVersionSelector) {
    this.librarySourceProviderFactories = requireNonNull(librarySourceProviderFactories,
        "librarySourceProviderFactories can not be null");
    this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null");
    this.adapterFactory = requireNonNull(adapterFactory, "adapterFactory can not be null");
    this.libraryVersionSelector =
        requireNonNull(libraryVersionSelector, "libraryVersionSelector can not be null");
  }

  @Override
  public LibrarySourceProvider create(EndpointInfo endpointInfo) {
    requireNonNull(endpointInfo, "endpointInfo can not be null");
    if (endpointInfo.getAddress() == null) {
      throw new IllegalArgumentException("endpointInfo must have a url defined");
    }

    if (endpointInfo.getType() == null) {
      endpointInfo.setType(detectType(endpointInfo.getAddress()));
    }

    LibrarySourceProvider contentProvider = this.getProvider(endpointInfo.getType(),
        endpointInfo.getAddress(), endpointInfo.getHeaders());

    return contentProvider;
  }

  protected IBaseCoding detectType(String url) {
    if (isFileUri(url)) {
      // Attempt to auto-detect the type of files.
      try {
        Path directoryPath = null;
        try {
          directoryPath = Paths.get(new URL(url).toURI());
        } catch (Exception e) {
          directoryPath = Paths.get(url);
        }

        File directory = new File(directoryPath.toAbsolutePath().toString());

        File[] files = directory.listFiles((d, name) -> name.endsWith(".cql"));

        if (files != null && files.length > 0) {
          return Constants.HL7_CQL_FILES_CODE;
        } else {
          return Constants.HL7_FHIR_FILES_CODE;
        }
      } catch (Exception e) {
        return Constants.HL7_FHIR_FILES_CODE;
      }
    } else {
      return Constants.HL7_FHIR_REST_CODE;
    }
  }

  protected LibrarySourceProvider getProvider(IBaseCoding connectionType, String url,
      List<String> headers) {
    for (TypedLibrarySourceProviderFactory factory : this.librarySourceProviderFactories) {
      if (factory.getType().equals(connectionType.getCode())) {
        return factory.create(url, headers);
      }
    }

    throw new IllegalArgumentException("invalid connectionType for loading Libraries");
  }

  @Override
  public LibrarySourceProvider create(IBaseBundle contentBundle) {
    requireNonNull(contentBundle, "contentBundle can not be null");

    if (!contentBundle.getStructureFhirVersionEnum()
        .equals(this.fhirContext.getVersion().getVersion())) {
      throw new IllegalArgumentException(
          "The FHIR version of dataBundle and the FHIR context do not match");
    }

    return new BundleFhirLibrarySourceProvider(this.fhirContext, contentBundle, this.adapterFactory,
        this.libraryVersionSelector);
  }
}
