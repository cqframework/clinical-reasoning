package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.validation.ValidatorResourceFetcher;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.validation.FhirValidator;
import java.io.IOException;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.opencds.cqf.fhir.cr.common.IValidateProcessor;

public class HapiValidateProcessor implements IValidateProcessor {

    private final FhirContext fhirContext;
    private final FhirValidator fhirValidator;
    private final DaoRegistry daoRegistry;

    public HapiValidateProcessor(FhirContext fhirContext, DaoRegistry daoRegistry) {
        this.fhirContext = fhirContext;
        this.fhirValidator = fhirContext.newValidator();
        this.daoRegistry = daoRegistry;
        var npm = new NpmPackageValidationSupport(fhirContext);
        registerValidator(npm);
    }

    @Override
    public IBaseOperationOutcome validate(IBaseBundle bundle, String mode, String profile) {
        if (mode != null) {
            throw new NotImplementedOperationException("'mode' Parameter is not implemented yet");
        }
        if (profile != null) {
            throw new NotImplementedOperationException("'profile' Parameter is not implemented yet");
        }
        if (bundle == null) {
            throw new UnprocessableEntityException("A FHIR bundle must be provided for validation");
        }

        return fhirValidator.validateWithResult(bundle).toOperationOutcome();
    }

    private void registerValidator(NpmPackageValidationSupport npm) {
        fhirValidator.setValidateAgainstStandardSchema(false);
        fhirValidator.setValidateAgainstStandardSchematron(false);
        try {
            npm.loadPackageFromClasspath("test");
        } catch (IOException e) {
            throw new InternalErrorException("Could not load package");
        }

        try {
            npm.loadPackageFromClasspath("test2");
        } catch (IOException e) {
            throw new InternalErrorException("Could not load package 2");
        }

        var vsmIG = new NpmPackageValidationSupport(fhirContext);
        var chain = new ValidationSupportChain(
            npm,
            vsmIG,
            new DefaultProfileValidationSupport(fhirContext),
            new InMemoryTerminologyServerValidationSupport(fhirContext),
            new CommonCodeSystemsTerminologyService(fhirContext),
            new SnapshotGeneratingValidationSupport(fhirContext)
        );
        var instanceValidatorModule = new FhirInstanceValidator(chain);
        instanceValidatorModule.setValidatorResourceFetcher(new ValidatorResourceFetcher(fhirContext, chain, daoRegistry));
        fhirValidator.registerValidatorModule(instanceValidatorModule);
    }
}
