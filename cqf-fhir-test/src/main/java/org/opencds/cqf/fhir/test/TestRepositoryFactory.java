package org.opencds.cqf.fhir.test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.IGFileStructureRepository;
import org.opencds.cqf.fhir.utility.repository.ResourceCategoryMode;
import org.opencds.cqf.fhir.utility.repository.ResourceFilenameMode;
import org.opencds.cqf.fhir.utility.repository.ResourceTypeMode;

public class TestRepositoryFactory {
    private TestRepositoryFactory() {
        // intentionally empty
    }

    public static Repository createRepository(FhirContext fhirContext, Class<?> clazz) {
        return createRepository(fhirContext, clazz, "");
    }

    public static Repository createRepository(FhirContext fhirContext, Class<?> clazz, String path) {
        return createRepository(fhirContext, clazz, path, ResourceTypeMode.FLAT);
    }

    public static Repository createRepository(
            FhirContext fhirContext, Class<?> clazz, String path, ResourceTypeMode layoutMode) {
        return new IGFileStructureRepository(
                fhirContext,
                clazz.getProtectionDomain().getCodeSource().getLocation().getPath() + path,
                layoutMode,
                ResourceCategoryMode.DIRECTORY_PER_CATEGORY,
                ResourceFilenameMode.ID_ONLY,
                EncodingEnum.JSON);
    }
}
