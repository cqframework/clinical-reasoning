package org.opencds.cqf.cql.evaluator.questionnaire.r4.generator.nestedquestionnaireitem;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opencds.cqf.cql.evaluator.questionnaire.r4.helpers.TestingHelper.withElementDefinition;
import static org.opencds.cqf.cql.evaluator.questionnaire.r4.helpers.TestingHelper.withQuestionnaireItemComponent;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Constants;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
class NestedQuestionnaireItemServiceTest {
  final static String TYPE_CODE = "CodeableConcept";
  final static String PATH_VALUE = "pathValue";
  final static String PROFILE_URL = "http://www.sample.com/profile/profileId";
  final static String SHORT_DEFINITION = "shortDefinition";
  final static String LABEL = "label";
  final static String CHILD_LINK_ID = "childLinkId";
  final static FhirContext fhirContext = FhirContext.forR4Cached();
  final static IParser parser = fhirContext.newJsonParser();
  @Mock
  protected Repository repository;
  @Mock
  protected QuestionnaireTypeIsChoice questionnaireTypeIsChoice;
  @Mock
  protected ElementHasDefaultValue elementHasDefaultValue;
  @Mock
  protected ElementHasCqfExpression elementHasCqfExpression;
  @InjectMocks
  @Spy
  private NestedQuestionnaireItemService myFixture;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(questionnaireTypeIsChoice);
    verifyNoMoreInteractions(elementHasDefaultValue);
    verifyNoMoreInteractions(elementHasCqfExpression);
    verifyNoMoreInteractions(elementHasCqfExpression);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void getNestedQuestionnaireItemShouldReturnQuestionnaireItemComponentIfItemTypeIsChoice()
      throws Exception {
    // setup
    final QuestionnaireItemComponent initialized = withQuestionnaireItemComponent();
    final QuestionnaireItemComponent expected = withQuestionnaireItemComponent();
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    doReturn(initialized).when(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.CHOICE,
        PROFILE_URL,
        elementDefinition,
        CHILD_LINK_ID);
    doReturn(expected).when(questionnaireTypeIsChoice).addProperties(elementDefinition,
        initialized);
    // execute
    final QuestionnaireItemComponent actual =
        myFixture.getNestedQuestionnaireItem(PROFILE_URL, elementDefinition, CHILD_LINK_ID, null);
    // validate
    verify(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.CHOICE,
        PROFILE_URL,
        elementDefinition,
        CHILD_LINK_ID);
    verify(questionnaireTypeIsChoice).addProperties(elementDefinition, initialized);
    var actualQuestionnaire = new Questionnaire().setItem(Collections.singletonList(actual));
    var expectedQuestionnaire = new Questionnaire().setItem(Collections.singletonList(expected));
    Assertions.assertEquals(parser.encodeResourceToString(actualQuestionnaire),
        parser.encodeResourceToString(expectedQuestionnaire));
  }

  @Test
  void getNestedQuestionnaireItemShouldReturnQuestionnaireItemComponentIfElementIsFixedOrHasPattern()
      throws Exception {
    // setup
    final QuestionnaireItemComponent initialized = withQuestionnaireItemComponent();
    final QuestionnaireItemComponent expected = withQuestionnaireItemComponent();
    final ElementDefinition elementDefinition = withElementDefinitionWithFixedType();
    doReturn(initialized).when(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.BOOLEAN,
        PROFILE_URL,
        elementDefinition,
        CHILD_LINK_ID);
    doReturn(expected).when(elementHasDefaultValue).addProperties(
        elementDefinition.getFixedOrPattern(),
        initialized);
    // execute
    final QuestionnaireItemComponent actual =
        myFixture.getNestedQuestionnaireItem(PROFILE_URL, elementDefinition, CHILD_LINK_ID, null);
    // validate
    verify(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.BOOLEAN,
        PROFILE_URL,
        elementDefinition,
        CHILD_LINK_ID);
    verify(elementHasDefaultValue).addProperties(elementDefinition.getFixedOrPattern(),
        initialized);
    var actualQuestionnaire = new Questionnaire().setItem(Collections.singletonList(actual));
    var expectedQuestionnaire = new Questionnaire().setItem(Collections.singletonList(expected));
    Assertions.assertEquals(parser.encodeResourceToString(actualQuestionnaire),
        parser.encodeResourceToString(expectedQuestionnaire));
  }

  @Nonnull
  ElementDefinition withElementDefinitionWithFixedType() {
    final ElementDefinition elementDefinition = withElementDefinition("boolean", PATH_VALUE);
    final BooleanType booleanType = new BooleanType(true);
    elementDefinition.setFixed(booleanType);
    return elementDefinition;
  }

  @Test
  void getNestedQuestionnaireItemShouldReturnQuestionnaireItemComponentIfElementHasDefaultValue()
      throws Exception {
    // setup
    final BooleanType defaultValue = new BooleanType(true);
    final QuestionnaireItemComponent initialized = withQuestionnaireItemComponent();
    final QuestionnaireItemComponent expected = withQuestionnaireItemComponent();
    final ElementDefinition elementDefinition = withElementDefinitionWithDefaultValue(defaultValue);
    doReturn(initialized).when(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.BOOLEAN,
        PROFILE_URL,
        elementDefinition,
        CHILD_LINK_ID);
    doReturn(expected).when(elementHasDefaultValue).addProperties(
        elementDefinition.getDefaultValue(),
        initialized);
    // execute
    final QuestionnaireItemComponent actual =
        myFixture.getNestedQuestionnaireItem(PROFILE_URL, elementDefinition, CHILD_LINK_ID, null);
    // validate
    verify(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.BOOLEAN,
        PROFILE_URL,
        elementDefinition,
        CHILD_LINK_ID);
    verify(elementHasDefaultValue).addProperties(elementDefinition.getDefaultValue(),
        initialized);
    var actualQuestionnaire = new Questionnaire().setItem(Collections.singletonList(actual));
    var expectedQuestionnaire = new Questionnaire().setItem(Collections.singletonList(expected));
    Assertions.assertEquals(parser.encodeResourceToString(actualQuestionnaire),
        parser.encodeResourceToString(expectedQuestionnaire));
  }

  @Nonnull
  ElementDefinition withElementDefinitionWithDefaultValue(Type defaultValue) {
    final ElementDefinition elementDefinition = withElementDefinition("boolean", PATH_VALUE);
    elementDefinition.setDefaultValue(defaultValue);
    return elementDefinition;
  }

  @Test
  void getNestedQuestionnaireItemShouldReturnQuestionnaireItemComponentIfElementHasExtension()
      throws Exception {
    // setup
    final QuestionnaireItemComponent initialized = withQuestionnaireItemComponent();
    final QuestionnaireItemComponent expected = withQuestionnaireItemComponent();
    final ElementDefinition elementDefinition = withElementDefinitionWithCqfExtension();
    doReturn(initialized).when(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.REFERENCE,
        PROFILE_URL,
        elementDefinition,
        CHILD_LINK_ID);
    doReturn(expected).when(elementHasCqfExpression).addProperties(elementDefinition, initialized);
    // execute
    final QuestionnaireItemComponent actual =
        myFixture.getNestedQuestionnaireItem(PROFILE_URL, elementDefinition, CHILD_LINK_ID, null);
    // validate
    verify(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.REFERENCE,
        PROFILE_URL,
        elementDefinition,
        CHILD_LINK_ID);
    verify(elementHasCqfExpression).addProperties(elementDefinition, initialized);
    var actualQuestionnaire = new Questionnaire().setItem(Collections.singletonList(actual));
    var expectedQuestionnaire = new Questionnaire().setItem(Collections.singletonList(expected));
    Assertions.assertEquals(parser.encodeResourceToString(actualQuestionnaire),
        parser.encodeResourceToString(expectedQuestionnaire));
  }

  @Nonnull
  ElementDefinition withElementDefinitionWithCqfExtension() {
    final ElementDefinition elementDefinition = withElementDefinition("Reference", PATH_VALUE);
    final Extension extension = new Extension(Constants.CQF_EXPRESSION);
    elementDefinition.setExtension(List.of(extension));
    return elementDefinition;
  }

  @Test
  void initializeQuestionnaireItemShouldReturnNewQuestionnaireItemComponent() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    elementDefinition.setLabel(LABEL);
    final QuestionnaireItemComponent actual = myFixture.initializeQuestionnaireItem(
        QuestionnaireItemType.GROUP,
        PROFILE_URL,
        elementDefinition,
        CHILD_LINK_ID);
    // validate
    Assertions.assertEquals(actual.getType(), QuestionnaireItemType.GROUP);
    Assertions.assertEquals(actual.getDefinition(),
        "http://www.sample.com/profile/profileId#pathValue");
    Assertions.assertEquals(actual.getLinkId(), CHILD_LINK_ID);
    Assertions.assertEquals(actual.getText(), LABEL);
  }

  @Test
  void getItemTypeShouldReturnQuestionnaireItemType() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    // execute
    final QuestionnaireItemType actual = myFixture.getItemType(elementDefinition);
    // validate
    Assertions.assertEquals(actual, QuestionnaireItemType.CHOICE);
  }

  @Test
  void getItemTypeShouldThrowExceptionIfItemTypeIsNull() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition("", PATH_VALUE);
    // execute
    final IllegalArgumentException actual = Assertions.assertThrows(
        IllegalArgumentException.class, () -> myFixture.getItemType(elementDefinition));
    // validate
    Assertions.assertEquals(actual.getMessage(), "Unable to determine type for element: null");
  }

  @Test
  void parseItemTypeShouldReturnQuestionnaireItemTypeIfItemHasBinding() {
    // setup
    final String elementType = "elementType";
    // execute
    final QuestionnaireItemType actual = myFixture.parseItemType(elementType, true);
    // validate
    Assertions.assertEquals(actual, QuestionnaireItemType.CHOICE);
  }

  @Test
  void parseItemTypeShouldReturnQuestionnaireItemTypeGivenCodeableConceptString() {
    // setup
    final String elementType = "CodeableConcept";
    // execute
    final QuestionnaireItemType actual = myFixture.parseItemType(elementType, false);
    // validate
    Assertions.assertEquals(actual, QuestionnaireItemType.CHOICE);
  }

  @Test
  void parseItemTypeShouldReturnQuestionnaireItemTypeGivenQuantityString() {
    // setup
    final String elementType = "Quantity";
    // execute
    final QuestionnaireItemType actual = myFixture.parseItemType(elementType, false);
    // validate
    Assertions.assertEquals(actual, QuestionnaireItemType.QUANTITY);
  }

  @Test
  void parseItemTypeShouldReturnQuestionnaireItemTypeGivenReferenceString() {
    // setup
    final String elementType = "Reference";
    // execute
    final QuestionnaireItemType actual = myFixture.parseItemType(elementType, false);
    // validate
    Assertions.assertEquals(actual, QuestionnaireItemType.REFERENCE);
  }

  @Test
  void parseItemTypeShouldReturnQuestionnaireItemTypeGivenUriString() {
    // setup
    final String elementType = "uri";
    // execute
    final QuestionnaireItemType actual = myFixture.parseItemType(elementType, false);
    // validate
    Assertions.assertEquals(actual, QuestionnaireItemType.URL);
  }

  @Test
  void parseItemTypeShouldReturnQuestionnaireItemTypeGivenBackboneElementString() {
    // setup
    final String elementType = "BackboneElement";
    // execute
    final QuestionnaireItemType actual = myFixture.parseItemType(elementType, false);
    // validate
    Assertions.assertEquals(actual, QuestionnaireItemType.GROUP);
  }

  @Test
  void parseItemTypeShouldThrowErrorIfUnknownElementType() {
    // setup
    final String elementType = "UnknownElementType";
    // execute
    final FHIRException actual = Assertions.assertThrows(
        FHIRException.class, () -> myFixture.parseItemType(elementType, false));
    // validate
    Assertions.assertEquals(actual.getMessage(),
        "Unknown QuestionnaireItemType code 'UnknownElementType'");
  }

  @Test
  void parseItemTypeShouldReturnReturnNull() {
    // setup
    final String elementType = "BackboneElement";
    // execute
    final QuestionnaireItemType actual = myFixture.parseItemType(elementType, false);
    // validate
    Assertions.assertEquals(actual, QuestionnaireItemType.GROUP);
  }

  @Test
  void getElementTextShouldReturnElementLabel() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    elementDefinition.setLabel(LABEL);
    // execute
    final String actual = myFixture.getElementText(elementDefinition);
    // validate
    Assertions.assertEquals(actual, LABEL);
  }

  @Test
  void getElementTextShouldReturnElementDescription() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    elementDefinition.setShort(SHORT_DEFINITION);
    // execute
    final String actual = myFixture.getElementText(elementDefinition);
    // validate
    verify(myFixture).getElementDescription(elementDefinition);
    Assertions.assertEquals(actual, SHORT_DEFINITION);
  }

  @Test
  void getElementDescriptionShouldGetElementShort() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    elementDefinition.setShort(SHORT_DEFINITION);
    // execute
    final String actual = myFixture.getElementDescription(elementDefinition);
    // validate
    Assertions.assertEquals(actual, SHORT_DEFINITION);
  }

  @Test
  void getElementDescriptionShouldGetElementPath() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    // execute
    final String actual = myFixture.getElementDescription(elementDefinition);
    // validate
    Assertions.assertEquals(actual, PATH_VALUE);
  }
}
