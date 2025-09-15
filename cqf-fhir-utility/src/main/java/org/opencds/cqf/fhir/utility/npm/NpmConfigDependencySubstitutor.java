package org.opencds.cqf.fhir.utility.npm;

import java.util.Optional;

/**
 * This class is meant to be used from Spring configuration classes, in the case of any missing
 * NpmPackageLoader bean definitions, which Spring will inject as empty Optionals.
 * <p/>
 * Helps implement a migration from the old world of FHIR/Repository based resources for Libraries,
 * Measures and eventually other clinical intelligence resources (such as PlanDefinitions or
 * ValueSets), and the new world where they're derived from NPM packages.
 * If Spring config is missing an instance of {@link NpmPackageLoader}, then * return the default
 * instance.
 */
public class NpmConfigDependencySubstitutor {

    private NpmConfigDependencySubstitutor() {
        // static utility class
    }

    public static NpmPackageLoader substituteNpmPackageLoaderIfEmpty(Optional<NpmPackageLoader> optNpmPackageLoader) {
        return NpmPackageLoader.getDefaultIfEmpty(optNpmPackageLoader.orElse(null));
    }
}
