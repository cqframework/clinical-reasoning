package org.opencds.cqf.cql.evaluator.builder.library;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isFileUri;

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

import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;

import ca.uhn.fhir.context.FhirContext;

@Named
public class LibraryContentProviderFactory implements org.opencds.cqf.cql.evaluator.builder.LibraryContentProviderFactory {

    protected Set<TypedLibraryContentProviderFactory> libraryContentProviderFactories;
    protected FhirContext fhirContext;
    protected AdapterFactory adapterFactory;
    protected LibraryVersionSelector libraryVersionSelector;

    protected Map<VersionedIdentifier, Model> globalModelCache = new ConcurrentHashMap<>();

    @Inject
    public LibraryContentProviderFactory(FhirContext fhirContext, AdapterFactory adapterFactory,
            Set<TypedLibraryContentProviderFactory> libraryContentProviderFactories, LibraryVersionSelector libraryVersionSelector) {
        this.libraryContentProviderFactories = requireNonNull(libraryContentProviderFactories,
                "libraryContentProviderFactories can not be null");
        this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null");
        this.adapterFactory = requireNonNull(adapterFactory, "adapterFactory can not be null");
        this.libraryVersionSelector = requireNonNull(libraryVersionSelector, "libraryVersionSelector can not be null");
    }

    @Override
    public LibraryContentProvider create(EndpointInfo endpointInfo) {
        requireNonNull(endpointInfo, "endpointInfo can not be null");
        if (endpointInfo.getAddress() == null) {
            throw new IllegalArgumentException("endpointInfo must have a url defined");
        }

        if (endpointInfo.getType() == null) {
            endpointInfo.setType(detectType(endpointInfo.getAddress()));
        }

        LibraryContentProvider contentProvider = this.getProvider(endpointInfo.getType(), endpointInfo.getAddress(),
                endpointInfo.getHeaders());

        return contentProvider;
    }

    protected IBaseCoding detectType(String url) {
        if (isFileUri(url)) {
            // Attempt to auto-detect the type of files.
            try {
                Path directoryPath = null;
                try {
                    directoryPath = Paths.get(new URL(url).toURI());
                }
                catch (Exception e) {
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

    protected LibraryContentProvider getProvider(IBaseCoding connectionType, String url, List<String> headers) {
        for (TypedLibraryContentProviderFactory factory : this.libraryContentProviderFactories) {
            if (factory.getType().equals(connectionType.getCode())) {
                return factory.create(url, headers);
            }
        }

        throw new IllegalArgumentException("invalid connectionType for loading Libraries");
    }

    @Override
    public LibraryContentProvider create(IBaseBundle contentBundle) {
        requireNonNull(contentBundle, "contentBundle can not be null");

        if (!contentBundle.getStructureFhirVersionEnum().equals(this.fhirContext.getVersion().getVersion())) {
            throw new IllegalArgumentException("The FHIR version of dataBundle and the FHIR context do not match");
        }

        return new BundleFhirLibraryContentProvider(this.fhirContext, contentBundle, this.adapterFactory, this.libraryVersionSelector);
    }
}