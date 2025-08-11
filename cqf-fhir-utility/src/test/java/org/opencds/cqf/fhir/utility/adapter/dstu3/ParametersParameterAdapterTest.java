package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.dstu3.model.IntegerType;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.Test;

class ParametersParameterAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var action = new PlanDefinitionActionComponent();
        assertThrows(IllegalArgumentException.class, () -> new ParametersParameterComponentAdapter(action));
    }

    @Test
    void test() {
        var parameters = new ParametersParameterComponent();
        var adapter = adapterFactory.createParametersParameter(parameters);
        assertNotNull(adapter);
        assertEquals(parameters, adapter.get());
        assertEquals(FhirVersionEnum.DSTU3, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());
    }

    @Test
    void testGet_SetName() {
        var parameter = new ParametersParameterComponent();
        var name = "name";
        parameter.setName("name");
        var adapter = adapterFactory.createParametersParameter(parameter);
        assertNotNull(adapter);
        assertEquals(name, adapter.getName());
        var newName = "name2";
        adapter.setName(newName);
        assertEquals(newName, parameter.getName());
    }

    @Test
    void testGet_SetPart() {
        var parameter = new ParametersParameterComponent();
        var part = parameter.addPart().setName("name").setValue(new StringType("value"));
        var adapter = adapterFactory.createParametersParameter(parameter);
        assertTrue(adapter.hasPart());
        assertEquals(part, adapter.getPart().get(0).get());
        assertEquals("value", adapter.getPartValues("name").get(0).toString());
        adapter.addPart();
        assertEquals(2, adapter.getPart().size());
        adapter.setPart(null);
        assertFalse(adapter.hasPart());
        adapter.setPart(
                List.of(new ParametersParameterComponent().setName("test").setValue(new IntegerType(1))));
        assertTrue(adapter.hasPart());
    }

    @Test
    void testGet_SetResource() {
        var parameter = new ParametersParameterComponent();
        var adapter = adapterFactory.createParametersParameter(parameter);
        assertFalse(adapter.hasResource());
        var resource = new Patient().setId("test");
        adapter.setResource(resource);
        assertTrue(adapter.hasResource());
        assertEquals(resource, adapter.getResource());
    }

    @Test
    void testGet_SetValue() {
        var parameter = new ParametersParameterComponent();
        var quantity = new Quantity().setValue(10).setUnit("cm");
        parameter.setValue(quantity);
        var adapter = adapterFactory.createParametersParameter(parameter);
        assertTrue(adapter.hasValue());
        assertFalse(adapter.hasPrimitiveValue());
        assertEquals(quantity, adapter.getValue());
        adapter.setValue(new StringType("test"));
        assertTrue(adapter.hasPrimitiveValue());
        assertEquals("test", adapter.getPrimitiveValue());
    }
}
