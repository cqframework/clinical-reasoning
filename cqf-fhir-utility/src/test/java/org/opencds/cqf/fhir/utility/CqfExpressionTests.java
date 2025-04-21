package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.r4.model.Expression;
import org.junit.jupiter.api.Test;

class CqfExpressionTests {
    @Test
    void testInvalidExtension() {
        var cqfExpression = CqfExpression.of((IBaseExtension<?, ?>) null, null);
        assertNull(cqfExpression);
        var dstu2Expression = CqfExpression.of(new org.hl7.fhir.dstu2.model.Extension(), null);
        assertNull(dstu2Expression);
        assertNull(CqfExpression.of(new org.hl7.fhir.r4.model.Extension(), null));
        assertNull(CqfExpression.of(new org.hl7.fhir.r5.model.Extension(), null));
    }

    @Test
    void testCqfExpression() {
        var language = "text/cql";
        var expression = "expression";
        var libraryUrl = "Library/lib";
        var referencedLibraries = Map.of("lib", libraryUrl);
        var altLanguage = "text/cql-identifier";
        var altExpression = "altExpression";
        var altLibraryUrl = "Library/alt";
        var cqfExpression = new CqfExpression();
        cqfExpression.setLanguage(language);
        cqfExpression.setExpression(expression);
        cqfExpression.setReferencedLibraries(referencedLibraries);
        cqfExpression.setLibraryUrl(referencedLibraries.get("lib"));
        cqfExpression.setAltLanguage(altLanguage);
        cqfExpression.setAltExpression(altExpression);
        cqfExpression.setAltLibraryUrl(altLibraryUrl);
        assertEquals(language, cqfExpression.getLanguage());
        assertEquals(expression, cqfExpression.getExpression());
        assertEquals(referencedLibraries, cqfExpression.getReferencedLibraries());
        // library url is always null for language type other than text/cql-identifier
        assertNull(cqfExpression.getLibraryUrl());
        assertEquals(altLanguage, cqfExpression.getAltLanguage());
        assertEquals(altExpression, cqfExpression.getAltExpression());
        assertEquals(altLibraryUrl, cqfExpression.getAltLibraryUrl());
    }

    @Test
    void testDstu3Extension() {
        var expression = new org.hl7.fhir.dstu3.model.StringType("expression");
        var ext = new org.hl7.fhir.dstu3.model.Extension(Constants.CQIF_CQL_EXPRESSION, expression);
        var cqfExpression = CqfExpression.of(ext, Map.of("test", "http://test.com/Library/test"));
        assertEquals(expression.getValue(), cqfExpression.getExpression());
    }

    @Test
    void testR4Extension() {
        var referencedLibraries = Map.of("test", "http://test.com/Library/test");
        var expression = new org.hl7.fhir.r4.model.Expression()
                .setLanguage("text/cql.identifier")
                .setExpression("expression");
        var ext = new org.hl7.fhir.r4.model.Extension(Constants.CQF_EXPRESSION, expression);
        var cqfExpression = CqfExpression.of(ext, referencedLibraries);
        assertEquals(expression.getExpression(), cqfExpression.getExpression());
        assertEquals(expression.getLanguage(), cqfExpression.getLanguage());
        assertEquals(referencedLibraries, cqfExpression.getReferencedLibraries());
        assertEquals(referencedLibraries.get("test"), cqfExpression.getLibraryUrl());
        assertNull(cqfExpression.getAltExpression());
        assertNull(cqfExpression.getAltLanguage());
        assertNull(cqfExpression.getAltLibraryUrl());
    }

    @Test
    void testR4ExtensionWithAlternate() {
        var referencedLibraries = Map.of("test", "http://test.com/Library/test");
        var expression = new org.hl7.fhir.r4.model.Expression()
                .setLanguage("text/cql.identifier")
                .setExpression("expression");
        var altExpression = new org.hl7.fhir.r4.model.Expression()
                .setLanguage("text/fhirpath")
                .setExpression("altExpression");
        expression.addExtension(Constants.ALT_EXPRESSION_EXT, altExpression);
        var ext = new org.hl7.fhir.r4.model.Extension(Constants.CQF_EXPRESSION, expression);
        var cqfExpression = CqfExpression.of(ext, referencedLibraries);
        assertEquals(expression.getExpression(), cqfExpression.getExpression());
        assertEquals(expression.getLanguage(), cqfExpression.getLanguage());
        assertEquals(referencedLibraries, cqfExpression.getReferencedLibraries());
        assertEquals(referencedLibraries.get("test"), cqfExpression.getLibraryUrl());
        assertEquals(altExpression.getExpression(), cqfExpression.getAltExpression());
        assertEquals(altExpression.getLanguage(), cqfExpression.getAltLanguage());
        assertNull(cqfExpression.getAltLibraryUrl());
    }

    @Test
    void testR5Extension() {
        var referencedLibraries = Map.of("test", "http://test.com/Library/test");
        var expression = new org.hl7.fhir.r5.model.Expression()
                .setLanguage("text/cql.identifier")
                .setExpression("expression");
        var ext = new org.hl7.fhir.r5.model.Extension(Constants.CQF_EXPRESSION, expression);
        var cqfExpression = CqfExpression.of(ext, referencedLibraries);
        assertEquals(expression.getExpression(), cqfExpression.getExpression());
        assertEquals(expression.getLanguage(), cqfExpression.getLanguage());
        assertEquals(referencedLibraries, cqfExpression.getReferencedLibraries());
        assertEquals(referencedLibraries.get("test"), cqfExpression.getLibraryUrl());
        assertNull(cqfExpression.getAltExpression());
        assertNull(cqfExpression.getAltLanguage());
        assertNull(cqfExpression.getAltLibraryUrl());
    }

    @Test
    void testR5ExtensionWithAlternate() {
        var referencedLibraries = Map.of("test", "http://test.com/Library/test");
        var expression = new org.hl7.fhir.r5.model.Expression()
                .setLanguage("text/cql.identifier")
                .setExpression("expression");
        var altExpression = new org.hl7.fhir.r5.model.Expression()
                .setLanguage("text/fhirpath")
                .setExpression("altExpression");
        expression.addExtension(Constants.ALT_EXPRESSION_EXT, altExpression);
        var ext = new org.hl7.fhir.r5.model.Extension(Constants.CQF_EXPRESSION, expression);
        var cqfExpression = CqfExpression.of(ext, referencedLibraries);
        assertEquals(expression.getExpression(), cqfExpression.getExpression());
        assertEquals(expression.getLanguage(), cqfExpression.getLanguage());
        assertEquals(referencedLibraries, cqfExpression.getReferencedLibraries());
        assertEquals(referencedLibraries.get("test"), cqfExpression.getLibraryUrl());
        assertEquals(altExpression.getExpression(), cqfExpression.getAltExpression());
        assertEquals(altExpression.getLanguage(), cqfExpression.getAltLanguage());
        assertNull(cqfExpression.getAltLibraryUrl());
    }

    @Test
    void testSingleLibraryReference() {
        var library1 = "TestLibrary1";
        var libraryUrl1 = "http://fhir.test/Library/TestLibrary1";
        var expression =
                new CqfExpression("text/cql-identifier", "TestLibrary1.testExpression", Map.of(library1, libraryUrl1));
        var result = expression.getLibraryUrl();
        assertEquals(libraryUrl1, result);
    }

    @Test
    void testMultipleLibraryReferences() {
        var library1 = "TestLibrary1";
        var libraryUrl1 = "http://fhir.test/Library/TestLibrary1";
        var library2 = "TestLibrary2";
        var libraryUrl2 = "http://fhir.test/Library/TestLibrary2";
        var expression = new Expression()
                .setLanguage("text/cql-identifier")
                .setExpression("testExpression")
                .setReference(libraryUrl1);
        var cqfExpression = CqfExpression.of(expression, Map.of(library1, libraryUrl1, library2, libraryUrl2));
        var result = cqfExpression.getLibraryUrl();
        assertEquals(libraryUrl1, result);
    }
}
