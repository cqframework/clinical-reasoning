package org.opencds.cqf.fhir.cql.npm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hl7.elm.r1.VersionedIdentifier;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoaderInMemory;

class NpmLibraryProviderTest {

    private static final String DOT_TGZ = ".tgz";

    private static final String CROSS_PACKAGE_SOURCE = "crosspackagesource";
    private static final String CROSS_PACKAGE_SOURCE_ID = "CrossPackageSource";
    private static final String CROSS_PACKAGE_TARGET = "crosspackagetarget";
    private static final String CROSS_PACKAGE_TARGET_ID = "CrossPackageTarget";

    private static final String CROSS_PACKAGE_SOURCE_URL = "http://crosspackagesource.npm.opencds.org";
    private static final String CROSS_PACKAGE_TARGET_URL = "http://crosspackagetarget.npm.opencds.org";

    private static final Path CROSS_PACKAGE_SOURCE_TGZ = Paths.get(CROSS_PACKAGE_SOURCE + DOT_TGZ);
    private static final Path CROSS_PACKAGE_TARGET_TGZ = Paths.get(CROSS_PACKAGE_TARGET + DOT_TGZ);

    private static final String EXPECTED_CQL_CROSS_SOURCE =
            """
            library opencds.crosspackagesource.CrossPackageSource version '0.2'

            using FHIR version '4.0.1'

            include FHIRHelpers version '4.0.1' called FHIRHelpers
            include opencds.crosspackagetarget.CrossPackageTarget version '0.3' called CrossPackageTarget

            parameter "Measurement Period" Interval<DateTime>
                default Interval[@2020-01-01T00:00:00.0-06:00, @2021-01-01T00:00:00.0-06:00)

            context Patient

            define "Initial Population":
                exists (CrossPackageTarget."Encounter Finished")
            """;

    private static final String EXPECTED_CQL_CROSS_TARGET =
            """
            library opencds.crosspackagetarget.CrossPackageTarget version '0.3'

            using FHIR version '4.0.1'

            include FHIRHelpers version '4.0.1' called FHIRHelpers

            context Patient

            define "Encounter Finished":
              [Encounter] E
                where E.status = 'finished'
            """;

    @Test
    void crossPackageLoadLibrary() throws IOException {

        var loader = setup(CROSS_PACKAGE_SOURCE_TGZ, CROSS_PACKAGE_TARGET_TGZ);

        var libraryProvider = new NpmLibraryProvider(loader);

        var versionedIdentifierSource =
                new VersionedIdentifier().withSystem(CROSS_PACKAGE_SOURCE_URL).withId(CROSS_PACKAGE_SOURCE_ID);

        var librarySourceInputStream = libraryProvider.getLibrarySource(versionedIdentifierSource);
        assertNotNull(librarySourceInputStream);

        var actualSourceCql = getStringFromInputStream(librarySourceInputStream);

        assertEquals(EXPECTED_CQL_CROSS_SOURCE, actualSourceCql);

        var versionedIdentifierTarget =
                new VersionedIdentifier().withSystem(CROSS_PACKAGE_TARGET_URL).withId(CROSS_PACKAGE_TARGET_ID);

        var libraryTargetInputStream = libraryProvider.getLibrarySource(versionedIdentifierTarget);

        assertNotNull(libraryTargetInputStream);

        var actualTargetCql = getStringFromInputStream(libraryTargetInputStream);

        assertEquals(EXPECTED_CQL_CROSS_TARGET, actualTargetCql);
    }

    @Nonnull
    private NpmPackageLoaderInMemory setup(Path... tgzPaths) {
        return NpmPackageLoaderInMemory.fromNpmPackageClasspath(getClass(), tgzPaths);
    }

    @Nonnull
    private String getStringFromInputStream(InputStream librarySourceInputStream) throws IOException {
        return new String(librarySourceInputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
