package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
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
        assertEquals(FhirVersionEnum.R4, adapter.fhirVersion());
        assertNotNull(adapter.getAdapterFactory());
        adapter.setId("test");
        var canonical = "test.com/Questionnaire/test";
        adapter.setQuestionnaire(canonical);
        assertTrue(adapter.hasQuestionnaire());
        assertEquals(canonical, adapter.getQuestionnaire());
        assertEquals(canonical, adapter.getQuestionnaireCanonical().getValueAsString());
        var id = new IdType("test1");
        adapter.setSubject(id);
        assertTrue(adapter.hasSubject());
        assertEquals(id.getValue(), adapter.getSubject().getValue());
        var status = "in-progress";
        adapter.setStatus(status);
        assertEquals(status, adapter.getStatus());
        assertFalse(adapter.hasAuthored());
        assertNull(adapter.getAuthored());
        var newDate = new Date();
        adapter.setAuthored(newDate);
        assertTrue(adapter.hasAuthored());
        assertEquals(newDate, adapter.getAuthored());
        var basedOn = new Reference("basedOn");
        response.addBasedOn(basedOn);
        assertEquals(basedOn, adapter.getBasedOn().get(0));
        var partOf = new Reference("partOf");
        response.addPartOf(partOf);
        assertEquals(partOf, adapter.getPartOf().get(0));
        assertFalse(adapter.hasEncounter());
        var encounter = new Reference("encounter");
        adapter.setEncounter(encounter);
        assertTrue(adapter.hasEncounter());
        assertEquals(encounter, adapter.getEncounter());
        assertFalse(adapter.hasAuthor());
        var author = new Reference("author");
        response.setAuthor(author);
        assertTrue(adapter.hasAuthor());
        assertEquals(author, adapter.getAuthor());
    }

    @Test
    void testItem() {
        var questionnaireResponse = new QuestionnaireResponse();
        var item1 =
                adapterFactory.createQuestionnaireResponseItem(new QuestionnaireResponseItemComponent().setLinkId("1"));
        var item1_1 = adapterFactory.createQuestionnaireResponseItem(
                new QuestionnaireResponseItemComponent().setLinkId("1.1"));
        item1.addItem(item1_1);
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
        assertTrue(adapter.hasItem("1"));
        assertFalse(adapter.hasItem("5"));
        assertEquals(2, adapter.getItem().size());
        adapter.addItem(item3);
        assertEquals(3, adapter.getItem().size());
        adapter.setItem(List.of(item1));
        assertEquals(1, adapter.getItem().size());
        adapter.addItems(List.of(item2, item3, item4));
        assertEquals(4, adapter.getItem().size());
        assertEquals(1, adapter.getItem("1.1").size());
        assertEquals(item1_1.get(), adapter.getItem("1.1").get(0).get());
    }
}
