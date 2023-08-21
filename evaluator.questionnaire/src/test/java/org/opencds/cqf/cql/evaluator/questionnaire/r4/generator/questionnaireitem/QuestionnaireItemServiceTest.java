package org.opencds.cqf.cql.evaluator.questionnaire.r4.generator.questionnaireitem;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import javax.annotation.Nonnull;

import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.evaluator.questionnaire.r4.helpers.TestingHelper;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
class QuestionnaireItemServiceTest {
  final static String PROFILE_URL = "http://www.sample.com/profile/profileId";
  final static String LINK_ID = "profileId";
  final static QuestionnaireItemType QUESTIONNAIRE_ITEM_TYPE = QuestionnaireItemType.GROUP;
  final static String PROFILE_TITLE = "Profile Title";
  final static String PROFILE_TYPE = "ProfileType";
  @Spy
  private QuestionnaireItemService myFixture;

  @Test
  void getProfileTextShouldReturnTextWhenProfileHasTitle() {
    // execute
    final String actual = myFixture.getProfileText(PROFILE_URL, withProfileWithTitle());
    // validate
    Assertions.assertEquals(actual, PROFILE_TITLE);
  }

  @Test
  void getProfileTextShouldReturnTextWhenProfileHasNoTitle() {
    // execute
    final String actual = myFixture.getProfileText(PROFILE_URL, withProfileWithNoTitle());
    // validate
    Assertions.assertEquals(actual, LINK_ID);
  }

  @Test
  void createQuestionnaireItemComponentShouldCreateQuestionnaireItemComponent() {
    // setup
    final QuestionnaireItemComponent expected = withQuestionnaireItemComponent();
    // execute
    final QuestionnaireItemComponent actual = myFixture.createQuestionnaireItemComponent(
        PROFILE_TITLE,
        LINK_ID,
        PROFILE_URL);
    // validate
    assertEquals(actual, expected);
  }

  @Test
  void getProfileUrlShouldReturnValue() {
    // setup
    final DataRequirement actionInput = TestingHelper.withActionInput();
    // execute
    final String actual = myFixture.getProfileUrl(actionInput);
    // validate
    Assertions.assertEquals(actual, PROFILE_URL);
  }

  @Test
  void getQuestionnaireItemShouldAssembleAndReturnQuestionnaireItem() {
    // setup
    final DataRequirement actionInput = TestingHelper.withActionInput();
    final StructureDefinition profile = withProfileWithTitle();
    final QuestionnaireItemComponent questionnaireItemComponent = withQuestionnaireItemComponent();
    final QuestionnaireItemComponent expected = withQuestionnaireItemComponentWithExtension();
    doReturn(PROFILE_URL).when(myFixture).getProfileUrl(actionInput);
    doReturn(PROFILE_TITLE).when(myFixture).getProfileText(PROFILE_URL, profile);
    doReturn(questionnaireItemComponent).when(myFixture).createQuestionnaireItemComponent(
        PROFILE_TITLE,
        LINK_ID,
        PROFILE_URL);
    // execute
    final QuestionnaireItemComponent actual =
        myFixture.createQuestionnaireItem(actionInput, LINK_ID, profile);
    // validate
    verify(myFixture).getProfileUrl(actionInput);
    verify(myFixture).getProfileText(PROFILE_URL, profile);
    verify(myFixture).createQuestionnaireItemComponent(
        PROFILE_TITLE,
        LINK_ID,
        PROFILE_URL);
    assertEquals(actual, expected);
  }

  void assertEquals(QuestionnaireItemComponent actual, QuestionnaireItemComponent expected) {
    Assertions.assertEquals(actual.getType(), expected.getType());
    Assertions.assertEquals(actual.getLinkId(), expected.getLinkId());
    Assertions.assertEquals(actual.getText(), expected.getText());
  }

  void assertEquals(Extension actual, Extension expected) {
    Assertions.assertEquals(actual.getUrl(), expected.getUrl());
    Assertions.assertEquals(expected.getValue().toString(), actual.getValue().toString());
  }

  @Nonnull
  StructureDefinition withProfileWithTitle() {
    StructureDefinition profile = new StructureDefinition();
    profile.setTitle(PROFILE_TITLE);
    profile.setType(PROFILE_TYPE);
    return profile;
  }

  @Nonnull
  StructureDefinition withProfileWithNoTitle() {
    return new StructureDefinition();
  }

  @Nonnull
  QuestionnaireItemComponent withQuestionnaireItemComponent() {
    return new QuestionnaireItemComponent()
        .setType(QUESTIONNAIRE_ITEM_TYPE)
        .setLinkId(LINK_ID)
        .setText(PROFILE_TITLE);
  }

  @Nonnull
  QuestionnaireItemComponent withQuestionnaireItemComponentWithExtension() {
    final QuestionnaireItemComponent questionnaireItemComponent = new QuestionnaireItemComponent()
        .setType(QUESTIONNAIRE_ITEM_TYPE)
        .setLinkId(LINK_ID)
        .setText(PROFILE_TITLE);
    final Extension extension = withExtension();
    questionnaireItemComponent.addExtension(extension);
    return questionnaireItemComponent;
  }

  @Nonnull
  Extension withExtension() {
    final Type codeType = new CodeType().setValue(PROFILE_TYPE);
    return new Extension().setUrl(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
        .setValue(codeType);
  }
}
