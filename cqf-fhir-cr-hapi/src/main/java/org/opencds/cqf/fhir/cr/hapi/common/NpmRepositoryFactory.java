package org.opencds.cqf.fhir.cr.hapi.common;

import org.opencds.cqf.fhir.utility.repository.INpmRepository;

public class NpmRepositoryFactory {

    private INpmRepository npmRepository;

    public INpmRepository getNpmRepository() {
        return npmRepository;
    }

    public void register(INpmRepository npmRepository) {
        this.npmRepository = npmRepository;
    }
}
