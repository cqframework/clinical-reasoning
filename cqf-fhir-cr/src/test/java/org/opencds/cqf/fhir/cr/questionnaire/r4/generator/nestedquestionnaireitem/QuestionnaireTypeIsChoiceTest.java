package org.opencds.cqf.fhir.cr.questionnaire.r4.generator.nestedquestionnaireitem;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opencds.cqf.fhir.cr.questionnaire.r4.helpers.TestingHelper.withElementDefinition;
import static org.opencds.cqf.fhir.cr.questionnaire.r4.helpers.TestingHelper.withQuestionnaireItemComponent;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemAnswerOptionComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetComposeComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;

@ExtendWith(MockitoExtension.class)
class QuestionnaireTypeIsChoiceTest {
    static final String CODE_VALUE = "codeValue";
    static final String SYSTEM_VALUE = "systemValue";
    static final String DISPLAY_VALUE = "displayValue";
    static final String TYPE_CODE = "typeCode";
    static final String PATH_VALUE = "pathValue";
    static final String VALUE_SET_URL = "valueSetUrl";

    @Mock
    protected Repository repository;

    @Spy
    @InjectMocks
    private QuestionnaireTypeIsChoice myFixture;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(repository);
    }

    @Test
    void addPropertiesShouldAddPropertiesIfValueSetHasExpansion() throws Exception {
        // setup
        final ElementDefinition elementDefinition = withElementDefinitionWithBindingComponent();
        final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
        final ValueSet valueSet = withValueSetWithExpansionComponent();
        doReturn(valueSet).when(myFixture).getValueSet(elementDefinition);
        // execute
        final QuestionnaireItemComponent actual = myFixture.addProperties(elementDefinition, questionnaireItem);
        // validate
        Assertions.assertTrue(valueSet.hasExpansion());
        verify(myFixture).getValueSet(elementDefinition);
        assertQuestionnaireItemsGetAppendedFromExpansionComponent(valueSet, actual);
    }

    @Test
    void addPropertiesShouldAddPropertiesIfValueSetDoesNotHaveExpansion() throws Exception {
        // setup
        final ElementDefinition elementDefinition = withElementDefinitionWithBindingComponent();
        final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
        final ValueSet valueSet = withValueSetWithComposeComponent();
        doReturn(valueSet).when(myFixture).getValueSet(elementDefinition);
        // execute
        final QuestionnaireItemComponent actual = myFixture.addProperties(elementDefinition, questionnaireItem);
        // validate
        Assertions.assertFalse(valueSet.hasExpansion());
        verify(myFixture).getValueSet(elementDefinition);
        assertQuestionnaireItemsGetAppendedFromConceptSets(valueSet, actual);
    }

    @Test
    void addAnswerOptionsForValueSetWithExpansionComponentShouldAddAnswerOptions() {
        // setup
        final ValueSet valueSet = withValueSetWithExpansionComponent();
        final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
        // execute
        myFixture.addAnswerOptionsForValueSetWithExpansionComponent(valueSet, questionnaireItem);
        // validate
        assertQuestionnaireItemsGetAppendedFromExpansionComponent(valueSet, questionnaireItem);
    }

    void assertQuestionnaireItemsGetAppendedFromExpansionComponent(
            ValueSet valueSet, QuestionnaireItemComponent questionnaireItem) {
        final List<ValueSetExpansionContainsComponent> expansion =
                valueSet.getExpansion().getContains();
        Assertions.assertEquals(
                expansion.size(), questionnaireItem.getAnswerOption().size());
        for (int i = 0; i < expansion.size(); i++) {
            final ValueSetExpansionContainsComponent containsComponent = expansion.get(i);
            final QuestionnaireItemAnswerOptionComponent answerOptionComponent =
                    questionnaireItem.getAnswerOption().get(i);
            final Coding coding = (Coding) answerOptionComponent.getValue();
            Assertions.assertEquals(containsComponent.getCode(), coding.getCode());
            Assertions.assertEquals(containsComponent.getSystem(), coding.getSystem());
            Assertions.assertEquals(containsComponent.getDisplay(), coding.getDisplay());
        }
    }

    @Test
    void addAnswerOptionsForValueSetWithComposeComponentShouldAddAnswerOptions() {
        // setup
        final ValueSet valueSet = withValueSetWithComposeComponent();
        final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
        // execute
        myFixture.addAnswerOptionsForValueSetWithComposeComponent(valueSet, questionnaireItem);
        // validate
        assertQuestionnaireItemsGetAppendedFromConceptSets(valueSet, questionnaireItem);
    }

    void assertQuestionnaireItemsGetAppendedFromConceptSets(
            ValueSet valueSet, QuestionnaireItemComponent questionnaireItem) {
        final List<ConceptSetComponent> conceptSets = valueSet.getCompose().getInclude();
        int totalItems = 0;
        for (ConceptSetComponent theConceptSet : conceptSets) {
            final String systemUri = theConceptSet.getSystem();
            final List<ConceptReferenceComponent> conceptReferenceComponents = theConceptSet.getConcept();
            for (ConceptReferenceComponent theConceptReferenceComponent : conceptReferenceComponents) {
                totalItems++;
                final QuestionnaireItemAnswerOptionComponent answerOptionComponent =
                        questionnaireItem.getAnswerOption().get(totalItems - 1);
                final Coding coding = (Coding) answerOptionComponent.getValue();
                Assertions.assertEquals(theConceptReferenceComponent.getCode(), coding.getCode());
                Assertions.assertEquals(theConceptReferenceComponent.getDisplay(), coding.getDisplay());
                Assertions.assertEquals(systemUri, coding.getSystem());
            }
        }
        Assertions.assertEquals(totalItems, questionnaireItem.getAnswerOption().size());
    }

    @Test
    void getCodingShouldReturnNewCodingGivenValueSetExpansion() {
        // execute
        final Coding actual = myFixture.getCoding(withBaseValueSetExpansion());
        // validate
        Assertions.assertEquals(actual.getCode(), CODE_VALUE);
        Assertions.assertEquals(actual.getSystem(), SYSTEM_VALUE);
        Assertions.assertEquals(actual.getDisplay(), DISPLAY_VALUE);
    }

    @Test
    void getCodingShouldReturnNewCodingGivenConceptReferenceComponent() {
        // execute
        final Coding actual = myFixture.getCoding(withConceptReferenceComponent(), SYSTEM_VALUE);
        // validate
        Assertions.assertEquals(actual.getCode(), CODE_VALUE);
        Assertions.assertEquals(actual.getSystem(), SYSTEM_VALUE);
        Assertions.assertEquals(actual.getDisplay(), DISPLAY_VALUE);
    }

    @Nonnull
    ElementDefinition withElementDefinitionWithBindingComponent() {
        final ElementDefinition element = withElementDefinition(TYPE_CODE, PATH_VALUE);
        final ElementDefinitionBindingComponent elementDefinitionBindingComponent =
                new ElementDefinitionBindingComponent();
        elementDefinitionBindingComponent.setValueSet(VALUE_SET_URL);
        element.setBinding(elementDefinitionBindingComponent);
        return element;
    }

    @Nonnull
    ConceptReferenceComponent withConceptReferenceComponent() {
        final ConceptReferenceComponent conceptReferenceComponent = new ConceptReferenceComponent();
        conceptReferenceComponent.setCode(CODE_VALUE);
        conceptReferenceComponent.setDisplay(DISPLAY_VALUE);
        return conceptReferenceComponent;
    }

    @Nonnull
    ValueSetExpansionContainsComponent withBaseValueSetExpansion() {
        final ValueSetExpansionContainsComponent valueSetExpansion = new ValueSetExpansionContainsComponent();
        valueSetExpansion.setCode(CODE_VALUE);
        valueSetExpansion.setDisplay(DISPLAY_VALUE);
        valueSetExpansion.setSystem(SYSTEM_VALUE);
        return valueSetExpansion;
    }

    @Nonnull
    ValueSetExpansionContainsComponent withValueSetExpansion(int index) {
        final ValueSetExpansionContainsComponent valueSetExpansion = new ValueSetExpansionContainsComponent();
        valueSetExpansion.setCode(withCode(index));
        valueSetExpansion.setDisplay(withDisplay(index));
        valueSetExpansion.setSystem(withSystem(index));
        return valueSetExpansion;
    }

    @Nonnull
    ConceptSetComponent withConceptSetComponent() {
        final ConceptSetComponent conceptSetComponent = new ConceptSetComponent();
        final List<ConceptReferenceComponent> conceptReferenceComponents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final ConceptReferenceComponent conceptReferenceComponent = new ConceptReferenceComponent();
            conceptReferenceComponent.setDisplay(withDisplay(i));
            conceptReferenceComponent.setCode(withCode(i));
            conceptReferenceComponents.add(conceptReferenceComponent);
        }
        conceptSetComponent.setConcept(conceptReferenceComponents);
        return conceptSetComponent;
    }

    @Nonnull
    ValueSet withValueSetWithExpansionComponent() {
        final ValueSet valueSet = new ValueSet();
        final ValueSetExpansionComponent valueSetExpansionComponent = new ValueSetExpansionComponent();
        final List<ValueSetExpansionContainsComponent> valueSetExpansionContainsComponentList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            valueSetExpansionContainsComponentList.add(withValueSetExpansion(i));
        }
        valueSetExpansionComponent.setContains(valueSetExpansionContainsComponentList);
        valueSet.setExpansion(valueSetExpansionComponent);
        return valueSet;
    }

    @Nonnull
    ValueSet withValueSetWithComposeComponent() {
        final ValueSet valueSet = new ValueSet();
        final ValueSetComposeComponent valueSetComposeComponent = new ValueSetComposeComponent();
        final List<ConceptSetComponent> conceptSetComponents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final ConceptSetComponent conceptSetComponent = withConceptSetComponent();
            conceptSetComponents.add(conceptSetComponent);
        }
        valueSetComposeComponent.setInclude(conceptSetComponents);
        valueSet.setCompose(valueSetComposeComponent);
        return valueSet;
    }

    String withCode(int index) {
        return CODE_VALUE + "_" + index;
    }

    String withDisplay(int index) {
        return DISPLAY_VALUE + "_" + index;
    }

    String withSystem(int index) {
        return SYSTEM_VALUE + "_" + index;
    }
}
