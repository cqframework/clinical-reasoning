package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.validation.ValidatorResourceFetcher;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.validation.FhirValidator;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.opencds.cqf.fhir.cr.common.IValidateProcessor;
import org.opencds.cqf.fhir.utility.repository.NpmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HapiValidateProcessor implements IValidateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(HapiValidateProcessor.class);

    private final FhirContext fhirContext;
    private final FhirValidator fhirValidator;
    private final DaoRegistry daoRegistry;
    private final NpmRepository npmRepository;

    public HapiValidateProcessor(FhirContext fhirContext, DaoRegistry daoRegistry) {
        this.fhirContext = fhirContext;
        this.fhirValidator = fhirContext.newValidator();
        this.daoRegistry = daoRegistry;
        List<String[]> packs = new ArrayList<>();
        packs.add(new String[] {"hl7.fhir.us.ecr", "2.1.2"});
        this.npmRepository = new NpmRepository(fhirContext, packs);
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

        PrePopulatedValidationSupport prePopulatedValidationSupport = loadPrePopulatedValidationSupport();

        var vsmIG = new NpmPackageValidationSupport(fhirContext);
        var chain = new ValidationSupportChain(
                npm,
                vsmIG,
                new DefaultProfileValidationSupport(fhirContext),
                new InMemoryTerminologyServerValidationSupport(fhirContext),
                new CommonCodeSystemsTerminologyService(fhirContext),
                new SnapshotGeneratingValidationSupport(fhirContext),
                prePopulatedValidationSupport);
        var instanceValidatorModule = new FhirInstanceValidator(chain);
        instanceValidatorModule.setValidatorResourceFetcher(
                new ValidatorResourceFetcher(fhirContext, chain, daoRegistry));
        fhirValidator.registerValidatorModule(instanceValidatorModule);
    }

    private PrePopulatedValidationSupport loadPrePopulatedValidationSupport() {
        PrePopulatedValidationSupport prePopulatedValidationSupport = new PrePopulatedValidationSupport(fhirContext);
        try {
            for (var npmPackage : npmRepository.getLoadedPackages()) {
                var dir = new File(npmPackage.getPath() + "/package");
                for (var file : Objects.requireNonNull(dir.listFiles())) {
                    if (file.getName().endsWith(".json")
                            && !file.getName().equals("package.json")
                            && !file.getName().startsWith(".")) {
                        logger.info("Loading pre-populated validation support from {}", file.getAbsolutePath());
                        try (Reader reader = Files.newBufferedReader(file.toPath())) {
                            prePopulatedValidationSupport.addResource(
                                    fhirContext.newJsonParser().parseResource(reader));
                        }
                        /*                       var content = Files.readString(file.toPath());
                        IBaseResource resource = fhirContext.newJsonParser().parseResource(content);
                        prePopulatedValidationSupport.addResource(resource);*/
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load pre-populated validation support", e);
        }

        return prePopulatedValidationSupport;
    }
}
