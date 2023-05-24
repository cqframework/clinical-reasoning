package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.questionnaireitem;

import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.dstu3.model.UriType;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.testng.Assert;
import org.junit.jupiter.api.Test;
import javax.annotation.Nonnull;
import java.util.List;

class QuestionnaireItemServiceTest {
  final static String PROFILE_URL = "http://www.sample.com/profile/profileId";
  final static String LINK_ID = "profileId";
  final static QuestionnaireItemType QUESTIONNAIRE_ITEM_TYPE = QuestionnaireItemType.GROUP;
  final static String PROFILE_TITLE = "Profile Title";
  final static String PROFILE_TYPE = "ProfileType";
  QuestionnaireItemService myFixture = new QuestionnaireItemService();

  @Test
  void getProfileTextShouldReturnTextWhenProfileHasTitle() {
    // execute
    final String actual = myFixture.getProfileText(PROFILE_URL, withProfileWithTitle());
    // validate
    Assert.assertEquals(actual, PROFILE_TITLE);
  }

  @Test
  void getProfileTextShouldReturnTextWhenProfileHasNoTitle() {
    // execute
    final String actual = myFixture.getProfileText(PROFILE_URL, withProfileWithNoTitle());
    // validate
    Assert.assertEquals(actual, LINK_ID);
  }

  @Test
  void createQuestionnaireItemComponentShouldCreateQuestionnaireItemComponent() {
    // setup
    final QuestionnaireItemComponent expected = withQuestionnaireItemComponent();
    // execute
    final QuestionnaireItemComponent actual = myFixture.createQuestionnaireItemComponent(PROFILE_TITLE, LINK_ID);
    // validate
    assertEquals(actual, expected);
  }

  @Test
  void createExtensionShouldCreateExtension() {
    // setup
    final Extension expected = withExtension();
    final StructureDefinition profile = withProfileWithTitle();
    // execute
    final Extension actual = myFixture.createExtension(profile);
    // validate
    assertEquals(actual, expected);
  }

  @Test
  void getProfileUrlShouldReturnValue() {
    // setup
    final DataRequirement actionInput = withActionInput();
    // execute
    final String actual = myFixture.getProfileUrl(actionInput);
    // validate
    Assert.assertEquals(actual, PROFILE_URL);
  }

  void assertEquals(QuestionnaireItemComponent actual, QuestionnaireItemComponent expected) {
    Assert.assertEquals(actual.getType(), expected.getType());
    Assert.assertEquals(actual.getLinkId(), expected.getLinkId());
    Assert.assertEquals(actual.getText(), expected.getText());
  }

  void assertEquals(Extension actual, Extension expected) {
    Assert.assertEquals(actual.getUrl(), expected.getUrl());
    Assert.assertEquals(expected.getValue().toString(), actual.getValue().toString());
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
  Extension withExtension() {
    final Type codeType = new CodeType().setValue(PROFILE_TYPE);
    return new Extension().setUrl(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT).setValue(codeType);
  }

  @Nonnull
  DataRequirement withActionInput() {
    DataRequirement actionInput = new DataRequirement();
    UriType uri = new UriType();
    uri.setValue(PROFILE_URL);
    actionInput.setProfile(List.of(uri));
    return actionInput;
  }
}
