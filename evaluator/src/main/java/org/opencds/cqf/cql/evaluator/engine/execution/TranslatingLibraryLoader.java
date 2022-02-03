package org.opencds.cqf.cql.evaluator.engine.execution;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.cql.evaluator.converter.VersionedIdentifierConverter.toElmIdentifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.CqlTranslatorException.ErrorSeverity;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.cql2elm.model.serialization.LibraryWrapper;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.exception.CqlException;
import org.opencds.cqf.cql.engine.execution.JsonCqlLibraryReader;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentType;
import org.opencds.cqf.cql.evaluator.engine.elm.LibraryMapper;
import org.opencds.cqf.cql.evaluator.engine.util.TranslatorOptionsUtil;

/**
 * The TranslatingLibraryLoader attempts to load a library from a set of
 * LibraryContentProviders. If pre-existing ELM is found for the requested
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
    protected List<LibraryContentProvider> libraryContentProviders;

    protected LibraryManager libraryManager;

    public TranslatingLibraryLoader(ModelManager modelManager, List<LibraryContentProvider> libraryContentProviders,
            CqlTranslatorOptions translatorOptions) {
        this.libraryContentProviders = requireNonNull(libraryContentProviders,
                "libraryContentProviders can not be null");

        this.cqlTranslatorOptions = translatorOptions != null ? translatorOptions
                : CqlTranslatorOptions.defaultOptions();

        this.libraryManager = new LibraryManager(modelManager);
        for (LibraryContentProvider provider : libraryContentProviders) {
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
        InputStream content = this.getLibraryContent(versionedIdentifier, LibraryContentType.JXSON);
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
        EnumSet<CqlTranslator.Options> options = TranslatorOptionsUtil.getTranslatorOptions(library);
        if (options == null) {
            return false;
        }

        return options.equals(this.cqlTranslatorOptions.getOptions());
    }

    protected InputStream getLibraryContent(org.hl7.elm.r1.VersionedIdentifier libraryIdentifier,
            LibraryContentType libraryContentType) {
        for (LibraryContentProvider libraryContentProvider : libraryContentProviders) {
            InputStream content = libraryContentProvider.getLibraryContent(libraryIdentifier, libraryContentType);
            if (content != null) {
                return content;
            }
        }

        return null;
    }

    protected Library translate(VersionedIdentifier libraryIdentifier) {
        TranslatedLibrary library = null;
        List<CqlTranslatorException> errors = new ArrayList<>();
        try {
            library = this.libraryManager.resolveLibrary(toElmIdentifier(libraryIdentifier),
                    this.cqlTranslatorOptions, errors);
        } catch (Exception e) {
            throw new CqlException(String.format("Unable translate library %s", libraryIdentifier.getId()), e);
        }

        if (!errors.isEmpty()) {
            for (CqlTranslatorException e : errors) {
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

    protected synchronized Library readJxson(String json) throws IOException {
        return this.readJxson(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    protected synchronized Library readJxson(InputStream inputStream) throws IOException {
        return JsonCqlLibraryReader.read(new InputStreamReader(inputStream));
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