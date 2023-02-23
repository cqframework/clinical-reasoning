package org.opencds.cqf.cql.evaluator.cql2elm.content.fhir;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.cqframework.cql.cql2elm.LibraryContentType;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class BaseFhirLibrarySourceProviderTests {

    private static BaseFhirLibrarySourceProvider testFhirLibrarySourceProvider;
    private static FhirContext fhirContext;
    private static IParser parser;

    @BeforeClass
    public void setup() {
        fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
        parser = fhirContext.newJsonParser();

        testFhirLibrarySourceProvider = new BaseFhirLibrarySourceProvider(new AdapterFactory()) {
            @Override
            public IBaseResource getLibrary(VersionedIdentifier versionedIdentifier) {
                String name = versionedIdentifier.getId();

                InputStream libraryStream = BaseFhirLibrarySourceProviderTests.class
                        .getResourceAsStream(name + ".json");

                return parser.parseResource(new InputStreamReader(libraryStream));
            }
        };
    }

    private String readToString(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
    }

    private String getContent(String libraryName, LibraryContentType libraryContentType) {
        VersionedIdentifier libraryIdentifier = new VersionedIdentifier().withId(libraryName);
        InputStream stream = testFhirLibrarySourceProvider.getLibraryContent(libraryIdentifier,
                libraryContentType);
        if (stream == null) {
            return null;
        }

        return readToString(stream);
    }

    @Test
    public void allSupportedContentReturnsContent() {
        String libraryName = "AllContent";
        String actual = this.getContent(libraryName, LibraryContentType.CQL);
        assertEquals(actual, "CQL");

        actual = this.getContent(libraryName, LibraryContentType.JSON);
        assertEquals(actual, "JSON");

        actual = this.getContent(libraryName, LibraryContentType.XML);
        assertEquals(actual, "XML");
    }

    public void coffeeContentIsNull() {
        String libraryName = "AllContent";
        String content = this.getContent(libraryName, LibraryContentType.COFFEE);
        assertNull(content);
    }

    @Test
    public void missingContentReturnsNull() {
        String libraryName = "CqlContent";
        String actual = this.getContent(libraryName, LibraryContentType.CQL);
        assertEquals(actual, "CQL");

        actual = this.getContent(libraryName, LibraryContentType.JSON);
        assertNull(actual);

        libraryName = "JsonContent";
        actual = this.getContent(libraryName, LibraryContentType.JSON);
        assertEquals(actual, "JSON");

        actual = this.getContent(libraryName, LibraryContentType.CQL);
        assertNull(actual);
    }

    @Test
    public void getSourceReturnsCql() {
        String actual = this.readToString(testFhirLibrarySourceProvider
                .getLibrarySource(new VersionedIdentifier().withId("AllContent")));
        assertEquals(actual, "CQL");
    }
}
