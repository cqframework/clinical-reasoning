package org.opencds.cqf.cql.evaluator.questionnaire.r4.generator.nestedquestionnaireitem;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.fhir.api.Repository;
import org.testng.Assert;

import javax.annotation.Nonnull;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opencds.cqf.cql.evaluator.questionnaire.r4.helpers.TestingHelper.withElementDefinition;
import static org.opencds.cqf.cql.evaluator.questionnaire.r4.helpers.TestingHelper.withQuestionnaireItemComponent;

@ExtendWith(MockitoExtension.class)
class NestedQuestionnaireItemServiceTest {
  final static String TYPE_CODE = "CodeableConcept";
  final static String PATH_VALUE = "pathValue";
  final static String PROFILE_URL = "http://www.sample.com/profile/profileId";
  final static String SHORT_DEFINITION = "shortDefinition";
  final static String LABEL = "label";
  final static String CHILD_LINK_ID = "childLinkId";
  @Mock
  protected Repository repository;
  @Mock
  protected QuestionnaireTypeIsChoice questionnaireTypeIsChoice;
  @Mock
  protected ElementIsFixedOrHasPattern elementIsFixedOrHasPattern;
  @Mock
  protected ElementHasCqfExtension elementHasCqfExtension;
  @InjectMocks
  @Spy
  private NestedQuestionnaireItemService myFixture;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(questionnaireTypeIsChoice);
    verifyNoMoreInteractions(elementIsFixedOrHasPattern);
    verifyNoMoreInteractions(elementHasCqfExtension);
    verifyNoMoreInteractions(elementHasCqfExtension);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void getNestedQuestionnaireItemShouldReturnQuestionnaireItemComponentIfItemTypeIsChoice()
      throws Exception {
    // setup
    final QuestionnaireItemComponent initialized = withQuestionnaireItemComponent();
    final QuestionnaireItemComponent expected = withQuestionnaireItemComponent();
    final StructureDefinition profile = new StructureDefinition();
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    doReturn(initialized).when(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.CHOICE,
        profile,
        elementDefinition,
        CHILD_LINK_ID
    );
    doReturn(expected).when(questionnaireTypeIsChoice).addProperties(elementDefinition, initialized);
    // execute
    final QuestionnaireItemComponent actual = myFixture.getNestedQuestionnaireItem(profile, elementDefinition, CHILD_LINK_ID);
    // validate
    verify(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.CHOICE,
        profile,
        elementDefinition,
        CHILD_LINK_ID
    );
    verify(questionnaireTypeIsChoice).addProperties(elementDefinition, initialized);
    Assertions.assertEquals(actual, expected);
  }

  @Test
  void getNestedQuestionnaireItemShouldReturnQuestionnaireItemComponentIfElementIsFixedOrHasPattern()
      throws Exception {
    // setup
    final QuestionnaireItemComponent initialized = withQuestionnaireItemComponent();
    final QuestionnaireItemComponent expected = withQuestionnaireItemComponent();
    final StructureDefinition profile = new StructureDefinition();
    final ElementDefinition elementDefinition = withElementDefinitionWithFixedType();
    doReturn(initialized).when(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.STRING,
        profile,
        elementDefinition,
        CHILD_LINK_ID
    );
    doReturn(expected).when(elementIsFixedOrHasPattern).addProperties(elementDefinition, initialized);
    // execute
    final QuestionnaireItemComponent actual = myFixture.getNestedQuestionnaireItem(profile, elementDefinition, CHILD_LINK_ID);
    // validate
    verify(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.STRING,
        profile,
        elementDefinition,
        CHILD_LINK_ID
    );
    verify(elementIsFixedOrHasPattern).addProperties(elementDefinition, initialized);
    Assertions.assertEquals(actual, expected);
  }

  @Nonnull
  ElementDefinition withElementDefinitionWithFixedType() {
    final ElementDefinition elementDefinition = withElementDefinition("Reference", PATH_VALUE);
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
    final StructureDefinition profile = new StructureDefinition();
    final ElementDefinition elementDefinition = withElementDefinitionWithDefaultValue(defaultValue);
    doReturn(initialized).when(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.STRING,
        profile,
        elementDefinition,
        CHILD_LINK_ID
    );
    // execute
    final QuestionnaireItemComponent actual = myFixture.getNestedQuestionnaireItem(profile, elementDefinition, CHILD_LINK_ID);
    // validate
    verify(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.STRING,
        profile,
        elementDefinition,
        CHILD_LINK_ID
    );
    Assertions.assertFalse(actual.getInitial().isEmpty());
    Assertions.assertEquals(actual.getInitial().get(0).getValue(), defaultValue);
  }

  @Nonnull
  ElementDefinition withElementDefinitionWithDefaultValue(Type defaultValue) {
    final ElementDefinition elementDefinition = withElementDefinition("Reference", PATH_VALUE);
    elementDefinition.setDefaultValue(defaultValue);
    return elementDefinition;
  }

  @Test
  void getNestedQuestionnaireItemShouldReturnQuestionnaireItemComponentIfElementHasExtension()
      throws Exception {
    // setup
    final QuestionnaireItemComponent initialized = withQuestionnaireItemComponent();
    final QuestionnaireItemComponent expected = withQuestionnaireItemComponent();
    final StructureDefinition profile = new StructureDefinition();
    final ElementDefinition elementDefinition = withElementDefinitionWithCqfExtension();
    doReturn(initialized).when(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.STRING,
        profile,
        elementDefinition,
        CHILD_LINK_ID
    );
    doReturn(expected).when(elementHasCqfExtension).addProperties(elementDefinition, initialized);
    // execute
    final QuestionnaireItemComponent actual = myFixture.getNestedQuestionnaireItem(profile, elementDefinition, CHILD_LINK_ID);
    // validate
    verify(myFixture).initializeQuestionnaireItem(
        QuestionnaireItemType.STRING,
        profile,
        elementDefinition,
        CHILD_LINK_ID
    );
    verify(elementHasCqfExtension).addProperties(elementDefinition, initialized);
    Assertions.assertEquals(actual, expected);
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
    final StructureDefinition profile = new StructureDefinition();
    profile.setUrl(PROFILE_URL);
    final QuestionnaireItemComponent actual = myFixture.initializeQuestionnaireItem(
        QuestionnaireItemType.GROUP,
        profile,
        elementDefinition,
        CHILD_LINK_ID
    );
    // validate
    Assert.assertEquals(actual.getType(), QuestionnaireItemType.GROUP);
    Assert.assertEquals(actual.getDefinition(), "http://www.sample.com/profile/profileId#pathValue");
    Assert.assertEquals(actual.getLinkId(), CHILD_LINK_ID);
    Assert.assertEquals(actual.getText(), LABEL);
  }

  @Test
  void getItemTypeShouldReturnQuestionnaireItemType() throws Exception {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    // execute
    final QuestionnaireItemType actual = myFixture.getItemType(elementDefinition);
    // validate
    Assert.assertEquals(actual, QuestionnaireItemType.CHOICE);
  }

  @Test
  void getItemTypeShouldThrowExceptionIfItemTypeIsNull() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition("", PATH_VALUE);
    // execute
    final Exception actual = Assertions.assertThrows(
        Exception.class, () -> myFixture.getItemType(elementDefinition));
    // validate
    Assert.assertEquals(actual.getMessage(), "Unable to determine type for element: null");
  }

  @Test
  void parseItemTypeShouldReturnQuestionnaireItemTypeIfItemHasBinding() {
    // setup
    final String elementType = "elementType";
    // execute
    final QuestionnaireItemType actual = myFixture.parseItemType(elementType, true);
    // validate
    Assert.assertEquals(actual, QuestionnaireItemType.CHOICE);
  }

  @Test
  void parseItemTypeShouldReturnQuestionnaireItemTypeGivenCodeableConceptString() {
    // setup
    final String elementType = "CodeableConcept";
    // execute
    final QuestionnaireItemType actual = myFixture.parseItemType(elementType, false);
    // validate
    Assert.assertEquals(actual, QuestionnaireItemType.CHOICE);
  }

  @Test
  void parseItemTypeShouldReturnQuestionnaireItemTypeGivenReferenceString() {
    // setup
    final String elementType = "Reference";
    // execute
    final QuestionnaireItemType actual = myFixture.parseItemType(elementType, false);
    // validate
    Assert.assertEquals(actual, QuestionnaireItemType.STRING);
  }

  @Test
  void parseItemTypeShouldReturnQuestionnaireItemTypeGivenUriString() {
    // setup
    final String elementType = "uri";
    // execute
    final QuestionnaireItemType actual = myFixture.parseItemType(elementType, false);
    // validate
    Assert.assertEquals(actual, QuestionnaireItemType.STRING);
  }

  @Test
  void parseItemTypeShouldReturnQuestionnaireItemTypeGivenBackboneElementString() {
    // setup
    final String elementType = "BackboneElement";
    // execute
    final QuestionnaireItemType actual = myFixture.parseItemType(elementType, false);
    // validate
    Assert.assertEquals(actual, QuestionnaireItemType.GROUP);
  }

  @Test
  void parseItemTypeShouldThrowErrorIfUnknownElementType() {
    // setup
    final String elementType = "UnknownElementType";
    // execute
    final FHIRException actual = Assertions.assertThrows(
        FHIRException.class, () -> myFixture.parseItemType(elementType, false));
    // validate
    Assert.assertEquals(actual.getMessage(), "Unknown QuestionnaireItemType code 'UnknownElementType'");
  }

  @Test
  void parseItemTypeShouldReturnReturnNull() {
    // setup
    final String elementType = "BackboneElement";
    // execute
    final QuestionnaireItemType actual = myFixture.parseItemType(elementType, false);
    // validate
    Assert.assertEquals(actual, QuestionnaireItemType.GROUP);
  }
  
  @Test
  void getElementTextShouldReturnElementLabel() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    elementDefinition.setLabel(LABEL);
    // execute
    final String actual = myFixture.getElementText(elementDefinition);
    // validate
    Assert.assertEquals(actual, LABEL);
  }
  
  @Test
  void getElementTextShouldReturnElementDefinition() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    doReturn(SHORT_DEFINITION).when(myFixture).getElementDefinition(elementDefinition);
    // execute
    final String actual = myFixture.getElementText(elementDefinition);
    // validate
    verify(myFixture).getElementDefinition(elementDefinition);
    Assert.assertEquals(actual, SHORT_DEFINITION);
  }
  @Test
  void getElementDefinitionShouldGetElementShortDefinition() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    elementDefinition.setPath(PATH_VALUE);
    elementDefinition.setShort(SHORT_DEFINITION);
    // execute
    final String actual = myFixture.getElementDefinition(elementDefinition);
    // validate
    Assert.assertEquals(actual, SHORT_DEFINITION);
  }

  @Test
  void getElementDefinitionShouldGetElementPath() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    elementDefinition.setPath(PATH_VALUE);
    // execute
    final String actual = myFixture.getElementDefinition(elementDefinition);
    // validate
    Assert.assertEquals(actual, PATH_VALUE);
  }
}
