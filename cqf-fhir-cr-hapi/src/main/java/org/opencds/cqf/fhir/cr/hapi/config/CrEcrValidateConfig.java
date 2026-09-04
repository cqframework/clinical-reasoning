package org.opencds.cqf.fhir.cr.hapi.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.validation.ValidatorResourceFetcher;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import java.io.IOException;
import java.io.InputStream;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.utility.repository.NpmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configures a {@link FhirValidator} to be used by HAPI FHIR's built-in {@code $validate} operation.
 *
 * <p>This configuration is only activated when {@code cr.ecr.validate.enabled=true} is set (or the
 * equivalent {@code CR_ECR_VALIDATE_ENABLED=true} environment variable). When disabled, the
 * default HAPI FHIR {@code FhirValidator} is used.
 */
@Configuration
@Conditional(CrEcrValidateCondition.class)
public class CrEcrValidateConfig {

    private static final Logger logger = LoggerFactory.getLogger(CrEcrValidateConfig.class);

    /**
     * Builds a {@link FhirInstanceValidator} using a {@link ValidationSupportChain} which contains
     * a {@link PrePopulatedValidationSupport} that is populated with the contents of specified
     * Npm Packages.
     *
     * <p>Marked {@link Primary} so it takes precedence when multiple {@link FhirInstanceValidator} beans
     * are present in the application context.
     */
    @Bean
    @Primary
    public FhirInstanceValidator crEcrFhirInstanceValidator(
            FhirContext fhirContext, DaoRegistry daoRegistry, CrSettings crSettings) {
        NpmRepository npmRepository = new NpmRepository(fhirContext, crSettings.getValidatorPackages());
        PrePopulatedValidationSupport prePopulatedValidationSupport = loadPrePopulatedValidationSupport(npmRepository);
        var supportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(fhirContext),
                new CommonCodeSystemsTerminologyService(fhirContext),
                new InMemoryTerminologyServerValidationSupport(fhirContext),
                new SnapshotGeneratingValidationSupport(fhirContext),
                prePopulatedValidationSupport);
        var instanceValidator = new FhirInstanceValidator(supportChain);
        instanceValidator.setValidatorResourceFetcher(
                new ValidatorResourceFetcher(fhirContext, supportChain, daoRegistry));
        return instanceValidator;
    }

    /**
     * This {@link FhirValidator} instance loads the above {@link FhirInstanceValidator} module.
     * This instance which will conditionally be used by HAPI FHIR's built-in {@code $validate} operation.
     *
     * <p>Marked {@link Primary} so it takes precedence when multiple {@link FhirValidator} beans
     * are present in the application context.
     */
    @Bean
    @Primary
    public FhirValidator crEcrFhirValidator(FhirContext fhirContext, FhirInstanceValidator crEcrFhirInstanceValidator) {
        var validator = fhirContext.newValidator().registerValidatorModule(crEcrFhirInstanceValidator);
        validator.setValidateAgainstStandardSchema(false);
        validator.setValidateAgainstStandardSchematron(false);
        return validator;
    }

    private PrePopulatedValidationSupport loadPrePopulatedValidationSupport(NpmRepository npmRepository) {
        PrePopulatedValidationSupport prePopulatedValidationSupport =
                new PrePopulatedValidationSupport(npmRepository.fhirContext());
        var parser = npmRepository.fhirContext().newJsonParser();
        for (var npmPackage : npmRepository.getLoadedPackages()) {
            try {
                var files = npmPackage.listResources("StructureDefinition", "ValueSet", "CodeSystem");
                for (var filename : files) {
                    logger.info("Loading pre-populated validation support from {}", filename);
                    addResource(npmPackage, filename, parser, prePopulatedValidationSupport);
                }

            } catch (IOException e) {
                logger.error("Error listing Resources from package {}", npmPackage.id(), e);
            }
        }
        return prePopulatedValidationSupport;
    }

    private void addResource(
            NpmPackage npmPackage,
            String filename,
            IParser parser,
            PrePopulatedValidationSupport prePopulatedValidationSupport) {
        try (InputStream is = npmPackage.load("package", filename)) {
            var resource = parser.parseResource(is);
            prePopulatedValidationSupport.addResource(resource);
        } catch (Exception e) {
            logger.error("Error loading Resource from package {}: {}", npmPackage.id(), filename, e);
        }
    }
}
