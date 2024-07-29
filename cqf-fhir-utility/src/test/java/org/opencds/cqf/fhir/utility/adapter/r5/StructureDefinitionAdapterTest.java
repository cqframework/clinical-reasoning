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
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.visitor.PackageVisitor;

public class StructureDefinitionAdapterTest {
    private final FhirContext fhirContext = FhirContext.forR5Cached();

    @Test
    void invalid_object_fails() {
        assertThrows(IllegalArgumentException.class, () -> new StructureDefinitionAdapter(new Library()));
    }

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new PackageVisitor(fhirContext));
        doReturn(new Bundle()).when(spyVisitor).visit(any(StructureDefinitionAdapter.class), any(), any());
        IDomainResource structureDef = new StructureDefinition();
        var adapter = new StructureDefinitionAdapter(structureDef);
        assertEquals(structureDef, adapter.get());
        adapter.accept(spyVisitor, null, null);
        verify(spyVisitor, times(1)).visit(any(StructureDefinitionAdapter.class), any(), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var structureDef = new StructureDefinition();
        var name = "name";
        structureDef.setName(name);
        var adapter = new StructureDefinitionAdapter(structureDef);
        assertEquals(name, adapter.getName());
        var newName = "name2";
        adapter.setName(newName);
        assertEquals(newName, structureDef.getName());
    }

    @Test
    void adapter_get_and_set_url() {
        var structureDef = new StructureDefinition();
        var url = "www.url.com";
        structureDef.setUrl(url);
        var adapter = new StructureDefinitionAdapter(structureDef);
        assertTrue(adapter.hasUrl());
        assertEquals(url, adapter.getUrl());
        var newUrl = "www.url2.com";
        adapter.setUrl(newUrl);
        assertTrue(adapter.hasUrl());
        assertEquals(newUrl, structureDef.getUrl());
    }

    @Test
    void adapter_get_and_set_version() {
        var structureDef = new StructureDefinition();
        var version = "1.0.0";
        structureDef.setVersion(version);
        var adapter = new StructureDefinitionAdapter(structureDef);
        assertTrue(adapter.hasVersion());
        assertEquals(version, adapter.getVersion());
        var newVersion = "1.0.1";
        adapter.setVersion(newVersion);
        assertEquals(newVersion, structureDef.getVersion());
    }

    @Test
    void adapter_get_and_set_status() {
        var structureDef = new StructureDefinition();
        var status = PublicationStatus.DRAFT;
        structureDef.setStatus(status);
        var adapter = new StructureDefinitionAdapter(structureDef);
        assertEquals(status.toCode(), adapter.getStatus());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setStatus("invalid-status"));
        var newStatus = PublicationStatus.ACTIVE;
        adapter.setStatus(newStatus.toCode());
        assertEquals(newStatus, PublicationStatus.fromCode(adapter.getStatus()));
    }

    @Test
    void adapter_get_and_set_dates() {
        // StructureDefinition does not have fields approvalDate and effectivePeriod
        var structureDef = new StructureDefinition();
        var date = new Date();
        var effectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2020-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2020-12-31")));
        structureDef.setDate(date);
        var adapter = new StructureDefinitionAdapter(structureDef);
        assertEquals(date, adapter.getDate());
        assertEquals(null, adapter.getApprovalDate());
        assertNotEquals(effectivePeriod, adapter.getEffectivePeriod());
        var newDate = new Date();
        newDate.setTime(100);
        adapter.setDate(newDate);
        assertEquals(newDate, structureDef.getDate());
        var newApprovalDate = new Date();
        newApprovalDate.setTime(100);
        adapter.setApprovalDate(newApprovalDate);
        assertEquals(null, adapter.getApprovalDate());
        var newEffectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2021-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2021-12-31")));
        adapter.setEffectivePeriod(newEffectivePeriod);
        assertNotEquals(newEffectivePeriod, adapter.getEffectivePeriod());
    }

    @Test
    void adapter_get_experimental() {
        var structureDef = new StructureDefinition();
        var experimental = true;
        structureDef.setExperimental(experimental);
        var adapter = new StructureDefinitionAdapter(structureDef);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_set_relatedArtifact() {
        var structureDef = new StructureDefinition();
        var relatedArtifactList = List.of(new RelatedArtifact());
        var adapter = new StructureDefinitionAdapter(structureDef);
        adapter.setRelatedArtifact(relatedArtifactList);
        assertEquals(0, adapter.getRelatedArtifact().size());
    }

    @Test
    void adapter_copy() {
        var structureDef = new StructureDefinition().setStatus(PublicationStatus.DRAFT);
        structureDef.setId("plan-1");
        var adapter = new StructureDefinitionAdapter(structureDef);
        var copy = adapter.copy();
        copy.setId("plan-2");
        assertNotEquals(structureDef.getId(), copy.getId());
        structureDef.setStatus(PublicationStatus.ACTIVE);
        assertNotEquals(adapter.getStatus(), copy.getStatus());
    }

    @Test
    void adapter_get_all_dependencies() {
        var dependencies = List.of(
                "profileRef",
                "baseDefinition",
                "cpgAssertionRef",
                "cpgFeatureRef",
                "cpgInferenceRef",
                "elementProfileRef",
                "elementTargetProfileRef",
                "elementValueSetBindingRef");
        var structureDef = new StructureDefinition();
        structureDef.getMeta().addProfile(dependencies.get(0));
        structureDef.setBaseDefinition(dependencies.get(1));
        structureDef.addExtension(
                Constants.CPG_ASSERTION_EXPRESSION, new Expression().setReference(dependencies.get(2)));
        structureDef.addExtension(Constants.CPG_FEATURE_EXPRESSION, new Expression().setReference(dependencies.get(3)));
        structureDef.addExtension(
                Constants.CPG_INFERENCE_EXPRESSION, new Expression().setReference(dependencies.get(4)));
        structureDef.getDifferential().addElement().addType().addProfile(dependencies.get(5));
        structureDef.getDifferential().addElement().addType().addTargetProfile(dependencies.get(6));
        structureDef
                .getDifferential()
                .addElement()
                .setBinding(new ElementDefinitionBindingComponent().setValueSet(dependencies.get(7)));
        var adapter = new StructureDefinitionAdapter(structureDef);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(dependencies.size(), extractedDependencies.size());
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.indexOf(dep.getReference()) >= 0);
        });
    }
}
