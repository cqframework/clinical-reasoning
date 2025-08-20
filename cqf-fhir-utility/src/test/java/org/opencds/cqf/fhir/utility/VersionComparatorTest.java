package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class VersionComparatorTest {

    private final VersionComparator comparator = new VersionComparator();

    @Test
    void testSemVerComparison() {
        assertTrue(comparator.compare("1.0.0", "1.0.1") < 0);
        assertTrue(comparator.compare("2.0.0", "1.9.9") > 0);
        assertEquals(0, comparator.compare("1.0.0", "1.0.0"));
    }

    @Test
    void testPreReleaseSemVerOrdering() {
        assertTrue(comparator.compare("1.0.0-alpha", "1.0.0-beta") < 0);
        assertTrue(comparator.compare("1.0.0-beta.2", "1.0.0-beta.11") < 0);
        assertTrue(comparator.compare("1.0.0-rc.1", "1.0.0") < 0);
        assertTrue(comparator.compare("1.0.0", "1.0.0-alpha") > 0);
    }

    @Test
    void testDateVersionComparison() {
        assertTrue(comparator.compare("2023-01-01", "2024-01-01") < 0);
        assertTrue(comparator.compare("20240101", "20230101") > 0);
        assertEquals(0, comparator.compare("2023-01-01", "2023-01-01"));
    }

    @Test
    void testLexicographicFallback() {
        assertTrue(comparator.compare("foo", "bar") > 0);
        assertTrue(comparator.compare("apple", "banana") < 0);
        assertEquals(0, comparator.compare("xyz", "xyz"));
    }

    @Test
    void testInconsistentFormatFallbackToLex() {
        // SemVer vs date
        assertTrue(comparator.compare("1.0.0", "20240101") < 0);
        assertTrue(comparator.compare("20240101", "1.0.0") > 0);
    }

    @Test
    void testSortingWithMixedVersions() {
        List<String> versions =
                Arrays.asList("1.0.0-alpha", "1.0.0", "1.0.0-rc.1", "2.0.0", "20230101", "20240101", "foo", "bar");

        versions.sort(comparator);
        assertEquals("1.0.0-alpha", versions.get(0));
        assertEquals("foo", versions.get(versions.size() - 1));
    }
}
