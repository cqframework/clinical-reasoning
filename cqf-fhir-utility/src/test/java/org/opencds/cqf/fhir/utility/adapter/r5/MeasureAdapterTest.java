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
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Measure;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.visitor.PackageVisitor;

public class MeasureAdapterTest {
    private final FhirContext fhirContext = FhirContext.forR5Cached();
    private final org.opencds.cqf.fhir.utility.adapter.AdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        assertThrows(IllegalArgumentException.class, () -> new MeasureAdapter(new Library()));
    }

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new PackageVisitor(fhirContext));
        doReturn(new Bundle()).when(spyVisitor).visit(any(MeasureAdapter.class), any(), any());
        IDomainResource measure = new Measure();
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(measure);
        adapter.accept(spyVisitor, null, null);
        verify(spyVisitor, times(1)).visit(any(MeasureAdapter.class), any(), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var measure = new Measure();
        var name = "name";
        measure.setName(name);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(measure);
        assertEquals(name, adapter.getName());
        var newName = "name2";
        adapter.setName(newName);
        assertEquals(newName, measure.getName());
    }

    @Test
    void adapter_get_and_set_url() {
        var measure = new Measure();
        var url = "www.url.com";
        measure.setUrl(url);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(measure);
        assertEquals(url, adapter.getUrl());
        var newUrl = "www.url2.com";
        adapter.setUrl(newUrl);
        assertEquals(newUrl, measure.getUrl());
    }

    @Test
    void adapter_get_and_set_version() {
        var measure = new Measure();
        var version = "1.0.0";
        measure.setVersion(version);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(measure);
        assertTrue(adapter.hasVersion());
        assertEquals(version, adapter.getVersion());
        var newVersion = "1.0.1";
        adapter.setVersion(newVersion);
        assertEquals(newVersion, measure.getVersion());
    }

    @Test
    void adapter_get_and_set_status() {
        var measure = new Measure();
        var status = PublicationStatus.DRAFT;
        measure.setStatus(status);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(measure);
        assertEquals(status.toCode(), adapter.getStatus());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setStatus("invalid-status"));
        var newStatus = PublicationStatus.ACTIVE;
        adapter.setStatus(newStatus.toCode());
        assertEquals(newStatus, PublicationStatus.fromCode(adapter.getStatus()));
    }

    @Test
    void adapter_get_and_set_dates() {
        var measure = new Measure();
        var date = new Date();
        var approvalDate = new Date();
        var effectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2020-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2020-12-31")));
        measure.setDate(date);
        measure.setApprovalDate(approvalDate);
        measure.setEffectivePeriod(effectivePeriod);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(measure);
        assertEquals(date, adapter.getDate());
        assertEquals(approvalDate, adapter.getApprovalDate());
        assertEquals(effectivePeriod, adapter.getEffectivePeriod());
        var newDate = new Date();
        newDate.setTime(100);
        adapter.setDate(newDate);
        assertEquals(newDate, measure.getDate());
        var newApprovalDate = new Date();
        newApprovalDate.setTime(100);
        adapter.setApprovalDate(newApprovalDate);
        assertEquals(newApprovalDate, measure.getApprovalDate());
        var newEffectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2021-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2021-12-31")));
        adapter.setEffectivePeriod(newEffectivePeriod);
        assertEquals(newEffectivePeriod, adapter.getEffectivePeriod());
    }

    @Test
    void adapter_get_experimental() {
        var measure = new Measure();
        var experimental = true;
        measure.setExperimental(experimental);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(measure);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_set_relatedArtifact() {
        var measure = new Measure();
        var relatedArtifactList = List.of(new RelatedArtifact());
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(measure);
        adapter.setRelatedArtifact(relatedArtifactList);
        assertEquals(relatedArtifactList, measure.getRelatedArtifact());
        assertEquals(relatedArtifactList, adapter.getRelatedArtifact());
    }

    @Test
    void adapter_copy() {
        var measure = new Measure().setStatus(PublicationStatus.DRAFT);
        measure.setId("plan-1");
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(measure);
        var copy = (Measure) adapter.copy();
        var adapterCopy = new MeasureAdapter(copy);
        adapterCopy.setId(new IdDt("Measure", "plan-2"));
        assertNotEquals(measure.getId(), copy.getId());
        measure.setStatus(PublicationStatus.ACTIVE);
        assertNotEquals(adapter.getStatus(), copy.getStatus());
    }

    @Test
    void adapter_get_all_dependencies() {
        var dependencies = List.of(
                "profileRef",
                "relatedArtifactRef",
                "libraryRef",
                "populationCriteriaRef",
                "stratifierCriteriaRef",
                "stratifierComponentCriteriaRef",
                "supplementalDataCriteriaRef",
                "inputParametersRef",
                "expansionParametersRef",
                "cqlOptionsRef",
                "componentRef");
        var measure = new Measure();
        measure.getMeta().addProfile(dependencies.get(0));
        measure.getRelatedArtifactFirstRep().setResource(dependencies.get(1));
        measure.getLibrary().add(new CanonicalType(dependencies.get(2)));
        measure.addGroup().addPopulation().setCriteria(new Expression().setReference(dependencies.get(3)));
        measure.addGroup().addStratifier().setCriteria(new Expression().setReference(dependencies.get(4)));
        measure.addGroup()
                .addStratifier()
                .addComponent()
                .setCriteria(new Expression().setReference(dependencies.get(5)));
        measure.addSupplementalData().setCriteria(new Expression().setReference(dependencies.get(6)));
        measure.addExtension(Constants.CQFM_INPUT_PARAMETERS, new Reference(dependencies.get(7)));
        measure.addExtension(Constants.CQF_EXPANSION_PARAMETERS, new Reference(dependencies.get(8)));
        measure.addExtension(Constants.CQF_CQL_OPTIONS, new Reference(dependencies.get(9)));
        measure.addExtension(Constants.CQFM_COMPONENT, new RelatedArtifact().setResource(dependencies.get(10)));
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(measure);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(extractedDependencies.size(), dependencies.size());
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.indexOf(dep.getReference()) >= 0);
        });
    }

    @Test
    void adapter_get_all_dependencies_with_effective_data_requirements() {
        var dependencies = List.of(
                "libraryProfileRef",
                "relatedArtifactRef",
                "dataRequirementProfileRef",
                "dataRequirementCodeFilterRef",
                "measureProfileRef");
        var library = new Library()
                .setType(new CodeableConcept(new Coding(
                        "http://terminology.hl7.org/CodeSystem/library-type",
                        "module-definition",
                        "Module Definition")));
        library.setId("test");
        library.getMeta().addProfile(dependencies.get(0));
        library.getRelatedArtifactFirstRep().setResource(dependencies.get(1));
        library.addDataRequirement().addProfile(dependencies.get(2));
        library.addDataRequirement().addCodeFilter().setValueSet(dependencies.get(3));
        var measure = new Measure().addContained(new Patient()).addContained(library);
        measure.getMeta().addProfile(dependencies.get(4));
        measure.addExtension(Constants.CQFM_EFFECTIVE_DATA_REQUIREMENTS, new CanonicalType("#test"));
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(measure);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(dependencies.size(), extractedDependencies.size());
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.indexOf(dep.getReference()) >= 0);
        });
    }
}
