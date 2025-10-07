package org.opencds.cqf.fhir.utility.npm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class BaseNpmResourceInfoForCqlTest {

    protected static final String DOT_TGZ = ".tgz";

    protected static final String SIMPLE_ALPHA_LOWER = "simplealpha";
    protected static final String SIMPLE_ALPHA_MIXED = "SimpleAlpha";
    protected static final String SIMPLE_BRAVO_LOWER = "simplebravo";
    protected static final String SIMPLE_BRAVO_MIXED = "SimpleBravo";
    protected static final String WITH_DERIVED_LIBRARY_LOWER = "withderivedlibrary";
    protected static final String WITH_DERIVED_LIBRARY_MIXED = "WithDerivedLibrary";
    protected static final String DERIVED_LIBRARY_ID = "DerivedLibrary";
    protected static final String DERIVED_LIBRARY = DERIVED_LIBRARY_ID;

    protected static final String DERIVED_LAYER_1_A = "DerivedLayer1a";
    protected static final String DERIVED_LAYER_1_B = "DerivedLayer1b";
    protected static final String DERIVED_LAYER_2_A = "DerivedLayer2a";
    protected static final String DERIVED_LAYER_2_B = "DerivedLayer2b";
    protected static final String CROSS_PACKAGE_SOURCE = "crosspackagesource";
    protected static final String CROSS_PACKAGE_SOURCE_ID = "CrossPackageSource";
    protected static final String CROSS_PACKAGE_TARGET = "crosspackagetarget";
    protected static final String CROSS_PACKAGE_TARGET_ID = "CrossPackageTarget";

    protected static final String NAMESPACE_PREFIX = "opencds.";

    protected static final String WITH_TWO_LAYERS_DERIVED_LIBRARIES = "withtwolayersderivedlibraries";
    protected static final String WITH_TWO_LAYERS_DERIVED_LIBRARIES_UPPER = "WithTwoLayersDerivedLibraries";

    protected static final String SIMPLE_ALPHA_NAMESPACE = NAMESPACE_PREFIX + SIMPLE_ALPHA_LOWER;
    protected static final String SIMPLE_BRAVO_NAMESPACE = NAMESPACE_PREFIX + SIMPLE_BRAVO_LOWER;
    protected static final String WITH_DERIVED_NAMESPACE = NAMESPACE_PREFIX + WITH_DERIVED_LIBRARY_LOWER;
    protected static final String WITH_TWO_LAYERS_NAMESPACE = NAMESPACE_PREFIX + WITH_TWO_LAYERS_DERIVED_LIBRARIES;
    protected static final String CROSS_PACKAGE_SOURCE_NAMESPACE = NAMESPACE_PREFIX + CROSS_PACKAGE_SOURCE;
    protected static final String CROSS_PACKAGE_TARGET_NAMESPACE = NAMESPACE_PREFIX + CROSS_PACKAGE_TARGET;


    protected static final String SIMPLE_ALPHA_TGZ = SIMPLE_ALPHA_LOWER + DOT_TGZ;
    protected static final String SIMPLE_BRAVO_TGZ = SIMPLE_BRAVO_LOWER + DOT_TGZ;
    protected static final Path WITH_DERIVED_LIBRARY_TGZ = Paths.get(WITH_DERIVED_LIBRARY_LOWER + DOT_TGZ);
    protected static final Path WITH_TWO_LAYERS_DERIVED_LIBRARIES_TGZ =
            Paths.get(WITH_TWO_LAYERS_DERIVED_LIBRARIES + DOT_TGZ);
    protected static final Path CROSS_PACKAGE_SOURCE_TGZ = Paths.get(CROSS_PACKAGE_SOURCE + DOT_TGZ);
    protected static final Path CROSS_PACKAGE_TARGET_TGZ = Paths.get(CROSS_PACKAGE_TARGET + DOT_TGZ);

    protected static final String SLASH_MEASURE_SLASH = "/Measure/";
    protected static final String SLASH_LIBRARY_SLASH = "/Library/";

    private static final String PIPE = "|";
    protected static final String VERSION_0_1 = "0.1";
    protected static final String VERSION_0_2 = "0.2";
    protected static final String VERSION_0_3 = "0.3";
    protected static final String VERSION_0_4 = "0.4";
    protected static final String VERSION_0_5 = "0.5";

    protected static final String SIMPLE_ALPHA_NAMESPACE_URL = "http://simplealpha.npm.opencds.org";
    protected static final String SIMPLE_BRAVO_NAMESPACE_URL = "http://simplebravo.npm.opencds.org";
    protected static final String WITH_DERIVED_URL = "http://withderivedlibrary.npm.opencds.org";
    protected static final String WITH_TWO_LAYERS_DERIVED_URL = "http://withtwolayersderivedlibraries.npm.opencds.org";
    protected static final String CROSS_PACKAGE_SOURCE_URL = "http://crosspackagesource.npm.opencds.org";
    protected static final String CROSS_PACKAGE_TARGET_URL = "http://crosspackagetarget.npm.opencds.org";

    protected static final String MEASURE_URL_ALPHA =
            SIMPLE_ALPHA_NAMESPACE_URL + SLASH_MEASURE_SLASH + SIMPLE_ALPHA_MIXED;
    protected static final String MEASURE_URL_BRAVO =
            SIMPLE_BRAVO_NAMESPACE_URL + SLASH_MEASURE_SLASH + SIMPLE_BRAVO_MIXED;
    protected static final String MEASURE_URL_WITH_DERIVED_LIBRARY =
            WITH_DERIVED_URL + SLASH_MEASURE_SLASH + WITH_DERIVED_LIBRARY_MIXED;
    protected static final String MEASURE_URL_WITH_DERIVED_LIBRARY_WITH_VERSION =
            MEASURE_URL_WITH_DERIVED_LIBRARY + PIPE + VERSION_0_4;
    protected static final String MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES =
            WITH_TWO_LAYERS_DERIVED_URL + SLASH_MEASURE_SLASH + WITH_TWO_LAYERS_DERIVED_LIBRARIES_UPPER;
    protected static final String MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION =
            MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES + PIPE + VERSION_0_5;

    protected static final String MEASURE_URL_CROSS_PACKAGE_SOURCE =
            CROSS_PACKAGE_SOURCE_URL + SLASH_MEASURE_SLASH + CROSS_PACKAGE_SOURCE_ID;

    protected static final String MEASURE_URL_CROSS_PACKAGE_TARGET =
        CROSS_PACKAGE_TARGET_URL + SLASH_MEASURE_SLASH + CROSS_PACKAGE_TARGET_ID;

    protected static final String LIBRARY_URL_ALPHA_NO_VERSION =
            SIMPLE_ALPHA_NAMESPACE_URL + SLASH_LIBRARY_SLASH + SIMPLE_ALPHA_MIXED;
    protected static final String LIBRARY_URL_ALPHA_WITH_VERSION = LIBRARY_URL_ALPHA_NO_VERSION + PIPE + VERSION_0_1;
    protected static final String LIBRARY_URL_BRAVO_NO_VERSION =
            SIMPLE_BRAVO_NAMESPACE_URL + SLASH_LIBRARY_SLASH + SIMPLE_BRAVO_MIXED;
    protected static final String LIBRARY_URL_BRAVO_WITH_VERSION = LIBRARY_URL_BRAVO_NO_VERSION + PIPE + VERSION_0_1;

    protected static final String LIBRARY_URL_WITH_DERIVED_LIBRARY_NO_VERSION =
            WITH_DERIVED_URL + SLASH_LIBRARY_SLASH + WITH_DERIVED_LIBRARY_MIXED;
    protected static final String LIBRARY_URL_WITH_DERIVED_LIBRARY_WITH_VERSION =
            WITH_DERIVED_URL + SLASH_LIBRARY_SLASH + WITH_DERIVED_LIBRARY_MIXED + PIPE + VERSION_0_4;
    protected static final String LIBRARY_URL_DERIVED_LIBRARY =
            WITH_DERIVED_URL + SLASH_LIBRARY_SLASH + DERIVED_LIBRARY;

    protected static final String LIBRARY_URL_CROSS_PACKAGE_SOURCE_NO_VERSION =
            CROSS_PACKAGE_SOURCE_URL + SLASH_LIBRARY_SLASH + CROSS_PACKAGE_SOURCE_ID;
    protected static final String LIBRARY_URL_CROSS_PACKAGE_SOURCE_WITH_VERSION =
            LIBRARY_URL_CROSS_PACKAGE_SOURCE_NO_VERSION + PIPE + VERSION_0_2;
    protected static final String LIBRARY_URL_CROSS_PACKAGE_TARGET_NO_VERSION =
            CROSS_PACKAGE_TARGET_URL + SLASH_LIBRARY_SLASH + CROSS_PACKAGE_TARGET_ID;
    protected static final String LIBRARY_URL_CROSS_PACKAGE_TARGET_WITH_VERSION =
        LIBRARY_URL_CROSS_PACKAGE_TARGET_NO_VERSION + PIPE + VERSION_0_3;

    protected static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_NO_VERSION =
            WITH_TWO_LAYERS_DERIVED_URL + SLASH_LIBRARY_SLASH + WITH_TWO_LAYERS_DERIVED_LIBRARIES_UPPER;
    protected static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION =
            LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_NO_VERSION + PIPE + VERSION_0_5;

    protected static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1A =
            WITH_TWO_LAYERS_DERIVED_URL + SLASH_LIBRARY_SLASH + DERIVED_LAYER_1_A;
    protected static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1B =
            WITH_TWO_LAYERS_DERIVED_URL + SLASH_LIBRARY_SLASH + DERIVED_LAYER_1_B;
    protected static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2A =
            WITH_TWO_LAYERS_DERIVED_URL + SLASH_LIBRARY_SLASH + DERIVED_LAYER_2_A;
    protected static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2B =
            WITH_TWO_LAYERS_DERIVED_URL + SLASH_LIBRARY_SLASH + DERIVED_LAYER_2_B;

    protected abstract FhirVersionEnum getExpectedFhirVersion();

    //    protected void derivedLibraryTwoLayers(
    //            String expectedCql,
    //            String expectedCqlDerived1a,
    //            String expectedCqlDerived1b,
    //            String expectedCqlDerived2a,
    //            String expectedCqlDerived2b) {
    //
    //        final R4NpmPackageLoaderInMemory loader = setup(WITH_TWO_LAYERS_DERIVED_LIBRARIES_TGZ);
    //
    //        var expectedNamespaceInfos = List.of(new NamespaceInfo(WITH_TWO_LAYERS_NAMESPACE,
    // WITH_DERIVED_TWO_LAYERS_URL));
    //
    //        final NpmResourceHolder resourceInfoNoVersion =
    //                loader.loadNpmResources(new CanonicalType(MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES));
    //        sanityCheckNpmResourceHolder(resourceInfoNoVersion);
    //        assertEquals(expectedNamespaceInfos, resourceInfoNoVersion.getNamespaceInfos());
    //        verifyMeasure(
    //                MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES,
    //                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION,
    //                resourceInfoNoVersion);
    //        verifyLibrary(
    //                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_NO_VERSION,
    //                expectedCql,
    //                resourceInfoNoVersion.getOptMainLibrary().orElse(null));
    //
    //        final NpmResourceHolder resourceInfoWithVersion =
    //                loader.loadNpmResources(new
    // CanonicalType(MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION));
    //        sanityCheckNpmResourceHolder(resourceInfoWithVersion);
    //        assertEquals(expectedNamespaceInfos, resourceInfoWithVersion.getNamespaceInfos());
    //        verifyMeasure(
    //                MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES,
    //                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION,
    //                resourceInfoWithVersion);
    //        verifyLibrary(
    //                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_NO_VERSION,
    //                expectedCql,
    //                resourceInfoWithVersion.getOptMainLibrary().orElse(null));
    //
    //        final ILibraryAdapter derivedLibrary1a = resourceInfoWithVersion
    //                .findMatchingLibrary(new VersionedIdentifier()
    //                        .withId(DERIVED_LAYER_1_A)
    //                        .withVersion(VERSION_0_5)
    //                        .withSystem(WITH_DERIVED_TWO_LAYERS_URL))
    //                .orElse(null);
    //
    //        verifyLibrary(LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1A, expectedCqlDerived1a, derivedLibrary1a);
    //
    //        final ILibraryAdapter derivedLibrary1b = resourceInfoWithVersion
    //                .findMatchingLibrary(
    //                        new VersionedIdentifier().withId(DERIVED_LAYER_1_B).withVersion(VERSION_0_5))
    //                .orElse(null);
    //
    //        verifyLibrary(LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1B, expectedCqlDerived1b, derivedLibrary1b);
    //
    //        final ILibraryAdapter derivedLibrary2a = resourceInfoWithVersion
    //                .findMatchingLibrary(
    //                        new VersionedIdentifier().withId(DERIVED_LAYER_2_A).withVersion(VERSION_0_5))
    //                .orElse(null);
    //
    //        verifyLibrary(LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2A, expectedCqlDerived2a, derivedLibrary2a);
    //
    //        final ILibraryAdapter derivedLibrary2b = resourceInfoWithVersion
    //                .findMatchingLibrary(
    //                        new VersionedIdentifier().withId(DERIVED_LAYER_2_B).withVersion(VERSION_0_5))
    //                .orElse(null);
    //
    //        verifyLibrary(LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2B, expectedCqlDerived2b, derivedLibrary2b);
    //    }

    //    protected void crossPackage(String expectedCqlSource, String expectedCqlTarget) {
    //
    //        final R4NpmPackageLoaderInMemory loader = setup(CROSS_PACKAGE_SOURCE_TGZ, CROSS_PACKAGE_TARGET_TGZ);
    //
    //        final NpmResourceHolder resourceInfoWithNoVersion =
    //                loader.loadNpmResources(new CanonicalType(MEASURE_URL_CROSS_PACKAGE_SOURCE));
    //        sanityCheckNpmResourceHolder(resourceInfoWithNoVersion);
    //        verifyMeasure(
    //                MEASURE_URL_CROSS_PACKAGE_SOURCE,
    //                LIBRARY_URL_CROSS_PACKAGE_SOURCE_WITH_VERSION,
    //                resourceInfoWithNoVersion);
    //        final NpmResourceHolder resourceInfoWithVersion =
    //                loader.loadNpmResources(new CanonicalType(MEASURE_URL_CROSS_PACKAGE_SOURCE_WITH_VERSION));
    //        sanityCheckNpmResourceHolder(resourceInfoWithVersion);
    //        verifyMeasure(
    //                MEASURE_URL_CROSS_PACKAGE_SOURCE,
    //                LIBRARY_URL_CROSS_PACKAGE_SOURCE_WITH_VERSION,
    //                resourceInfoWithVersion);
    //
    //        verifyLibrary(
    //                LIBRARY_URL_CROSS_PACKAGE_SOURCE,
    //                expectedCqlSource,
    //                resourceInfoWithVersion.getOptMainLibrary().orElse(null));
    //
    //        final Optional<ILibraryAdapter> matchingLibraryWithSourceResult =
    //                resourceInfoWithVersion.findMatchingLibrary(new
    // VersionedIdentifier().withId(CROSS_PACKAGE_TARGET_ID));
    //
    //        // We expect NOT to find the target Library here since it's not in the source package at all
    //        assertTrue(matchingLibraryWithSourceResult.isEmpty());
    //
    //        // On the other hand, we can load the Library directly from the loader by URL:
    //        final Optional<ILibraryAdapter> optLibraryTarget =
    // loader.loadLibraryByUrl(LIBRARY_URL_CROSS_PACKAGE_TARGET);
    //
    //        assertTrue(optLibraryTarget.isPresent());
    //        verifyLibrary(LIBRARY_URL_CROSS_PACKAGE_TARGET, expectedCqlTarget, optLibraryTarget.get());
    //    }

}
