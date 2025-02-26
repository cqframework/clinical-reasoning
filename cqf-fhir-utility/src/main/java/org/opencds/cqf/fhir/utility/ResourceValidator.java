package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.opencds.cqf.fhir.api.Repository;

public class ResourceValidator {
    protected Repository repo;
    protected FhirContext context;
    protected FhirValidator validator;
    protected Map<String, ValidationProfile> profiles;

    public ResourceValidator(FhirContext context, Map<String, ValidationProfile> profiles, Repository repo) {
        this.repo = repo;
        this.context = context;
        this.profiles = profiles == null ? new HashMap<>() : profiles;
        setValidator();
    }

    public ResourceValidator(FhirVersionEnum version, Map<String, ValidationProfile> profiles, Repository repo) {
        this.repo = repo;
        this.context = FhirContext.forCached(version);
        this.profiles = profiles == null ? new HashMap<>() : profiles;
        setValidator();
    }

    protected void setValidator() {
        if (this.profiles.isEmpty()) {
            this.validator = this.context.newValidator();
        } else {
            var supportChain = new ValidationSupportChain();
            supportChain.addValidationSupport(new DefaultProfileValidationSupport(this.context));
            supportChain.addValidationSupport(new CommonCodeSystemsTerminologyService(this.context));
            supportChain.addValidationSupport(new InMemoryTerminologyServerValidationSupport(this.context));

            var profileSupport = new PrePopulatedValidationSupport(this.context);
            for (var profile : this.profiles.entrySet()) {
                var ig = this.repo.read(
                        ImplementationGuide.class,
                        new IdType("ImplementationGuide", profile.getValue().getName()));
                if (ig == null) {
                    continue;
                }
                for (var resourceComponent : ig.getDefinition().getResource()) {
                    if (Arrays.asList("CodeSystem", "StructureDefinition", "ValueSet")
                            .contains(resourceComponent
                                    .getReference()
                                    .getReference()
                                    .split("/")[0])) {
                        try {
                            var resource = this.repo.read(
                                    IBaseResource.class,
                                    new IdType(resourceComponent.getReference().getReference()));
                            if (resource != null) {
                                profileSupport.addResource(resource);
                            }
                        } catch (Exception e) {
                            // TODO: Log exception
                        }
                    }
                }
            }

            supportChain.addValidationSupport(profileSupport);

            this.validator = this.context
                    .newValidator()
                    .registerValidatorModule(new FhirInstanceValidator(supportChain));
        }
    }

    public IBaseResource validate(IBaseResource resource) {
        return this.validate(resource, false);
    }

    public IBaseResource validate(IBaseResource resource, Boolean error) {
        var validationResult = this.validator.validateWithResult(resource);
        var errors = validationResult.getMessages().stream()
                .filter(m -> m.getSeverity().compareTo(ResultSeverityEnum.ERROR) > -1
                        && this.profiles.entrySet().stream()
                                .flatMap(p -> p.getValue().getIgnoreKeys().stream())
                                .noneMatch(m.getMessage()::contains))
                .collect(Collectors.toList());

        if (errors.isEmpty()) {
            return resource;
        }

        if (Boolean.TRUE.equals(error)) {
            var messages =
                    errors.stream().map(SingleValidationMessage::getMessage).collect(Collectors.toList());
            var issues = String.join("; ", messages);
            throw new RuntimeException("Unable to validate resource. The following problems were found: " + issues);
        } else {
            return validationResult.toOperationOutcome();
        }
    }
}
