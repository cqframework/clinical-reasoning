package org.opencds.cqf.fhir.cr.questionnaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.opencds.cqf.fhir.cr.questionnaire.helpers.PopulateRequestHelpers.newPopulateRequestForVersion;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemInitialComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.populate.ProcessResponseItem;
import org.opencds.cqf.fhir.utility.Constants;

@ExtendWith(MockitoExtension.class)
public class ProcessResponseItemTests {
    @Mock
    private LibraryEngine libraryEngine;

    @Spy
    private final ProcessResponseItem fixture = new ProcessResponseItem();

    private final ProcessResponseItem processResponseItem = new ProcessResponseItem();

    @Test
    void processResponseItemShouldSetBasePropertiesOnQuestionnaireResponseItemComponent() {
        // setup
        final String linkId = "linkId";
        final String definition = "definition";
        final StringType textElement = new StringType("textElement");
        final Questionnaire questionnaire = new Questionnaire();
        final PopulateRequest request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        final IBaseBackboneElement questionnaireItem = new QuestionnaireItemComponent()
                .setLinkId(linkId)
                .setDefinition(definition)
                .setTextElement(textElement);
        // execute
        final QuestionnaireResponseItemComponent actual =
                (QuestionnaireResponseItemComponent) fixture.processResponseItem(request, questionnaireItem);
        // validate
        assertEquals(linkId, actual.getLinkId());
        assertEquals(definition, actual.getDefinition());
        assertEquals(textElement, actual.getTextElement());
    }

    @Test
    void processResponseItemShouldProcessResponseItemsRecursivelyIfQuestionnaireItemHasItems() {
        // setup
        final Questionnaire questionnaire = new Questionnaire();
        final PopulateRequest request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        final QuestionnaireItemComponent questionnaireItem = new QuestionnaireItemComponent();
        final List<QuestionnaireItemComponent> nestedQuestionnaireItems = List.of(
                new QuestionnaireItemComponent().setLinkId("linkId1"),
                new QuestionnaireItemComponent().setLinkId("linkId2"),
                new QuestionnaireItemComponent().setLinkId("linkId3"));
        questionnaireItem.setItem(nestedQuestionnaireItems);
        List<QuestionnaireResponseItemComponent> expectedResponseItems = List.of(
                new QuestionnaireResponseItemComponent(),
                new QuestionnaireResponseItemComponent(),
                new QuestionnaireResponseItemComponent());
        doReturn(expectedResponseItems).when(fixture).processResponseItems(request, nestedQuestionnaireItems);
        // execute
        final QuestionnaireResponseItemComponent actual =
                (QuestionnaireResponseItemComponent) fixture.processResponseItem(request, questionnaireItem);
        // validate
        verify(fixture).processResponseItems(request, nestedQuestionnaireItems);
        assertEquals(3, actual.getItem().size());
        for (int i = 0; i < actual.getItem().size(); i++) {
            assertEquals(expectedResponseItems.get(i), actual.getItem().get(i));
        }
    }

    @Test
    void processResponseItemShouldSetAnswersIfTheQuestionnaireItemHasInitialValuesDstu3() {
        // setup
        final FhirVersionEnum fhirVersion = FhirVersionEnum.DSTU3;
        final org.hl7.fhir.dstu3.model.Questionnaire questionnaire = new org.hl7.fhir.dstu3.model.Questionnaire();
        final PopulateRequest request = newPopulateRequestForVersion(fhirVersion, libraryEngine, questionnaire);
        final List<IBaseDatatype> expectedValues = withTypeValues(fhirVersion);
        final IBaseBackboneElement questionnaireItemComponent =
                withQuestionnaireItemComponentWithInitialValues(fhirVersion, expectedValues);
        // execute
        final IBaseBackboneElement actual =
                processResponseItem.processResponseItem(request, questionnaireItemComponent);
        // validate
        validateQuestionnaireResponseItemAnswers(fhirVersion, expectedValues, actual);
    }

    @Test
    void processResponseItemShouldSetAnswersIfTheQuestionnaireItemHasInitialValuesR4() {
        // setup
        final FhirVersionEnum fhirVersion = FhirVersionEnum.R4;
        final Questionnaire questionnaire = new Questionnaire();
        final PopulateRequest request = newPopulateRequestForVersion(fhirVersion, libraryEngine, questionnaire);
        final List<IBaseDatatype> expectedValues = withTypeValues(fhirVersion);
        final IBaseBackboneElement questionnaireItemComponent =
                withQuestionnaireItemComponentWithInitialValues(fhirVersion, expectedValues);
        // execute
        final IBaseBackboneElement actual =
                processResponseItem.processResponseItem(request, questionnaireItemComponent);
        // validate
        validateQuestionnaireResponseItemAnswers(fhirVersion, expectedValues, actual);
    }

    @Test
    void processResponseItemShouldSetAnswersIfTheQuestionnaireItemHasInitialValuesR5() {
        // setup
        final FhirVersionEnum fhirVersion = FhirVersionEnum.R5;
        final org.hl7.fhir.r5.model.Questionnaire questionnaire = new org.hl7.fhir.r5.model.Questionnaire();
        final PopulateRequest request = newPopulateRequestForVersion(fhirVersion, libraryEngine, questionnaire);
        final List<IBaseDatatype> expectedValues = withTypeValues(fhirVersion);
        final IBaseBackboneElement questionnaireItemComponent =
                withQuestionnaireItemComponentWithInitialValues(fhirVersion, expectedValues);
        // execute
        final IBaseBackboneElement actual =
                processResponseItem.processResponseItem(request, questionnaireItemComponent);
        // validate
        validateQuestionnaireResponseItemAnswers(fhirVersion, expectedValues, actual);
    }

    @Test
    void setAnswersForInitialShouldPopulateQuestionnaireResponseItemWithAnswers() {
        // setup
        final FhirVersionEnum fhirVersion = FhirVersionEnum.R4;
        final Questionnaire questionnaire = new Questionnaire();
        final PopulateRequest request = newPopulateRequestForVersion(fhirVersion, libraryEngine, questionnaire);
        final var expectedValues = withTypeValues(fhirVersion);
        final IBaseBackboneElement questionnaireItemComponent =
                withQuestionnaireItemComponentWithInitialValues(fhirVersion, expectedValues);
        // execute
        final QuestionnaireResponseItemComponent actual =
                (QuestionnaireResponseItemComponent) fixture.setAnswersForInitial(
                        request, questionnaireItemComponent, new QuestionnaireResponseItemComponent());
        // validate
        validateQuestionnaireResponseItemAnswers(fhirVersion, expectedValues, actual);
    }

    private void validateQuestionnaireResponseItemAnswers(
            FhirVersionEnum fhirVersion, List<IBaseDatatype> expectedValues, IBaseBackboneElement responseItem) {
        switch (fhirVersion) {
            case DSTU3:
                var dstu3Item = (org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemComponent)
                        responseItem;
                assertEquals(1, dstu3Item.getAnswer().size());
                for (int i = 0; i < dstu3Item.getAnswer().size(); i++) {
                    final List<org.hl7.fhir.dstu3.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent>
                            answers = dstu3Item.getAnswer();
                    assertEquals(expectedValues.get(i), answers.get(i).getValue());
                }
                break;
            case R4:
                var r4Item = (QuestionnaireResponseItemComponent) responseItem;
                assertEquals(3, r4Item.getAnswer().size());
                for (int i = 0; i < r4Item.getAnswer().size(); i++) {
                    final List<QuestionnaireResponseItemAnswerComponent> answers = r4Item.getAnswer();
                    assertEquals(expectedValues.get(i), answers.get(i).getValue());
                }
                break;
            case R5:
                var r5Item =
                        (org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent) responseItem;
                assertEquals(3, r5Item.getAnswer().size());
                for (int i = 0; i < r5Item.getAnswer().size(); i++) {
                    final List<org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent>
                            answers = r5Item.getAnswer();
                    assertEquals(expectedValues.get(i), answers.get(i).getValue());
                }
                break;

            default:
                break;
        }
    }

    @Test
    void processResponseItemShouldAddExtensionIfResponseExtensionPresent() {
        // setup
        final Questionnaire questionnaire = new Questionnaire();
        final PopulateRequest request = newPopulateRequestForVersion(FhirVersionEnum.R4, libraryEngine, questionnaire);
        final QuestionnaireItemComponent questionnaireItemComponent = new QuestionnaireItemComponent();
        final Extension extension = new Extension(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR, new StringType("theAuthor"));
        questionnaireItemComponent.addExtension(extension);
        // execute
        final QuestionnaireResponseItemComponent actual =
                (QuestionnaireResponseItemComponent) fixture.processResponseItem(request, questionnaireItemComponent);
        // validate
        assertEquals(extension, actual.getExtensionByUrl(Constants.QUESTIONNAIRE_RESPONSE_AUTHOR));
    }

    private IBaseBackboneElement withQuestionnaireItemComponentWithInitialValues(
            FhirVersionEnum fhirVersion, List<IBaseDatatype> initialValues) {
        switch (fhirVersion) {
            case DSTU3:
                final var dstu3QuestionnaireItemComponent =
                        new org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent();
                dstu3QuestionnaireItemComponent.setInitial((org.hl7.fhir.dstu3.model.Type) initialValues.get(0));
                return dstu3QuestionnaireItemComponent;
            case R4:
                final var r4QuestionnaireItemComponent = new QuestionnaireItemComponent();
                final var r4InitialComponents = initialValues.stream()
                        .map(v -> (QuestionnaireItemInitialComponent) withInitialWithValue(fhirVersion, v))
                        .collect(Collectors.toList());
                r4InitialComponents.forEach(r4QuestionnaireItemComponent::addInitial);
                return r4QuestionnaireItemComponent;
            case R5:
                final var r5QuestionnaireItemComponent =
                        new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent();
                final var r5InitialComponents = initialValues.stream()
                        .map(v -> (org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemInitialComponent)
                                withInitialWithValue(fhirVersion, v))
                        .collect(Collectors.toList());
                r5InitialComponents.forEach(r5QuestionnaireItemComponent::addInitial);
                return r5QuestionnaireItemComponent;

            default:
                return null;
        }
    }

    private IBaseBackboneElement withInitialWithValue(FhirVersionEnum fhirVersion, IBaseDatatype value) {
        switch (fhirVersion) {
            case R4:
                final var r4InitialComponent = new QuestionnaireItemInitialComponent();
                r4InitialComponent.setValue((Type) value);
                return r4InitialComponent;
            case R5:
                final var r5InitialComponent =
                        new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemInitialComponent();
                r5InitialComponent.setValue((org.hl7.fhir.r5.model.DataType) value);
                return r5InitialComponent;

            default:
                return null;
        }
    }

    private List<IBaseDatatype> withTypeValues(FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                return List.of(new org.hl7.fhir.dstu3.model.StringType("sample string"));
            case R4:
                return List.of(
                        new org.hl7.fhir.r4.model.BooleanType(true),
                        new org.hl7.fhir.r4.model.StringType("sample string"),
                        new org.hl7.fhir.r4.model.IntegerType(3));
            case R5:
                return List.of(
                        new org.hl7.fhir.r5.model.BooleanType(true),
                        new org.hl7.fhir.r5.model.StringType("sample string"),
                        new org.hl7.fhir.r5.model.IntegerType(3));

            default:
                return null;
        }
    }
}
