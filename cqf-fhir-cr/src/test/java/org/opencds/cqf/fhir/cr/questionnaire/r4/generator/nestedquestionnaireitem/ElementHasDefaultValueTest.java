package org.opencds.cqf.fhir.cr.questionnaire.r4.generator.nestedquestionnaireitem;

import static org.opencds.cqf.fhir.cr.questionnaire.r4.helpers.TestingHelper.withElementDefinition;
import static org.opencds.cqf.fhir.cr.questionnaire.r4.helpers.TestingHelper.withQuestionnaireItemComponent;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
class ElementHasDefaultValueTest {
    static final String TYPE_CODE = "typeCode";
    static final String PATH_VALUE = "pathValue";

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
                myFixture.addProperties(elementDefinition.getFixedOrPattern(), questionnaireItem);
        // validate
        Assertions.assertEquals(actual.getInitial().size(), 1);
        Assertions.assertEquals(actual.getInitial().get(0).getValue(), booleanType);
        final Extension urlExtension = actual.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_HIDDEN);
        Assertions.assertEquals(urlExtension.getValueAsPrimitive().getValueAsString(), "true");
        Assertions.assertTrue(actual.getReadOnly());
    }
}
