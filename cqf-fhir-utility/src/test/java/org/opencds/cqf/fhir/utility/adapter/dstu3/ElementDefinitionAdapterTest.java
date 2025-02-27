package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.ElementDefinition.TypeRefComponent;
import org.junit.jupiter.api.Test;

class ElementDefinitionAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var codeableConcept = new CodeableConcept();
        assertThrows(IllegalArgumentException.class, () -> new ElementDefinitionAdapter(codeableConcept));
    }

    @SuppressWarnings("squid:S5961")
    @Test
    void test() {
        var element = exampleDefinition();
        var adapter = adapterFactory.createElementDefinition(element);
        assertNotNull(adapter);
        assertEquals(element, adapter.get());
        assertEquals(FhirVersionEnum.DSTU3, adapter.fhirContext().getVersion().getVersion());
        assertNotNull(adapter.getModelResolver());

        assertEquals(element.getId(), adapter.getId());
        assertEquals(element.getPath(), adapter.getPath());
        assertEquals(element.getSliceName(), adapter.getSliceName());
        assertEquals(element.hasSlicing(), adapter.hasSlicing());
        assertEquals(element.getLabel(), adapter.getLabel());
        assertEquals(element.hasLabel(), adapter.hasLabel());
        assertEquals(element.getCode().size(), adapter.getCode().size());
        assertEquals(element.getShort(), adapter.getShort());
        assertEquals(element.hasShort(), adapter.hasShort());
        assertEquals(element.getDefinition(), adapter.getDefinition());
        assertEquals(element.getComment(), adapter.getComment());
        assertEquals(element.getRequirements(), adapter.getRequirements());
        assertEquals(element.getAlias(), adapter.getAlias());
        assertEquals(element.getMin(), adapter.getMin());
        assertEquals(element.hasMin(), adapter.hasMin());
        assertTrue(adapter.isRequired());
        assertEquals(element.getMax(), adapter.getMax());
        assertEquals(element.hasMax(), adapter.hasMax());
        assertEquals(element.getType().size(), adapter.getType().size());
        assertEquals(element.getTypeFirstRep().getCode(), adapter.getTypeCode());
        assertTrue(StringUtils.isBlank(adapter.getTypeProfile()));
        assertEquals(element.getDefaultValue(), adapter.getDefaultValue());
        assertEquals(element.hasDefaultValue(), adapter.hasDefaultValue());
        assertEquals(element.getFixed(), adapter.getFixed());
        assertEquals(element.hasFixed(), adapter.hasFixed());
        assertEquals(element.getPattern(), adapter.getPattern());
        assertEquals(element.hasPattern(), adapter.hasPattern());
        assertNull(adapter.getFixedOrPattern());
        assertFalse(adapter.hasFixedOrPattern());
        assertNull(adapter.getDefaultOrFixedOrPattern());
        assertFalse(adapter.hasDefaultOrFixedOrPattern());
        assertEquals(element.getMustSupport(), adapter.getMustSupport());
        assertEquals(element.getBinding(), adapter.getBinding());
        assertEquals(element.hasBinding(), adapter.hasBinding());
        assertNull(adapter.getBindingValueSet());
    }

    private ElementDefinition exampleDefinition() {
        var element = new ElementDefinition()
                .setPath("Observation.value[x]")
                .setSliceName("valueQuantity")
                .setShort("Patient Height in centimeters")
                .setDefinition("Patient Height in centimeters")
                .setMin(1)
                .setMax("1")
                .addType(new TypeRefComponent().setCode("Quantity"))
                .setMustSupport(true);
        element.setId("Observation.value[x]:valueQuantity");

        return element;
    }
}
