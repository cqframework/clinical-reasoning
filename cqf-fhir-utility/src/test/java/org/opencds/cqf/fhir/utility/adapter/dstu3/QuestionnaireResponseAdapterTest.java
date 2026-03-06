package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse;
import org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

class QuestionnaireResponseAdapterTest {
    private final IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var library = new Library();
        assertThrows(IllegalArgumentException.class, () -> adapterFactory.createQuestionnaireResponse(library));
    }

    @Test
    void test() {
        var response = new QuestionnaireResponse();
        var adapter = adapterFactory.createQuestionnaireResponse(response);
        assertNotNull(adapterFactory.createResource(response));
        assertNotNull(adapter);
        assertEquals(response, adapter.get());
        assertEquals(FhirVersionEnum.DSTU3, adapter.fhirVersion());
        assertNotNull(adapter.getModelResolver());
        assertNotNull(adapter.getAdapterFactory());
        adapter.setId("test");
        adapter.setQuestionnaire("test.com/Questionnaire/test");
        adapter.setSubject(new IdType("test1"));
        adapter.setAuthored(new Date());
        adapter.setStatus("in-progress");
    }

    @Test
    void testItem() {
        var questionnaireResponse = new QuestionnaireResponse();
        var item1 =
                adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent().setLinkId("1"));
        var item2 =
                adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent().setLinkId("2"));
        var item3 =
                adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent().setLinkId("3"));
        var item4 =
                adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent().setLinkId("4"));
        questionnaireResponse.addItem((QuestionnaireResponseItemComponent) item1.get());
        questionnaireResponse.addItem((QuestionnaireResponseItemComponent) item2.get());
        var adapter = adapterFactory.createQuestionnaireResponse(questionnaireResponse);
        assertTrue(adapter.hasItem());
        assertEquals(2, adapter.getItem().size());
        adapter.addItem(item3);
        assertEquals(3, adapter.getItem().size());
        adapter.setItem(List.of(item1));
        assertEquals(1, adapter.getItem().size());
        adapter.addItems(List.of(item2, item3, item4));
        assertEquals(4, adapter.getItem().size());
    }
}
