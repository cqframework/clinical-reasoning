package org.opencds.cqf.cql.evaluator.questionnaire.r4.nestedquestionnaireitem;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.opencds.cqf.cql.evaluator.questionnaire.r4.bundle.BundleParser;
import org.opencds.cqf.cql.evaluator.questionnaire.r4.exceptions.QuestionnaireParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class QuestionnaireTypeIsChoice {
  protected static final Logger logger = LoggerFactory.getLogger(QuestionnaireTypeIsChoice.class);
  protected BundleParser parsingService;
  public QuestionnaireItemComponent addProperties(
      ElementDefinition element,
      QuestionnaireItemComponent item
  ) throws QuestionnaireParsingException {
    final String valueSetUrl = element.getBinding().getValueSet();
    final ValueSet valueSet = parsingService.getValueSet(valueSetUrl);
    if (valueSet.hasExpansion()) {
      valueSetIsExpanded(valueSet, item);
    } else {
      valueSetIsNotExpanded(valueSet, item);
    }
    return item;
  }

  protected void valueSetIsExpanded(ValueSet valueSet, QuestionnaireItemComponent item) {
    final List<ValueSetExpansionContainsComponent> expansion = valueSet.getExpansion().getContains();
    for (var code : expansion) {
      Coding coding = getCoding(code);
      item.addAnswerOption().setValue(coding);
    }
  }

  protected void valueSetIsNotExpanded(ValueSet valueSet, QuestionnaireItemComponent item) {
    final List<ConceptSetComponent> systems = valueSet.getCompose().getInclude();
    for (var system : systems) {
      final String systemUri = system.getSystem();
      for (var concept : system.getConcept()) {
        Coding coding = getCoding(concept, systemUri);
        item.addAnswerOption().setValue(coding);
      }
    }
  }

  protected Coding getCoding(ConceptReferenceComponent code, String systemUri) {
    return new Coding().setCode(code.getCode()).setSystem(systemUri).setDisplay(code.getDisplay());
  }

  protected Coding getCoding(ValueSetExpansionContainsComponent code) {
    return new Coding().setCode(code.getCode()).setSystem(code.getSystem()).setDisplay(code.getDisplay());
  }
}
