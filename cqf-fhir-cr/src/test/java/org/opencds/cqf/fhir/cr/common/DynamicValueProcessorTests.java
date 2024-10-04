package org.opencds.cqf.fhir.cr.common;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import ca.uhn.fhir.context.FhirVersionEnum;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.helpers.RequestHelpers;
import org.opencds.cqf.fhir.cr.inputparameters.IInputParameterResolver;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.slf4j.event.Level;

@ExtendWith(MockitoExtension.class)
class DynamicValueProcessorTests {
    @Mock
    private LibraryEngine libraryEngine;

    @Mock
    private IInputParameterResolver inputParameterResolver;

    @Spy
    @InjectMocks
    DynamicValueProcessor fixture = new DynamicValueProcessor();

    @Test
    void unsupportedFhirVersion() {
        var request =
                RequestHelpers.newPDApplyRequestForVersion(FhirVersionEnum.R4B, libraryEngine, inputParameterResolver);
        assertNull(fixture.getDynamicValueExpression(request, null));
    }

    @Test
    void testNullExpressionResult() {
        var cqfExpression = new CqfExpression().setExpression("NullTest");
        var request =
                RequestHelpers.newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine, inputParameterResolver);
        var dynamicValue = new org.hl7.fhir.r4.model.ActivityDefinition.ActivityDefinitionDynamicValueComponent()
                .setPath("action.extension")
                .setExpression(new org.hl7.fhir.r4.model.Expression()
                        .setExpression("priority")
                        .setLanguage("text/cql-identifier"));
        var requestAction = new org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent();
        doReturn(cqfExpression).when(fixture).getDynamicValueExpression(request, dynamicValue);
        doReturn(null).when(fixture).getDynamicValueExpressionResult(request, cqfExpression, null, null);

        var logger = TestLoggerFactory.getTestLogger(DynamicValueProcessor.class);
        logger.clear();

        fixture.resolveDynamicValue(request, dynamicValue, null, null, requestAction);

        assertEquals(Level.WARN, logger.getLoggingEvents().get(0).getLevel());
        assertEquals(
                "Null value received when evaluating dynamic value expression: NullTest",
                logger.getAllLoggingEvents().get(0).getMessage());
    }

    @Test
    void dstu3PriorityExt() {
        // works in dstu3, throws in other versions
        var cqfExpression = new CqfExpression();
        var requestDstu3 = RequestHelpers.newPDApplyRequestForVersion(
                FhirVersionEnum.DSTU3, libraryEngine, inputParameterResolver);
        var dvDstu3 = new org.hl7.fhir.dstu3.model.ActivityDefinition.ActivityDefinitionDynamicValueComponent()
                .setPath("action.extension")
                .setExpression("priority")
                .setLanguage("text/cql-identifier");
        var raDstu3 = new org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionComponent();
        var expressionResultsDstu3 = withExpressionResults(FhirVersionEnum.DSTU3);
        doReturn(cqfExpression).when(fixture).getDynamicValueExpression(requestDstu3, dvDstu3);
        doReturn(expressionResultsDstu3)
                .when(fixture)
                .getDynamicValueExpressionResult(requestDstu3, cqfExpression, null, null);

        fixture.resolveDynamicValue(requestDstu3, dvDstu3, null, null, raDstu3);

        var requestR4 =
                RequestHelpers.newPDApplyRequestForVersion(FhirVersionEnum.R4, libraryEngine, inputParameterResolver);
        var dvR4 = new org.hl7.fhir.r4.model.ActivityDefinition.ActivityDefinitionDynamicValueComponent()
                .setPath("action.extension")
                .setExpression(new org.hl7.fhir.r4.model.Expression()
                        .setExpression("priority")
                        .setLanguage("text/cql-identifier"));
        var raR4 = new org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent();
        var expressionResultsR4 = withExpressionResults(FhirVersionEnum.R4);
        doReturn(cqfExpression).when(fixture).getDynamicValueExpression(requestR4, dvR4);
        doReturn(expressionResultsR4)
                .when(fixture)
                .getDynamicValueExpressionResult(requestR4, cqfExpression, null, null);
        assertThrows(IllegalArgumentException.class, () -> {
            fixture.resolveDynamicValue(requestR4, dvR4, null, null, raR4);
        });
    }

    // @Test
    // void testActionDynamicValue() {}

    // @Test
    // void testDynamicValueDstu3() {}

    // @Test
    // void testDynamicValueR4() {}

    // @Test
    // void testDynamicValueR5() {}

    // @Test
    // void testDynamicValueWithNestedPathR4() {}

    // @Test
    // void testDynamicValueWithFhirPathContext() {}

    // @Test
    // void testDynamicValueWithExtensionFunctionInPath() {}

    private List<IBase> withExpressionResults(FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                return List.of(new org.hl7.fhir.dstu3.model.StringType("string type value"));
            case R4:
                return List.of(
                        new org.hl7.fhir.r4.model.StringType("string type value"),
                        new org.hl7.fhir.r4.model.BooleanType(true),
                        new org.hl7.fhir.r4.model.IntegerType(3));
            case R5:
                return List.of(
                        new org.hl7.fhir.r5.model.StringType("string type value"),
                        new org.hl7.fhir.r5.model.BooleanType(true),
                        new org.hl7.fhir.r5.model.IntegerType(3));

            default:
                return null;
        }
    }
}
