package org.opencds.cqf.cql.evaluator.questionnaire.r4.nestedquestionnaireitem;

import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.ElementDefinition.TypeRefComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.questionnaire.r4.TestingHelper;
import org.opencds.cqf.cql.evaluator.questionnaire.r4.nestedquestionnaireitem.ElementHasCqfExtension;
import org.testng.Assert;

import javax.annotation.Nonnull;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ElementHasCqfExtensionTest {
  final static String EXPECTED_VALUE = "expectedValueString";
  final static String LINK_ID = "profileId";
  final static QuestionnaireItemType QUESTIONNAIRE_ITEM_TYPE = QuestionnaireItemType.GROUP;
  final static String PROFILE_TITLE = "Profile Title";
  final static String TYPE_CODE = "typeCode";
  final static String PATH_VALUE = "pathValue";
  @InjectMocks
  @Spy
  private ElementHasCqfExtension myFixture;

  @Test
  void addPropertiesShouldAddAllProperties() {
    // setup
    final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
    final ElementDefinition element = TestingHelper.withElementDefinition(TYPE_CODE, PATH_VALUE);

    // execute
    final QuestionnaireItemComponent actual = myFixture.addProperties(questionnaireItem, element);
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
