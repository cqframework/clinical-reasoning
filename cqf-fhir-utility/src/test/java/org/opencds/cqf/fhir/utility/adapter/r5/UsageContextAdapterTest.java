package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.UsageContext;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;

class UsageContextAdapterTest {
    private final IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void constructorThrowsWhenNotUsageContext() {
        // Arrange: pass a CodeableConcept (not a UsageContext)
        CodeableConcept cc = new CodeableConcept();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> adapterFactory.createUsageContext(cc));
    }

    @Test
    void getReturnsUnderlyingUsageContext() {
        UsageContext uc = new UsageContext();
        uc.setCode(new Coding().setCode("focus"));

        UsageContextAdapter adapter = new UsageContextAdapter(uc);
        assertSame(uc, adapter.get(), "get() should return the underlying UsageContext");
    }

    @Test
    void hasCodeAndGetCodeWork() {
        UsageContext uc = new UsageContext();
        assertFalse(uc.hasCode());

        uc.setCode(new Coding().setCode("priority"));
        UsageContextAdapter adapter = new UsageContextAdapter((IBase) uc);

        assertTrue(adapter.hasCode(), "adapter should report having a code");
        ICodingAdapter codeAdapter = adapter.getCode();
        assertNotNull(codeAdapter, "getCode() should not return null when code exists");

        // Expect the CodingAdapter.get() to return the actual Coding with the set code
        assertEquals("priority", codeAdapter.getCode());
    }

    @Test
    void setCodeSetsUnderlyingCoding() {
        UsageContext uc = new UsageContext();
        UsageContextAdapter adapter = new UsageContextAdapter((IBase) uc);

        // Create a Coding instance and wrap in CodingAdapter (class present in your codebase)
        Coding newCoding = new Coding().setSystem("http://example").setCode("routine");
        ICodingAdapter codingAdapter = new CodingAdapter((IBase) newCoding);

        // set code via the adapter
        adapter.setCode(codingAdapter);

        // underlying UsageContext should now have that coding
        assertTrue(uc.hasCode());
        assertEquals("routine", uc.getCode().getCode());
        assertEquals("http://example", uc.getCode().getSystem());
    }

    @Test
    void valueCodeableConceptAccessorsWork() {
        UsageContext uc = new UsageContext();

        // initially no value
        UsageContextAdapter adapter = new UsageContextAdapter((IBase) uc);
        assertFalse(adapter.hasValue());
        assertFalse(adapter.hasValueCodeableConcept());
        assertNull(adapter.getValueCodeableConcept());

        // set a CodeableConcept as the value
        CodeableConcept cc = new CodeableConcept();
        cc.addCoding(new Coding().setSystem("http://sys").setCode("C1"));
        uc.setValue(cc);

        // create adapter again (or reuse)
        UsageContextAdapter adapterWithValue = new UsageContextAdapter((IBase) uc);

        assertTrue(adapterWithValue.hasValue());
        assertTrue(adapterWithValue.hasValueCodeableConcept());

        ICodeableConceptAdapter cca = adapterWithValue.getValueCodeableConcept();
        assertNotNull(cca);
        assertTrue(cca.hasCoding());
        assertEquals("C1", cca.getCoding().get(0).getCode());
    }

    @Test
    void equalsDeepReturnsTrueForEquivalentAdapters() {
        // build two UsageContext instances with same structure
        UsageContext uc1 = new UsageContext();
        uc1.setCode(new Coding().setCode("focus"));
        uc1.setValue(new CodeableConcept().addCoding(new Coding().setSystem("s").setCode("c")));

        UsageContext uc2 = new UsageContext();
        uc2.setCode(new Coding().setCode("focus"));
        uc2.setValue(new CodeableConcept().addCoding(new Coding().setSystem("s").setCode("c")));

        UsageContextAdapter a1 = new UsageContextAdapter((IBase) uc1);
        UsageContextAdapter a2 = new UsageContextAdapter((IBase) uc2);

        // equalsDeep should delegate to underlying equalsDeep => should be true
        assertTrue(a1.equalsDeep(a2));
        assertTrue(a2.equalsDeep(a1));
    }

    @Test
    void equalsDeepReturnsFalseForDifferentAdapters() {
        UsageContext uc1 = new UsageContext();
        uc1.setCode(new Coding().setCode("focus"));
        uc1.setValue(new CodeableConcept().addCoding(new Coding().setCode("c1")));

        UsageContext uc2 = new UsageContext();
        uc2.setCode(new Coding().setCode("priority"));
        uc2.setValue(new CodeableConcept().addCoding(new Coding().setCode("c2")));

        UsageContextAdapter a1 = new UsageContextAdapter((IBase) uc1);
        UsageContextAdapter a2 = new UsageContextAdapter((IBase) uc2);

        assertFalse(a1.equalsDeep(a2));
    }
}
