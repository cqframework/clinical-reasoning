package org.opencds.cqf.cql.evaluator.questionnaire.r4.nestedquestionnaireitem;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.testng.Assert;

import static org.opencds.cqf.cql.evaluator.questionnaire.r4.TestingHelper.withElementDefinition;
import static org.opencds.cqf.cql.evaluator.questionnaire.r4.TestingHelper.withQuestionnaireItemComponent;

@ExtendWith(MockitoExtension.class)
class ElementIsFixedOrHasPatternTest {
  final static String TYPE_CODE = "typeCode";
  final static String PATH_VALUE = "pathValue";
  @InjectMocks
  private ElementIsFixedOrHasPattern myFixture;

  @Test
  void addPropertiesShouldAddAllPropertiesToQuestionnaireItem() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    final BooleanType booleanType = new BooleanType(true);
    elementDefinition.setFixed(booleanType);
    final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
    // execute
    final QuestionnaireItemComponent actual = myFixture.addProperties(elementDefinition, questionnaireItem);
    // validate
    Assert.assertEquals(actual.getInitial().size(), 1);
    Assert.assertEquals(actual.getInitial().get(0).getValue(), booleanType);
    final Extension urlExtension = actual.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_HIDDEN);
    Assert.assertEquals(urlExtension.getValueAsPrimitive().getValueAsString(), "true");
    Assert.assertTrue(actual.getReadOnly());
  }

}
