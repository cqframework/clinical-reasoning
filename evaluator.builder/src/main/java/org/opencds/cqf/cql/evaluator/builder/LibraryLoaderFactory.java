package org.opencds.cqf.cql.evaluator.builder;

import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isFileUri;
import static org.opencds.cqf.cql.evaluator.fhir.ClientFactory.createClient;
import static org.opencds.cqf.cql.evaluator.fhir.DirectoryBundler.bundle;

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
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.api.Constants;
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
        if (endpointInfo.getAddress() == null) {
            throw new IllegalArgumentException("endpointInfo must have a url defined");
        }

        if (endpointInfo.getType() == null)
        {
            endpointInfo.setType(detectType(endpointInfo.getAddress()));
        }

        return create(endpointInfo.getAddress(), endpointInfo.getType(), endpointInfo.getHeaders(), translatorOptions);
    }

    protected IBaseCoding detectType(String url) {
        if (isFileUri(url)) {
             // Attempt to auto-detect the type of files.
             Path directoryPath = Paths.get(url);
             File directory = new File(directoryPath.toAbsolutePath().toString());
             File[] files = directory.listFiles((d, name) -> name.endsWith(".cql"));
 
             if (files != null && files.length > 0) {
                 return Constants.HL7_CQL_FILES_CODE;
             }
             else {
                 return Constants.HL7_FHIR_FILES_CODE;
             }
        }
        else {
            return Constants.HL7_FHIR_REST_CODE;
        }
    }

    protected LibraryLoader create(String url, IBaseCoding connectionType, List<String> headers, CqlTranslatorOptions translatorOptions) {
        switch (connectionType.getCode()) {
            case Constants.HL7_FHIR_REST:
            return createForUrl(url, headers, translatorOptions);
            case Constants.HL7_FHIR_FILES:
            return createForFhirFiles(url, translatorOptions);
            case Constants.HL7_CQL_FILES:
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