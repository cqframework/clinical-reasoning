package org.opencds.cqf.fhir.utility.npm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class NpmConfigDependencySubstitutorTest {

    @Test
    void present() {
        var npmPackageLoader = mock(NpmPackageLoader.class);

        var npmPackageLoaderFromSubstitutor =
                NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(Optional.of(npmPackageLoader));

        assertNotEquals(NpmPackageLoader.DEFAULT, npmPackageLoaderFromSubstitutor);
        assertEquals(npmPackageLoader, npmPackageLoaderFromSubstitutor);
    }

    @Test
    void empty() {
        var npmPackageLoaderFromSubstitutor =
                NpmConfigDependencySubstitutor.substituteNpmPackageLoaderIfEmpty(Optional.empty());

        assertEquals(NpmPackageLoader.DEFAULT, npmPackageLoaderFromSubstitutor);
    }
}
