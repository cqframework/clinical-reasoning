package org.opencds.cqf.cql.evaluator.questionnaire.r5;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.DataRequirement;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.ValueSet;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionnaireItemGenerator {
  protected static final Logger logger = LoggerFactory.getLogger(QuestionnaireItemGenerator.class);
  protected Repository repository;
  protected String patientId;
  protected IBaseParameters parameters;
  protected IBaseBundle bundle;
  protected LibraryEngine libraryEngine;
  protected static final String subjectType = "Patient";

  public QuestionnaireItemGenerator(Repository repository, String patientId,
      IBaseParameters parameters, IBaseBundle bundle, LibraryEngine libraryEngine) {
    this.repository = repository;
    this.patientId = patientId;
    this.parameters = parameters;
    this.bundle = bundle;
    this.libraryEngine = libraryEngine;
  }

  public Questionnaire.QuestionnaireItemComponent generateItem(DataRequirement actionInput,
      int itemCount) {
    if (!actionInput.hasProfile()) {
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
      item.addExtension(new Extension().setUrl(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
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
      var searchResult =
          repository.search(Bundle.class, StructureDefinition.class, Searches.byUrl(url));
      if (searchResult.hasEntry()) {
        profile = (StructureDefinition) searchResult.getEntry().get(0).getResource();
      }
    } catch (Exception ex) {
      logger.error("Error retrieving definition (%s): %s", url, ex.getMessage());
    }

    return profile;
  }

  protected ValueSet getValueSet(String url) {
    ValueSet valueSet = null;
    try {
      var searchResult = repository.search(Bundle.class, ValueSet.class, Searches.byUrl(url));
      if (searchResult.hasEntry()) {
        valueSet = (ValueSet) searchResult.getEntry().get(0).getResource();
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
      var elementType = getElementType(element);
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
        var childText = getElementText(element);
        var childItem = new QuestionnaireItemComponent().setType(itemType).setDefinition(definition)
            .setLinkId(childLinkId).setText(childText);
        if (itemType == QuestionnaireItemType.QUESTION && element.hasBinding()) {
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
          var results = this.libraryEngine.getExpressionResult(this.patientId, subjectType,
              expression.getExpression(), expression.getLanguage(), expression.getReference(),
              parameters, this.bundle);
          for (var result : results) {
            childItem.addInitial().setValue((DataType) result);
          }
        }
        childItem.setRequired(element.hasMin() && element.getMin() == 1);
        // set readonly based on?
        // set repeat based on? if expression result type is a list?
        // set enableWhen based on? use
        // http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-enableWhenExpression
        item.addItem(childItem);
        paths.add(element.getPath());
      } catch (Exception ex) {
        var message = String.format("An error occurred during item creation: %s", ex.getMessage());
        logger.error(message);
        item.addItem(createErrorItem(childLinkId, message));
      }
    }
  }

  protected String getElementText(ElementDefinition element) {
    return element.hasLabel() ? element.getLabel()
        : element.hasShort() ? element.getShort() : element.getPath();
  }

  protected String getElementType(ElementDefinition element) { // , StructureDefinition profile) {
    return element.hasType() ? element.getType().get(0).getCode() : null;
    // if (elementType == null) {
    // var snapshot = profile.getSnapshot().getElement().stream().filter(e ->
    // e.getId().equals(element.getId())).collect(Collectors.toList());
    // elementType = snapshot == null || snapshot.size() == 0 ? null : snapshot.get(0).hasType()
    // ? snapshot.get(0).getType().get(0).getCode() : null;
    // }

    // return elementType;
  }

  protected QuestionnaireItemType getItemType(String elementType, Boolean hasBinding) {
    if (Boolean.TRUE.equals(hasBinding)) {
      return QuestionnaireItemType.QUESTION;
    }

    switch (elementType) {
      case "CodeableConcept":
        return QuestionnaireItemType.QUESTION;
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
