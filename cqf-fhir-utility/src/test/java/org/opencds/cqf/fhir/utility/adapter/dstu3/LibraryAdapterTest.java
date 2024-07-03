package org.opencds.cqf.fhir.utility.adapter.dstu3;

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
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.UsageContext;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.visitor.PackageVisitor;

public class LibraryAdapterTest {
    private final FhirContext fhirContext = FhirContext.forDstu3Cached();

    @Test
    void invalid_object_fails() {
        assertThrows(IllegalArgumentException.class, () -> new LibraryAdapter(new PlanDefinition()));
    }

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new PackageVisitor(fhirContext));
        doReturn(new Bundle()).when(spyVisitor).visit(any(LibraryAdapter.class), any(), any());
        IDomainResource library = new Library();
        var adapter = new LibraryAdapter(library);
        adapter.accept(spyVisitor, null, null);
        verify(spyVisitor, times(1)).visit(any(LibraryAdapter.class), any(), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var library = new Library();
        var name = "name";
        library.setName(name);
        var adapter = new LibraryAdapter(library);
        assertEquals(name, adapter.getName());
        var newName = "name2";
        adapter.setName(newName);
        assertEquals(newName, library.getName());
    }

    @Test
    void adapter_get_and_set_url() {
        var library = new Library();
        var url = "www.url.com";
        library.setUrl(url);
        var adapter = new LibraryAdapter(library);
        assertEquals(url, adapter.getUrl());
        var newUrl = "www.url2.com";
        adapter.setUrl(newUrl);
        assertEquals(newUrl, library.getUrl());
    }

    @Test
    void adapter_get_and_set_version() {
        var library = new Library();
        var version = "1.0.0";
        library.setVersion(version);
        var adapter = new LibraryAdapter(library);
        assertTrue(adapter.hasVersion());
        assertEquals(version, adapter.getVersion());
        var newVersion = "1.0.1";
        adapter.setVersion(newVersion);
        assertEquals(newVersion, library.getVersion());
    }

    @Test
    void adapter_get_and_set_status() {
        var library = new Library();
        var status = PublicationStatus.DRAFT;
        library.setStatus(status);
        var adapter = new LibraryAdapter(library);
        assertEquals(status.toCode(), adapter.getStatus());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setStatus("invalid-status"));
        var newStatus = PublicationStatus.ACTIVE;
        adapter.setStatus(newStatus.toCode());
        assertEquals(newStatus, PublicationStatus.fromCode(adapter.getStatus()));
    }

    @Test
    void adapter_get_and_set_dates() {
        var library = new Library();
        var date = new Date();
        var approvalDate = new Date();
        var effectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2020-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2020-12-31")));
        library.setDate(date);
        library.setApprovalDate(approvalDate);
        library.setEffectivePeriod(effectivePeriod);
        var adapter = new LibraryAdapter(library);
        assertEquals(date, adapter.getDate());
        assertEquals(approvalDate, adapter.getApprovalDate());
        assertEquals(effectivePeriod, adapter.getEffectivePeriod());
        var newDate = new Date();
        newDate.setTime(100);
        adapter.setDate(newDate);
        assertEquals(newDate, library.getDate());
        var newApprovalDate = new Date();
        newApprovalDate.setTime(100);
        adapter.setApprovalDate(newApprovalDate);
        assertEquals(newApprovalDate, library.getApprovalDate());
        var newEffectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2021-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2021-12-31")));
        adapter.setEffectivePeriod(newEffectivePeriod);
        assertEquals(newEffectivePeriod, adapter.getEffectivePeriod());
    }

    @Test
    void adapter_get_experimental() {
        var library = new Library();
        var experimental = true;
        library.setExperimental(experimental);
        var adapter = new LibraryAdapter(library);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_set_relatedArtifact() {
        var library = new Library();
        var relatedArtifactList = List.of(new RelatedArtifact());
        var adapter = new LibraryAdapter(library);
        adapter.setRelatedArtifact(relatedArtifactList);
        assertEquals(relatedArtifactList, library.getRelatedArtifact());
        assertEquals(relatedArtifactList, adapter.getRelatedArtifact());
    }

    @Test
    void adapter_copy() {
        var library = new Library().setStatus(PublicationStatus.DRAFT);
        library.setId("library-1");
        var adapter = new LibraryAdapter(library);
        var copy = adapter.copy();
        var adapterCopy = new LibraryAdapter(copy);
        adapterCopy.setId(new IdDt("Library", "library-2"));
        assertNotEquals(library.getId(), copy.getId());
        library.setStatus(PublicationStatus.ACTIVE);
        assertNotEquals(adapter.getStatus(), copy.getStatus());
    }

    @Test
    void adapter_get_all_dependencies() {}

    @Test
    void adapter_get_and_set_type() {
        var type = new CodeableConcept(new Coding("www.test.com", "test", "Test"));
        var library = new Library().setType(type);
        var adapter = new LibraryAdapter(library);
        assertEquals(type, adapter.getType());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setType("test"));
        var newType = new CodeableConcept(new Coding("http://hl7.org/fhir/ValueSet/library-type", "logic-library", ""));
        adapter.setType(newType.getCoding().get(0).getCode());
        assertEquals(
                newType.getCoding().get(0).getCode(),
                ((CodeableConcept) adapter.getType()).getCoding().get(0).getCode());
        assertEquals(
                newType.getCoding().get(0).getSystem(),
                ((CodeableConcept) adapter.getType()).getCoding().get(0).getSystem());
    }

    @Test
    void adapter_get_and_set_dataRequirement() {
        var library = new Library();
        var adapter = new LibraryAdapter(library);
        var dataRequirements = new ArrayList<DataRequirement>();
        dataRequirements.add(new DataRequirement().setType("Patient"));
        adapter.setDataRequirement(dataRequirements);
        assertEquals(dataRequirements, library.getDataRequirement());
        adapter.addDataRequirement(new DataRequirement().setType("Observation"));
        assertEquals(library.getDataRequirement(), adapter.getDataRequirement());
        assertEquals(2, adapter.getDataRequirement().size());
    }

    @Test
    void adapter_get_useContext() {
        var library = new Library();
        var useContext = new UsageContext().setCode(new Coding("www.test.com", "test", "Test"));
        library.setUseContext(Collections.singletonList(useContext));
        var adapter = new LibraryAdapter(library);
        assertEquals(useContext, adapter.getUseContext().get(0));
    }
}
