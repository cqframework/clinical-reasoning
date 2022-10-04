package org.opencds.cqf.cql.evaluator.fhir.util;

import java.util.List;
import java.util.stream.Collectors;

//import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;

public class ResourceValidator {
    protected FhirContext context;
    protected FhirValidator validator;
    protected List<String> ignoreKeys;

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
    }

    public IBaseResource validate(IBaseResource resource) {
        return this.validate(resource, false);
    }
    public IBaseResource validate(IBaseResource resource, Boolean error) {
        var validationResult = this.validator.validateWithResult(resource);
        var errors = validationResult.getMessages().stream().filter(m ->
            m.getSeverity().compareTo(ResultSeverityEnum.ERROR) > -1 &&
            this.ignoreKeys.stream().noneMatch(m.getMessage()::contains)
        ).collect(Collectors.toList());

        if (errors.isEmpty()) {
            return resource;
        }

        if (Boolean.TRUE.equals(error)) {
            var messages = errors.stream().map(SingleValidationMessage::getMessage).collect(Collectors.toList());
            var issues = String.join("; ", messages);
            throw new RuntimeException("Unable to validate resource. The following problems were found: " + issues);
        } else {
            return validationResult.toOperationOutcome();
        }
    }
}
