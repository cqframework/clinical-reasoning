package org.opencds.cqf.cql.evaluator.engine.execution;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.cql.evaluator.converter.VersionedIdentifierConverter.toElmIdentifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.elm.r1.ObjectFactory;
import org.opencds.cqf.cql.engine.execution.CqlLibraryReader;
import org.opencds.cqf.cql.engine.execution.JsonCqlLibraryReader;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentType;
import org.opencds.cqf.cql.evaluator.engine.util.TranslatorOptionsUtil;

/**
 * The TranslatingLibraryLoader attempts to load a library from a set of LibraryContentProviders. If pre-existing ELM
 * is found for the requested library and the ELM was generated using the same set of translator options as is provided
 * to the TranslatingLibraryLoader, it will use that ELM. If the ELM is not found, or the ELM translation options do not
 * match, the TranslatingLibraryLoader will attempt to regenerate the ELM by translating CQL content with the requested
 * options. If neither matching ELM content nor CQL content is found for the requested Library, null is returned.
 */
public class TranslatingLibraryLoader implements TranslatorOptionAwareLibraryLoader {

    protected static JAXBContext jaxbContext;
    protected static Marshaller marshaller;
    protected CqlTranslatorOptions cqlTranslatorOptions;
    protected List<LibraryContentProvider> libraryContentProviders;

    protected LibraryManager libraryManager;

    public TranslatingLibraryLoader(ModelManager modelManager, List<LibraryContentProvider> libraryContentProviders,
            CqlTranslatorOptions translatorOptions) {
        this.libraryContentProviders = requireNonNull(libraryContentProviders,
                "libraryContentProviders can not be null");

        this.cqlTranslatorOptions = translatorOptions != null ? translatorOptions : CqlTranslatorOptions.defaultOptions();

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

        content = this.getLibraryContent(versionedIdentifier, LibraryContentType.XML);
        if (content != null) {
            try {
                return this.readXml(content);
            } catch (Exception e) {
                // Intentionally empty. Fall through to null
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
        try {
            List<CqlTranslatorException> errors = new ArrayList<>();
            TranslatedLibrary library = this.libraryManager.resolveLibrary(toElmIdentifier(libraryIdentifier),
                    this.cqlTranslatorOptions, errors);
            // Here we are mapping from a cql-translator ELM library to a cql-engine ELM library.
            // They use different object hierarchies so this serializes out to XML and then reads
            // the library back in.
            return this.readXml(this.toXml(library.getLibrary()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected synchronized Library readJxson(InputStream inputStream) throws IOException {
        return JsonCqlLibraryReader.read(new InputStreamReader(inputStream));
    }

    protected synchronized Library readXml(InputStream inputStream) throws IOException, JAXBException {
        return CqlLibraryReader.read(inputStream);
    }

    protected synchronized Library readXml(String xml) throws IOException, JAXBException {
        return this.readXml(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    protected synchronized String toXml(org.hl7.elm.r1.Library library) {
        try {
            return convertToXml(library);
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Could not convert library to XML.", e);
        }
    }

    protected synchronized String convertToXml(org.hl7.elm.r1.Library library) throws JAXBException {
        StringWriter writer = new StringWriter();
        this.getMarshaller().marshal(new ObjectFactory().createLibrary(library), writer);
        return writer.getBuffer().toString();
    }

    protected synchronized Marshaller getMarshaller() throws JAXBException {
        if (marshaller == null) {
            marshaller = this.getJaxbContext().createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        }

        return marshaller;
    }

    protected synchronized JAXBContext getJaxbContext() throws JAXBException {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(org.hl7.elm.r1.Library.class,
                    org.hl7.cql_annotations.r1.Annotation.class);
        }

        return jaxbContext;
    }
}