package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.model.primitive.IdDt;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.slf4j.event.Level;

class ResourceAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();
    private static final TestLogger logger = TestLoggerFactory.getTestLogger(IAdapter.class);

    @BeforeEach
    void setUp() {
        logger.clearAll();
    }

    @Test
    void invalid_object_fails() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceAdapter(null));
        var library = new org.hl7.fhir.r5.model.Library();
        assertThrows(IllegalArgumentException.class, () -> new ResourceAdapter(library));
    }

    @Test
    void adapter_get_and_set_property() {
        var resource = new Patient();
        var id = new IdDt("patient-1");
        resource.setId(id);
        var adapter = adapterFactory.createResource(resource);
        assertTrue(((ResourceAdapter) adapter).isDomainResource());
        assertEquals(resource, ((ResourceAdapter) adapter).getDomainResource().get());
        assertEquals(id.getValue(), ((IIdType) adapter.getSingleProperty("id")).getValue());
        var newId = new IdType("patient-2");
        adapter.setProperty("id", newId);
        assertEquals(newId, resource.getIdElement());
        assertEquals("id", adapter.getTypesForProperty("id")[0]);
        assertNotNull(adapter.makeProperty("language"));
        assertNull(adapter.getSingleProperty("meta"));
        var meta = (Meta) adapter.addChild("meta");
        var date = new Date();
        meta.setLastUpdated(date);
        assertEquals(date, ((Meta) adapter.getProperty("meta")[0]).getLastUpdated());
        resource.addName(new HumanName().addGiven("name1"));
        resource.addName(new HumanName().addGiven("name2"));
        assertThrows(IllegalArgumentException.class, () -> adapter.getSingleProperty("name"));
    }

    @Test
    void adapter_copy() {
        var resource = new Patient();
        resource.setId("patient-1");
        resource.setMeta(new Meta().setLastUpdated(new Date()));
        var adapter = adapterFactory.createResource(resource);
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
        var bundle = new Bundle();
        var bundleAdapter = new ResourceAdapter(bundle);
        assertFalse(bundleAdapter.hasExtension());
        bundleAdapter.setExtension(List.of(new Extension()));
        assertEquals(1, logger.getLoggingEvents().size());
        assertEquals(Level.DEBUG, logger.getLoggingEvents().get(0).getLevel());
        bundleAdapter.addExtension(new Extension());
        assertEquals(2, logger.getLoggingEvents().size());
        assertEquals(Level.DEBUG, logger.getLoggingEvents().get(1).getLevel());
        var resource = new Patient();
        var extensionList = List.of(new Extension().setUrl("test-extension-url").setValue(new BooleanType(true)));
        var adapter = adapterFactory.createResource(resource);
        adapter.setExtension(extensionList);
        assertTrue(adapter.hasExtension());
        assertEquals(extensionList, resource.getExtension());
        assertEquals(extensionList, adapter.getExtension());
        assertTrue(adapter.hasExtension("test-extension-url"));
    }

    @Test
    void adapter_get_contained() {
        var resource = new PlanDefinition();
        resource.addContained(new Library());
        var adapter = adapterFactory.createResource(resource);
        assertTrue(adapter.hasContained());
        assertNotNull(adapter.getContained());
        assertFalse(adapter.hasContained(adapter.getContained().get(0)));
    }
}
