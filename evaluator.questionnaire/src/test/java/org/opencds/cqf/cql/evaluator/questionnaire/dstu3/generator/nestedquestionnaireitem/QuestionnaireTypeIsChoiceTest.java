package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.generator.nestedquestionnaireitem;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opencds.cqf.cql.evaluator.questionnaire.dstu3.helpers.TestingHelper.withElementDefinition;
import static org.opencds.cqf.cql.evaluator.questionnaire.dstu3.helpers.TestingHelper.withQuestionnaireItemComponent;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemOptionComponent;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetComposeComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.testng.Assert;

@ExtendWith(MockitoExtension.class)
class QuestionnaireTypeIsChoiceTest {
  final static String CODE_VALUE = "codeValue";
  final static String SYSTEM_VALUE = "systemValue";
  final static String DISPLAY_VALUE = "displayValue";
  final static String TYPE_CODE = "typeCode";
  final static String PATH_VALUE = "pathValue";
  final static String VALUE_SET_URL = "valueSetUrl";
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
    final QuestionnaireItemComponent actual =
        myFixture.addProperties(elementDefinition, questionnaireItem);
    // validate
    Assert.assertTrue(valueSet.hasExpansion());
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
    final QuestionnaireItemComponent actual =
        myFixture.addProperties(elementDefinition, questionnaireItem);
    // validate
    Assert.assertFalse(valueSet.hasExpansion());
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

  void assertQuestionnaireItemsGetAppendedFromExpansionComponent(ValueSet valueSet,
      QuestionnaireItemComponent questionnaireItem) {
    final List<ValueSetExpansionContainsComponent> expansion =
        valueSet.getExpansion().getContains();
    Assert.assertEquals(expansion.size(), questionnaireItem.getOption().size());
    for (int i = 0; i < expansion.size(); i++) {
      final ValueSetExpansionContainsComponent containsComponent = expansion.get(i);
      final QuestionnaireItemOptionComponent answerOptionComponent =
          questionnaireItem.getOption().get(i);
      final Coding coding = (Coding) answerOptionComponent.getValue();
      Assert.assertEquals(containsComponent.getCode(), coding.getCode());
      Assert.assertEquals(containsComponent.getSystem(), coding.getSystem());
      Assert.assertEquals(containsComponent.getDisplay(), coding.getDisplay());
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

  void assertQuestionnaireItemsGetAppendedFromConceptSets(ValueSet valueSet,
      QuestionnaireItemComponent questionnaireItem) {
    final List<ConceptSetComponent> conceptSets = valueSet.getCompose().getInclude();
    int totalItems = 0;
    for (ConceptSetComponent theConceptSet : conceptSets) {
      final String systemUri = theConceptSet.getSystem();
      final List<ConceptReferenceComponent> conceptReferenceComponents = theConceptSet.getConcept();
      for (ConceptReferenceComponent theConceptReferenceComponent : conceptReferenceComponents) {
        totalItems++;
        final QuestionnaireItemOptionComponent answerOptionComponent = questionnaireItem
            .getOption()
            .get(totalItems - 1);
        final Coding coding = (Coding) answerOptionComponent.getValue();
        Assert.assertEquals(theConceptReferenceComponent.getCode(), coding.getCode());
        Assert.assertEquals(theConceptReferenceComponent.getDisplay(), coding.getDisplay());
        Assert.assertEquals(systemUri, coding.getSystem());
      }
    }
    Assert.assertEquals(totalItems, questionnaireItem.getOption().size());
  }

  @Test
  void getCodingShouldReturnNewCodingGivenValueSetExpansion() {
    // execute
    final Coding actual = myFixture.getCoding(withBaseValueSetExpansion());
    // validate
    Assert.assertEquals(actual.getCode(), CODE_VALUE);
    Assert.assertEquals(actual.getSystem(), SYSTEM_VALUE);
    Assert.assertEquals(actual.getDisplay(), DISPLAY_VALUE);
  }

  @Test
  void getCodingShouldReturnNewCodingGivenConceptReferenceComponent() {
    // execute
    final Coding actual = myFixture.getCoding(withConceptReferenceComponent(), SYSTEM_VALUE);
    // validate
    Assert.assertEquals(actual.getCode(), CODE_VALUE);
    Assert.assertEquals(actual.getSystem(), SYSTEM_VALUE);
    Assert.assertEquals(actual.getDisplay(), DISPLAY_VALUE);
  }

  @Nonnull
  ElementDefinition withElementDefinitionWithBindingComponent() {
    final ElementDefinition element = withElementDefinition(TYPE_CODE, PATH_VALUE);
    final ElementDefinitionBindingComponent elementDefinitionBindingComponent =
        new ElementDefinitionBindingComponent();
    elementDefinitionBindingComponent.setValueSet(new UriType(VALUE_SET_URL));
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
    final ValueSetExpansionContainsComponent valueSetExpansion =
        new ValueSetExpansionContainsComponent();
    valueSetExpansion.setCode(CODE_VALUE);
    valueSetExpansion.setDisplay(DISPLAY_VALUE);
    valueSetExpansion.setSystem(SYSTEM_VALUE);
    return valueSetExpansion;
  }

  @Nonnull
  ValueSetExpansionContainsComponent withValueSetExpansion(int index) {
    final ValueSetExpansionContainsComponent valueSetExpansion =
        new ValueSetExpansionContainsComponent();
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
    final List<ValueSetExpansionContainsComponent> valueSetExpansionContainsComponentList =
        new ArrayList<>();
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
