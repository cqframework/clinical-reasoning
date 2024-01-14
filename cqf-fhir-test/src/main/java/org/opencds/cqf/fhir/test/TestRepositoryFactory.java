package org.opencds.cqf.fhir.test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.IGFileStructureRepository;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;
import org.opencds.cqf.fhir.utility.repository.operations.IActivityDefinitionProcessorFactory;

public class TestRepositoryFactory {
    private TestRepositoryFactory() {
        // intentionally empty
    }

    public static Repository createRepository(FhirContext fhirContext, Class<?> clazz) {
        return createRepository(fhirContext, clazz, "");
    }

    public static Repository createRepository(FhirContext fhirContext, Class<?> clazz, String path) {
        return createRepository(fhirContext, clazz, path, IGLayoutMode.TYPE_PREFIX);
    }

    public static Repository createRepository(
            FhirContext fhirContext, Class<?> clazz, String path, IGLayoutMode layoutMode) {
        return createRepository(fhirContext, clazz, path, layoutMode, null);
    }

    public static Repository createRepository(
            FhirContext fhirContext,
            Class<?> clazz,
            String path,
            IGLayoutMode layoutMode,
            IActivityDefinitionProcessorFactory activityDefinitionProcessorFactory) {
        return new IGFileStructureRepository(
                fhirContext,
                clazz.getProtectionDomain().getCodeSource().getLocation().getPath() + path,
                layoutMode,
                EncodingEnum.JSON,
                activityDefinitionProcessorFactory);
    }
}
