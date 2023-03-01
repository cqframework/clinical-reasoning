package org.opencds.cqf.cql.evaluator.questionnaire.r4;

import static org.opencds.cqf.cql.evaluator.fhir.Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.cql.evaluator.expression.ExpressionEvaluator;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.opencds.cqf.cql.evaluator.questionnaire.BaseQuestionnaireProcessor;

import ca.uhn.fhir.context.FhirContext;

public class QuestionnaireProcessor extends BaseQuestionnaireProcessor<Questionnaire> {
  public QuestionnaireProcessor(FhirContext fhirContext, FhirDal fhirDal,
      LibraryProcessor libraryProcessor, ExpressionEvaluator expressionEvaluator) {
    super(fhirContext, fhirDal, libraryProcessor, expressionEvaluator);
  }

  @Override
  public Object resolveParameterValue(IBase value) {
    if (value == null)
      return null;
    return ((Parameters.ParametersParameterComponent) value).getValue();
  }

  @Override
  public IBaseResource getSubject() {
    return this.fhirDal.read(new IdType("Patient", this.patientId));
  }

  @Override
  public Questionnaire prePopulate(Questionnaire questionnaire, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpoint,
      IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
    if (questionnaire == null) {
      throw new IllegalArgumentException("No questionnaire passed in");
    }
    this.patientId = patientId;
    this.parameters = parameters;
    this.bundle = bundle;
    this.dataEndpoint = dataEndpoint;
    this.contentEndpoint = contentEndpoint;
    this.terminologyEndpoint = terminologyEndpoint;

    var libraryUrl =
        ((CanonicalType) questionnaire.getExtensionByUrl(Constants.CQF_LIBRARY).getValue())
            .getValue();
    var oc = new OperationOutcome();
    oc.setId("prepopulate-outcome-" + questionnaire.getIdPart());

    processItems(questionnaire.getItem(), libraryUrl, oc);

    if (oc.getIssue().size() > 0) {
      questionnaire.addContained(oc);
      questionnaire.addExtension(Constants.EXT_CRMI_MESSAGES, new Reference("#" + oc.getIdPart()));
    }

    return questionnaire;
  }

  protected void processItems(List<QuestionnaireItemComponent> items, String defaultLibrary,
      OperationOutcome oc) {
    items.forEach(item -> {
      if (item.hasItem()) {
        processItems(item.getItem(), defaultLibrary, oc);
      } else {
        if (item.hasExtension(Constants.CQF_EXPRESSION)) {
          // evaluate expression and set the result as the initialAnswer on the item
          var expression = (Expression) item.getExtensionByUrl(Constants.CQF_EXPRESSION).getValue();
          var libraryUrl = expression.hasReference() ? expression.getReference() : defaultLibrary;
          try {
            var result = getExpressionResult(expression.getExpression(), expression.getLanguage(),
                libraryUrl, this.parameters);
            // TODO: what to do with choice answerOptions of type valueCoding with an
            // expression that returns a valueString
            item.addInitial(
                new Questionnaire.QuestionnaireItemInitialComponent().setValue((Type) result));
          } catch (Exception ex) {
            var message =
                String.format("Error encountered evaluating expression (%s) for item (%s): %s",
                    expression.getExpression(), item.getLinkId(), ex.getMessage());
            logger.error(message);
            oc.addIssue().setCode(OperationOutcome.IssueType.EXCEPTION)
                .setSeverity(OperationOutcome.IssueSeverity.ERROR).setDiagnostics(message);
          }
        }
      }
    });
  }

  @Override
  public IBaseResource populate(Questionnaire questionnaire, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpopint,
      IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
    var populatedQuestionnaire = prePopulate(questionnaire, patientId, parameters, bundle,
        dataEndpopint, contentEndpoint, terminologyEndpoint);
    var response = new QuestionnaireResponse();
    response.setId(populatedQuestionnaire.getIdPart() + "-response");
    if (questionnaire.hasExtension(Constants.EXT_CRMI_MESSAGES)) {
      var ocExt = questionnaire.getExtensionByUrl(Constants.EXT_CRMI_MESSAGES);
      var ocId = ((Reference) ocExt.getValue()).getReference().replaceFirst("#", "");
      var ocList = questionnaire.getContained().stream()
          .filter(resource -> resource.getIdPart().equals(ocId)).collect(Collectors.toList());
      var oc = ocList == null || ocList.size() == 0 ? null : ocList.get(0);
      if (oc != null) {
        oc.setId("populate-outcome-" + populatedQuestionnaire.getIdPart());
        response.addContained(oc);
        response.addExtension(Constants.EXT_CRMI_MESSAGES, new Reference("#" + oc.getIdPart()));
      }
    }
    response.setQuestionnaire(populatedQuestionnaire.getUrl());
    response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
    response.setSubject(new Reference(new IdType("Patient", patientId)));
    var responseItems = new ArrayList<QuestionnaireResponseItemComponent>();
    processResponseItems(populatedQuestionnaire.getItem(), responseItems);
    response.setItem(responseItems);

    return response;
  }

  protected void processResponseItems(List<QuestionnaireItemComponent> items,
      List<QuestionnaireResponseItemComponent> responseItems) {
    items.forEach(item -> {
      var responseItem =
          new QuestionnaireResponse.QuestionnaireResponseItemComponent(item.getLinkIdElement());
      responseItem.setDefinition(item.getDefinition());
      responseItem.setTextElement(item.getTextElement());
      if (item.hasItem()) {
        var nestedResponseItems = new ArrayList<QuestionnaireResponseItemComponent>();
        processResponseItems(item.getItem(), nestedResponseItems);
        responseItem.setItem(nestedResponseItems);
      } else if (item.hasInitial()) {
        item.getInitial().forEach(answer -> {
          responseItem
              .addAnswer(new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                  .setValue(answer.getValue()));
        });
      }
      responseItems.add(responseItem);
    });
  }

  @Override
  public Questionnaire generateQuestionnaire(String theId, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, IBaseResource dataEndpoint,
      IBaseResource contentEndpoint, IBaseResource terminologyEndpoint) {
    this.patientId = patientId;
    this.parameters = parameters;
    this.bundle = bundle;
    this.dataEndpoint = dataEndpoint;
    this.contentEndpoint = contentEndpoint;
    this.terminologyEndpoint = terminologyEndpoint;

    var questionnaire = new Questionnaire();
    questionnaire.setId(new IdType("Questionnaire", theId));

    return questionnaire;
  }

  public Questionnaire.QuestionnaireItemComponent generateItem(DataRequirement actionInput,
      Integer itemCount) {
    if (!actionInput.hasProfile() || actionInput.getProfile().size() == 0) {
      throw new IllegalArgumentException("No profile defined for input. Unable to generate item.");
    }

    var linkId = String.valueOf(itemCount + 1);
    try {
      var profileUrl = actionInput.getProfile().get(0).getValue();
      var profile = getProfileDefinition(profileUrl);
      if (profile == null) {
        var message =
            String.format("Unable to retrieve StructureDefinition for profile: %s", profileUrl);
        logger.error(message);
        throw new IllegalArgumentException(message);
      }
      // TODO: define an extension for the text?
      var text = profile.hasTitle() ? profile.getTitle()
          : profileUrl.substring(profileUrl.lastIndexOf("/") + 1);
      var item = new QuestionnaireItemComponent().setType(Questionnaire.QuestionnaireItemType.GROUP)
          .setLinkId(linkId).setText(text);
      item.addExtension(new Extension().setUrl(SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
          .setValue(new CodeType().setValue(profile.getType())));

      var paths = new ArrayList<String>();
      processElements(profile.getDifferential().getElement(), profile, item, paths);
      // Should we do this?
      // var requiredElements = profile.getSnapshot().getElement().stream()
      // .filter(e -> !paths.contains(e.getPath()) && e.getPath().split("\\.").length == 2 &&
      // e.getMin() > 0).collect(Collectors.toList());
      // processElements(requiredElements, profile, item, paths);

      return item;
    } catch (Exception ex) {
      var message = String.format("An error occurred during item creation: %s", ex.getMessage());
      logger.error(message);

      return createErrorItem(linkId, message);
    }
  }

  protected Questionnaire.QuestionnaireItemComponent createErrorItem(String linkId,
      String errorMessage) {
    return new QuestionnaireItemComponent().setLinkId(linkId).setType(QuestionnaireItemType.DISPLAY)
        .setText(errorMessage);
  }

  protected StructureDefinition getProfileDefinition(String url) {
    StructureDefinition profile = null;
    try {
      // Check fhirDal first
      var searchResult = this.fhirDal.searchByUrl("StructureDefinition", url);
      if (searchResult.iterator().hasNext()) {
        profile = (StructureDefinition) searchResult.iterator().next();
      }
      if (profile == null) {
        // Check contentEndpoint
        // profile = this.contentEndpoint.
      }
    } catch (Exception ex) {
      logger.error("Error retrieving definition (%s): %s", url, ex.getMessage());
    }

    return profile;
  }

  protected ValueSet getValueSet(String url) {
    ValueSet valueSet = null;
    try {
      var searchResult = this.fhirDal.searchByUrl("ValueSet", url);
      if (searchResult.iterator().hasNext()) {
        valueSet = (ValueSet) searchResult.iterator().next();
      }
      if (valueSet == null) {
        // Check terminologyEndpoint
      }
    } catch (Exception ex) {
      logger.error("Error retrieving ValueSet (%s): %s", url, ex.getMessage());
    }

    return valueSet;
  }

  protected void processElements(List<ElementDefinition> elements, StructureDefinition profile,
      Questionnaire.QuestionnaireItemComponent item, List<String> paths) {
    var childCount = item.getItem().size();
    for (var element : elements) {
      var elementType = getElementType(element, profile);
      if (elementType == null) {
        continue;
      }
      childCount++;
      var childLinkId = String.format("%s.%s", item.getLinkId(), childCount);
      try {
        var itemType = getItemType(elementType, element.hasBinding());
        if (itemType == null) {
          var message = String.format("Unable to determine type for element: %s", element.getId());
          logger.warn(message);
          item.addItem(createErrorItem(childLinkId, message));
          continue;
        }
        var definition = profile.getUrl() + "#" + element.getPath();
        var childText = element.hasLabel() ? element.getLabel()
            : element.hasShort() ? element.getShort() : element.getPath();
        var childItem = new QuestionnaireItemComponent().setType(itemType).setDefinition(definition)
            .setLinkId(childLinkId).setText(childText);
        if (itemType == QuestionnaireItemType.CHOICE && element.hasBinding()) {
          var valueSetUrl = element.getBinding().getValueSet();
          var valueSet = getValueSet(valueSetUrl);
          if (valueSet == null) {
            var message =
                String.format("Unable to retrieve ValueSet for Choice item: %s", valueSetUrl);
            item.addItem(createErrorItem(childLinkId, message));
            continue;
          } else {
            if (valueSet.hasExpansion()) {
              var expansion = valueSet.getExpansion().getContains();
              for (var code : expansion) {
                childItem.addAnswerOption().setValue(new Coding().setCode(code.getCode())
                    .setSystem(code.getSystem()).setDisplay(code.getDisplay()));
              }
            } else {
              var systems = valueSet.getCompose().getInclude();
              for (var system : systems) {
                var systemUri = system.getSystem();
                for (var concept : system.getConcept()) {
                  childItem.addAnswerOption().setValue(new Coding().setSystem(systemUri)
                      .setCode(concept.getCode()).setDisplay(concept.getDisplay()));
                }
              }
            }
          }
        }
        if (element.hasFixedOrPattern()) {
          childItem.addInitial().setValue(element.getFixedOrPattern());
          childItem.addExtension(Constants.SDC_QUESTIONNAIRE_HIDDEN, new BooleanType(true));
          childItem.setReadOnly(true);
        } else if (element.hasDefaultValue()) {
          childItem.addInitial().setValue(element.getDefaultValue());
        } else if (element.hasExtension(Constants.CQF_EXPRESSION)) {
          var expression =
              (Expression) element.getExtensionByUrl(Constants.CQF_EXPRESSION).getValue();
          var result = getExpressionResult(expression.getExpression(), expression.getLanguage(),
              expression.getReference(), parameters);
          childItem.addInitial().setValue((Type) result);
        }
        childItem.setRequired(element.hasMin() && element.getMin() == 1);
        // set readonly based on?
        // set repeat based on?
        // set enableWhen based on?
        item.addItem(childItem);
        paths.add(element.getPath());
      } catch (Exception ex) {
        var message = String.format("An error occurred during item creation: %s", ex.getMessage());
        logger.error(message);
        item.addItem(createErrorItem(childLinkId, message));
      }
    }
  }

  protected String getElementType(ElementDefinition element, StructureDefinition profile) {
    var elementType = element.hasType() ? element.getType().get(0).getCode() : null;
    // if (elementType == null) {
    // var snapshot = profile.getSnapshot().getElement().stream().filter(e ->
    // e.getId().equals(element.getId())).collect(Collectors.toList());
    // elementType = snapshot == null || snapshot.size() == 0 ? null : snapshot.get(0).hasType()
    // ? snapshot.get(0).getType().get(0).getCode() : null;
    // }

    return elementType;
  }

  protected QuestionnaireItemType getItemType(String elementType, Boolean hasBinding) {
    if (hasBinding) {
      return QuestionnaireItemType.CHOICE;
    }

    switch (elementType) {
      case "CodeableConcept":
        return QuestionnaireItemType.CHOICE;
      case "Reference":
      case "uri":
        return QuestionnaireItemType.STRING;
      case "BackboneElement":
        return QuestionnaireItemType.GROUP;
      default:
        return QuestionnaireItemType.fromCode(elementType);
    }
  }
}
