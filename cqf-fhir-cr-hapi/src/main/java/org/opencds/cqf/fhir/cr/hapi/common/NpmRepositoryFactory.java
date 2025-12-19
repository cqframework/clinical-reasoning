package org.opencds.cqf.fhir.cr.hapi.common;

import org.opencds.cqf.fhir.utility.repository.INpmRepository;

public class NpmRepositoryFactory {

    private INpmRepository npmRepository;

    /**
     * Fetch the INpmRepository
     */
    public INpmRepository getNpmRepository() {
        return npmRepository;
    }

    /**
     * Register the INpmRepository
     */
    public void register(INpmRepository npmRepository) {
        this.npmRepository = npmRepository;
    }
}
