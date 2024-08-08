package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.visitor.PackageVisitor;

public class ValueSetAdapterTest {
    private final FhirContext fhirContext = FhirContext.forR5Cached();
    private final org.opencds.cqf.fhir.utility.adapter.AdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        assertThrows(IllegalArgumentException.class, () -> new ValueSetAdapter(new Library()));
    }

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new PackageVisitor(fhirContext));
        doReturn(new Bundle()).when(spyVisitor).visit(any(ValueSetAdapter.class), any(), any());
        IDomainResource valueSet = new ValueSet();
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(valueSet);
        assertEquals(valueSet, adapter.get());
        adapter.accept(spyVisitor, null, null);
        verify(spyVisitor, times(1)).visit(any(ValueSetAdapter.class), any(), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var valueSet = new ValueSet();
        var name = "name";
        valueSet.setName(name);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(valueSet);
        assertEquals(name, adapter.getName());
        var newName = "name2";
        adapter.setName(newName);
        assertEquals(newName, valueSet.getName());
    }

    @Test
    void adapter_get_and_set_url() {
        var valueSet = new ValueSet();
        var url = "www.url.com";
        valueSet.setUrl(url);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(valueSet);
        assertTrue(adapter.hasUrl());
        assertEquals(url, adapter.getUrl());
        var newUrl = "www.url2.com";
        adapter.setUrl(newUrl);
        assertTrue(adapter.hasUrl());
        assertEquals(newUrl, valueSet.getUrl());
    }

    @Test
    void adapter_get_and_set_version() {
        var valueSet = new ValueSet();
        var version = "1.0.0";
        valueSet.setVersion(version);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(valueSet);
        assertTrue(adapter.hasVersion());
        assertEquals(version, adapter.getVersion());
        var newVersion = "1.0.1";
        adapter.setVersion(newVersion);
        assertEquals(newVersion, valueSet.getVersion());
    }

    @Test
    void adapter_get_and_set_status() {
        var valueSet = new ValueSet();
        var status = PublicationStatus.DRAFT;
        valueSet.setStatus(status);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(valueSet);
        assertEquals(status.toCode(), adapter.getStatus());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setStatus("invalid-status"));
        var newStatus = PublicationStatus.ACTIVE;
        adapter.setStatus(newStatus.toCode());
        assertEquals(newStatus, PublicationStatus.fromCode(adapter.getStatus()));
    }

    @Test
    void adapter_get_and_set_dates() {
        var valueSet = new ValueSet();
        var date = new Date();
        var approvalDate = new Date();
        var effectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2020-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2020-12-31")));
        valueSet.setDate(date);
        valueSet.setApprovalDate(approvalDate);
        valueSet.setEffectivePeriod(effectivePeriod);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(valueSet);
        assertEquals(date, adapter.getDate());
        assertEquals(approvalDate, adapter.getApprovalDate());
        assertEquals(effectivePeriod, adapter.getEffectivePeriod());
        var newDate = new Date();
        newDate.setTime(100);
        adapter.setDate(newDate);
        assertEquals(newDate, valueSet.getDate());
        var newApprovalDate = new Date();
        newApprovalDate.setTime(100);
        adapter.setApprovalDate(newApprovalDate);
        assertEquals(newApprovalDate, valueSet.getApprovalDate());
        var newEffectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2021-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2021-12-31")));
        adapter.setEffectivePeriod(newEffectivePeriod);
        assertEquals(newEffectivePeriod, valueSet.getEffectivePeriod());
    }

    @Test
    void adapter_get_experimental() {
        var valueSet = new ValueSet();
        var experimental = true;
        valueSet.setExperimental(experimental);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(valueSet);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_set_relatedArtifact() {
        var valueSet = new ValueSet();
        var relatedArtifactList = List.of(new RelatedArtifact());
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(valueSet);
        adapter.setRelatedArtifact(relatedArtifactList);
        assertEquals(relatedArtifactList, valueSet.getRelatedArtifact());
        assertEquals(relatedArtifactList, adapter.getRelatedArtifact());
    }

    @Test
    void adapter_copy() {
        var valueSet = new ValueSet().setStatus(PublicationStatus.DRAFT);
        valueSet.setId("valueset-1");
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(valueSet);
        var copy = (ValueSet) adapter.copy();
        copy.setId("valueset-2");
        assertNotEquals(valueSet.getId(), copy.getId());
        valueSet.setStatus(PublicationStatus.ACTIVE);
        assertNotEquals(adapter.getStatus(), copy.getStatus());
    }

    @Test
    void adapter_get_all_dependencies() {
        var dependencies = List.of("profileRef");
        var valueSet = new ValueSet();
        valueSet.getMeta().addProfile(dependencies.get(0));
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(valueSet);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(dependencies.size(), extractedDependencies.size());
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.indexOf(dep.getReference()) >= 0);
        });
    }
}
