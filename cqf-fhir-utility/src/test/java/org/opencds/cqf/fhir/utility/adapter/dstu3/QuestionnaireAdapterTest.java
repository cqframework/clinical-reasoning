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
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.visitor.PackageVisitor;

public class QuestionnaireAdapterTest {
    private final FhirContext fhirContext = FhirContext.forDstu3Cached();

    @Test
    void invalid_object_fails() {
        assertThrows(IllegalArgumentException.class, () -> new QuestionnaireAdapter(new Library()));
    }

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new PackageVisitor(fhirContext));
        doReturn(new Bundle()).when(spyVisitor).visit(any(QuestionnaireAdapter.class), any(), any());
        IDomainResource questionnaire = new Questionnaire();
        var adapter = new QuestionnaireAdapter(questionnaire);
        adapter.accept(spyVisitor, null, null);
        verify(spyVisitor, times(1)).visit(any(QuestionnaireAdapter.class), any(), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var questionnaire = new Questionnaire();
        var name = "name";
        questionnaire.setName(name);
        var adapter = new QuestionnaireAdapter(questionnaire);
        assertEquals(name, adapter.getName());
        var newName = "name2";
        adapter.setName(newName);
        assertEquals(newName, questionnaire.getName());
    }

    @Test
    void adapter_get_and_set_url() {
        var questionnaire = new Questionnaire();
        var url = "www.url.com";
        questionnaire.setUrl(url);
        var adapter = new QuestionnaireAdapter(questionnaire);
        assertEquals(url, adapter.getUrl());
        var newUrl = "www.url2.com";
        adapter.setUrl(newUrl);
        assertEquals(newUrl, questionnaire.getUrl());
    }

    @Test
    void adapter_get_and_set_version() {
        var questionnaire = new Questionnaire();
        var version = "1.0.0";
        questionnaire.setVersion(version);
        var adapter = new QuestionnaireAdapter(questionnaire);
        assertTrue(adapter.hasVersion());
        assertEquals(version, adapter.getVersion());
        var newVersion = "1.0.1";
        adapter.setVersion(newVersion);
        assertEquals(newVersion, questionnaire.getVersion());
    }

    @Test
    void adapter_get_and_set_status() {
        var questionnaire = new Questionnaire();
        var status = PublicationStatus.DRAFT;
        questionnaire.setStatus(status);
        var adapter = new QuestionnaireAdapter(questionnaire);
        assertEquals(status.toCode(), adapter.getStatus());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setStatus("invalid-status"));
        var newStatus = PublicationStatus.ACTIVE;
        adapter.setStatus(newStatus.toCode());
        assertEquals(newStatus, PublicationStatus.fromCode(adapter.getStatus()));
    }

    @Test
    void adapter_get_and_set_dates() {
        var questionnaire = new Questionnaire();
        var date = new Date();
        var approvalDate = new Date();
        var effectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2020-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2020-12-31")));
        questionnaire.setDate(date);
        questionnaire.setApprovalDate(approvalDate);
        questionnaire.setEffectivePeriod(effectivePeriod);
        var adapter = new QuestionnaireAdapter(questionnaire);
        assertEquals(date, adapter.getDate());
        assertEquals(approvalDate, adapter.getApprovalDate());
        assertEquals(effectivePeriod, adapter.getEffectivePeriod());
        var newDate = new Date();
        newDate.setTime(100);
        adapter.setDate(newDate);
        assertEquals(newDate, questionnaire.getDate());
        var newApprovalDate = new Date();
        newApprovalDate.setTime(100);
        adapter.setApprovalDate(newApprovalDate);
        assertEquals(newApprovalDate, questionnaire.getApprovalDate());
        var newEffectivePeriod = new Period()
                .setStart(java.sql.Date.valueOf(LocalDate.parse("2021-01-01")))
                .setEnd(java.sql.Date.valueOf(LocalDate.parse("2021-12-31")));
        adapter.setEffectivePeriod(newEffectivePeriod);
        assertEquals(newEffectivePeriod, adapter.getEffectivePeriod());
    }

    @Test
    void adapter_get_experimental() {
        var questionnaire = new Questionnaire();
        var experimental = true;
        questionnaire.setExperimental(experimental);
        var adapter = new QuestionnaireAdapter(questionnaire);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_set_relatedArtifact() {
        var questionnaire = new Questionnaire();
        var relatedArtifactList = List.of(new RelatedArtifact());
        var adapter = new QuestionnaireAdapter(questionnaire);
        adapter.setRelatedArtifact(relatedArtifactList);
        assertEquals(0, adapter.getRelatedArtifact().size());
    }

    @Test
    void adapter_copy() {
        var questionnaire = new Questionnaire().setStatus(PublicationStatus.DRAFT);
        questionnaire.setId("plan-1");
        var adapter = new QuestionnaireAdapter(questionnaire);
        var copy = adapter.copy();
        copy.setId("plan-2");
        assertNotEquals(questionnaire.getId(), copy.getId());
        questionnaire.setStatus(PublicationStatus.ACTIVE);
        assertNotEquals(adapter.getStatus(), copy.getStatus());
    }

    @Test
    void adapter_get_all_dependencies() {
        var dependencies = List.of(
                "profileRef",
                "cqfLibraryRef",
                // "variableRef",
                "itemDefinitionRef"
                // "answerValueSetRef",
                // "itemMediaRef",
                // "itemAnswerMediaRef",
                // "unitValueSetRef",
                // "referenceProfileRef",
                // "candidateExpressionRef",
                // "lookupQuestionnaireRef",
                // "itemVariableRef",
                // "initialExpressionRef",
                // "calculatedExpressionRef",
                // "calculatedValueRef",
                // "expressionRef",
                // "subQuestionnaireRef"
                );
        var questionnaire = new Questionnaire();
        questionnaire.getMeta().addProfile(dependencies.get(0));
        questionnaire.addExtension(Constants.CQIF_LIBRARY, new Reference(dependencies.get(1)));
        // var variableExt = new Extension(Constants.VARIABLE_EXTENSION).setValue(new
        // Expression().setReference(dependencies.get(2)));
        // questionnaire.addExtension(variableExt);
        questionnaire.addItem().setDefinition(dependencies.get(2) + "#Observation");
        var adapter = new QuestionnaireAdapter(questionnaire);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(dependencies.size(), extractedDependencies.size());
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.indexOf(dep.getReference()) >= 0);
        });
    }
}
