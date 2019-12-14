package com.alphora.cql.service.loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.TranslatedLibrary;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.elm.r1.ObjectFactory;
import org.opencds.cqf.cql.execution.CqlLibraryReader;
import org.opencds.cqf.cql.execution.LibraryLoader;

public class TranslatingLibraryLoader implements LibraryLoader {

    
    private LibraryManager libraryManager;
    private JAXBContext jaxbContext;

    public TranslatingLibraryLoader(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
    }

    public Library load(VersionedIdentifier libraryIdentifier) {
        try {
            List<CqlTranslatorException> errors = new ArrayList<>();
            TranslatedLibrary library = this.libraryManager.resolveLibrary(toElmIdentifier(libraryIdentifier.getId(), libraryIdentifier.getVersion()), errors);
            return this.readXml(this.toXml(library.getLibrary()));
        }
        catch (Exception e) {
            return null;
        }
    }

    private Library readXml(String xml) throws IOException, JAXBException {
        return CqlLibraryReader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String toXml(org.hl7.elm.r1.Library library) {
        try {
            return convertToXml(library);
        }
        catch (JAXBException e) {
            throw new IllegalArgumentException("Could not convert library to XML.", e);
        }
    }

    public String convertToXml(org.hl7.elm.r1.Library library) throws JAXBException {
        Marshaller marshaller = getJaxbContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter writer = new StringWriter();
        marshaller.marshal(new ObjectFactory().createLibrary(library), writer);
        return writer.getBuffer().toString();
    }

    private JAXBContext getJaxbContext() throws JAXBException {
        if (this.jaxbContext == null) {
            this.jaxbContext = JAXBContext.newInstance(org.hl7.elm.r1.Library.class, org.hl7.cql_annotations.r1.Annotation.class);
        }

        return this.jaxbContext;
    }

    public org.hl7.elm.r1.VersionedIdentifier toElmIdentifier(String name, String version) {
        return new org.hl7.elm.r1.VersionedIdentifier().withId(name).withVersion(version);
    }
}