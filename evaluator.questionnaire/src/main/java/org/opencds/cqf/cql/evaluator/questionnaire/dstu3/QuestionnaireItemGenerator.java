package org.opencds.cqf.cql.evaluator.questionnaire.dstu3;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.library.LibraryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionnaireItemGenerator {
  protected static final Logger logger = LoggerFactory.getLogger(QuestionnaireItemGenerator.class);

  protected FhirDal fhirDal;
  protected String patientId;
  protected IBaseParameters parameters;
  protected IBaseBundle bundle;
  protected LibraryEngine libraryEngine;
  protected static final String subjectType = "Patient";

  public QuestionnaireItemGenerator(FhirDal fhirDal, String patientId, IBaseParameters parameters,
      IBaseBundle bundle, LibraryEngine libraryEngine) {
    this.fhirDal = fhirDal;
    this.patientId = patientId;
    this.parameters = parameters;
    this.bundle = bundle;
    this.libraryEngine = libraryEngine;
  }

  public Questionnaire.QuestionnaireItemComponent generateItem(DataRequirement actionInput,
      Integer itemCount) {
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
    throw new NotImplementedException();
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
