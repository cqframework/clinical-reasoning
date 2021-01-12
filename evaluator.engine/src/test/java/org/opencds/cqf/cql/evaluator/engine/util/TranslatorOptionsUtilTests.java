package org.opencds.cqf.cql.evaluator.engine.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumSet;

import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.elm.execution.Library;
import org.testng.annotations.Test;
import org.opencds.cqf.cql.engine.execution.JsonCqlLibraryReader;

public class TranslatorOptionsUtilTests {

    private Library getLibrary(String libraryName) {
        InputStream libraryStream = TranslatorOptionsUtil.class.getResourceAsStream(libraryName + ".json");
        try 
        {
            return JsonCqlLibraryReader.read(new InputStreamReader(libraryStream));
        }
        catch(Exception e) {
            return null;
        }
    }

    @Test
    public void noOptionsReturnsNull() {
        Library test = this.getLibrary("LibraryNoOptions");

        EnumSet<CqlTranslator.Options> actual = TranslatorOptionsUtil.getTranslatorOptions(test);

        assertNull(actual);
    }

    @Test
    public void canReadDefaultOptions() {
        Library test = this.getLibrary("LibraryDefaultOptions");

        EnumSet<CqlTranslator.Options> expected = CqlTranslatorOptions.defaultOptions().getOptions();

        EnumSet<CqlTranslator.Options> actual = TranslatorOptionsUtil.getTranslatorOptions(test);
        
        assertEquals(expected, actual);
    }

    @Test
    public void canReadNonDefaultOptions() {
        Library test = this.getLibrary("LibraryLocatorsAndAnnotations");

        EnumSet<CqlTranslator.Options> expected = EnumSet.of(CqlTranslator.Options.EnableAnnotations, CqlTranslator.Options.EnableLocators);
        EnumSet<CqlTranslator.Options> actual = TranslatorOptionsUtil.getTranslatorOptions(test);
        
        assertEquals(expected, actual);
    }
    
}
