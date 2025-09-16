package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
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
        var adapter = new QuestionnaireResponseAdapter(response);
        assertNotNull(adapter);
        assertEquals(response, adapter.get());
        assertEquals(FhirVersionEnum.R5, adapter.fhirVersion());
        assertNotNull(adapter.getModelResolver());
        assertNotNull(adapter.getAdapterFactory());
    }

    @Test
    void testInfo() {}

    @Test
    void testItem() {
        var questionnaire = new QuestionnaireResponse();
        var item1 = adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent("1"));
        var item2 = adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent("2"));
        var item3 = adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent("3"));
        var item4 = adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent("4"));
        questionnaire.addItem((QuestionnaireResponseItemComponent) item1.get());
        questionnaire.addItem((QuestionnaireResponseItemComponent) item2.get());
        var adapter = adapterFactory.createQuestionnaireResponse(questionnaire);
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
