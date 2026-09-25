package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.opencds.cqf.fhir.cr.helpers.RequestHelpers.newExtractRequestForVersion;
import static org.opencds.cqf.fhir.cr.questionnaireresponse.TestQuestionnaireResponse.open;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import com.google.common.collect.Multimap;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.Constants;

@SuppressWarnings("UnstableApiUsage")
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
        var itemPair = new ItemPair(fhirVersion, item, responseItem);
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
        var itemPair = new ItemPair(fhirVersion, item, responseItem);
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
        var itemPair = new ItemPair(fhirVersion, item, responseItem);
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
        var itemPair = new ItemPair(fhirVersion, item, responseItem);
        var questionnaire = new Questionnaire();
        var response = new QuestionnaireResponse();
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var actual = fixture.processDefinitionItem(request, itemPair);
        assertNotNull(actual);
    }

    @Test
    void testItemWithContextExtensionWithMultipleAnswers() {
        var fhirVersion = FhirVersionEnum.R4;
        var item = new QuestionnaireItemComponent().setLinkId("1").setType(QuestionnaireItemType.STRING);
        item.setDefinition("http://hl7.org/fhir/Patient#Patient.name.given");
        var responseItem = new QuestionnaireResponseItemComponent().setLinkId("1");
        responseItem.addAnswer(new QuestionnaireResponseItemAnswerComponent().setValue(new StringType("test1")));
        responseItem.addAnswer(new QuestionnaireResponseItemAnswerComponent().setValue(new StringType("test2")));
        var questionnaire = new Questionnaire().setItem(List.of(item));
        var extension =
                new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT).setValue(new CodeType("Patient"));
        questionnaire.addExtension(extension);
        var response = new QuestionnaireResponse().setItem(List.of(responseItem));
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
        var questionnaire = new Questionnaire().setItem(List.of(item));
        var extension =
                new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT).setValue(new CodeType("Patient"));
        questionnaire.addExtension(extension);
        var response = new QuestionnaireResponse().setItem(List.of(responseItem));
        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var itemPair = new ItemPair(null, null);
        var actual = fixture.processDefinitionItem(request, itemPair);
        assertInstanceOf(Patient.class, actual);
        var names = ((Patient) actual).getName();
        assertEquals(2, names.size());
        assertEquals("test1", names.get(0).getText());
        assertEquals("test2", names.get(1).getText());
    }

    @Test
    void testExtractsMultipleIdentifierSlices() {
        var fhirVersion = FhirVersionEnum.R4;
        var profileUrl = "http://example.org/fhir/StructureDefinition/ChPatient";
        var ahvSystem = "urn:oid:2.16.756.5.32";
        var zidSystem = "urn:oid:2.16.756.5.30.1.127.3.10.3";
        var ahvValue = "7561234567890";
        var zidValue = "ZID-001";

        var profile = new StructureDefinition().setUrl(profileUrl).setType("Patient");
        profile.getDifferential().addElement().setPath("Patient.identifier").setId("Patient.identifier");
        var ahvSlice = profile.getDifferential().addElement().setPath("Patient.identifier");
        ahvSlice.setId("Patient.identifier:ahv");
        ahvSlice.setSliceName("ahv");
        var ahvSystemElement = profile.getDifferential().addElement().setPath("Patient.identifier.system");
        ahvSystemElement.setId("Patient.identifier:ahv.system");
        ahvSystemElement.setFixed(new UriType(ahvSystem));
        var zidSlice = profile.getDifferential().addElement().setPath("Patient.identifier");
        zidSlice.setId("Patient.identifier:zid");
        zidSlice.setSliceName("zid");
        var zidSystemElement = profile.getDifferential().addElement().setPath("Patient.identifier.system");
        zidSystemElement.setId("Patient.identifier:zid.system");
        zidSystemElement.setFixed(new UriType(zidSystem));

        var searchResult = new Bundle();
        searchResult.addEntry().setResource(profile);
        doReturn(searchResult)
                .when(repository)
                .search(eq(Bundle.class), any(), any(Multimap.class));

        var ahvItem = new QuestionnaireItemComponent()
                .setLinkId("ahv")
                .setType(QuestionnaireItemType.STRING)
                .setDefinition(profileUrl + "#Patient.identifier:ahv.value");
        var zidItem = new QuestionnaireItemComponent()
                .setLinkId("zid")
                .setType(QuestionnaireItemType.STRING)
                .setDefinition(profileUrl + "#Patient.identifier:zid.value");
        var questionnaire = new Questionnaire().setItem(List.of(ahvItem, zidItem));
        questionnaire.addExtension(new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                .setValue(new CanonicalType().setValue(profileUrl)));

        var ahvResponse = new QuestionnaireResponseItemComponent().setLinkId("ahv");
        ahvResponse.addAnswer(new QuestionnaireResponseItemAnswerComponent().setValue(new StringType(ahvValue)));
        var zidResponse = new QuestionnaireResponseItemComponent().setLinkId("zid");
        zidResponse.addAnswer(new QuestionnaireResponseItemAnswerComponent().setValue(new StringType(zidValue)));
        var response = new QuestionnaireResponse().setItem(List.of(ahvResponse, zidResponse));

        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var actual = fixture.processDefinitionItem(request, new ItemPair(null, null));

        assertInstanceOf(Patient.class, actual);
        var identifiers = ((Patient) actual).getIdentifier();
        assertEquals(2, identifiers.size());
        assertTrue(identifiers.stream()
                .anyMatch(id -> ahvSystem.equals(id.getSystem()) && ahvValue.equals(id.getValue())));
        assertTrue(identifiers.stream()
                .anyMatch(id -> zidSystem.equals(id.getSystem()) && zidValue.equals(id.getValue())));
    }

    @Test
    void testExtractsGroupedIdentifierSlicesWithoutResolvingSlicePath() {
        var fhirVersion = FhirVersionEnum.R4;
        var profileUrl = "http://example.org/fhir/StructureDefinition/ChPatient";
        var ahvSystem = "urn:oid:2.16.756.5.32";
        var zidSystem = "urn:oid:2.16.756.5.30.1.127.3.10.3";
        var ahvValue = "7561234567897";
        var zidValue = "761337610411353650";

        var profile = new StructureDefinition().setUrl(profileUrl).setType("Patient");
        profile.getDifferential().addElement().setPath("Patient.identifier").setId("Patient.identifier");
        var ahvSlice = profile.getDifferential().addElement().setPath("Patient.identifier");
        ahvSlice.setId("Patient.identifier:AHVN13");
        ahvSlice.setSliceName("AHVN13");
        ahvSlice.setPattern(new Identifier().setSystem(ahvSystem));
        var zidSlice = profile.getDifferential().addElement().setPath("Patient.identifier");
        zidSlice.setId("Patient.identifier:EPR-SPID");
        zidSlice.setSliceName("EPR-SPID");
        zidSlice.setPattern(new Identifier().setSystem(zidSystem));

        var searchResult = new Bundle();
        searchResult.addEntry().setResource(profile);
        doReturn(searchResult)
                .when(repository)
                .search(eq(Bundle.class), any(), any(Multimap.class));

        var ahvGroup = new QuestionnaireItemComponent()
                .setLinkId("ahvn13")
                .setType(QuestionnaireItemType.GROUP)
                .setDefinition(profileUrl + "#Patient.identifier:AHVN13")
                .setItem(List.of(new QuestionnaireItemComponent()
                        .setLinkId("ahvn13-value")
                        .setType(QuestionnaireItemType.STRING)
                        .setDefinition(profileUrl + "#Patient.identifier:AHVN13.value")));
        var zidGroup = new QuestionnaireItemComponent()
                .setLinkId("epr-spid")
                .setType(QuestionnaireItemType.GROUP)
                .setDefinition(profileUrl + "#Patient.identifier:EPR-SPID")
                .setItem(List.of(new QuestionnaireItemComponent()
                        .setLinkId("epr-spid-value")
                        .setType(QuestionnaireItemType.STRING)
                        .setDefinition(profileUrl + "#Patient.identifier:EPR-SPID.value")));
        var questionnaire = new Questionnaire().setItem(List.of(ahvGroup, zidGroup));
        questionnaire.addExtension(new Extension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                .setValue(new CanonicalType().setValue(profileUrl)));

        var ahvResponse = new QuestionnaireResponseItemComponent().setLinkId("ahvn13");
        ahvResponse.addItem(new QuestionnaireResponseItemComponent()
                .setLinkId("ahvn13-value")
                .addAnswer(new QuestionnaireResponseItemAnswerComponent().setValue(new StringType(ahvValue))));
        var zidResponse = new QuestionnaireResponseItemComponent().setLinkId("epr-spid");
        zidResponse.addItem(new QuestionnaireResponseItemComponent()
                .setLinkId("epr-spid-value")
                .addAnswer(new QuestionnaireResponseItemAnswerComponent().setValue(new StringType(zidValue))));
        var response = new QuestionnaireResponse().setItem(List.of(ahvResponse, zidResponse));

        var request = newExtractRequestForVersion(fhirVersion, libraryEngine, response, questionnaire);
        var actual = fixture.processDefinitionItem(request, new ItemPair(null, null));

        assertInstanceOf(Patient.class, actual);
        var identifiers = ((Patient) actual).getIdentifier();
        assertEquals(2, identifiers.size());
        assertTrue(identifiers.stream()
                .anyMatch(id -> ahvSystem.equals(id.getSystem()) && ahvValue.equals(id.getValue())));
        assertTrue(identifiers.stream()
                .anyMatch(id -> zidSystem.equals(id.getSystem()) && zidValue.equals(id.getValue())));
    }
}
