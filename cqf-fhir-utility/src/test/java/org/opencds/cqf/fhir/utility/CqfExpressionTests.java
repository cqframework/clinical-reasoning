package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hl7.fhir.instance.model.api.IBaseExtension;
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
        var altLanguage = "text/fhirpath";
        var altExpression = "altExpression";
        var altLibraryUrl = "Library/alt";
        var cqfExpression = new CqfExpression();
        cqfExpression.setLanguage(language);
        cqfExpression.setExpression(expression);
        cqfExpression.setLibraryUrl(libraryUrl);
        cqfExpression.setAltLanguage(altLanguage);
        cqfExpression.setAltExpression(altExpression);
        cqfExpression.setAltLibraryUrl(altLibraryUrl);
        assertEquals(language, cqfExpression.getLanguage());
        assertEquals(expression, cqfExpression.getExpression());
        assertEquals(libraryUrl, cqfExpression.getLibraryUrl());
        assertEquals(altLanguage, cqfExpression.getAltLanguage());
        assertEquals(altExpression, cqfExpression.getAltExpression());
        assertEquals(altLibraryUrl, cqfExpression.getAltLibraryUrl());
    }

    @Test
    void testDstu3Extension() {
        var expression = new org.hl7.fhir.dstu3.model.StringType("expression");
        var ext = new org.hl7.fhir.dstu3.model.Extension(Constants.CQIF_CQL_EXPRESSION, expression);
        var cqfExpression = CqfExpression.of(ext, "http://test.com/Library/test");
        assertEquals(expression.getValue(), cqfExpression.getExpression());
    }

    @Test
    void testR4Extension() {
        var defaultLibraryUrl = "http://test.com/Library/test";
        var expression = new org.hl7.fhir.r4.model.Expression()
                .setLanguage("text/cql.identifier")
                .setExpression("expression");
        var ext = new org.hl7.fhir.r4.model.Extension(Constants.CQF_EXPRESSION, expression);
        var cqfExpression = CqfExpression.of(ext, defaultLibraryUrl);
        assertEquals(expression.getExpression(), cqfExpression.getExpression());
        assertEquals(expression.getLanguage(), cqfExpression.getLanguage());
        assertEquals(defaultLibraryUrl, cqfExpression.getLibraryUrl());
        assertNull(cqfExpression.getAltExpression());
        assertNull(cqfExpression.getAltLanguage());
        assertNull(cqfExpression.getAltLibraryUrl());
    }

    @Test
    void testR4ExtensionWithAlternate() {
        var defaultLibraryUrl = "http://test.com/Library/test";
        var expression = new org.hl7.fhir.r4.model.Expression()
                .setLanguage("text/cql.identifier")
                .setExpression("expression");
        var altExpression = new org.hl7.fhir.r4.model.Expression()
                .setLanguage("text/fhirpath")
                .setExpression("altExpression");
        expression.addExtension(Constants.ALT_EXPRESSION_EXT, altExpression);
        var ext = new org.hl7.fhir.r4.model.Extension(Constants.CQF_EXPRESSION, expression);
        var cqfExpression = CqfExpression.of(ext, defaultLibraryUrl);
        assertEquals(expression.getExpression(), cqfExpression.getExpression());
        assertEquals(expression.getLanguage(), cqfExpression.getLanguage());
        assertEquals(defaultLibraryUrl, cqfExpression.getLibraryUrl());
        assertEquals(altExpression.getExpression(), cqfExpression.getAltExpression());
        assertEquals(altExpression.getLanguage(), cqfExpression.getAltLanguage());
        assertNull(cqfExpression.getAltLibraryUrl());
    }

    @Test
    void testR5Extension() {
        var defaultLibraryUrl = "http://test.com/Library/test";
        var expression = new org.hl7.fhir.r5.model.Expression()
                .setLanguage("text/cql.identifier")
                .setExpression("expression");
        var ext = new org.hl7.fhir.r5.model.Extension(Constants.CQF_EXPRESSION, expression);
        var cqfExpression = CqfExpression.of(ext, defaultLibraryUrl);
        assertEquals(expression.getExpression(), cqfExpression.getExpression());
        assertEquals(expression.getLanguage(), cqfExpression.getLanguage());
        assertEquals(defaultLibraryUrl, cqfExpression.getLibraryUrl());
        assertNull(cqfExpression.getAltExpression());
        assertNull(cqfExpression.getAltLanguage());
        assertNull(cqfExpression.getAltLibraryUrl());
    }

    @Test
    void testR5ExtensionWithAlternate() {
        var defaultLibraryUrl = "http://test.com/Library/test";
        var expression = new org.hl7.fhir.r5.model.Expression()
                .setLanguage("text/cql.identifier")
                .setExpression("expression");
        var altExpression = new org.hl7.fhir.r5.model.Expression()
                .setLanguage("text/fhirpath")
                .setExpression("altExpression");
        expression.addExtension(Constants.ALT_EXPRESSION_EXT, altExpression);
        var ext = new org.hl7.fhir.r5.model.Extension(Constants.CQF_EXPRESSION, expression);
        var cqfExpression = CqfExpression.of(ext, defaultLibraryUrl);
        assertEquals(expression.getExpression(), cqfExpression.getExpression());
        assertEquals(expression.getLanguage(), cqfExpression.getLanguage());
        assertEquals(defaultLibraryUrl, cqfExpression.getLibraryUrl());
        assertEquals(altExpression.getExpression(), cqfExpression.getAltExpression());
        assertEquals(altExpression.getLanguage(), cqfExpression.getAltLanguage());
        assertNull(cqfExpression.getAltLibraryUrl());
    }
}
