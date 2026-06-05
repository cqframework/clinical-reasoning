package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.validation.ValidatorResourceFetcher;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.jspecify.annotations.NonNull;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.utility.repository.NpmRepository;

public class FhirValidatorRegistry {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FhirValidatorRegistry.class);
    private final DaoRegistry daoRegistry;
    private final ConcurrentHashMap<FhirVersionEnum, FhirValidator> validators = new ConcurrentHashMap<>();
    private final List<String[]> packagesToLoad = new ArrayList<>();

    public FhirValidatorRegistry(DaoRegistry daoRegistry, CrSettings crSettings) {
        this.daoRegistry = daoRegistry;
        packagesToLoad.addAll(crSettings.getValidatorPackages());
    }

    public FhirValidator getValidator(FhirVersionEnum version) {
        return validators.computeIfAbsent(version, this::buildValidatorForVersion);
    }

    private FhirValidator buildValidatorForVersion(FhirVersionEnum version) {
        FhirContext ctx = FhirContext.forVersion(version);
        NpmRepository npmRepository = new NpmRepository(ctx, packagesToLoad);
        var fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchema(false);
        fhirValidator.setValidateAgainstStandardSchematron(false);

        PrePopulatedValidationSupport prePopulatedValidationSupport = loadPrePopulatedValidationSupport(npmRepository);

        var instanceValidatorModule = getFhirInstanceValidator(ctx, prePopulatedValidationSupport);
        fhirValidator.registerValidatorModule(instanceValidatorModule);
        return fhirValidator;
    }

    private @NonNull FhirInstanceValidator getFhirInstanceValidator(
            FhirContext ctx, PrePopulatedValidationSupport prePopulatedValidationSupport) {
        var chain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx),
                new CommonCodeSystemsTerminologyService(ctx),
                new SnapshotGeneratingValidationSupport(ctx),
                prePopulatedValidationSupport);
        var instanceValidatorModule = new FhirInstanceValidator(chain);
        instanceValidatorModule.setValidatorResourceFetcher(new ValidatorResourceFetcher(ctx, chain, daoRegistry));
        return instanceValidatorModule;
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
