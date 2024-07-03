package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.model.primitive.IdDt;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.Test;

public class ResourceAdapterTest {
    @Test
    void invalid_object_fails() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceAdapter(null));
        assertThrows(IllegalArgumentException.class, () -> new ResourceAdapter(new org.hl7.fhir.r5.model.Library()));
    }

    @Test
    void adapter_get_and_set_property() {
        var resource = new Patient();
        var id = new IdDt("patient-1");
        resource.setId(id);
        var adapter = new ResourceAdapter(resource);
        assertEquals(id.getValue(), ((IIdType) adapter.getSingleProperty("id")).getValue());
        var newId = new IdType("patient-2");
        adapter.setProperty("id", newId);
        assertEquals(newId, resource.getIdElement());
        assertEquals("id", adapter.getTypesForProperty("id")[0]);
        assertNotNull(adapter.makeProperty("language"));
        var meta = (Meta) adapter.addChild("meta");
        var date = new Date();
        meta.setLastUpdated(date);
        assertEquals(date, ((Meta) adapter.getSingleProperty("meta")).getLastUpdated());
    }

    @Test
    void adapter_copy() {
        var resource = new Patient();
        resource.setId("patient-1");
        resource.setMeta(new Meta().setLastUpdated(new Date()));
        var adapter = new ResourceAdapter(resource);
        var copy = (Patient) adapter.copy();
        assertTrue(adapter.equalsDeep(copy));
        var newDate = new Date();
        newDate.setTime(100);
        copy.setMeta(new Meta().setLastUpdated(newDate));
        assertFalse(adapter.equalsDeep(copy));
        assertTrue(adapter.equalsShallow(copy));
        copy.setId("patient-2");
        assertFalse(adapter.equalsShallow(copy));
        resource.setLanguage("FR");
        adapter.copyValues(copy);
        assertEquals("FR", copy.getLanguage());
    }

    @Test
    void adapter_get_and_set_extension() {
        var resource = new Patient();
        var extensionList = List.of(new Extension().setUrl("test-extension-url").setValue(new BooleanType(true)));
        var adapter = new ResourceAdapter(resource);
        adapter.setExtension(extensionList);
        assertEquals(extensionList, resource.getExtension());
        assertEquals(extensionList, adapter.getExtension());
        assertTrue(adapter.hasExtension("test-extension-url"));
    }

    @Test
    void adapter_get_contained() {
        var resource = new PlanDefinition();
        resource.addContained(new Library());
        var adapter = new ResourceAdapter(resource);
        assertTrue(adapter.hasContained());
    }
}
