package org.opencds.cqf.fhir.utility.adapter.dstu3;

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
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UriType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.TestVisitor;

class PlanDefinitionAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var library = new Library();
        assertThrows(IllegalArgumentException.class, () -> adapterFactory.createPlanDefinition(library));
    }

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new TestVisitor());
        var planDef = new PlanDefinition();
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(planDef);
        doReturn(planDef).when(spyVisitor).visit(any(PlanDefinitionAdapter.class), any());
        adapter.accept(spyVisitor, null);
        verify(spyVisitor, times(1)).visit(any(PlanDefinitionAdapter.class), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var planDef = new PlanDefinition();
        var name = "name";
        planDef.setName(name);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(planDef);
        assertEquals(name, adapter.getName());
        var newName = "name2";
        adapter.setName(newName);
        assertEquals(newName, planDef.getName());
    }

    @Test
    void adapter_get_and_set_url() {
        var planDef = new PlanDefinition();
        var url = "www.url.com";
        planDef.setUrl(url);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(planDef);
        assertEquals(url, adapter.getUrl());
        var newUrl = "www.url2.com";
        adapter.setUrl(newUrl);
        assertEquals(newUrl, planDef.getUrl());
    }

    @Test
    void adapter_get_and_set_version() {
        var planDef = new PlanDefinition();
        var version = "1.0.0";
        planDef.setVersion(version);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(planDef);
        assertTrue(adapter.hasVersion());
        assertEquals(version, adapter.getVersion());
        var newVersion = "1.0.1";
        adapter.setVersion(newVersion);
        assertEquals(newVersion, planDef.getVersion());
    }

    @Test
    void adapter_get_and_set_status() {
        var planDef = new PlanDefinition();
        var status = PublicationStatus.DRAFT;
        planDef.setStatus(status);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(planDef);
        assertEquals(status.toCode(), adapter.getStatus());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setStatus("invalid-status"));
        var newStatus = PublicationStatus.ACTIVE;
        adapter.setStatus(newStatus.toCode());
        assertEquals(newStatus, PublicationStatus.fromCode(adapter.getStatus()));
    }

    @Test
    void adapter_get_and_set_dates() {
        var planDef = new PlanDefinition();
        var date = new Date();
        var approvalDate = new Date();
        var effectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2020-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2020-12-31")));
        planDef.setDate(date);
        planDef.setApprovalDate(approvalDate);
        planDef.setEffectivePeriod(effectivePeriod);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(planDef);
        assertEquals(date, adapter.getDate());
        assertEquals(approvalDate, adapter.getApprovalDate());
        assertEquals(effectivePeriod, adapter.getEffectivePeriod());
        var newDate = new Date();
        newDate.setTime(100);
        adapter.setDate(newDate);
        assertEquals(newDate, planDef.getDate());
        var newApprovalDate = new Date();
        newApprovalDate.setTime(100);
        adapter.setApprovalDate(newApprovalDate);
        assertEquals(newApprovalDate, planDef.getApprovalDate());
        var newEffectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2021-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2021-12-31")));
        adapter.setEffectivePeriod(newEffectivePeriod);
        assertEquals(newEffectivePeriod, adapter.getEffectivePeriod());
    }

    @Test
    void adapter_get_experimental() {
        var planDef = new PlanDefinition();
        var experimental = true;
        planDef.setExperimental(experimental);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(planDef);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_set_relatedArtifact() {
        var planDef = new PlanDefinition();
        var relatedArtifactList = List.of(new RelatedArtifact());
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(planDef);
        adapter.setRelatedArtifact(relatedArtifactList);
        assertEquals(relatedArtifactList, planDef.getRelatedArtifact());
        assertEquals(relatedArtifactList, adapter.getRelatedArtifact());
    }

    @Test
    void adapter_copy() {
        var planDef = new PlanDefinition().setStatus(PublicationStatus.DRAFT);
        planDef.setId("plan-1");
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(planDef);
        var copy = (PlanDefinition) adapter.copy();
        var adapterCopy = new PlanDefinitionAdapter(copy);
        adapterCopy.setId(new IdDt("PlanDefinition", "plan-2"));
        assertNotEquals(planDef.getId(), copy.getId());
        planDef.setStatus(PublicationStatus.ACTIVE);
        assertNotEquals(adapter.getStatus(), copy.getStatus().toCode());
    }

    @Test
    void adapter_get_all_dependencies() {
        var dependencies = List.of(
                "profileRef",
                "relatedArtifactRef",
                "libraryRef",
                "actionTriggerDataReqProfile",
                "actionTriggerDataReqCodeFilterValueSet",
                "actionInputProfile",
                "actionInputCodeFilterValueSet",
                "actionOutputProfile",
                "actionOutputCodeFilterValueSet",
                "actionDefinitionRef",
                // no dependency test for action.condition.expression.reference and
                // action.dynamicValue.expression.reference since these are not defined in DSTU3
                // "actionConditionExpressionReference",
                // "actionDynamicValueExpressionRef",
                "cpgPartOfExtRef",
                "nestedActionDefinitionReference");
        var planDef = new PlanDefinition();
        planDef.getMeta().addProfile(dependencies.get(0));
        planDef.getRelatedArtifactFirstRep()
                .setResource(new Reference(dependencies.get(1)))
                .setType(RelatedArtifactType.DEPENDSON);
        planDef.getLibraryFirstRep().setReference(dependencies.get(2));
        var action = planDef.getActionFirstRep();
        action.getTriggerDefinitionFirstRep()
                .getEventData()
                .setProfile(List.of(new UriType(dependencies.get(3))))
                .getCodeFilterFirstRep()
                .setValueSet(new StringType(dependencies.get(4)));
        action.getInputFirstRep()
                .setProfile(List.of(new UriType(dependencies.get(5))))
                .getCodeFilterFirstRep()
                .setValueSet(new StringType(dependencies.get(6)));
        action.getOutputFirstRep()
                .setProfile(List.of(new UriType(dependencies.get(7))))
                .getCodeFilterFirstRep()
                .setValueSet(new StringType(dependencies.get(8)));
        action.getDefinition().setReference(dependencies.get(9));
        planDef.addExtension(new Extension(
                "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-partOf", new UriType(dependencies.get(10))));
        action.addAction().getDefinition().setReference(dependencies.get(11));
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(planDef);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(extractedDependencies.size(), dependencies.size());
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.contains(dep.getReference()));
        });
    }

    @Test
    void adapter_get_all_dependencies_with_non_depends_on_related_artifacts() {
        var dependencies = List.of(
                "profileRef",
                "relatedArtifactRef",
                "libraryRef",
                "actionTriggerDataReqProfile",
                "actionTriggerDataReqCodeFilterValueSet",
                "actionInputProfile",
                "actionInputCodeFilterValueSet",
                "actionOutputProfile",
                "actionOutputCodeFilterValueSet",
                "actionDefinitionRef",
                // no dependency test for action.condition.expression.reference and
                // action.dynamicValue.expression.reference since these are not defined in DSTU3
                // "actionConditionExpressionReference",
                // "actionDynamicValueExpressionRef",
                "cpgPartOfExtRef",
                "nestedActionDefinitionReference");
        var planDef = new PlanDefinition();
        planDef.getMeta().addProfile(dependencies.get(0));
        planDef.getRelatedArtifactFirstRep().setResource(new Reference(dependencies.get(1)));
        planDef.getLibraryFirstRep().setReference(dependencies.get(2));
        var action = planDef.getActionFirstRep();
        action.getTriggerDefinitionFirstRep()
                .getEventData()
                .setProfile(List.of(new UriType(dependencies.get(3))))
                .getCodeFilterFirstRep()
                .setValueSet(new StringType(dependencies.get(4)));
        action.getInputFirstRep()
                .setProfile(List.of(new UriType(dependencies.get(5))))
                .getCodeFilterFirstRep()
                .setValueSet(new StringType(dependencies.get(6)));
        action.getOutputFirstRep()
                .setProfile(List.of(new UriType(dependencies.get(7))))
                .getCodeFilterFirstRep()
                .setValueSet(new StringType(dependencies.get(8)));
        action.getDefinition().setReference(dependencies.get(9));
        planDef.addExtension(new Extension(
                "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-partOf", new UriType(dependencies.get(10))));
        action.addAction().getDefinition().setReference(dependencies.get(11));
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(planDef);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(extractedDependencies.size(), dependencies.size() - 1);
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.contains(dep.getReference()));
        });
    }

    @Test
    void testDescription() {
        var planDef = new PlanDefinition();
        var description = "test description";
        planDef.setDescription(description);
        var adapter = (IPlanDefinitionAdapter) adapterFactory.createKnowledgeArtifactAdapter(planDef);
        assertEquals(description, adapter.getDescription());
    }

    @Test
    void testAction() {
        var action = new PlanDefinitionActionComponent().setDefinition(new Reference("test"));
        var planDef = new PlanDefinition().addAction(action);
        var adapter = adapterFactory.createPlanDefinition(planDef);
        assertTrue(adapter.hasAction());
        assertEquals(action, adapter.getAction().get(0).get());
    }

    @Test
    void testLibrary() {
        var planDef = new PlanDefinition();
        var library = "Library/test";
        var libraryRef = new Reference(library);
        planDef.addLibrary(libraryRef);
        var adapter = (IPlanDefinitionAdapter) adapterFactory.createKnowledgeArtifactAdapter(planDef);
        assertTrue(adapter.hasLibrary());
        assertEquals(List.of(library), adapter.getLibrary());
        assertEquals(Map.of("test", library), adapter.getReferencedLibraries());
    }
}
