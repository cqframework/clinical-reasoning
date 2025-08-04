package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newExtractRequestForVersion;
import static org.opencds.cqf.fhir.cr.questionnaireresponse.TestQuestionnaireResponse.open;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.Arrays;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
class ProcessDefinitionItemTests {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

    @Mock
    private IRepository repository;

    @Mock
    ExpressionProcessor expressionProcessor;

    @Mock
    private LibraryEngine libraryEngine;

    private ProcessDefinitionItem fixture;

    @BeforeEach
    void setup() {
        doReturn(fhirContextR4).when(repository).fhirContext();
        doReturn(repository).when(libraryEngine).getRepository();
        fixture = new ProcessDefinitionItem(expressionProcessor);
    }

    @Test
    void testItemWithNoDefinitionThrows() {
        var fhirVersion = FhirVersionEnum.R4;
        var item = new QuestionnaireItemComponent();
        var responseItem = new QuestionnaireResponseItemComponent();
        var itemPair = new ItemPair(item, responseItem);
        var questionnaire = new Questionnaire();
        var response = new QuestionnaireResponse();
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        assertThrows(IllegalArgumentException.class, () -> fixture.processDefinitionItem(request, itemPair));
    }

    @Test
    void testItemWithInvalidDefinitionThrows() {
        var fhirVersion = FhirVersionEnum.R4;
        var item = new QuestionnaireItemComponent();
        item.setDefinition("http://hl7.org/fhir/StructureDefinition/RelatedPerson.name.text");
        var responseItem = new QuestionnaireResponseItemComponent();
        var itemPair = new ItemPair(item, responseItem);
        var questionnaire = new Questionnaire();
        var response = new QuestionnaireResponse();
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        assertThrows(IllegalArgumentException.class, () -> fixture.processDefinitionItem(request, itemPair));
    }

    @Test
    void testItemWithContextExtensionWithType() {
        var fhirVersion = FhirVersionEnum.R4;
        var item = new QuestionnaireItemComponent().setLinkId("1");
        item.addExtension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT, new CodeType("Condition"));
        var responseItem = new QuestionnaireResponseItemComponent().setLinkId("1");
        var itemPair = new ItemPair(item, responseItem);
        var questionnaire = new Questionnaire();
        var response = new QuestionnaireResponse();
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var actual = fixture.processDefinitionItem(request, itemPair);
        assertInstanceOf(Condition.class, actual);
    }

    @Test
    void testItemWithContextExtensionWithProfile() {
        var fhirVersion = FhirVersionEnum.R4;
        var profile = "http://hl7.org/fhir/Patient";
        var item = new QuestionnaireItemComponent().setLinkId("1");
        var extension = new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                .setValue(new CanonicalType().setValue(profile));
        item.addExtension(extension);
        var responseItem = new QuestionnaireResponseItemComponent().setLinkId("1");
        var itemPair = new ItemPair(item, responseItem);
        var questionnaire = new Questionnaire();
        var response = new QuestionnaireResponse();
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var actual = fixture.processDefinitionItem(request, itemPair);
        assertNotNull(actual);
    }

    @Test
    void testItemWithContextExtensionWithMultipleAnswers() {
        var fhirVersion = FhirVersionEnum.R4;
        var item = new QuestionnaireItemComponent().setLinkId("1");
        item.setDefinition("http://hl7.org/fhir/Patient#Patient.name.given");
        var responseItem = new QuestionnaireResponseItemComponent().setLinkId("1");
        responseItem.addAnswer(new QuestionnaireResponseItemAnswerComponent().setValue(new StringType("test1")));
        responseItem.addAnswer(new QuestionnaireResponseItemAnswerComponent().setValue(new StringType("test2")));
        var questionnaire = new Questionnaire().setItem(Arrays.asList(item));
        var extension =
                new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT).setValue(new CodeType("Patient"));
        questionnaire.addExtension(extension);
        var response = new QuestionnaireResponse().setItem(Arrays.asList(responseItem));
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var itemPair = new ItemPair(null, null);
        var actual = fixture.processDefinitionItem(request, itemPair);
        assertInstanceOf(Patient.class, actual);
        var patient = (Patient) actual;
        assertEquals("test1", patient.getNameFirstRep().getGiven().get(0).asStringValue());
        assertEquals("test2", patient.getNameFirstRep().getGiven().get(1).asStringValue());
    }

    @Test
    void testRepeatingItemWithContextExtensionAndNestedPath() {
        var fhirVersion = FhirVersionEnum.R4;
        var parser = fhirContextR4.newJsonParser();
        var questionnaire = (Questionnaire)
                parser.parseResource(open("r4/input/resources/Questionnaire-extract-defn-walkthrough-4.json"));
        var response = (QuestionnaireResponse)
                parser.parseResource(open("r4/input/tests/QuestionnaireResponse-extract-defn-walkthrough-4.json"));
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var itemPair = new ItemPair(null, null);
        var actual = fixture.processDefinitionItem(request, itemPair);
        assertInstanceOf(Patient.class, actual);
        var names = ((Patient) actual).getName();
        assertEquals(2, names.size());
        assertEquals("test1", names.get(0).getGiven().get(0).getValue());
        assertEquals("test2", names.get(0).getGiven().get(1).getValue());
        assertEquals("official", names.get(0).getUse().toCode());
        assertEquals("test3", names.get(1).getGiven().get(0).getValue());
        assertEquals("old", names.get(1).getUse().toCode());
    }

    @Test
    void testItemWithContextExtensionAndRepeatingNestedPath() {
        var fhirVersion = FhirVersionEnum.R4;
        var item = new QuestionnaireItemComponent().setLinkId("1");
        item.setDefinition("http://hl7.org/fhir/Patient#Patient.name.text");
        item.setRepeats(true);
        var responseItem = new QuestionnaireResponseItemComponent().setLinkId("1");
        responseItem.addAnswer(new QuestionnaireResponseItemAnswerComponent().setValue(new StringType("test1")));
        responseItem.addAnswer(new QuestionnaireResponseItemAnswerComponent().setValue(new StringType("test2")));
        var questionnaire = new Questionnaire().setItem(Arrays.asList(item));
        var extension =
                new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT).setValue(new CodeType("Patient"));
        questionnaire.addExtension(extension);
        var response = new QuestionnaireResponse().setItem(Arrays.asList(responseItem));
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var itemPair = new ItemPair(null, null);
        var actual = fixture.processDefinitionItem(request, itemPair);
        assertInstanceOf(Patient.class, actual);
        var names = ((Patient) actual).getName();
        assertEquals(2, names.size());
        assertEquals("test1", names.get(0).getText());
        assertEquals("test2", names.get(1).getText());
    }
}
