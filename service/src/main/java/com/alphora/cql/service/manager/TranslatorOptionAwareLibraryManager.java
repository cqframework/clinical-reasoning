package com.alphora.cql.service.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.CqlTranslatorIncludeException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceLoader;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.PriorityLibrarySourceLoader;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.hl7.elm.r1.VersionedIdentifier;

import static org.cqframework.cql.cql2elm.CqlTranslatorException.HasErrors;

public class TranslatorOptionAwareLibraryManager extends LibraryManager {

    private ModelManager modelManager;
    private final Map<String, TranslatedLibrary> libraries;
    private LibrarySourceLoader librarySourceLoader;
    
    private EnumSet<CqlTranslator.Options> translatorOptions;
    public TranslatorOptionAwareLibraryManager(ModelManager modelManager, EnumSet<CqlTranslator.Options> translatorOptions) {
        super(modelManager);

        libraries = new HashMap<>();
        this.modelManager = modelManager;
        this.librarySourceLoader = new PriorityLibrarySourceLoader();
        this.translatorOptions = translatorOptions;
    }

    @Override
    public LibrarySourceLoader getLibrarySourceLoader() {
      return this.librarySourceLoader;
    }

    @Override
    public void setLibrarySourceLoader(LibrarySourceLoader librarySourceLoader) {
        this.librarySourceLoader = librarySourceLoader;
    }

    @Override
    public Map<String, TranslatedLibrary> getTranslatedLibraries() {
        return this.libraries;
    }


    @Override
    public TranslatedLibrary resolveLibrary(VersionedIdentifier libraryIdentifier, List<CqlTranslatorException> errors) {
        if (libraryIdentifier == null) {
            throw new IllegalArgumentException("libraryIdentifier is null.");
        }

        if (libraryIdentifier.getId() == null || libraryIdentifier.getId().equals("")) {
            throw new IllegalArgumentException("libraryIdentifier Id is null");
        }

        TranslatedLibrary library = libraries.get(libraryIdentifier.getId());

        if (library != null
                && libraryIdentifier.getVersion() != null
                && !libraryIdentifier.getVersion().equals(library.getIdentifier().getVersion())) {
            throw new CqlTranslatorIncludeException(String.format("Could not resolve reference to library %s, version %s because version %s is already loaded.",
                    libraryIdentifier.getId(), libraryIdentifier.getVersion(), library.getIdentifier().getVersion()), libraryIdentifier.getId(), libraryIdentifier.getVersion());
        }

        else if (library != null) {
            return library;
        }

        else {
            library = translateLibrary(libraryIdentifier, errors);
            if (!HasErrors(errors)) {
                libraries.put(libraryIdentifier.getId(), library);
            }
        }

        return library;
    }

    private TranslatedLibrary translateLibrary(VersionedIdentifier libraryIdentifier, List<CqlTranslatorException> errors) {
        InputStream librarySource = null;
        try {
            librarySource = this.librarySourceLoader.getLibrarySource(libraryIdentifier);
        }
        catch (Exception e) {
            throw new CqlTranslatorIncludeException(e.getMessage(), libraryIdentifier.getId(), libraryIdentifier.getVersion(), e);
        }

        if (librarySource == null) {
            throw new CqlTranslatorIncludeException(String.format("Could not load source for library %s, version %s.",
                    libraryIdentifier.getId(), libraryIdentifier.getVersion()), libraryIdentifier.getId(), libraryIdentifier.getVersion());
        }
        try {
            CqlTranslator translator = CqlTranslator.fromStream(librarySource, modelManager, this, 
                this.translatorOptions != null ? this.translatorOptions.toArray(new CqlTranslator.Options[this.translatorOptions.size()]) : new CqlTranslator.Options[0]);
            if (errors != null) {
                errors.addAll(translator.getExceptions());
            }

            TranslatedLibrary result = translator.getTranslatedLibrary();
            if (libraryIdentifier.getVersion() != null && !libraryIdentifier.getVersion().equals(result.getIdentifier().getVersion())) {
                throw new CqlTranslatorIncludeException(String.format("Library %s was included as version %s, but version %s of the library was found.",
                        libraryIdentifier.getId(), libraryIdentifier.getVersion(), result.getIdentifier().getVersion()),
                        libraryIdentifier.getId(), libraryIdentifier.getVersion());
            }

            return result;
        } catch (IOException e) {
            throw new CqlTranslatorIncludeException(String.format("Errors occurred translating library %s, version %s.",
                    libraryIdentifier.getId(), libraryIdentifier.getVersion()), libraryIdentifier.getId(), libraryIdentifier.getVersion(), e);
        }
    }
}