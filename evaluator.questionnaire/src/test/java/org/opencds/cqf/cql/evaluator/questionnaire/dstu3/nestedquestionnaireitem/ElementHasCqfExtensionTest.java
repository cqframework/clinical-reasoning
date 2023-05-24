package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.nestedquestionnaireitem;

import ca.uhn.fhir.util.ExtensionUtil;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.testng.Assert;

import javax.annotation.Nonnull;

import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ElementHasCqfExtensionTest {
  final static String EXPECTED_VALUE = "expectedValueString";
  final static String LINK_ID = "profileId";
  final static QuestionnaireItemType QUESTIONNAIRE_ITEM_TYPE = QuestionnaireItemType.GROUP;
  final static String PROFILE_TITLE = "Profile Title";
  @InjectMocks
  @Spy
  private ElementHasCqfExtension myFixture;

  @Test
  void getExpressionReturnsStringValueOfExpressionExtension() {
    // setup
    final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
    doReturn(withIBaseExtension()).when(myFixture).getExtension(questionnaireItem, Constants.CQF_EXPRESSION);
    // execute
    final String actual = myFixture.getExpression(questionnaireItem);
    // validate
    Assert.assertEquals(actual, EXPECTED_VALUE);
    verify(myFixture).getExtension(questionnaireItem, Constants.CQF_EXPRESSION);
  }


  @Test
  void getLanguageReturnsStringValueOfLanguageExtension() {
    // setup
    final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
    doReturn(withIBaseExtension()).when(myFixture).getExtension(questionnaireItem, Constants.CQF_EXPRESSION_LANGUAGE);
    // execute
    final String actual = myFixture.getLanguage(questionnaireItem);
    // validate
    Assert.assertEquals(actual, EXPECTED_VALUE);
    verify(myFixture).getExtension(questionnaireItem, Constants.CQF_EXPRESSION_LANGUAGE);
  }

  @Test
  void getLibraryReturnsStringValueOfLibraryExtension() {
    // setup
    final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
    doReturn(withIBaseExtension()).when(myFixture).getExtension(questionnaireItem, Constants.CQF_LIBRARY);
    // execute
    final String actual = myFixture.getLibrary(questionnaireItem);
    // validate
    Assert.assertEquals(actual, EXPECTED_VALUE);
    verify(myFixture).getExtension(questionnaireItem, Constants.CQF_LIBRARY);
  }

  @Test
  void addPropertiesShouldAddAllProperties() {
    // setup
    final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
    // execute
    final QuestionnaireItemComponent actual = myFixture.addProperties(questionnaireItem);
    // validate
  }

  @Nonnull
  QuestionnaireItemComponent withQuestionnaireItemComponent() {
    return new QuestionnaireItemComponent()
        .setType(QUESTIONNAIRE_ITEM_TYPE)
        .setLinkId(LINK_ID)
        .setText(PROFILE_TITLE);
  }

  @Nonnull
  MockIBaseDatatype withValue() {
    final MockIBaseDatatype mockIBaseDatatype = new MockIBaseDatatype();
    mockIBaseDatatype.setMockValue(EXPECTED_VALUE);
    return mockIBaseDatatype;
  }

  @Nonnull
  <D, T>
  IBaseExtension<?, ?> withIBaseExtension() {
    final MockIBaseExtension<?, ?> mockExtension = new MockIBaseExtension<D, T>();
    final MockIBaseDatatype value = withValue();
    mockExtension.setValue(value);
    return mockExtension;
  }

  @Nonnull
  List<IBase> withResult() {
    return List.of(withValue(), withValue(), withValue());
  }
}
