package org.opencds.cqf.cql.evaluator.engine.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.EnumSet;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.testng.annotations.Test;
import org.opencds.cqf.cql.engine.execution.JsonCqlLibraryReader;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentType;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;

import static org.cqframework.cql.cql2elm.CqlTranslator.Options.*;

public class TranslatorOptionsUtilTests {

    private Library getLibraryJson(String libraryName) {
        InputStream libraryStream = TranslatorOptionsUtil.class.getResourceAsStream(libraryName + ".json");
        try 
        {
            return JsonCqlLibraryReader.read(new InputStreamReader(libraryStream));
        }
        catch(Exception e) {
            return null;
        }
    }

    private TranslatingLibraryLoader libraryLoader;

    private TranslatingLibraryLoader getTranslatingLibraryLoader() {
        if (this.libraryLoader == null) {
            ModelManager modelManger = new ModelManager();
            this.libraryLoader = new TranslatingLibraryLoader(modelManger,
            Collections.singletonList(new LibraryContentProvider(){

                @Override
                public InputStream getLibraryContent(org.hl7.elm.r1.VersionedIdentifier libraryIdentifier,
                        LibraryContentType libraryContentType) {

                        if (libraryContentType == LibraryContentType.CQL) {
                            return TranslatorOptionsUtilTests.class.getResourceAsStream(libraryIdentifier.getId() + ".cql");
                        }

                        return null;
            
                }
            }), CqlTranslatorOptions.defaultOptions());
        }

        return this.libraryLoader;
    }

    public Library translateLibrary(String libraryName) {
        return this.getTranslatingLibraryLoader().load(new VersionedIdentifier().withId(libraryName));
    }

    @Test
    public void noOptionsReturnsNull() {
        Library test = this.getLibraryJson("LibraryNoOptions");

        EnumSet<CqlTranslator.Options> actual = TranslatorOptionsUtil.getTranslatorOptions(test);

        assertNull(actual);
    }

    @Test
    public void canReadDefaultOptions() {
        Library test = this.getLibraryJson("LibraryDefaultOptions");

        EnumSet<CqlTranslator.Options> expected = CqlTranslatorOptions.defaultOptions().getOptions();

        EnumSet<CqlTranslator.Options> actual = TranslatorOptionsUtil.getTranslatorOptions(test);
        
        assertEquals(actual, expected);
    }

    @Test
    public void canReadNonDefaultOptions() {
        Library test = this.getLibraryJson("LibraryLocatorsAndAnnotations");

        EnumSet<CqlTranslator.Options> expected = EnumSet.of(EnableAnnotations, EnableLocators);
        EnumSet<CqlTranslator.Options> actual = TranslatorOptionsUtil.getTranslatorOptions(test);
        
        assertEquals(actual, expected);
    }

    @Test
    public void canReadMappedOptions() {
        Library test = this.translateLibrary("LibraryTestMappedOptions");
        EnumSet<CqlTranslator.Options> expected = CqlTranslatorOptions.defaultOptions().getOptions();
        EnumSet<CqlTranslator.Options> actual = TranslatorOptionsUtil.getTranslatorOptions(test);
        assertEquals(actual, expected);
    }
    
}
