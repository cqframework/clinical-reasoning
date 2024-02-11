package org.opencds.cqf.fhir.test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.IGFileStructureRepository;
import org.opencds.cqf.fhir.utility.repository.RepositoryConfig;
import org.opencds.cqf.fhir.utility.repository.operations.IRepositoryOperationProvider;

public class TestRepositoryFactory {
    private TestRepositoryFactory() {
        // intentionally empty
    }

    public static Repository createRepository(FhirContext fhirContext, Class<?> clazz) {
        return createRepository(fhirContext, clazz, "");
    }

    public static Repository createRepository(FhirContext fhirContext, Class<?> clazz, String path) {
        return createRepository(fhirContext, clazz, path, RepositoryConfig.WITH_CATEGORY_AND_TYPE_DIRECTORIES);
    }

    public static Repository createRepository(
            FhirContext fhirContext, Class<?> clazz, String path, RepositoryConfig config) {
        return createRepository(fhirContext, clazz, path, config, null);
    }

    public static Repository createRepository(
            FhirContext fhirContext,
            Class<?> clazz,
            String path,
            RepositoryConfig config,
            IRepositoryOperationProvider operationProvider) {
        return new IGFileStructureRepository(
                fhirContext,
                clazz.getProtectionDomain().getCodeSource().getLocation().getPath() + path,
                config,
                EncodingEnum.JSON,
                operationProvider);
    }
}
