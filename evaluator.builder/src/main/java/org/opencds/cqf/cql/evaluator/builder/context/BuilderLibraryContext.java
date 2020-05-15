package org.opencds.cqf.cql.evaluator.builder.context;

import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.implementation.bundle.BundleLibraryLoaderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.file.FileLibraryLoaderBuilder;
import org.opencds.cqf.cql.evaluator.builder.implementation.string.StringLibraryLoaderBuilder;
import org.opencds.cqf.cql.evaluator.Helpers;
import org.opencds.cqf.cql.evaluator.builder.context.api.LibraryContext;
import org.opencds.cqf.cql.evaluator.builder.helper.ModelVersionHelper;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;

/**
 * Provides LibraryContext needed for CQL Evaluation
 */
public class BuilderLibraryContext extends BuilderContext implements LibraryContext {
    // All the different ways we can load libraries:
    // 1. A pre-constructed library loader, and a reference to the primary library
    // 2. Strings of CQL content (and if there's more than one library, a reference
    // to the primary library)
    // 3. A remote library repository, and reference to the primary library
    // 4. A filesystem with library content, and a reference to the primary library
    // 5. A Bundle with FHIR Libraries
    // Figure out Version of libraries
    @Override
    public BuilderTerminologyContext withLibraryLoader(String libraryContent) {
        Objects.requireNonNull(libraryContent, "libraryContent can not be null");
        List<String> libraryList = new ArrayList<String>();
        libraryList.add(libraryContent);
        if (Helpers.isFileUri(libraryContent)) {
            ModelVersionHelper.setModelVersionFromLibraryPath(this.models, libraryContent);
            FileLibraryLoaderBuilder fileLibraryLoaderBuilder = new FileLibraryLoaderBuilder();
            fileLibraryLoaderBuilder.build(libraryList, cqlTranslatorOptions);
        }
        else {
            ModelVersionHelper.setModelVersionFromLibraryString(this.models, libraryContent);
            StringLibraryLoaderBuilder stringLibraryLoaderBuilder = new StringLibraryLoaderBuilder();
            stringLibraryLoaderBuilder.build(libraryList, cqlTranslatorOptions);
        }
        return asTerminologyContext(this);
    }

    @Override
    public BuilderTerminologyContext withLibraryLoader(List<String> libraryContent, VersionedIdentifier libraryIdentifier) {
        Objects.requireNonNull(libraryContent, "libraryContent can not be null");
        if (libraryContent.isEmpty()) {
            throw new IllegalArgumentException("libraryContent can not be empty");
        }
        Objects.requireNonNull(libraryIdentifier, "libraryIdentifier can not be null");

        List<String> fileUriLibraries = libraryContent.stream().filter(library -> Helpers.isFileUri(library))
                .collect(Collectors.toList());
        if (fileUriLibraries.isEmpty()) {
            ModelVersionHelper.setModelVersionFromLibraryPaths(this.models, fileUriLibraries);
            FileLibraryLoaderBuilder fileLibraryLoaderBuilder = new FileLibraryLoaderBuilder();
            libraryLoader = fileLibraryLoaderBuilder.build(fileUriLibraries, cqlTranslatorOptions);
        } else {
            ModelVersionHelper.setModelVersionFromLibraryStrings(this.models, libraryContent);
            StringLibraryLoaderBuilder stringLibraryLoaderBuilder = new StringLibraryLoaderBuilder();
            stringLibraryLoaderBuilder.build(libraryContent, cqlTranslatorOptions);
        }       
        return asTerminologyContext(this);
    }

    @Override
	public BuilderTerminologyContext withLibraryLoader(URL libraryUrl, VersionedIdentifier libraryIdentifier) {
        //FileUri vs Remote
        Objects.requireNonNull(libraryUrl, "libraryUrl can not be null");
        Objects.requireNonNull(libraryIdentifier, "libraryIdentifier can not be null");
		return null;
	}

    @Override
    public BuilderTerminologyContext withLibraryLoader(IBaseBundle bundle, VersionedIdentifier libraryIdentifier) {
        Objects.requireNonNull(bundle, "libraryUrl can not be null");
        Objects.requireNonNull(libraryIdentifier, "libraryIdentifier can not be null");
        BundleLibraryLoaderBuilder bundleLibraryLoaderBuilder = new BundleLibraryLoaderBuilder();
        bundleLibraryLoaderBuilder.build(bundle, cqlTranslatorOptions);
        return asTerminologyContext(this);
    }

    @Override
    public BuilderTerminologyContext withLibraryLoader(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier) {
        Objects.requireNonNull(libraryLoader, "libraryLoader can not be null");
        Objects.requireNonNull(libraryIdentifier, "libraryIdentifier can not be null");
        this.libraryLoader = libraryLoader;
        return asTerminologyContext(this);
    }

    //Should the remote uri come from a Endpoint just incase there needs to be some sort of Authentication?
    //I understand this is Fhir specific, not sure how we want to do this....
    // public BuilderTerminologyContext withLibraryLoader(Endpoint remoteEndpoint) {
    //     return asTerminologyContext(this);
    // }

    private CqlTranslatorOptions cqlTranslatorOptions;
    @Override
    public CqlTranslatorOptions getTranslatorOptions() {
        return this.cqlTranslatorOptions;
    }

    @Override
    public BuilderContext setTranslatorOptions(CqlTranslatorOptions cqlTranslatorOptions) {
        Objects.requireNonNull(cqlTranslatorOptions, "cqlTranslatorOptions can not be null.");
        this.cqlTranslatorOptions = cqlTranslatorOptions;
        return this;
    }
    
    private BuilderTerminologyContext asTerminologyContext(BuilderContext thisBuilderContext) {
        BuilderTerminologyContext terminologyContext = (BuilderTerminologyContext)thisBuilderContext;
        return terminologyContext;
    }
}