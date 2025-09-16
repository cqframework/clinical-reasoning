package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

class QuestionnaireResponseItemAnswerComponentAdapterTest {
    private final IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var library = new Library();
        assertThrows(
                IllegalArgumentException.class, () -> adapterFactory.createQuestionnaireResponseItemAnswer(library));
    }

    @Test
    void test() {
        var answer = new QuestionnaireResponseItemAnswerComponent();
        var adapter = new QuestionnaireResponseItemAnswerComponentAdapter(answer);
        assertNotNull(adapter);
        assertEquals(answer, adapter.get());
        assertEquals(FhirVersionEnum.R4, adapter.fhirVersion());
        assertNotNull(adapter.getModelResolver());
        assertNotNull(adapter.getAdapterFactory());
    }

    @Test
    void testValue() {
        var testValue = new StringType("test");
        var answer = new QuestionnaireResponseItemAnswerComponent();
        answer.setValue(testValue);
        var adapter = adapterFactory.createQuestionnaireResponseItemAnswer(answer);
        assertTrue(adapter.hasValue());
        assertEquals(testValue, adapter.getValue());
        var newValue = new IntegerType(1);
        adapter.setValue(newValue);
        assertEquals(newValue, adapter.getValue());
    }

    @Test
    void testItem() {
        var answer = new QuestionnaireResponseItemAnswerComponent();
        var item1 =
                adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent().setLinkId("1"));
        var item2 =
                adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent().setLinkId("2"));
        var item3 =
                adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent().setLinkId("3"));
        var item4 =
                adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent().setLinkId("4"));
        answer.addItem((QuestionnaireResponseItemComponent) item1.get());
        answer.addItem((QuestionnaireResponseItemComponent) item2.get());
        var adapter = adapterFactory.createQuestionnaireResponseItemAnswer(answer);
        assertTrue(adapter.hasItem());
        assertEquals(2, adapter.getItem().size());
        adapter.setItem(List.of(item1, item2, item3, item4));
        assertEquals(4, adapter.getItem().size());
    }
}
