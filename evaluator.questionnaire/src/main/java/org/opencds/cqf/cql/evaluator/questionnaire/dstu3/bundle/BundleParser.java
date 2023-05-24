package org.opencds.cqf.cql.evaluator.questionnaire.dstu3.bundle;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.opencds.cqf.cql.evaluator.questionnaire.dstu3.exceptions.QuestionnaireParsingException;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

public class BundleParser {
  protected Repository repository;
  protected static final Logger logger = LoggerFactory.getLogger(BundleParser.class);
  protected static final String BUNDLE_PARSING_ERROR = "Error retrieving definition (%s): %s";
  protected static final String VALUE_SET_PARSING_ERROR = "Error retrieving ValueSet (%s): %s";
  protected static final String STRUCTURE_DEFINITION_ERROR = "Unable to retrieve StructureDefinition for profile: %s";
  protected static final String VALUE_SET_ERROR = "Unable to retrieve ValueSet for Choice item: %s";

  public StructureDefinition getProfileDefinition(DataRequirement actionInput) {
    final String profileUrl = actionInput.getProfile().get(0).getValue();
    final Optional<StructureDefinition> profile = parseProfileFromBundle(profileUrl);
    if (!profile.isPresent()) {
      throw new IllegalArgumentException(String.format(STRUCTURE_DEFINITION_ERROR, profileUrl));
    }
    return profile.get();
  }

  public ValueSet getValueSet(String url) throws QuestionnaireParsingException {
    final Optional<ValueSet> valueSet = parseValueSet(url);
    if (!valueSet.isPresent()) {
      throw new QuestionnaireParsingException(String.format(VALUE_SET_ERROR, url));
    }
    return valueSet.get();
  }
  protected Optional<StructureDefinition> parseProfileFromBundle(String profileUrl) {
    try {
      final Bundle bundle = repository.search(Bundle.class, StructureDefinition.class, Searches.byUrl(profileUrl));
      return bundle.getEntry().stream()
          .findFirst()
          .map(BundleEntryComponent::getResource)
          .map(StructureDefinition.class::cast);
    } catch (Exception ex) {
      logger.error(String.format(BUNDLE_PARSING_ERROR, profileUrl, ex.getMessage()));
    }
    return Optional.empty();
  }

  protected Optional<ValueSet> parseValueSet(String url) {
    try {
      final Bundle searchBundle = repository.search(Bundle.class, ValueSet.class, Searches.byUrl(url));
      return searchBundle.getEntry().stream()
          .findFirst()
          .map(BundleEntryComponent::getResource)
          .map(ValueSet.class::cast);
    } catch (Exception ex) {
      logger.error(String.format(VALUE_SET_PARSING_ERROR, url, ex.getMessage()));
    }
    return Optional.empty();
  }
}
