package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.generator.nestedquestionnaireitem;

import static org.opencds.cqf.cql.evaluator.questionnaire.dstu3.helpers.TestingHelper.withElementDefinition;
import static org.opencds.cqf.cql.evaluator.questionnaire.dstu3.helpers.TestingHelper.withQuestionnaireItemComponent;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.testng.Assert;

@ExtendWith(MockitoExtension.class)
class ElementHasDefaultValueTest {
  final static String TYPE_CODE = "typeCode";
  final static String PATH_VALUE = "pathValue";
  @InjectMocks
  private ElementHasDefaultValue myFixture;

  @Test
  void addPropertiesShouldAddAllPropertiesToQuestionnaireItem() {
    // setup
    final ElementDefinition elementDefinition = withElementDefinition(TYPE_CODE, PATH_VALUE);
    final BooleanType booleanType = new BooleanType(true);
    elementDefinition.setFixed(booleanType);
    final QuestionnaireItemComponent questionnaireItem = withQuestionnaireItemComponent();
    // execute
    final QuestionnaireItemComponent actual =
        myFixture.addProperties(elementDefinition.getFixed(), questionnaireItem);
    // validate
    Assert.assertEquals(actual.getInitial(), booleanType);
    final Extension urlExtension = actual.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_HIDDEN);
    Assert.assertEquals(urlExtension.getValueAsPrimitive().getValueAsString(), "true");
    Assert.assertTrue(actual.getReadOnly());
  }

}
