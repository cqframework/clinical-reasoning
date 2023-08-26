package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.generator.questionnaireitem;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.annotation.Nonnull;

import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.StructureDefinition.StructureDefinitionDifferentialComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.generator.nestedquestionnaireitem.NestedQuestionnaireItemService;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.helpers.TestingHelper;
import org.opencds.cqf.fhir.api.Repository;

@ExtendWith(MockitoExtension.class)
class QuestionnaireItemGeneratorTest {
  final static String ERROR_MESSAGE = "Expected error message";
  final static String EXCEPTION_ERROR_PREFIX = "An error occurred during item creation";
  final static String EXPECTED_ERROR_MESSAGE = EXCEPTION_ERROR_PREFIX + ": " + ERROR_MESSAGE;
  final static String NO_PROFILE_ERROR = "No profile defined for input. Unable to generate item.";
  final static String QUESTIONNAIRE_TEXT = "Questionnaire Text";
  final static String CHILD_LINK_ID = "linkId.1";
  final static String LINK_ID = "linkId";
  final static String TYPE_CODE = "typeCode";
  final static String TYPE_CODE_2 = "typeCode2";
  final static String TYPE_CODE_3 = "typeCode3";
  final static String PATH_VALUE = "pathValue";
  final static String PATH_VALUE_2 = "pathValue2";
  final static String PATH_VALUE_3 = "pathValue3";
  final static QuestionnaireItemType QUESTIONNAIRE_ITEM_TYPE = QuestionnaireItemType.DISPLAY;
  @Mock
  private Repository repository;
  @Mock
  private NestedQuestionnaireItemService nestedQuestionnaireItemService;
  @Mock
  private QuestionnaireItemService questionnaireItemService;
  @Spy
  @InjectMocks
  private QuestionnaireItemGenerator myFixture;

  @BeforeEach
  void setUp() {
    myFixture.questionnaireItem = withQuestionnaireItemComponent();
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(nestedQuestionnaireItemService);
    verifyNoMoreInteractions(questionnaireItemService);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void createErrorItemShouldCreateErrorItem() {
    // setup
    final QuestionnaireItemComponent expected = withErrorItem(ERROR_MESSAGE);
    // execute
    final QuestionnaireItemComponent actual = myFixture.createErrorItem(LINK_ID, ERROR_MESSAGE);
    // validate
    assertEquals(actual, expected);
  }

  @Test
  void getElementTypeShouldReturnString() {
    // setup
    final ElementDefinition elementDefinition =
        TestingHelper.withElementDefinition(TYPE_CODE, PATH_VALUE);
    // execute
    final String actual = myFixture.getElementType(elementDefinition);
    // validate
    Assertions.assertEquals(actual, TYPE_CODE);
  }

  @Test
  void getElementTypeShouldReturnNullWhenNoElementTypeExists() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinitionWithNullType();
    // execute
    final String actual = myFixture.getElementType(elementDefinition);
    // validate
    Assertions.assertNull(actual);
  }

  @Test
  void getElementsWithNonNullElementTypeShouldReturnListOfElementDefinitions() {
    // setup
    final StructureDefinition profile = withProfile();
    final List<ElementDefinition> expected = List.of(
        TestingHelper.withElementDefinition(TYPE_CODE, PATH_VALUE),
        TestingHelper.withElementDefinition(TYPE_CODE_2, PATH_VALUE_2),
        TestingHelper.withElementDefinition(TYPE_CODE_3, PATH_VALUE_3));
    // execute
    final List<ElementDefinition> actual = myFixture.getElementsWithNonNullElementType(profile);
    // validate
    Assertions.assertEquals(actual.size(), expected.size());
    assertEquals(actual, expected);
  }

  @Test
  void processElementsShouldCallProcessElementForEveryElementWithNonNullElementType() {
    // setup
    final StructureDefinition profile = withProfile();
    final List<ElementDefinition> expectedElements = List.of(
        TestingHelper.withElementDefinition(TYPE_CODE, PATH_VALUE),
        TestingHelper.withElementDefinition(TYPE_CODE_2, PATH_VALUE_2),
        TestingHelper.withElementDefinition(TYPE_CODE_3, PATH_VALUE_3));
    doNothing().when(myFixture).processElement(
        any(StructureDefinition.class),
        any(ElementDefinition.class),
        anyInt());
    doReturn(expectedElements).when(myFixture).getElementsWithNonNullElementType(profile);
    // execute
    myFixture.processElements(profile);
    // validate
    for (int i = 0; i < expectedElements.size(); i++) {
      verify(myFixture).processElement(profile, expectedElements.get(i), i + 1);
    }
  }

  @Test
  void processElementShouldAddNestedQuestionnaireItem() throws Exception {
    // setup
    final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
    final StructureDefinition profile = withProfile();
    final ElementDefinition element = TestingHelper.withElementDefinition(TYPE_CODE, PATH_VALUE);
    final int childCount = 1;
    doReturn(questionnaireItem).when(nestedQuestionnaireItemService)
        .getNestedQuestionnaireItem(profile.getUrl(), element, CHILD_LINK_ID);
    // execute
    myFixture.processElement(profile, element, childCount);
    // validate
    verify(nestedQuestionnaireItemService).getNestedQuestionnaireItem(profile.getUrl(), element,
        CHILD_LINK_ID);
    assertEquals(myFixture.questionnaireItem.getItem().get(0), questionnaireItem);
  }

  @Test
  void processElementShouldAddErrorItemWhenExceptionThrown() throws Exception {
    // setup
    final QuestionnaireItemComponent errorItem = withErrorItem(EXPECTED_ERROR_MESSAGE);
    final StructureDefinition profile = withProfile();
    final ElementDefinition element = TestingHelper.withElementDefinition(TYPE_CODE, PATH_VALUE);
    final int childCount = 1;
    when(nestedQuestionnaireItemService.getNestedQuestionnaireItem(profile.getUrl(), element,
        CHILD_LINK_ID))
            .thenAnswer(invocation -> {
              throw new Exception(ERROR_MESSAGE);
            });
    doReturn(errorItem).when(myFixture).createErrorItem(CHILD_LINK_ID, EXPECTED_ERROR_MESSAGE);
    // execute
    myFixture.processElement(profile, element, childCount);
    // validate
    verify(myFixture).createErrorItem(CHILD_LINK_ID, EXPECTED_ERROR_MESSAGE);
    verify(nestedQuestionnaireItemService).getNestedQuestionnaireItem(profile.getUrl(), element,
        CHILD_LINK_ID);
    assertEquals(myFixture.questionnaireItem.getItem().get(0), errorItem);
  }

  @Test
  void generateItemShouldThrowArgumentIfNoProfileExists() {
    // setup
    final DataRequirement actionInput = withActionInputWithNoProfile();
    final int itemCount = 3;
    // execute
    IllegalArgumentException actual = assertThrows(
        IllegalArgumentException.class, () -> myFixture.generateItem(actionInput, itemCount));
    // validate
    Assertions.assertEquals(actual.getMessage(), NO_PROFILE_ERROR);
  }

  @Test
  void generateItemShouldProcessElements() {
    // setup
    final DataRequirement actionInput = TestingHelper.withActionInput();
    final QuestionnaireItemComponent expected = withQuestionnaireItemComponent();
    final StructureDefinition profile = withProfile();
    final int itemCount = 3;
    doReturn(profile).when(myFixture).getProfileDefinition(actionInput);
    doReturn(expected).when(questionnaireItemService).createQuestionnaireItem(actionInput, "4",
        profile);
    doNothing().when(myFixture).processElements(profile);
    // execute
    final QuestionnaireItemComponent actual = myFixture.generateItem(actionInput, itemCount);
    // validate
    verify(myFixture).getProfileDefinition(actionInput);
    verify(questionnaireItemService).createQuestionnaireItem(actionInput, "4", profile);
    verify(myFixture).processElements(profile);
    assertEquals(actual, expected);
  }

  @Test
  void generateItemShouldReturnErrorItemIfExceptionThrown() {
    // setup
    final int itemCount = 3;
    final String linkId = "4";
    final DataRequirement actionInput = TestingHelper.withActionInput();
    final QuestionnaireItemComponent expected = withErrorItem(EXPECTED_ERROR_MESSAGE, linkId);
    final StructureDefinition profile = withProfile();
    doReturn(expected).when(questionnaireItemService).createQuestionnaireItem(actionInput, linkId,
        profile);
    doReturn(profile).when(myFixture).getProfileDefinition(actionInput);
    doAnswer((invocation) -> {
      throw new Exception(ERROR_MESSAGE);
    }).when(myFixture).processElements(profile);
    // execute
    final QuestionnaireItemComponent actual = myFixture.generateItem(actionInput, itemCount);
    // validate
    verify(myFixture).getProfileDefinition(actionInput);
    assertEquals(actual, expected);
  }

  // @Test
  // void getProfileDefinitionShouldReturnResource() {
  // // setup
  // final Resource expected = withStructureDefinitionAsResource();
  // final CanonicalType canonicalType = new CanonicalType();
  // final DataRequirement actionInput = new DataRequirement();
  // actionInput.setProfile(List.of(canonicalType));
  // doReturn(expected).when(myFixture).getResource(canonicalType);
  // // execute
  // final Resource actual = myFixture.getProfileDefinition(actionInput);
  // // validate
  // Assertions.assertEquals(actual, expected);
  // verify(myFixture).getResource(canonicalType);
  // }

  final Resource withStructureDefinitionAsResource() {
    final StructureDefinition structureDefinition = new StructureDefinition();
    structureDefinition.setId("id");
    structureDefinition.setUrl("url");
    return structureDefinition;
  }

  void assertEquals(QuestionnaireItemComponent actual, QuestionnaireItemComponent expected) {
    Assertions.assertEquals(actual.getType(), expected.getType());
    Assertions.assertEquals(actual.getLinkId(), expected.getLinkId());
    Assertions.assertEquals(actual.getText(), expected.getText());
  }

  void assertEquals(List<ElementDefinition> actual, List<ElementDefinition> expected) {
    for (int i = 0; i < expected.size(); i++) {
      final ElementDefinition actualElement = actual.get(i);
      final ElementDefinition expectedElement = expected.get(i);
      Assertions.assertEquals(actualElement.getPath(), expectedElement.getPath());
      Assertions.assertEquals(actualElement.getType().size(), expectedElement.getType().size());
      for (int q = 0; q < expectedElement.getType().size(); q++) {
        final TypeRefComponent actualType = actualElement.getType().get(q);
        final TypeRefComponent expectedType = expectedElement.getType().get(q);
        Assertions.assertEquals(actualType.getCode(), expectedType.getCode());
      }
    }
  }

  @Nonnull
  DataRequirement withActionInputWithNoProfile() {
    return new DataRequirement();
  }

  @Nonnull
  QuestionnaireItemComponent withQuestionnaireItemComponent() {
    return new QuestionnaireItemComponent().setType(QUESTIONNAIRE_ITEM_TYPE).setLinkId(LINK_ID)
        .setText(QUESTIONNAIRE_TEXT);
  }

  @Nonnull
  QuestionnaireItemComponent withErrorItem(String errorMessage) {
    return new QuestionnaireItemComponent().setLinkId(LINK_ID)
        .setType(QuestionnaireItemType.DISPLAY).setText(errorMessage);
  }

  @Nonnull
  QuestionnaireItemComponent withErrorItem(String errorMessage, String linkId) {
    return new QuestionnaireItemComponent().setLinkId(linkId).setType(QuestionnaireItemType.DISPLAY)
        .setText(errorMessage);
  }

  @Nonnull
  ElementDefinition withElementDefinitionWithNullType() {
    return new ElementDefinition();
  }

  @Nonnull
  StructureDefinition withProfile() {
    final StructureDefinition profile = new StructureDefinition();
    final ElementDefinition elementDefinition1 =
        TestingHelper.withElementDefinition(TYPE_CODE, PATH_VALUE);
    final ElementDefinition elementDefinition2 =
        TestingHelper.withElementDefinition(TYPE_CODE_2, PATH_VALUE_2);
    final ElementDefinition elementDefinition3 =
        TestingHelper.withElementDefinition(TYPE_CODE_3, PATH_VALUE_3);
    final ElementDefinition elementDefinition4 = withElementDefinitionWithNullType();
    final StructureDefinitionDifferentialComponent differential =
        new StructureDefinitionDifferentialComponent();
    differential.addElement(elementDefinition1);
    differential.addElement(elementDefinition2);
    differential.addElement(elementDefinition3);
    differential.addElement(elementDefinition4);
    profile.setDifferential(differential);
    return profile;
  }
}
