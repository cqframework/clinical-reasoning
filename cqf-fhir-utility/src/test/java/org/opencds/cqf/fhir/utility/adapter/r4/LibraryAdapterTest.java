package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.UsageContext;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.TestVisitor;

class LibraryAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var planDefinition = new PlanDefinition();
        assertThrows(IllegalArgumentException.class, () -> new LibraryAdapter(planDefinition));
    }

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new TestVisitor());
        var library = new Library();
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(library);
        doReturn(library).when(spyVisitor).visit(any(LibraryAdapter.class), any());
        adapter.accept(spyVisitor, null);
        verify(spyVisitor, times(1)).visit(any(LibraryAdapter.class), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var library = new Library();
        var name = "name";
        library.setName(name);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(library);
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
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(library);
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
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(library);
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
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(library);
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
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(library);
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
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(library);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_set_relatedArtifact() {
        var library = new Library();
        var relatedArtifactList = List.of(new RelatedArtifact());
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(library);
        adapter.setRelatedArtifact(relatedArtifactList);
        assertEquals(relatedArtifactList, library.getRelatedArtifact());
        assertEquals(relatedArtifactList, adapter.getRelatedArtifact());
    }

    @Test
    void adapter_copy() {
        var library = new Library().setStatus(PublicationStatus.DRAFT);
        library.setId("library-1");
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(library);
        var copy = (Library) adapter.copy();
        var adapterCopy = new LibraryAdapter(copy);
        adapterCopy.setId(new IdDt("Library", "library-2"));
        assertNotEquals(library.getId(), copy.getId());
        library.setStatus(PublicationStatus.ACTIVE);
        assertNotEquals(adapter.getStatus(), copy.getStatus().toCode());
    }

    @Test
    void adapter_get_all_dependencies() {
        var dependencies = List.of(
                "profileRef", "relatedArtifactRef", "dataRequirementProfileRef", "dataRequirementCodeFilterRef");
        var library = new Library();
        library.getMeta().addProfile(dependencies.get(0));
        library.getRelatedArtifactFirstRep().setResource(dependencies.get(1));
        library.addDataRequirement().addProfile(dependencies.get(2));
        library.addDataRequirement().addCodeFilter().setValueSet(dependencies.get(3));
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(library);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(extractedDependencies.size(), dependencies.size());
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.contains(dep.getReference()));
        });
    }

    @Test
    void adapter_get_and_set_content() {
        var library = new Library();
        var adapter = (LibraryAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);
        var contentList = new ArrayList<Attachment>();
        contentList.add(new Attachment().setContentType("text/cql").setData(new byte[10]));
        adapter.setContent(contentList);
        assertTrue(adapter.hasContent());
        assertEquals(contentList, adapter.getContent());
        adapter.addContent().setContentType("text/xml").setData(new byte[20]);
        assertEquals(2, adapter.getContent().size());
        assertEquals("text/xml", adapter.getContent().get(1).getContentType());
    }

    @Test
    void adapter_get_and_set_type() {
        var type = new CodeableConcept(new Coding("www.test.com", "test", "Test"));
        var library = new Library().setType(type);
        var adapter = (LibraryAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);
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
        var adapter = (LibraryAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);
        var dataRequirements = new ArrayList<DataRequirement>();
        dataRequirements.add(new DataRequirement().setType("Patient"));
        adapter.setDataRequirement(dataRequirements);
        assertEquals(dataRequirements, library.getDataRequirement());
        adapter.addDataRequirement(new DataRequirement().setType("Observation"));
        assertEquals(
                library.getDataRequirement(),
                adapter.getDataRequirement().stream().map(IAdapter::get).toList());
        assertEquals(2, adapter.getDataRequirement().size());
    }

    @Test
    void adapter_get_useContext() {
        var library = new Library();
        var useContext = new UsageContext().setCode(new Coding("www.test.com", "test", "Test"));
        library.setUseContext(Collections.singletonList(useContext));
        var adapter = (LibraryAdapter) adapterFactory.createKnowledgeArtifactAdapter(library);
        assertEquals(useContext, adapter.getUseContext().get(0));
    }
}
