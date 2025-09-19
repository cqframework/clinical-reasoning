package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Tuple;
import org.junit.jupiter.api.Test;

public class TupleAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var library = new Library();
        assertThrows(IllegalArgumentException.class, () -> adapterFactory.createTuple(library));
    }

    @Test
    void test_properties() {
        var stringType = new StringType("testString");
        var codeType = new CodeType("testCode");
        var tuple = new Tuple();
        tuple.addProperty("1", List.of(stringType));
        tuple.addProperty("2", List.of(codeType));
        var adapter = adapterFactory.createTuple(tuple);
        assertEquals(2, adapter.getProperties().size());
        var property1 = ((List<?>) adapter.getProperties().get("1")).get(0);
        assertInstanceOf(StringType.class, property1);
        assertEquals(stringType, property1);
        var property2 = ((List<?>) adapter.getProperties().get("2")).get(0);
        assertInstanceOf(CodeType.class, property2);
        assertEquals(codeType, property2);
    }
}
