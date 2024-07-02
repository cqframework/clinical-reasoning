package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.context.FhirContext;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.visitor.PackageVisitor;

public class PlanDefinitionAdapterTest {
    private final FhirContext fhirContext = FhirContext.forDstu3Cached();

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new PackageVisitor(fhirContext));
        doReturn(new Bundle()).when(spyVisitor).visit(any(PlanDefinitionAdapter.class), any(), any());
        IDomainResource planDef = new PlanDefinition();
        var adapter = new PlanDefinitionAdapter(planDef);
        adapter.accept(spyVisitor, null, null);
        verify(spyVisitor, times(1)).visit(any(PlanDefinitionAdapter.class), any(), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var planDef = new PlanDefinition();
        var name = "name";
        planDef.setName(name);
        var adapter = new PlanDefinitionAdapter(planDef);
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
        var adapter = new PlanDefinitionAdapter(planDef);
        assertEquals(url, adapter.getUrl());
        var newUrl = "www.url2.com";
        adapter.setUrl(newUrl);
        assertEquals(newUrl, planDef.getUrl());
    }

    @Test
    void adapter_get_and_set_approvalDate() {
        var planDef = new PlanDefinition();
        var approvalDate = new Date();
        planDef.setApprovalDate(approvalDate);
        var adapter = new PlanDefinitionAdapter(planDef);
        assertEquals(approvalDate, adapter.getApprovalDate());
        var newApprovalDate = new Date();
        newApprovalDate.setTime(100);
        adapter.setApprovalDate(newApprovalDate);
        assertEquals(newApprovalDate, planDef.getApprovalDate());
    }

    @Test
    void adapter_get_experimental() {
        var planDef = new PlanDefinition();
        var experimental = true;
        planDef.setExperimental(experimental);
        var adapter = new PlanDefinitionAdapter(planDef);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_set_relatedArtifact() {
        var planDef = new PlanDefinition();
        var relatedArtifactList = List.of(new RelatedArtifact());
        var adapter = new PlanDefinitionAdapter(planDef);
        adapter.setRelatedArtifact(relatedArtifactList);
        assertEquals(relatedArtifactList, planDef.getRelatedArtifact());
        assertEquals(relatedArtifactList, adapter.getRelatedArtifact());
    }

    @Test
    void adapter_get_all_dependencies() {
        var dependencies = List.of(
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
        planDef.getRelatedArtifactFirstRep().setResource(new Reference(dependencies.get(0)));
        planDef.getLibraryFirstRep().setReference(dependencies.get(1));
        var action = planDef.getActionFirstRep();
        action.getTriggerDefinitionFirstRep()
                .getEventData()
                .setProfile(List.of(new UriType(dependencies.get(2))))
                .getCodeFilterFirstRep()
                .setValueSet(new StringType(dependencies.get(3)));
        action.getInputFirstRep()
                .setProfile(List.of(new UriType(dependencies.get(4))))
                .getCodeFilterFirstRep()
                .setValueSet(new StringType(dependencies.get(5)));
        action.getOutputFirstRep()
                .setProfile(List.of(new UriType(dependencies.get(6))))
                .getCodeFilterFirstRep()
                .setValueSet(new StringType(dependencies.get(7)));
        action.getDefinition().setReference(dependencies.get(8));
        planDef.addExtension(new Extension(
                "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-partOf", new UriType(dependencies.get(9))));
        action.addAction().getDefinition().setReference(dependencies.get(10));
        var adapter = new PlanDefinitionAdapter(planDef);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(extractedDependencies.size(), dependencies.size());
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.indexOf(dep.getReference()) >= 0);
        });
    }
}
