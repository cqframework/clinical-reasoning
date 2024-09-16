package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import ca.uhn.fhir.context.FhirContext;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Date;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.runtime.Code;

class ValueSetsTest {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

    @Test
    void testGetCompose() {
        var valueSet = new ValueSet();
        assertNull(ValueSets.getCompose(fhirContextR4, valueSet));
        var compose = new ValueSet.ValueSetComposeComponent();
        valueSet.setCompose(compose);
        assertEquals(compose, ValueSets.getCompose(fhirContextR4, valueSet));
    }

    @Test
    void testGetIncludes() {
        var valueSet = new ValueSet();
        assertNull(ValueSets.getIncludes(fhirContextR4, valueSet));
        var include = new ValueSet.ConceptSetComponent();
        var compose = new ValueSet.ValueSetComposeComponent();
        valueSet.setCompose(compose);
        assertNull(ValueSets.getIncludes(fhirContextR4, valueSet));
        compose.addInclude(include);
        assertEquals(include, ValueSets.getIncludes(fhirContextR4, valueSet).get(0));
    }

    @Test
    void testGetIncludeConcepts() {
        var concept = new ValueSet.ConceptReferenceComponent();
        var include = new ValueSet.ConceptSetComponent().addConcept(concept);
        var valueSet = new ValueSet();
        assertNull(ValueSets.getIncludeConcepts(fhirContextR4, valueSet));
        valueSet.setCompose(new ValueSet.ValueSetComposeComponent().addInclude(include));
        assertEquals(
                concept, ValueSets.getIncludeConcepts(fhirContextR4, valueSet).get(0));
    }

    @Test
    void testGetIncludeFilters() {
        var filter = new ValueSet.ConceptSetFilterComponent();
        var include = new ValueSet.ConceptSetComponent().addFilter(filter);
        var valueSet = new ValueSet();
        assertNull(ValueSets.getIncludeFilters(fhirContextR4, valueSet));
        valueSet.setCompose(new ValueSet.ValueSetComposeComponent().addInclude(include));
        assertEquals(
                filter, ValueSets.getIncludeFilters(fhirContextR4, valueSet).get(0));
    }

    @Test
    void testGetExcludes() {
        var valueSet = new ValueSet();
        assertNull(ValueSets.getExcludes(fhirContextR4, valueSet));
        var exclude = new ValueSet.ConceptSetComponent();
        var compose = new ValueSet.ValueSetComposeComponent();
        valueSet.setCompose(compose);
        assertNull(ValueSets.getExcludes(fhirContextR4, valueSet));
        compose.addExclude(exclude);
        assertEquals(exclude, ValueSets.getExcludes(fhirContextR4, valueSet).get(0));
    }

    @Test
    void testGetExcludeConcepts() {
        var concept = new ValueSet.ConceptReferenceComponent();
        var exclude = new ValueSet.ConceptSetComponent().addConcept(concept);
        var valueSet = new ValueSet();
        assertNull(ValueSets.getExcludeConcepts(fhirContextR4, valueSet));
        valueSet.setCompose(new ValueSet.ValueSetComposeComponent().addExclude(exclude));
        assertEquals(
                concept, ValueSets.getExcludeConcepts(fhirContextR4, valueSet).get(0));
    }

    @Test
    void testGetExcludeFilters() {
        var filter = new ValueSet.ConceptSetFilterComponent();
        var exclude = new ValueSet.ConceptSetComponent().addFilter(filter);
        var valueSet = new ValueSet();
        assertNull(ValueSets.getExcludeFilters(fhirContextR4, valueSet));
        valueSet.setCompose(new ValueSet.ValueSetComposeComponent().addExclude(exclude));
        assertEquals(
                filter, ValueSets.getExcludeFilters(fhirContextR4, valueSet).get(0));
    }

    @Test
    void testGetExpansion() {
        var valueSet = new ValueSet();
        assertNull(ValueSets.getExpansion(fhirContextR4, valueSet));
        var expansion = new ValueSet.ValueSetExpansionComponent();
        valueSet.setExpansion(expansion);
        assertEquals(expansion, ValueSets.getExpansion(fhirContextR4, valueSet));
    }

    @Test
    void testSetExpansionTimetamp()
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                    NoSuchMethodException, SecurityException {
        var expansion = new ValueSet.ValueSetExpansionComponent();
        assertNull(expansion.getTimestamp());
        var timeStamp = new Date();
        ValueSets.setExpansionTimestamp(fhirContextR4, expansion, timeStamp);
        assertEquals(timeStamp.getTime(), expansion.getTimestamp().getTime());
    }

    @Test
    void testGetContainsInExpansion() {
        assertNull(ValueSets.getContainsInExpansion(fhirContextR4, null));
        var expansion = new ValueSet.ValueSetExpansionComponent();
        assertNull(ValueSets.getContainsInExpansion(fhirContextR4, expansion));
        var contains = new ValueSet.ValueSetExpansionContainsComponent();
        expansion.addContains(contains);
        assertEquals(
                contains,
                ValueSets.getContainsInExpansion(fhirContextR4, expansion).get(0));
    }

    @Test
    void testGetContains() {
        var contains = new ValueSet.ValueSetExpansionContainsComponent();
        var valueSet = new ValueSet().setExpansion(new ValueSet.ValueSetExpansionComponent().addContains(contains));
        assertEquals(contains, ValueSets.getContains(fhirContextR4, valueSet).get(0));
    }

    @Test
    void testGetCodesInCompose() {
        var code = "test";
        var display = "Test";
        var version = "1.0.0";
        var system = "http://fhir.test";
        var valueSet = new ValueSet().setCompose(new ValueSet.ValueSetComposeComponent());
        assertNull(ValueSets.getCodesInCompose(fhirContextR4, valueSet));
        valueSet.getCompose()
                .addInclude(new ValueSet.ConceptSetComponent()
                        .setVersion(version)
                        .setSystem(system)
                        .addConcept(new ValueSet.ConceptReferenceComponent()
                                .setCode(code)
                                .setDisplay(display)));
        var actualCode = ValueSets.getCodesInCompose(fhirContextR4, valueSet).get(0);
        assertEquals(code, actualCode.getCode());
        assertEquals(display, actualCode.getDisplay());
        assertEquals(version, actualCode.getVersion());
        assertEquals(system, actualCode.getSystem());
    }

    @Test
    void testGetCodesInContains() {
        var code = "test";
        var display = "Test";
        var version = "1.0.0";
        var system = "http://fhir.test";
        assertNull(ValueSets.getCodesInContains(fhirContextR4, null));
        var contains = new ValueSet.ValueSetExpansionContainsComponent()
                .setCode(code)
                .setDisplay(display)
                .setVersion(version)
                .setSystem(system);
        var actualCode = ValueSets.getCodesInContains(fhirContextR4, Collections.singletonList(contains))
                .get(0);
        assertEquals(code, actualCode.getCode());
        assertEquals(display, actualCode.getDisplay());
        assertEquals(version, actualCode.getVersion());
        assertEquals(system, actualCode.getSystem());
    }

    @Test
    void testGetCodesInExpansion() {
        var code = "test";
        var display = "Test";
        var version = "1.0.0";
        var system = "http://fhir.test";
        var expansion = new ValueSet.ValueSetExpansionComponent()
                .addContains(new ValueSet.ValueSetExpansionContainsComponent()
                        .setCode(code)
                        .setDisplay(display)
                        .setVersion(version)
                        .setSystem(system));
        var valueSet = new ValueSet().setExpansion(expansion);
        var expansionCode =
                ValueSets.getCodesInExpansion(fhirContextR4, expansion).get(0);
        assertEquals(code, expansionCode.getCode());
        assertEquals(display, expansionCode.getDisplay());
        assertEquals(version, expansionCode.getVersion());
        assertEquals(system, expansionCode.getSystem());
        var actualCode = ValueSets.getCodesInExpansion(fhirContextR4, valueSet).get(0);
        assertEquals(code, actualCode.getCode());
        assertEquals(display, actualCode.getDisplay());
        assertEquals(version, actualCode.getVersion());
        assertEquals(system, actualCode.getSystem());
    }

    @Test
    void testAddCodeToExpansion() {
        var code = new Code();
        code.setCode("test");
        code.setDisplay("Test");
        code.setSystem("www.test.com");
        code.setVersion("1.0.0");
        var expansion = new ValueSet.ValueSetExpansionComponent();
        try {
            ValueSets.addCodeToExpansion(fhirContextR4, expansion, code);
        } catch (InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        var codes = ValueSets.getCodesInExpansion(fhirContextR4, expansion);
        assertEquals(code.getCode(), codes.get(0).getCode());
        assertEquals(code.getDisplay(), codes.get(0).getDisplay());
        assertEquals(code.getSystem(), codes.get(0).getSystem());
        assertEquals(code.getVersion(), codes.get(0).getVersion());
    }
}
