package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

class QuestionnaireItemComponentAdapterTest {
    private final IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var library = new Library();
        assertThrows(IllegalArgumentException.class, () -> adapterFactory.createQuestionnaireItem(library));
    }

    @Test
    void test() {
        var text = "test";
        var item = new QuestionnaireItemComponent().addCode(new Coding().setCode(text));
        var adapter = adapterFactory.createQuestionnaireItem(item);
        assertNotNull(adapterFactory.createBase(item));
        assertEquals(item, adapter.get());
        assertEquals(FhirVersionEnum.DSTU3, adapter.fhirVersion());
        assertNotNull(adapter.getModelResolver());
        assertNotNull(adapter.getAdapterFactory());
        var linkId = "1";
        adapter.setLinkId(linkId);
        assertEquals(linkId, adapter.getLinkId());
        assertEquals(text, adapter.getCode().get(0).getCode());
        adapter.setText(text);
        assertEquals(text, adapter.getText());
        assertFalse(adapter.hasDefinition());
        var definition = "Observation.valueBoolean";
        adapter.setDefinition(definition);
        assertEquals(definition, adapter.getDefinition());
        adapter.setType("choice");
        assertFalse(adapter.isGroupItem());
        assertTrue(adapter.isChoiceItem());
        adapter.setRequired(true);
        assertTrue(adapter.getRequired());
        adapter.setRepeats(false);
        assertFalse(adapter.getRepeats());
        var optionValue = "option";
        adapter.addAnswerOption(new CodingAdapter(new Coding().setCode(optionValue)));
        assertEquals(
                optionValue,
                ((Coding) ((QuestionnaireItemComponent) adapter.get())
                                .getOption()
                                .get(0)
                                .getValue())
                        .getCode());
        assertFalse(adapter.hasInitial());
        assertEquals(0, adapter.getInitial().size());
        assertNotNull(adapter.newResponseItem());
        assertNull(adapter.newExpression("expression"));
    }

    @Test
    void testItem() {
        var item = new QuestionnaireItemComponent();
        var item1 = adapterFactory.createQuestionnaireItem(
                new QuestionnaireItemComponent().setLinkId("1").setType(QuestionnaireItemType.BOOLEAN));
        var item2 = adapterFactory.createQuestionnaireItem(
                new QuestionnaireItemComponent().setLinkId("2").setType(QuestionnaireItemType.BOOLEAN));
        var item3 = adapterFactory.createQuestionnaireItem(
                new QuestionnaireItemComponent().setLinkId("3").setType(QuestionnaireItemType.BOOLEAN));
        var item4 = adapterFactory.createQuestionnaireItem(
                new QuestionnaireItemComponent().setLinkId("4").setType(QuestionnaireItemType.BOOLEAN));
        item.addItem((QuestionnaireItemComponent) item1.get());
        item.addItem((QuestionnaireItemComponent) item2.get());
        var adapter = adapterFactory.createQuestionnaireItem(item);
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
