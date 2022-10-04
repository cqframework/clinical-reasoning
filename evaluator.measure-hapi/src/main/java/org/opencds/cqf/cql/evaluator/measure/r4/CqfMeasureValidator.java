package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.opencds.cqf.cql.evaluator.fhir.util.ResourceValidator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;

public class CqfMeasureValidator extends ResourceValidator {

    public static void main(String[] args) {
        new CqfMeasureValidator(FhirContext.forR4()).setValidator();
    }
    protected IParser parser;

    public CqfMeasureValidator(FhirContext context) {
        super(context);
    }

    public CqfMeasureValidator(FhirVersionEnum version) {
        // We only have the cqf-measure profiles for R4
        super(FhirVersionEnum.R4);
    }

    @Override
    protected void setValidator() {
        this.parser = this.context.newJsonParser();
        this.ignoreKeys = Arrays.asList("exc-1", "exc-2", "exc-3", "cmp-32", "cmp-33", "cmp-34", "cmp-35");
        var supportChain = new ValidationSupportChain();
        supportChain.addValidationSupport(new DefaultProfileValidationSupport(this.context));
        supportChain.addValidationSupport(new CommonCodeSystemsTerminologyService(this.context));
        supportChain.addValidationSupport(new InMemoryTerminologyServerValidationSupport(this.context));

        var cqfMeasureSupport = new PrePopulatedValidationSupport(this.context);
        var profileBundle = this.parser.parseResource(Bundle.class, CqfMeasureValidator.class.getResourceAsStream("CqfMeasureValidationProfiles-bundle.json"));
        for (var resource : profileBundle.getEntry().stream().map(BundleEntryComponent::getResource).collect(Collectors.toList()))
        {
            if (resource != null) {
                cqfMeasureSupport.addResource(resource);
            }
        }
        supportChain.addValidationSupport(cqfMeasureSupport);

        this.validator = this.context.newValidator().registerValidatorModule(new FhirInstanceValidator(new CachingValidationSupport(supportChain)));
    }
}
