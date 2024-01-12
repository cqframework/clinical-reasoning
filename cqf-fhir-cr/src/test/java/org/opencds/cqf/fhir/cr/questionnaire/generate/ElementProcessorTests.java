package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newGenerateRequestForVersion;
import static org.opencds.cqf.fhir.cr.questionnaire.generate.IElementProcessor.createInitial;
import static org.opencds.cqf.fhir.cr.questionnaire.generate.IElementProcessor.createProcessor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Collections;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
public class ElementProcessorTests {
    private final FhirContext fhirContextR4B = FhirContext.forR4BCached();

    @Mock
    Repository repository;

    @Mock
    ExpressionProcessor expressionProcessor;

    @Test
    void createProcessorShouldReturnNullForUnsupportedVersion() {
        doReturn(fhirContextR4B).when(repository).fhirContext();
        assertNull(createProcessor(repository));
    }

    @Test
    void createInitialShouldReturnNullForUnsupportedVersion() {
        var request = newGenerateRequestForVersion(FhirVersionEnum.DSTU2);
        var initial = createInitial(request, new BooleanType(true));
        assertNull(initial);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testElementWithCqfExpressionWithResourceResult() {
        var request = newGenerateRequestForVersion(FhirVersionEnum.R4);
        var cqfExpression = new CqfExpression();
        var expectedResource = new Patient().setId("test");
        var item = new QuestionnaireItemComponent().setLinkId("test").setType(QuestionnaireItemType.REFERENCE);
        doReturn(cqfExpression)
                .when(expressionProcessor)
                .getCqfExpression(request, Collections.EMPTY_LIST, Constants.CQF_EXPRESSION);
        doReturn(Collections.singletonList(expectedResource))
                .when(expressionProcessor)
                .getExpressionResult(request, cqfExpression);
        var actual = (QuestionnaireItemComponent)
                new ElementHasCqfExpression(expressionProcessor).addProperties(request, Collections.EMPTY_LIST, item);
        assertNotNull(actual);
        assertTrue(actual.hasInitial());
        assertEquals("test", actual.getInitial().get(0).getValueReference().getReference());
    }
}
