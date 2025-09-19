package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

class QuestionnaireResponseItemComponentAdapterTest {
    private final IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var library = new Library();
        assertThrows(IllegalArgumentException.class, () -> adapterFactory.createQuestionnaireResponseItem(library));
    }

    @Test
    void test() {
        var item = new QuestionnaireResponseItemComponent();
        var adapter = adapterFactory.createQuestionnaireResponseItem(item);
        assertNotNull(adapterFactory.createBase(item));
        assertNotNull(adapter);
        assertEquals(item, adapter.get());
        assertEquals(FhirVersionEnum.R4, adapter.fhirVersion());
        assertNotNull(adapter.getModelResolver());
        assertNotNull(adapter.getAdapterFactory());
        var linkId = "1";
        adapter.setLinkId(linkId);
        assertEquals(linkId, adapter.getLinkId());
        assertFalse(adapter.hasDefinition());
        var definition = "Observation.valueBoolean";
        adapter.setDefinition(definition);
        assertEquals(definition, adapter.getDefinition());
    }

    @Test
    void testAnswer() {
        var testValue = new StringType("test");
        var item = new QuestionnaireResponseItemComponent();
        item.addAnswer().setValue(testValue);
        var adapter = adapterFactory.createQuestionnaireResponseItem(item);
        assertTrue(adapter.hasAnswer());
        assertEquals(1, adapter.getAnswer().size());
        assertEquals(testValue, adapter.getAnswer().get(0).getValue());
        var newValue = new IntegerType(1);
        var newAnswer = adapter.newAnswer(newValue);
        adapter.setAnswer(List.of(newAnswer));
        assertEquals(1, adapter.getAnswer().size());
        assertEquals(newValue, adapter.getAnswer().get(0).getValue());
    }

    @Test
    void testItem() {
        var item = new QuestionnaireResponseItemComponent();
        var item1 =
                adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent().setLinkId("1"));
        var item2 =
                adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent().setLinkId("2"));
        var item3 =
                adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent().setLinkId("3"));
        var item4 =
                adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent().setLinkId("4"));
        item.addItem((QuestionnaireResponseItemComponent) item1.get());
        item.addItem((QuestionnaireResponseItemComponent) item2.get());
        var adapter = adapterFactory.createQuestionnaireResponseItem(item);
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
