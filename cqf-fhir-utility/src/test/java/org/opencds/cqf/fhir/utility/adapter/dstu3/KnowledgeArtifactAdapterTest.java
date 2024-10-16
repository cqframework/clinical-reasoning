package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CompartmentDefinition;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.visitor.PackageVisitor;

class KnowledgeArtifactAdapterTest {
    private final FhirContext fhirContext = FhirContext.forDstu3Cached();
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KnowledgeArtifactAdapter(new org.hl7.fhir.r4.model.Library()));
    }

    @Test
    void test() {
        var resource = new CompartmentDefinition();
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(resource);
        assertNotNull(adapter);
    }

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new PackageVisitor(fhirContext));
        doReturn(new Bundle()).when(spyVisitor).visit(any(KnowledgeArtifactAdapter.class), any(), any());
        IDomainResource def = new CompartmentDefinition();
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(def);
        adapter.accept(spyVisitor, null, null);
        verify(spyVisitor, times(1)).visit(any(KnowledgeArtifactAdapter.class), any(), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var def = new CompartmentDefinition();
        var name = "name";
        def.setName(name);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(def);
        assertEquals(name, adapter.getName());
        var newName = "name2";
        adapter.setName(newName);
        assertEquals(newName, def.getName());
    }

    @Test
    void adapter_get_and_set_url() {
        var def = new CompartmentDefinition();
        var url = "www.url.com";
        def.setUrl(url);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(def);
        assertEquals(url, adapter.getUrl());
        var newUrl = "www.url2.com";
        adapter.setUrl(newUrl);
        assertEquals(newUrl, def.getUrl());
    }

    @Test
    void adapter_get_and_set_version() {
        var def = new CompartmentDefinition();
        var version = "1.0.0";
        def.setVersion(version);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(def);
        assertTrue(adapter.hasVersion());
        assertEquals(version, adapter.getVersion());
        var newVersion = "1.0.1";
        adapter.setVersion(newVersion);
        assertEquals(newVersion, def.getVersion());
    }

    @Test
    void adapter_get_and_set_status() {
        var def = new CompartmentDefinition();
        var status = PublicationStatus.DRAFT;
        def.setStatus(status);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(def);
        assertEquals(status.toCode(), adapter.getStatus());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setStatus("invalid-status"));
        var newStatus = PublicationStatus.ACTIVE;
        adapter.setStatus(newStatus.toCode());
        assertEquals(newStatus, PublicationStatus.fromCode(adapter.getStatus()));
    }

    @Test
    void adapter_get_and_set_dates() {
        var def = new CompartmentDefinition();
        var date = new Date();
        def.setDate(date);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(def);
        assertEquals(date, adapter.getDate());
        var newDate = new Date();
        newDate.setTime(100);
        adapter.setDate(newDate);
        assertEquals(newDate, def.getDate());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setDateElement(new DateType()));
        var newDateElement = new DateTimeType().setValue(new Date());
        adapter.setDateElement(newDateElement);
        assertEquals(newDateElement, def.getDateElement());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setEffectivePeriod(new Extension()));
    }

    @Test
    void adapter_get_experimental() {
        var def = new CompartmentDefinition();
        var experimental = true;
        def.setExperimental(experimental);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(def);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_set_relatedArtifact() {
        var def = new CompartmentDefinition();
        var relatedArtifactList = List.of(new RelatedArtifact());
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(def);
        adapter.setRelatedArtifact(relatedArtifactList);
        assertEquals(0, adapter.getRelatedArtifact().size());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setRelatedArtifact(Arrays.asList(new Period())));
        assertThrows(UnprocessableEntityException.class, () -> adapter.getRelatedArtifactsOfType("depends"));
    }

    @Test
    void adapter_copy() {
        var def = new CompartmentDefinition().setStatus(PublicationStatus.DRAFT);
        def.setId("def-1");
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(def);
        var copy = (CompartmentDefinition) adapter.copy();
        copy.setId("def-2");
        assertNotEquals(def.getId(), copy.getId());
        def.setStatus(PublicationStatus.ACTIVE);
        assertNotEquals(adapter.getStatus(), copy.getStatus());
    }
}
