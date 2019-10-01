package com.alphora.cql.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.*;

import javax.xml.bind.JAXBException;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.execution.CqlLibraryReader;

public class Helpers {
    public static LibraryManager toLibraryManager(ModelManager modelManager, List<String> libraries) throws IOException, JAXBException {
        LibraryManager libraryManager = new LibraryManager(modelManager);
        libraryManager.getLibrarySourceLoader().registerProvider(new InMemoryLibrarySourceProvider(libraries));
        return libraryManager;
    }

    private static Library toLibrary(String text) throws IOException, JAXBException  {
        ModelManager modelManager = new ModelManager();
        LibraryManager libraryManager = toLibraryManager(modelManager, Collections.singletonList(text));

        return toLibrary(text, modelManager, libraryManager);
    }

    private static Library toLibrary(String text, ModelManager modelManager, LibraryManager libraryManager) throws IOException, JAXBException {
        CqlTranslator translator = CqlTranslator.fromText(text, modelManager, libraryManager);
        return readXml(translator.toXml());
    }

    public static Library readXml(String xml) throws IOException, JAXBException {
        return CqlLibraryReader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private Map<VersionedIdentifier, Set<String>> toExpressionMap(VersionedIdentifier libraryIdentifier, String... expressions) {
        Map<VersionedIdentifier, Set<String>> expressionMap = new HashMap<>();
        expressionMap.put(libraryIdentifier, new HashSet<String>(asList(expressions)));
        return expressionMap;
    }

    private Map<VersionedIdentifier, Set<String>> mergeExpressionMaps(Map<VersionedIdentifier, Set<String>>... maps) {
        Map<VersionedIdentifier, Set<String>> mergedMaps = new HashMap<VersionedIdentifier, Set<String>>();
        for (Map<VersionedIdentifier, Set<String>> map : maps) {
           mergedMaps.putAll(map);
        }

        return mergedMaps;
    }

    public static org.hl7.elm.r1.VersionedIdentifier toElmIdentifier(String name, String version) {
        return new org.hl7.elm.r1.VersionedIdentifier().withId(name).withVersion(version);
    }

    public static VersionedIdentifier toExecutionIdentifier(String name, String version) {
        return new VersionedIdentifier().withId(name).withVersion(version);
    }
}