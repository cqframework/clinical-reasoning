package org.opencds.cqf.fhir.cr.hapi.config.test;

import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;

public class TestCrStorageSettingsConfigurer {

    private final JpaStorageSettings storageSettings;

    public TestCrStorageSettingsConfigurer(JpaStorageSettings storageSettings) {
        this.storageSettings = storageSettings;
    }

    public void setUpConfiguration() {
        storageSettings.setAllowExternalReferences(true);
        storageSettings.setEnforceReferentialIntegrityOnWrite(false);
        storageSettings.setEnforceReferenceTargetTypes(false);
        storageSettings.setResourceClientIdStrategy(JpaStorageSettings.ClientIdStrategyEnum.ANY);
    }
}
