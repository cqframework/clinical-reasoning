package org.opencds.cqf.cql.evaluator.fhir.util;

import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.validation.FhirValidator;

public class ResourceValidator {
    protected FhirContext context;
    protected FhirValidator validator;

    public ResourceValidator(FhirContext context) {
        this.context = context;
        setValidator();
    }

    public ResourceValidator(FhirVersionEnum version) {
        this.context = FhirContext.forCached(version);
        setValidator();
    }

    protected void setValidator() {
        this.validator = this.context.newValidator();
        var module = new FhirInstanceValidator(this.context);
        this.validator.registerValidatorModule(module);
    }

    public IBaseResource validate(IBaseResource resource) {
        var validationResult = validator.validateWithResult(resource);

        if (validationResult.isSuccessful()) {
            return resource;
        } else {
            // var messages = validationResult.getMessages().stream().map(m -> m.getMessage()).collect(Collectors.toList());
            // var issues = String.join(",", messages);
            // throw new RuntimeException("Unable to validate resource. The following problems were found: " + issues);
            return validationResult.toOperationOutcome();
        }
    }
}
