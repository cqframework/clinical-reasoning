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

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.TestVisitor;

class QuestionnaireAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var library = new Library();
        assertThrows(IllegalArgumentException.class, () -> adapterFactory.createQuestionnaire(library));
    }

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new TestVisitor());
        var questionnaire = new Questionnaire();
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(questionnaire);
        doReturn(questionnaire).when(spyVisitor).visit(any(QuestionnaireAdapter.class), any());
        adapter.accept(spyVisitor, null);
        verify(spyVisitor, times(1)).visit(any(QuestionnaireAdapter.class), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var questionnaire = new Questionnaire();
        var name = "name";
        questionnaire.setName(name);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(questionnaire);
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
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(questionnaire);
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
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(questionnaire);
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
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(questionnaire);
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
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(questionnaire);
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
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(questionnaire);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_set_relatedArtifact() {
        var questionnaire = new Questionnaire();
        var relatedArtifactList = List.of(new RelatedArtifact());
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(questionnaire);
        adapter.setRelatedArtifact(relatedArtifactList);
        assertEquals(0, adapter.getRelatedArtifact().size());
    }

    @Test
    void adapter_copy() {
        var questionnaire = new Questionnaire().setStatus(PublicationStatus.DRAFT);
        questionnaire.setId("plan-1");
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(questionnaire);
        var copy = (Questionnaire) adapter.copy();
        copy.setId("plan-2");
        assertNotEquals(questionnaire.getId(), copy.getId());
        questionnaire.setStatus(PublicationStatus.ACTIVE);
        assertNotEquals(adapter.getStatus(), copy.getStatus().toCode());
    }

    @Test
    void adapter_get_all_dependencies() {
        var dependencies = List.of(
                "profileRef",
                "derivedRef",
                "cqfLibraryRef",
                "variableRef",
                "itemDefinitionRef",
                "answerValueSetRef",
                "unitValueSetRef",
                "referenceProfileRef",
                "candidateExpressionRef",
                "lookupQuestionnaireRef",
                "itemVariableRef",
                "initialExpressionRef",
                "calculatedExpressionRef",
                "expressionRef",
                "subQuestionnaireRef");
        var questionnaire = new Questionnaire();
        questionnaire.getMeta().addProfile(dependencies.get(0));
        questionnaire.addDerivedFrom(dependencies.get(1));
        questionnaire.addExtension(Constants.CQF_LIBRARY, new CanonicalType(dependencies.get(2)));
        var variableExt = new Extension(Constants.VARIABLE_EXTENSION)
                .setValue(new Expression().setReference(dependencies.get(3)));
        questionnaire.addExtension(variableExt);
        questionnaire.addItem().setDefinition(dependencies.get(4) + "#Observation");
        questionnaire.addItem().setAnswerValueSet(dependencies.get(5));
        questionnaire
                .addItem()
                .setType(QuestionnaireItemType.QUANTITY)
                .addExtension(Constants.QUESTIONNAIRE_UNIT_VALUE_SET, new CanonicalType(dependencies.get(6)));
        questionnaire
                .addItem()
                .addExtension(Constants.QUESTIONNAIRE_REFERENCE_PROFILE, new CanonicalType(dependencies.get(7)));
        var candidateExpressionExt = new Extension(Constants.SDC_QUESTIONNAIRE_CANDIDATE_EXPRESSION)
                .setValue(new Expression().setReference(dependencies.get(8)));
        questionnaire.addItem().addExtension(candidateExpressionExt);
        var lookupQuestionnaireExt = new Extension(Constants.SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE)
                .setValue(new CanonicalType(dependencies.get(9)));
        questionnaire.addItem().addExtension(lookupQuestionnaireExt);
        var itemVariableExt = new Extension(Constants.VARIABLE_EXTENSION)
                .setValue(new Expression().setReference(dependencies.get(10)));
        questionnaire.addItem().addExtension(itemVariableExt);
        var initialExpressionExt = new Extension(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION)
                .setValue(new Expression().setReference(dependencies.get(11)));
        questionnaire.addItem().addExtension(initialExpressionExt);
        var calculatedExpressionExt = new Extension(Constants.SDC_QUESTIONNAIRE_CALCULATED_EXPRESSION)
                .setValue(new Expression().setReference(dependencies.get(12)));
        questionnaire.addItem().addExtension(calculatedExpressionExt);
        var expressionExt =
                new Extension(Constants.CQF_EXPRESSION).setValue(new Expression().setReference(dependencies.get(13)));
        questionnaire.addItem().addExtension(expressionExt);
        questionnaire
                .addItem()
                .addExtension(Constants.SDC_QUESTIONNAIRE_SUB_QUESTIONNAIRE, new CanonicalType(dependencies.get(14)));
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(questionnaire);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(dependencies.size(), extractedDependencies.size());
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.contains(dep.getReference()));
        });
    }

    @Test
    void testItem() {
        var questionnaire = new Questionnaire();
        var item1 = adapterFactory.createQuestionnaireItem(
                new QuestionnaireItemComponent().setLinkId("1").setType(QuestionnaireItemType.BOOLEAN));
        var item2 = adapterFactory.createQuestionnaireItem(
                new QuestionnaireItemComponent().setLinkId("2").setType(QuestionnaireItemType.BOOLEAN));
        var item3 = adapterFactory.createQuestionnaireItem(
                new QuestionnaireItemComponent().setLinkId("3").setType(QuestionnaireItemType.BOOLEAN));
        var item4 = adapterFactory.createQuestionnaireItem(
                new QuestionnaireItemComponent().setLinkId("4").setType(QuestionnaireItemType.BOOLEAN));
        questionnaire.addItem((QuestionnaireItemComponent) item1.get());
        questionnaire.addItem((QuestionnaireItemComponent) item2.get());
        var adapter = adapterFactory.createQuestionnaire(questionnaire);
        assertTrue(adapter.hasItem());
        assertEquals(2, adapter.getItem().size());
        adapter.addItem(item3);
        assertEquals(3, adapter.getItem().size());
        adapter.addItem((IBaseBackboneElement) item4.get());
        assertEquals(4, adapter.getItem().size());
        adapter.setItem(List.of(item1));
        assertEquals(1, adapter.getItem().size());
        adapter.addItems(List.of(item2, item3, item4));
        assertEquals(4, adapter.getItem().size());
    }
}
