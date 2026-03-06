package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r5.model.PlanDefinition;
import org.hl7.fhir.r5.model.StringType;
import org.junit.jupiter.api.Test;

class ParametersAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var planDefinition = new PlanDefinition();
        assertThrows(IllegalArgumentException.class, () -> new ParametersAdapter(planDefinition));
    }

    @Test
    void testGetParameters() {
        var parameters = new Parameters();
        parameters.addParameter("param1", "value1");
        parameters.addParameter("param2", true);
        parameters.addParameter("param3", 1);
        var adapter = adapterFactory.createParameters(parameters);
        assertNotNull(adapter);
        assertEquals(parameters, adapter.get());
        assertTrue(adapter.hasParameter());
        assertEquals(3, adapter.getParameter().size());
        assertTrue(adapter.hasParameter("param1"));
        var param1Values = adapter.getParameterValues("param1");
        assertEquals(1, param1Values.size());
        var param1Value = param1Values.get(0);
        assertInstanceOf(StringType.class, param1Value);
        assertEquals("value1", param1Value.toString());
        var param2Adapter = adapter.getParameter("param2");
        assertNotNull(param2Adapter);
        assertTrue(((BooleanType) param2Adapter.getValue()).booleanValue());
    }

    @Test
    void testAdd_SetParameters() {
        var parameters = new Parameters();
        var adapter = adapterFactory.createParameters(parameters);
        assertNotNull(adapter);
        assertEquals(parameters, adapter.get());
        assertFalse(adapter.hasParameter());
        adapter.addParameter("param1", "value1");
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.addParameter("param2", new org.hl7.fhir.r4.model.IntegerType(1)));
        adapter.addParameter("param2", new IntegerType(1));
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.addParameter("param3", new org.hl7.fhir.r4.model.Parameters()));
        adapter.addParameter("param3", new Parameters());
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.addParameter(
                        "param4", new org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent()));
        adapter.addParameter(new ParametersParameterComponent("param4").setValue(new BooleanType(true)));
        ((ParametersParameterComponent) adapter.addParameter())
                .setName("param5")
                .setValue(new StringType("value5"));
        assertTrue(adapter.hasParameter("param5"));
        assertEquals("value5", adapter.getParameter("param5").getPrimitiveValue());
        adapter.setParameter("param6", 100);
        assertEquals(6, parameters.getParameter().size());
        adapter.setParameter(null);
        assertFalse(adapter.hasParameter());
        adapter.setParameter(List.of(new ParametersParameterComponent("param1").setValue(new StringType("value1"))));
        assertEquals("value1", adapter.getParameter("param1").getPrimitiveValue());
    }
}
