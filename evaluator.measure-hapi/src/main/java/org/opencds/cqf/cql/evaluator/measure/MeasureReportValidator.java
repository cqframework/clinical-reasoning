package org.opencds.cqf.cql.evaluator.measure;

import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.validation.FhirValidator;

public class MeasureReportValidator {
  private FhirContext context;
  private FhirValidator validator;
  // private IParser parser;

  public MeasureReportValidator(FhirVersionEnum version) {
    this.context = FhirContext.forCached(version);
    this.validator = this.context.newValidator();
    var module = new FhirInstanceValidator(this.context);
    this.validator.registerValidatorModule(module);
    // this.parser = context.newXmlParser();
  }

  public IBaseResource validate(IBaseResource measureReport) {
    var validationResult = validator.validateWithResult(measureReport);

    if (validationResult.isSuccessful()) {
        return measureReport;
    } else {
        // var messages = validationResult.getMessages().stream().map(m -> m.getMessage()).collect(Collectors.toList());
        // var issues = String.join(",", messages);
        // throw new RuntimeException("Unable to validate MeasureReport resource. The following problems were found: " + issues);
        return validationResult.toOperationOutcome();
    }
  }
}
