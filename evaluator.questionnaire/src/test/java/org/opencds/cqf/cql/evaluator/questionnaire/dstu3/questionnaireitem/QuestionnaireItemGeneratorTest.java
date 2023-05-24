package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.questionnaireitem;

import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.StructureDefinition.StructureDefinitionDifferentialComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.Mockito;
import org.mockito.Spy;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.nestedquestionnaireitem.NestedQuestionnaireItemService;
import org.testng.Assert;
import javax.annotation.Nonnull;
import java.util.List;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class QuestionnaireItemGeneratorTest {
  final static String ERROR_MESSAGE = "Expected error message";
  final static String LINK_ID = "linkId";
  final static String TYPE_CODE = "typeCode";
  final static String TYPE_CODE_2 = "typeCode2";
  final static String TYPE_CODE_3 = "typeCode3";
  final static String PATH_VALUE = "pathValue";
  final static String PATH_VALUE_2 = "pathValue2";
  final static String PATH_VALUE_3 = "pathValue3";
  final static QuestionnaireItemType QUESTIONNAIRE_ITEM_TYPE = QuestionnaireItemType.DISPLAY;
  @Mock
  private NestedQuestionnaireItemService nestedQuestionnaireItemService;
  @Spy
  @InjectMocks
  private QuestionnaireItemGenerator myFixture;

  @BeforeEach
  void setUp() {
    myFixture = new QuestionnaireItemGenerator();
    myFixture.nestedQuestionnaireItemService = new NestedQuestionnaireItemService();
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(nestedQuestionnaireItemService);
  }

  @Test
  void createErrorItemShouldCreateErrorItem() {
    // setup
    final QuestionnaireItemComponent expected = withQuestionnaireItemComponent();
    // execute
    final QuestionnaireItemComponent actual = myFixture.createErrorItem(LINK_ID, ERROR_MESSAGE);
    // validate
    assertEquals(actual, expected);
  }

  @Test
  void getElementTypeShouldReturnString() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition();
    // execute
    final String actual = myFixture.getElementType(elementDefinition);
    // validate
    Assert.assertEquals(actual, TYPE_CODE);
  }

  @Test
  void getElementTypeShouldReturnNullWhenNoElementTypeExists() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinitionWithNullType();
    // execute
    final String actual = myFixture.getElementType(elementDefinition);
    // validate
    Assert.assertNull(actual);
  }

  @Test
  void getElementsWithNonNullElementTypeShouldReturnListOfElementDefinitions() {
    // setup
    final StructureDefinition profile = withProfile();
    final List<ElementDefinition> expected = List.of(withElementDefinition(), withElementDefinition2());
    // execute
    final List<ElementDefinition> actual = myFixture.getElementsWithNonNullElementType(profile);
    // validate
    Assert.assertEquals(actual.size(), expected.size());
    assertEquals(actual, expected);
  }

//  @Test
//  void processElementsShouldCallProcessElementForEveryElementWithNonNullElementType() {
//    // setup
//    myFixture.questionnaireItem = withQuestionnaireItemComponent();
//    final StructureDefinition profile = withProfile();
//    final List<ElementDefinition> expectedElements = List.of(
//        withElementDefinition(),
//        withElementDefinition2(),
//        withElementDefinition3()
//    );
//    // execute
//    myFixture.processElements(profile);
//    // validate
//    for (int i = 0; i < expectedElements.size(); i++) {
//      verify(myFixture).processElement(profile, expectedElements.get(i), i);
//    }
//  }
//
//  @Test
//  void processElementShouldAddNestedQuestionnaireItem() {
//
//  }
//
//  @Test
//  void processElementShouldProcessElement() {
//
//  }

  void assertEquals(QuestionnaireItemComponent actual, QuestionnaireItemComponent expected) {
    Assert.assertEquals(actual.getType(), expected.getType());
    Assert.assertEquals(actual.getLinkId(), expected.getLinkId());
    Assert.assertEquals(actual.getText(), expected.getText());
  }

  void assertEquals(List<ElementDefinition> actual, List<ElementDefinition> expected) {
    for(int i = 0; i < expected.size(); i++) {
      final ElementDefinition actualElement = actual.get(i);
      final ElementDefinition expectedElement = expected.get(i);
      Assert.assertEquals(actualElement.getPath(), expectedElement.getPath());
      Assert.assertEquals(actualElement.getType().size(), expectedElement.getType().size());
      for(int q = 0; q < expectedElement.getType().size(); q++) {
        final TypeRefComponent actualType = actualElement.getType().get(q);
        final TypeRefComponent expectedType = expectedElement.getType().get(q);
        Assert.assertEquals(actualType.getCode(), expectedType.getCode());
      }
    }
  }

  @Nonnull
  QuestionnaireItemComponent withQuestionnaireItemComponent() {
    return new QuestionnaireItemComponent().setType(QUESTIONNAIRE_ITEM_TYPE).setLinkId(LINK_ID).setText(ERROR_MESSAGE);
  }

  @Nonnull
  ElementDefinition withElementDefinition() {
    final ElementDefinition elementDefinition = new ElementDefinition().setPath(PATH_VALUE);
    final TypeRefComponent type = new TypeRefComponent();
    type.setCode(TYPE_CODE);
    elementDefinition.setType(List.of(type));
    return elementDefinition;
  }

  @Nonnull
  ElementDefinition withElementDefinition2() {
    final ElementDefinition elementDefinition = new ElementDefinition().setPath(PATH_VALUE_2);
    final TypeRefComponent type = new TypeRefComponent();
    type.setCode(TYPE_CODE_2);
    elementDefinition.setType(List.of(type));
    return elementDefinition;
  }

  @Nonnull
  ElementDefinition withElementDefinition3() {
    final ElementDefinition elementDefinition = new ElementDefinition().setPath(PATH_VALUE_3);
    final TypeRefComponent type = new TypeRefComponent();
    type.setCode(TYPE_CODE_3);
    elementDefinition.setType(List.of(type));
    return elementDefinition;
  }

  @Nonnull
  ElementDefinition withElementDefinitionWithNullType() {
    return new ElementDefinition();
  }

  @Nonnull
  StructureDefinition withProfile() {
    final StructureDefinition profile = new StructureDefinition();
    final ElementDefinition elementDefinition1 = withElementDefinition();
    final ElementDefinition elementDefinition2 = withElementDefinition2();
    final ElementDefinition elementDefinition3 = withElementDefinitionWithNullType();
    final ElementDefinition elementDefinition4 = withElementDefinition3();
    final StructureDefinitionDifferentialComponent differential = new StructureDefinitionDifferentialComponent();
    differential.addElement(elementDefinition1);
    differential.addElement(elementDefinition2);
    differential.addElement(elementDefinition3);
    differential.addElement(elementDefinition4);
    profile.setDifferential(differential);
    return profile;
  }
}
