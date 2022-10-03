package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.stream.Collectors;

import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.opencds.cqf.cql.evaluator.fhir.util.ResourceValidator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;

public class CqfMeasureValidator extends ResourceValidator {
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
        var supportChain = new ValidationSupportChain();
        var defaultSupport = new DefaultProfileValidationSupport(this.context);
        supportChain.addValidationSupport(defaultSupport);
        supportChain.addValidationSupport(new CommonCodeSystemsTerminologyService(this.context));
        supportChain.addValidationSupport(new InMemoryTerminologyServerValidationSupport(this.context));

        // var resources = new ArrayList<IBaseResource>();
        // try {
        //     var files = IOUtils.readLines(CqfMeasureValidator.class.getClassLoader().getResourceAsStream("org/opencds/cqf/cql/evaluator/measure/r4"), StandardCharsets.UTF_8);
        //     // System.out.println(files);

        //     // IBaseResource res = this.parser.parseResource(CqfMeasureValidator.class.getResourceAsStream(files.get(0)));
        //     // System.out.println(res.getIdElement().getIdPart());
        //     resources.add(this.parser.parseResource(CqfMeasureValidator.class.getResourceAsStream(files.get(0))));

        // } catch (Exception e) {
        //     e.printStackTrace();
        // }

        
        var cqfMeasureSupport = new PrePopulatedValidationSupport(this.context);
        // for (var resource : resources) {
        //     cqfMeasureSupport.addResource(resource);
        // }
        var profileBundle = this.parser.parseResource(Bundle.class, CqfMeasureValidator.class.getResourceAsStream("CqfMeasureValidationProfiles-bundle.json"));
        for (var resource : profileBundle.getEntry().stream().map(BundleEntryComponent::getResource).collect(Collectors.toList()))
        {
            cqfMeasureSupport.addResource(resource);
        }
        supportChain.addValidationSupport(cqfMeasureSupport);

        var cache = new CachingValidationSupport(supportChain);
        var validatorModule = new FhirInstanceValidator(cache);
        this.validator = this.context.newValidator().registerValidatorModule(validatorModule);
    }

    @Override
    public IBaseResource validate(IBaseResource measure) {
        var validationResult = this.validator.validateWithResult(measure);

        if (validationResult.isSuccessful()) {
            return measure;
        } else {
            // var messages = validationResult.getMessages().stream().map(m ->
            // m.getMessage()).collect(Collectors.toList());
            // var issues = String.join(",", messages);
            // throw new RuntimeException("Unable to validate MeasureReport resource. The
            // following problems were found: " + issues);
            return validationResult.toOperationOutcome();
        }
    }
}
