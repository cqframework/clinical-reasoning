package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Questionnaire;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.visitor.PackageVisitor;

public class QuestionnaireAdapterTest {
    private final FhirContext fhirContext = FhirContext.forR4Cached();

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
    void adapter_get_experimental() {
        var questionnaire = new Questionnaire();
        var experimental = true;
        questionnaire.setExperimental(experimental);
        var adapter = new QuestionnaireAdapter(questionnaire);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_get_all_dependencies() {}
}
