package org.opencds.cqf.cql.evaluator.builder;

import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isFileUri;
import static org.opencds.cqf.cql.evaluator.fhir.common.ClientFactory.createClient;
import static org.opencds.cqf.cql.evaluator.fhir.common.DirectoryBundler.bundle;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.api.model.ConnectionType;
import org.opencds.cqf.cql.evaluator.builder.api.model.EndpointInfo;
import org.opencds.cqf.cql.evaluator.cql2elm.BundleLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.FhirServerLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.InMemoryLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class LibraryLoaderFactory implements org.opencds.cqf.cql.evaluator.builder.api.LibraryLoaderFactory {

    protected FhirContext fhirContext;

    @Inject
    public LibraryLoaderFactory(FhirContext fhirContext) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null");
    }

    @Override
    public LibraryLoader create(LibraryManager libraryManager, CqlTranslatorOptions translatorOptions) {
        Objects.requireNonNull(libraryManager, "libraryManager can not be null");
        LibraryLoader libraryLoader = new TranslatingLibraryLoader(libraryManager, 
            translatorOptions == null ? CqlTranslatorOptions.defaultOptions(): translatorOptions);

        return libraryLoader;
    }

    @Override
    public LibraryLoader create(List<LibrarySourceProvider> librarySourceProviders, CqlTranslatorOptions translatorOptions) {
        Objects.requireNonNull(librarySourceProviders, "librarySourceProviders can not be null");

        LibraryManager libraryManager = this.createLibraryManager();
        for (LibrarySourceProvider provider : librarySourceProviders){
            libraryManager.getLibrarySourceLoader().registerProvider(provider);
        }

        libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());

        return this.create(libraryManager, translatorOptions);
    }

    @Override
    public LibraryLoader create(EndpointInfo endpointInfo, CqlTranslatorOptions translatorOptions) {
        Objects.requireNonNull(endpointInfo, "endpointInfo can not be null");
        if (endpointInfo.getUrl() == null) {
            throw new IllegalArgumentException("endpointInfo must have a url defined");
        }

        if (endpointInfo.getType() == null)
        {
            endpointInfo.setType(autoDetectTypeFromUrl(endpointInfo.getUrl()));
        }

        return create(endpointInfo.getUrl(), endpointInfo.getType(), endpointInfo.getHeaders(), translatorOptions);
    }

    protected ConnectionType autoDetectTypeFromUrl(String url) {
        if (isFileUri(url)) {
             // Attempt to auto-detect the type of files.
             Path directoryPath = Paths.get(url);
             File directory = new File(directoryPath.toAbsolutePath().toString());
             File[] files = directory.listFiles((d, name) -> name.endsWith(".cql"));
 
             if (files != null && files.length > 0) {
                 return ConnectionType.HL7_CQL_FILES;
             }
             else {
                 return ConnectionType.HL7_FHIR_FILES;
             }
        }
        else {
            return ConnectionType.HL7_FHIR_REST;
        }
    }

    protected LibraryLoader create(String url, ConnectionType type, List<String> headers, CqlTranslatorOptions translatorOptions) {
        switch (type) {
            case HL7_FHIR_REST:
            return createForUrl(url, headers, translatorOptions);
            case HL7_FHIR_FILES:
            return createForFhirFiles(url, translatorOptions);
            case HL7_CQL_FILES:
                return createForCqlFiles(url, translatorOptions);
            default:
                throw new IllegalArgumentException("invalid connectionType for loading CQL libraries");
        }
    }

    protected LibraryManager createLibraryManager() {
        ModelManager modelManager = new ModelManager();
        return new LibraryManager(modelManager);
    }


    protected LibraryLoader createForUrl(String url, List<String> headers, CqlTranslatorOptions translatorOptions) {
        IGenericClient client = createClient(fhirContext, url, headers);
        return this.create(Collections.singletonList(new FhirServerLibrarySourceProvider(client)), translatorOptions);

    }

    protected LibraryLoader createForFhirFiles(String path, CqlTranslatorOptions translatorOptions) {
        IBaseBundle bundle = bundle(fhirContext, path);
        return this.create(Collections.singletonList(new BundleLibrarySourceProvider(fhirContext, bundle)), translatorOptions);
    }

    protected LibraryLoader createForCqlFiles(String path, CqlTranslatorOptions translatorOptions) {
        List<String> libraries = this.getLibrariesFromPath(path);
        return this.create(Collections.singletonList(new InMemoryLibrarySourceProvider(libraries)), translatorOptions);
    }

    protected List<String> getLibrariesFromPath(String path) {
        Path directoryPath = Paths.get(path);
        File directory = new File(directoryPath.toAbsolutePath().toString());
        File[] files = directory.listFiles((d, name) -> name.endsWith(".cql"));

        return Arrays.asList(files).stream().map(x -> x.toPath()).filter(Files::isRegularFile).map(t -> {
            try {
                return Files.readAllBytes(t);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        })
        .filter(x -> x != null)
        .map(x -> new String(x, StandardCharsets.UTF_8))
        .collect(Collectors.toList());
    }
}