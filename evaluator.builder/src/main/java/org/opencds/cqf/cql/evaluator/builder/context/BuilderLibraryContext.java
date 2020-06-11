package org.opencds.cqf.cql.evaluator.builder.context;

import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.implementation.bundle.BundleLibraryLoaderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.file.FileLibraryLoaderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.remote.RemoteLibraryLoaderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.string.StringLibraryLoaderBuilder;
import org.opencds.cqf.cql.evaluator.Helpers;
import org.opencds.cqf.cql.evaluator.builder.context.api.LibraryContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseBundle;

/**
 * Provides LibraryContext needed for CQL Evaluation
 *    1. A pre-constructed library loader
 *    2. String representations of Library Resources
 *    3. A remote library repository
 *    4. A filesystem with library content
 *    5. A Bundle with FHIR Libraries
 */
public class BuilderLibraryContext extends BuilderContext implements LibraryContext {
    /**
     * set LibraryLoader with Preconfigured LibraryLoader used to execute
     * 
     * @param libraryLoader preconfigured LibraryLoader
     * @return BuilderTerminologyContext a new instance with the appropriate context filled out.
     */
    @Override
    public BuilderTerminologyContext withLibraryLoader(LibraryLoader libraryLoader) {
        Objects.requireNonNull(libraryLoader, "libraryLoader can not be null");
        this.libraryLoader = libraryLoader;
        return asTerminologyContext(this);
    }

    /**
     * set LibraryLoader using either a URI of a single Library or a String representation of a single Library
     * 
     * @param library either a File URI with Library Content, or String Representation of a Library
     * @return BuilderTerminologyContext a new instance with the appropriate context filled out.
     */
    @Override
    public BuilderTerminologyContext withLibraryLoader(String library) {
        Objects.requireNonNull(library, "libraryContent can not be null");
        List<String> libraryList = new ArrayList<String>();
        libraryList.add(library);
        if (Helpers.isFileUri(library)) {
            FileLibraryLoaderBuilder fileLibraryLoaderBuilder = new FileLibraryLoaderBuilder();
            fileLibraryLoaderBuilder.build(libraryList, this.models, this.getTranslatorOptions());
        } else {
            StringLibraryLoaderBuilder stringLibraryLoaderBuilder = new StringLibraryLoaderBuilder();
            stringLibraryLoaderBuilder.build(libraryList, this.models, this.getTranslatorOptions());
        }
        return asTerminologyContext(this);
    }

    /**
     * set LibraryLoader using either a list of URIs needed for any Libraries required for execution
     * or set LibraryLoader using either a list of String representations needed for any Libraries required for execution
     * 
     * @param libraries needed for execution
     * @return BuilderTerminologyContext a new instance with the appropriate context filled out.
     */
    @Override
    public BuilderTerminologyContext withLibraryLoader(List<String> libraries) {
        Objects.requireNonNull(libraries, "libraries can not be null");
        if (libraries.isEmpty()) {
            throw new IllegalArgumentException("libraries can not be empty");
        }

        // This allows String representations to be passed in, but will not evaluate
        // them... Is this a bug?
        for (String library : libraries) {
            Helpers.isFileUri(library);
        }
        List<String> fileUriLibraries = libraries.stream().filter(library -> Helpers.isFileUri(library))
                .collect(Collectors.toList());
        if (!fileUriLibraries.isEmpty()) {
            FileLibraryLoaderBuilder fileLibraryLoaderBuilder = new FileLibraryLoaderBuilder();
            libraryLoader = fileLibraryLoaderBuilder.build(fileUriLibraries, this.models, this.getTranslatorOptions());
        } else {
            StringLibraryLoaderBuilder stringLibraryLoaderBuilder = new StringLibraryLoaderBuilder();
            libraryLoader = stringLibraryLoaderBuilder.build(libraries, this.models, this.getTranslatorOptions());
        }
        return asTerminologyContext(this);
    }

    /**
     * set LibraryLoader using a URL pointing to Remote repository containing Libraries needed for execution
     * If now ClientFactory is provided a DefaultClientFactory will be used.
     * Must be a URL of a HAPI FHIR Client as of now.
     * 
     * @param libraryUrl needed for execution
     * @return BuilderTerminologyContext a new instance with the appropriate context filled out.
     */
    @Override
    public BuilderTerminologyContext withRemoteLibraryLoader(URL libraryUrl)
            throws IOException, InterruptedException, URISyntaxException {
        Objects.requireNonNull(libraryUrl, "libraryUrl can not be null");
        Objects.requireNonNull(clientFactory, "clientFactory can not be null");
        RemoteLibraryLoaderBuilder remoteLibraryLoaderBuilder = new RemoteLibraryLoaderBuilder();
        libraryLoader = remoteLibraryLoaderBuilder.build(libraryUrl, this.models, this.clientFactory);
        return asTerminologyContext(this);
    }

    /**
     * set LibraryLoader using a FHIR Bundle containing the libraries needed for execution
     * 
     * @param bundle of libraries needed for execution
     * @return BuilderTerminologyContext a new instance with the appropriate context filled out.
     */
    @Override
    public BuilderTerminologyContext withBundleLibraryLoader(IBaseBundle bundle) {
        Objects.requireNonNull(bundle, "libraryBundle can not be null");
        BundleLibraryLoaderBuilder bundleLibraryLoaderBuilder = new BundleLibraryLoaderBuilder();
        libraryLoader = bundleLibraryLoaderBuilder.build(bundle, this.models, this.getTranslatorOptions());
        return asTerminologyContext(this);
    }    
    
    private BuilderTerminologyContext asTerminologyContext(BuilderContext thisBuilderContext) {
        BuilderTerminologyContext terminologyContext = new BuilderTerminologyContext();
        // This is a hack for now (figure out casting)
        terminologyContext.libraryLoader = thisBuilderContext.libraryLoader;
        terminologyContext.dataProviderMap = thisBuilderContext.dataProviderMap;
        terminologyContext.terminologyProvider = thisBuilderContext.terminologyProvider;
        terminologyContext.models = thisBuilderContext.models;
        terminologyContext.clientFactory = thisBuilderContext.clientFactory;
        terminologyContext.engineOptions = thisBuilderContext.engineOptions;
        terminologyContext.parameterDeserializer = thisBuilderContext.parameterDeserializer;
        terminologyContext.setTranslatorOptions(thisBuilderContext.getTranslatorOptions());
        return terminologyContext;
    }
}