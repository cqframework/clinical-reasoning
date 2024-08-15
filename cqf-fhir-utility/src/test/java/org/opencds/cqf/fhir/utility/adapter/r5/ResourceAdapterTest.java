package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.model.primitive.IdDt;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Meta;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.PlanDefinition;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.Adapter;
import org.slf4j.LoggerFactory;

class ResourceAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.AdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceAdapter(null));
        assertThrows(IllegalArgumentException.class, () -> new ResourceAdapter(new org.hl7.fhir.r4.model.Library()));
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
        var logger = (Logger) LoggerFactory.getLogger(Adapter.class);
        var listAppender = new ListAppender<ILoggingEvent>();
        listAppender.start();
        logger.addAppender(listAppender);
        var bundle = new Bundle();
        var bundleAdapter = adapterFactory.createResource(bundle);
        assertFalse(bundleAdapter.hasExtension());
        bundleAdapter.setExtension(List.of(new Extension()));
        assertEquals(1, listAppender.list.size());
        assertEquals(Level.DEBUG, listAppender.list.get(0).getLevel());
        bundleAdapter.addExtension(new Extension());
        assertEquals(2, listAppender.list.size());
        assertEquals(Level.DEBUG, listAppender.list.get(1).getLevel());
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
