package org.opencds.cqf.fhir.utility.npm;

import java.util.Optional;

/**
 * Help implement a migration from the old world of FHIR/Repository based resources for Libraries,
 * Measures and other clinical intelligence resources, and the new world where they're derived
 * from NPM packages.  If Spring config is missing an instance of {@link NpmPackageLoader}, then
 * return the default instance.
 */
public class NpmConfigDependencySubstitutor {

    // LUKETODO: reuse this everywhere
    // LUKETODO: javadoc
    public static NpmPackageLoader substituteNpmPackageLoaderIfEmpty(Optional<NpmPackageLoader> optNpmPackageLoader) {
        return NpmPackageLoader.getDefaultIfEmpty(optNpmPackageLoader.orElse(null));
    }
}
