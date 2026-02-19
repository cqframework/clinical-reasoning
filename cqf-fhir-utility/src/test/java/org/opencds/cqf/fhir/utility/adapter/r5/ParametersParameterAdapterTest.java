package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r5.model.Quantity;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.Tuple;
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
        assertNotNull(adapterFactory.createBase(parameters));
        assertNotNull(adapter);
        assertEquals(parameters, adapter.get());
        assertEquals(FhirVersionEnum.R5, adapter.fhirContext().getVersion().getVersion());
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
        var stringPart = parameter.addPart().setName("name").setValue(new StringType("patient"));
        var reference = new Reference("value");
        var referencePart = parameter.addPart().setName("content").setValue(reference);
        var patient = new Patient().addName(new HumanName().addGiven("test").setFamily("test"));
        patient.setId("patient1");
        var resourcePart = parameter.addPart().setName("content").setResource(patient);
        var adapter = adapterFactory.createParametersParameter(parameter);
        assertTrue(adapter.hasPart());
        assertEquals(stringPart, adapter.getPart().get(0).get());
        assertEquals("patient", adapter.getPartValues("name").get(0).toString());
        assertEquals(referencePart, adapter.getPart().get(1).get());
        assertEquals(resourcePart, adapter.getPart().get(2).get());
        var contentValue = adapter.getPartValues("content");
        assertEquals(2, contentValue.size());
        assertTrue(contentValue.contains(patient));
        assertTrue(contentValue.contains(reference));
        adapter.addPart();
        assertEquals(4, adapter.getPart().size());
        adapter.setPart(null);
        assertFalse(adapter.hasPart());
        assertFalse(adapter.hasPart("test"));
        adapter.setPart(
                List.of(new ParametersParameterComponent().setName("test").setValue(new IntegerType(1))));
        assertTrue(adapter.hasPart("test"));
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

    @Test
    void testNewTupleWithParts() {
        var stringType = new StringType("test");
        var booleanType = new BooleanType(true);
        var parameter = new ParametersParameterComponent().setName("param");
        parameter.addPart().setName("part1").setValue(stringType);
        parameter.addPart().setName("part2").setValue(booleanType);
        var adapter = adapterFactory.createParametersParameter(parameter);
        assertTrue(adapter.hasPart());
        var tuple = (Tuple) adapter.newTupleWithParts();
        assertEquals(2, tuple.children().size());
        var part1Value = tuple.listChildrenByName("part1").get(0);
        assertEquals(stringType, part1Value);
        var part2Value = tuple.listChildrenByName("part2").get(0);
        assertEquals(booleanType, part2Value);
    }
}
