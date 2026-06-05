package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.CrSettings;

class FhirValidatorRegistryTest {

    @Test
    void test_get_r4_validator() {
        DaoRegistry daoRegistry = new DaoRegistry();
        CrSettings crSettings = CrSettings.getDefault();
        FhirValidatorRegistry fhirValidatorRegistry = new FhirValidatorRegistry(daoRegistry, crSettings);
        var validator = fhirValidatorRegistry.getValidator(FhirVersionEnum.R4);
        Assertions.assertNotNull(validator);
    }

    @Test
    void test_get_r5_validator() {
        DaoRegistry daoRegistry = new DaoRegistry();
        CrSettings crSettings = CrSettings.getDefault();
        FhirValidatorRegistry fhirValidatorRegistry = new FhirValidatorRegistry(daoRegistry, crSettings);
        var validator = fhirValidatorRegistry.getValidator(FhirVersionEnum.R5);
        Assertions.assertNotNull(validator);
    }

    @Test
    void test_get_dstu3_validator() {
        DaoRegistry daoRegistry = new DaoRegistry();
        CrSettings crSettings = CrSettings.getDefault();
        FhirValidatorRegistry fhirValidatorRegistry = new FhirValidatorRegistry(daoRegistry, crSettings);
        var validator = fhirValidatorRegistry.getValidator(FhirVersionEnum.DSTU3);
        Assertions.assertNotNull(validator);
    }
}
