package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IActivityDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.TestVisitor;

class ActivityDefinitionAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var library = new Library();
        assertThrows(IllegalArgumentException.class, () -> adapterFactory.createActivityDefinition(library));
    }

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new TestVisitor());
        var activityDef = new ActivityDefinition();
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(activityDef);
        doReturn(activityDef)
                .when(spyVisitor)
                .visit(any(org.opencds.cqf.fhir.utility.adapter.r5.ActivityDefinitionAdapter.class), any());
        adapter.accept(spyVisitor, null);
        verify(spyVisitor, times(1))
                .visit(any(org.opencds.cqf.fhir.utility.adapter.r5.ActivityDefinitionAdapter.class), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var activityDef = new ActivityDefinition();
        var name = "name";
        activityDef.setName(name);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(activityDef);
        assertEquals(name, adapter.getName());
        var newName = "name2";
        adapter.setName(newName);
        assertEquals(newName, activityDef.getName());
    }

    @Test
    void adapter_get_and_set_url() {
        var activityDef = new ActivityDefinition();
        var url = "www.url.com";
        activityDef.setUrl(url);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(activityDef);
        assertEquals(url, adapter.getUrl());
        var newUrl = "www.url2.com";
        adapter.setUrl(newUrl);
        assertEquals(newUrl, activityDef.getUrl());
    }

    @Test
    void adapter_get_and_set_version() {
        var activityDef = new ActivityDefinition();
        var version = "1.0.0";
        activityDef.setVersion(version);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(activityDef);
        assertTrue(adapter.hasVersion());
        assertEquals(version, adapter.getVersion());
        var newVersion = "1.0.1";
        adapter.setVersion(newVersion);
        assertEquals(newVersion, activityDef.getVersion());
    }

    @Test
    void adapter_get_and_set_status() {
        var activityDef = new ActivityDefinition();
        var status = PublicationStatus.DRAFT;
        activityDef.setStatus(status);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(activityDef);
        assertEquals(status.toCode(), adapter.getStatus());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setStatus("invalid-status"));
        var newStatus = PublicationStatus.ACTIVE;
        adapter.setStatus(newStatus.toCode());
        assertEquals(newStatus, PublicationStatus.fromCode(adapter.getStatus()));
    }

    @Test
    void adapter_get_and_set_dates() {
        var activityDef = new ActivityDefinition();
        var date = new Date();
        var approvalDate = new Date();
        var effectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2020-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2020-12-31")));
        activityDef.setDate(date);
        activityDef.setApprovalDate(approvalDate);
        activityDef.setEffectivePeriod(effectivePeriod);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(activityDef);
        assertEquals(date, adapter.getDate());
        assertEquals(approvalDate, adapter.getApprovalDate());
        assertEquals(effectivePeriod, adapter.getEffectivePeriod());
        var newDate = new Date();
        newDate.setTime(100);
        adapter.setDate(newDate);
        assertEquals(newDate, activityDef.getDate());
        var newApprovalDate = new Date();
        newApprovalDate.setTime(100);
        adapter.setApprovalDate(newApprovalDate);
        assertEquals(newApprovalDate, activityDef.getApprovalDate());
        var newEffectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2021-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2021-12-31")));
        adapter.setEffectivePeriod(newEffectivePeriod);
        assertEquals(newEffectivePeriod, adapter.getEffectivePeriod());
    }

    @Test
    void adapter_get_experimental() {
        var activityDef = new ActivityDefinition();
        var experimental = true;
        activityDef.setExperimental(experimental);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(activityDef);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_set_relatedArtifact() {
        var activityDef = new ActivityDefinition();
        var relatedArtifactList = List.of(new RelatedArtifact());
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(activityDef);
        adapter.setRelatedArtifact(relatedArtifactList);
        assertEquals(relatedArtifactList, activityDef.getRelatedArtifact());
        assertEquals(relatedArtifactList, adapter.getRelatedArtifact());
    }

    @Test
    void adapter_copy() {
        var activityDef = new ActivityDefinition().setStatus(PublicationStatus.DRAFT);
        activityDef.setId("plan-1");
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(activityDef);
        var copy = (ActivityDefinition) adapter.copy();
        var adapterCopy = new ActivityDefinitionAdapter(copy);
        adapterCopy.setId(new IdDt("ActivityDefinition", "plan-2"));
        assertNotEquals(activityDef.getId(), copy.getId());
        activityDef.setStatus(PublicationStatus.ACTIVE);
        assertNotEquals(adapter.getStatus(), copy.getStatus().toCode());
    }

    @Test
    void adapter_get_all_dependencies() {
        var dependencies = List.of("profileRef", "relatedArtifactRef", "libraryRef");
        var activityDef = new ActivityDefinition();
        activityDef.getMeta().addProfile(dependencies.get(0));
        activityDef
                .getRelatedArtifactFirstRep()
                .setResource(dependencies.get(1))
                .setType(RelatedArtifactType.DEPENDSON);
        activityDef.addLibrary(dependencies.get(2));
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(activityDef);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(extractedDependencies.size(), dependencies.size());
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.contains(dep.getReference()));
        });
    }

    @Test
    void adapter_get_all_dependencies_with_non_depends_on_related_artifacts() {
        var dependencies = List.of("profileRef", "relatedArtifactRef", "libraryRef");
        var activityDef = new ActivityDefinition();
        activityDef.getMeta().addProfile(dependencies.get(0));
        activityDef.getRelatedArtifactFirstRep().setResource(dependencies.get(1));
        activityDef.addLibrary(dependencies.get(2));
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(activityDef);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(extractedDependencies.size(), dependencies.size() - 1);
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.contains(dep.getReference()));
        });
    }

    @Test
    void testDescription() {
        var activityDef = new ActivityDefinition();
        var description = "test description";
        activityDef.setDescription(description);
        var adapter = (IActivityDefinitionAdapter) adapterFactory.createKnowledgeArtifactAdapter(activityDef);
        assertEquals(description, adapter.getDescription());
    }

    @Test
    void testLibrary() {
        var activityDef = new ActivityDefinition();
        var library = "Library/test";
        activityDef.addLibrary(library);
        var adapter = (IActivityDefinitionAdapter) adapterFactory.createKnowledgeArtifactAdapter(activityDef);
        assertTrue(adapter.hasLibrary());
        assertEquals(List.of(library), adapter.getLibrary());
        assertEquals(Map.of("test", library), adapter.getReferencedLibraries());
    }
}
