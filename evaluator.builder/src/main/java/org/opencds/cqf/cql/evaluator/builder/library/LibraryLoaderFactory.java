package org.opencds.cqf.cql.evaluator.builder.library;

import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isFileUri;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.cql2elm.BundleLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.CacheAwareModelManager;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;

import ca.uhn.fhir.context.FhirContext;

public class LibraryLoaderFactory implements org.opencds.cqf.cql.evaluator.builder.LibraryLoaderFactory {

    protected Set<TypedLibrarySourceProviderFactory> librarySourceProviderFactories;
    protected FhirContext fhirContext;
    protected AdapterFactory adapterFactory;

    protected Map<VersionedIdentifier, Model> globalModelCache = new ConcurrentHashMap<>();

    @Inject
    public LibraryLoaderFactory(FhirContext fhirContext, AdapterFactory adapterFactory,
            Set<TypedLibrarySourceProviderFactory> librarySourceProviderFactories) {
        this.librarySourceProviderFactories = requireNonNull(librarySourceProviderFactories,
                "librarySourceProviderFactories can not be null");
        this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null");
        this.adapterFactory = requireNonNull(adapterFactory, "adapterFactory can not be null");
    }

    protected LibraryLoader create(LibraryManager libraryManager, CqlTranslatorOptions translatorOptions) {
        requireNonNull(libraryManager, "libraryManager can not be null");
        LibraryLoader libraryLoader = new TranslatingLibraryLoader(libraryManager,
                translatorOptions == null ? CqlTranslatorOptions.defaultOptions() : translatorOptions);

        return libraryLoader;
    }

    protected LibraryLoader create(List<LibrarySourceProvider> librarySourceProviders,
            CqlTranslatorOptions translatorOptions) {
        requireNonNull(librarySourceProviders, "librarySourceProviders can not be null");

        LibraryManager libraryManager = this.createLibraryManager();
        for (LibrarySourceProvider provider : librarySourceProviders) {
            libraryManager.getLibrarySourceLoader().registerProvider(provider);
        }

        libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());

        return this.create(libraryManager, translatorOptions);
    }

    @Override
    public LibraryLoader create(EndpointInfo endpointInfo, CqlTranslatorOptions translatorOptions) {
        requireNonNull(endpointInfo, "endpointInfo can not be null");
        if (endpointInfo.getAddress() == null) {
            throw new IllegalArgumentException("endpointInfo must have a url defined");
        }

        if (endpointInfo.getType() == null) {
            endpointInfo.setType(detectType(endpointInfo.getAddress()));
        }

        LibrarySourceProvider sourceProvider = this.getProvider(endpointInfo.getType(), endpointInfo.getAddress(),
                endpointInfo.getHeaders());

        return create(Collections.singletonList(sourceProvider), translatorOptions);
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

    protected LibrarySourceProvider getProvider(IBaseCoding connectionType, String url, List<String> headers) {
        for (TypedLibrarySourceProviderFactory factory : this.librarySourceProviderFactories) {
            if (factory.getType().equals(connectionType.getCode())) {
                return factory.create(url, headers);
            }
        }

        throw new IllegalArgumentException("invalid connectionType for loading Libraries");
    }

    protected LibraryManager createLibraryManager() {
        ModelManager modelManager = new CacheAwareModelManager(this.globalModelCache);
        return new LibraryManager(modelManager);
    }

    @Override
    public LibraryLoader create(IBaseBundle contentBundle, CqlTranslatorOptions translatorOptions) {
        requireNonNull(contentBundle, "contentBundle can not be null");

        if (!contentBundle.getStructureFhirVersionEnum().equals(this.fhirContext.getVersion().getVersion())) {
            throw new IllegalArgumentException("The FHIR version of dataBundle and the FHIR context do not match");
        }

        return this.create(Collections.singletonList(new BundleLibrarySourceProvider(this.fhirContext, contentBundle, this.adapterFactory)), translatorOptions);
    }
}