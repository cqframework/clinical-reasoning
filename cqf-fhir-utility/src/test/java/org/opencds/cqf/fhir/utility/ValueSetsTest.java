package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import java.lang.reflect.InvocationTargetException;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.runtime.Code;

class ValueSetsTest {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

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
