package org.opencds.cqf.cql.evaluator.engine.execution;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.cql.evaluator.converter.VersionedIdentifierConverter.toElmIdentifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlCompilerException.ErrorSeverity;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryContentType;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.cql2elm.model.serialization.LibraryWrapper;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.exception.CqlException;
import org.opencds.cqf.cql.engine.execution.JsonCqlLibraryReader;
import org.opencds.cqf.cql.evaluator.engine.elm.LibraryMapper;
import org.opencds.cqf.cql.evaluator.engine.util.TranslatorOptionsUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

/**
 * The TranslatingLibraryLoader attempts to load a library from a set of
 * LibrarySourceProviders. If pre-existing ELM is found for the requested
 * library and the ELM was generated using the same set of translator options as
 * is provided to the TranslatingLibraryLoader, it will use that ELM. If the ELM
 * is not found, or the ELM translation options do not match, the
 * TranslatingLibraryLoader will attempt to regenerate the ELM by translating
 * CQL content with the requested options. If neither matching ELM content nor
 * CQL content is found for the requested Library, null is returned.
 */
public class TranslatingLibraryLoader implements TranslatorOptionAwareLibraryLoader {

    protected static ObjectMapper objectMapper;

    protected CqlTranslatorOptions cqlTranslatorOptions;
    protected List<LibrarySourceProvider> librarySourceProviders;

    protected LibraryManager libraryManager;

    public TranslatingLibraryLoader(ModelManager modelManager, List<LibrarySourceProvider> librarySourceProviders,
            CqlTranslatorOptions translatorOptions) {
        this.librarySourceProviders = requireNonNull(librarySourceProviders,
                "librarySourceProviders can not be null");

        this.cqlTranslatorOptions = translatorOptions != null ? translatorOptions
                : CqlTranslatorOptions.defaultOptions();

        this.libraryManager = new LibraryManager(modelManager);
        for (LibrarySourceProvider provider : librarySourceProviders) {
            libraryManager.getLibrarySourceLoader().registerProvider(provider);
        }
    }

    public Library load(VersionedIdentifier libraryIdentifier) {
        Library library = this.getLibraryFromElm(libraryIdentifier);

        if (library != null && this.translatorOptionsMatch(library)) {
            return library;
        }

        return this.translate(libraryIdentifier);
    }

    @Override
    public CqlTranslatorOptions getCqlTranslatorOptions() {
        return this.cqlTranslatorOptions;
    }

    protected Library getLibraryFromElm(VersionedIdentifier libraryIdentifier) {
        org.hl7.elm.r1.VersionedIdentifier versionedIdentifier = toElmIdentifier(libraryIdentifier);
        InputStream content = this.getLibraryContent(versionedIdentifier, LibraryContentType.JSON);
        if (content != null) {
            try {
                return this.readJxson(content);
            } catch (Exception e) {
                // Intentionally empty. Fall through to xml
            }
        }

        return null;
    }

    protected Boolean translatorOptionsMatch(Library library) {
        EnumSet<CqlTranslatorOptions.Options> options = TranslatorOptionsUtil.getTranslatorOptions(library);
        if (options == null) {
            return false;
        }

        return options.equals(this.cqlTranslatorOptions.getOptions());
    }

    protected InputStream getLibraryContent(org.hl7.elm.r1.VersionedIdentifier libraryIdentifier,
            LibraryContentType libraryContentType) {
        for (LibrarySourceProvider librarySourceProvider : librarySourceProviders) {
            InputStream content = librarySourceProvider.getLibraryContent(libraryIdentifier, libraryContentType);
            if (content != null) {
                return content;
            }
        }

        return null;
    }

    protected Library translate(VersionedIdentifier libraryIdentifier) {
        CompiledLibrary library = null;
        List<CqlCompilerException> errors = new ArrayList<>();
        try {
            library = this.libraryManager.resolveLibrary(toElmIdentifier(libraryIdentifier),
                    this.cqlTranslatorOptions, errors);
        } catch (Exception e) {
            throw new CqlException(String.format("Unable translate library %s", libraryIdentifier.getId()), e);
        }

        if (!errors.isEmpty()) {
            for (CqlCompilerException e : errors) {
                if (e.getSeverity() == ErrorSeverity.Error) {
                    throw new CqlException(String.format("Translation of library %s failed with the following message: %s", libraryIdentifier.getId(), e.getMessage()));
                }
            }
        }

        try {
            return LibraryMapper.INSTANCE.map(library.getLibrary());
        }
        catch(Exception e) {
            throw new CqlException(String.format("Mapping of library %s failed", libraryIdentifier.getId()), e);
        }
    }

    protected synchronized Library readJxson(InputStream inputStream) throws IOException {
        return JsonCqlLibraryReader.read(inputStream);
    }

    protected synchronized String toJxson(org.hl7.elm.r1.Library library) {
        try {
            return convertToJxson(library);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not convert library to JXSON.", e);
        }
    }

    protected synchronized ObjectMapper getJxsonMapper() {
        if (objectMapper == null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT);
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            JaxbAnnotationModule annotationModule = new JaxbAnnotationModule();
            mapper.registerModule(annotationModule);
            objectMapper = mapper;
        }

        return objectMapper;
    }

    public String convertToJxson(org.hl7.elm.r1.Library library) throws JsonProcessingException {
        LibraryWrapper wrapper = new LibraryWrapper();
        wrapper.setLibrary(library);
        return this.getJxsonMapper().writeValueAsString(wrapper);
    }

}