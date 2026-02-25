package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class VersionComparatorTest {

    private final VersionComparator comparator = new VersionComparator();

    @Test
    void testStrictSemVerValidation_validVersions() {
        assertTrue(comparator.isStrictSemVer("1.0.0"));
        assertTrue(comparator.isStrictSemVer("2.1.3-alpha"));
        assertTrue(comparator.isStrictSemVer("10.20.30-beta.1+build.123"));
        assertTrue(comparator.isStrictSemVer("0.1.0+exp.sha.5114f85"));
    }

    @Test
    void testStrictSemVerValidation_invalidVersions() {
        assertFalse(comparator.isStrictSemVer("1")); // incomplete
        assertFalse(comparator.isStrictSemVer("1.0")); // missing patch
        assertFalse(comparator.isStrictSemVer("01.0.0")); // leading zero
        assertFalse(comparator.isStrictSemVer("1.0.0-")); // dangling prerelease
        assertFalse(comparator.isStrictSemVer("1.0.0+")); // dangling build
        assertFalse(comparator.isStrictSemVer("a.b.c")); // non-numeric core
    }

    @Test
    void testSemVerComparison() {
        assertTrue(comparator.compare("1.0.0", "2.0.0") < 0);
        assertTrue(comparator.compare("2.1.0", "2.1.1") < 0);
        assertTrue(comparator.compare("1.0.0-alpha", "1.0.0") < 0);
        assertTrue(comparator.compare("1.0.0-alpha", "1.0.0-beta") < 0);
        assertTrue(comparator.compare("1.0.0-beta", "1.0.0-beta.2") < 0);
        assertTrue(comparator.compare("1.0.0-beta.2", "1.0.0-beta.11") < 0);
        assertTrue(comparator.compare("1.0.0-rc.1", "1.0.0") < 0);
    }

    @Test
    void testDateComparison() {
        assertTrue(comparator.isStrictSemVer("1.0.0")); // sanity check
        assertTrue(comparator.compare("20240101", "20230101") > 0);
        assertTrue(comparator.compareDatesDirect("2025-08-19", "2024-12-31") > 0);
        assertTrue(comparator.compareDatesDirect("202501", "202412") > 0);
    }

    @Test
    void testLexicographicComparison() {
        // Neither semver nor date, so falls back to lex ordering
        String v1 = "feature-abc";
        String v2 = "feature-def";
        assertTrue(comparator.compare(v1, v2) < 0);
    }

    @Test
    void testFindLatestVersionInList() {
        List<String> versions = Arrays.asList("1.0.0-alpha", "1.0.0-beta", "1.0.0", "2.0.0", "20240101");
        String latest = Collections.max(versions, comparator);
        assertEquals("20240101", latest, "Expected latest date to win over semver");
    }

    @Test
    void testNullVersionHandling() {
        // null compared to null should be equal
        assertEquals(0, comparator.compare(null, null));

        // null should be less than any non-null version
        assertTrue(comparator.compare(null, "1.0.0") < 0);
        assertTrue(comparator.compare(null, "20240101") < 0);
        assertTrue(comparator.compare(null, "feature-abc") < 0);

        // non-null should be greater than null
        assertTrue(comparator.compare("1.0.0", null) > 0);
        assertTrue(comparator.compare("20240101", null) > 0);
        assertTrue(comparator.compare("feature-abc", null) > 0);
    }
}
