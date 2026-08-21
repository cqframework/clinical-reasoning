package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newExtractRequestForVersion;
import static org.opencds.cqf.fhir.utility.Constants.QUESTIONNAIRE_UNIT;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.time.Instant;
import java.util.Date;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class ObservationResolverTests {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

    @Mock
    private IRepository repository;

    @Mock
    private LibraryEngine libraryEngine;

    private ObservationResolver fixture;

    @BeforeEach
    void setup() {
        doReturn(fhirContextR4).when(repository).fhirContext();
        doReturn(repository).when(libraryEngine).getRepository();
        fixture = spy(new ObservationResolver());
    }

    @Test
    void test_getAnswerValue() {
        var fhirVersion = FhirVersionEnum.R4;
        var questionnaire = new Questionnaire();
        var response = new QuestionnaireResponse();
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        // test integer
        var integerValue = new IntegerType(50);
        var integerAnswer = new QuestionnaireResponseItemAnswerComponent().setValue(integerValue);
        var integerActual = getValue(request, integerAnswer, null);
        assertEquals(integerValue, integerActual);
        // test decimal
        var decimalValue = new DecimalType(50.5);
        var decimalAnswer = new QuestionnaireResponseItemAnswerComponent().setValue(decimalValue);
        var decimalActual = getValue(request, decimalAnswer, null);
        assertEquals(decimalValue, decimalActual);
        doReturn(decimalValue).when(fixture).getQuantity(eq(request), any(), any());
        var itemWithUnit = new QuestionnaireItemComponent();
        itemWithUnit.addExtension(new Extension(QUESTIONNAIRE_UNIT, new Coding()));
        var decimalWithUnitActual = getValue(request, decimalAnswer, itemWithUnit);
        assertEquals(decimalValue, decimalWithUnitActual);
        // test Date
        var dateValue = new DateTimeType(Date.from(Instant.now()));
        var dateAnswer = new QuestionnaireResponseItemAnswerComponent().setValue(dateValue);
        var dateActual = getValue(request, dateAnswer, null);
        assertEquals(dateValue, dateActual);
        // test Coding
        var codingValue = new Coding("test.com", "test", "Test");
        var codingAnswer = new QuestionnaireResponseItemAnswerComponent().setValue(codingValue);
        var codingActual = getValue(request, codingAnswer, null);
        assertInstanceOf(CodeableConcept.class, codingActual);
        assertEquals(codingValue, ((CodeableConcept) codingActual).getCodingFirstRep());
    }

    private IBaseDatatype getValue(
            ExtractRequest request, QuestionnaireResponseItemAnswerComponent answer, QuestionnaireItemComponent item) {
        return fixture.getAnswerValue(
                request,
                request.getAdapterFactory().createQuestionnaireResponseItemAnswer(answer),
                item == null ? null : request.getAdapterFactory().createQuestionnaireItem(item));
    }

    @Test
    void test_getQuantity() {
        var fhirVersion = FhirVersionEnum.R4;
        var questionnaire = new Questionnaire();
        var response = new QuestionnaireResponse();
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);

        var cmCoding = new Coding("http://unitsofmeasure.org", "cm", "Centimeter");
        var integerValue = new IntegerType(50);
        var integerAnswer = request.getAdapterFactory()
                .createQuestionnaireResponseItemAnswer(
                        new QuestionnaireResponseItemAnswerComponent().setValue(integerValue));
        var integerActual = fixture.getQuantity(request, integerAnswer, cmCoding);
        assertInstanceOf(Quantity.class, integerActual);
        var integerQuantity = (Quantity) integerActual;
        assertEquals(integerValue.asStringValue(), integerQuantity.getValue().toString());
        assertEquals(cmCoding.getCode(), integerQuantity.getCode());
        assertEquals(cmCoding.getSystem(), integerQuantity.getSystem());
        assertEquals(cmCoding.getDisplay(), integerQuantity.getUnit());

        var kgCoding = new Coding("http://unitsofmeasure.org", "kg", "Kilogram");
        var decimalValue = new DecimalType(50.5);
        var decimalAnswer = request.getAdapterFactory()
                .createQuestionnaireResponseItemAnswer(
                        new QuestionnaireResponseItemAnswerComponent().setValue(decimalValue));
        var decimalActual = fixture.getQuantity(request, decimalAnswer, kgCoding);
        assertInstanceOf(Quantity.class, decimalActual);
        var decimalQuantity = (Quantity) decimalActual;
        assertEquals(decimalValue.getValue(), decimalQuantity.getValue());
        assertEquals(kgCoding.getCode(), decimalQuantity.getCode());
        assertEquals(kgCoding.getSystem(), decimalQuantity.getSystem());
        assertEquals(kgCoding.getDisplay(), decimalQuantity.getUnit());
    }
}
