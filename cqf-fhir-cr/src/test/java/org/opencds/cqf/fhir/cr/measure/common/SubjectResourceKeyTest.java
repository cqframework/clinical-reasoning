package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SubjectResourceKey}.
 *
 * <p>These tests validate the behavior that prevents the stratifier deduplication bug
 * where primitive population values (like Date) were incorrectly deduplicated across subjects.
 */
class SubjectResourceKeyTest {

    @Nested
    class ResourceOnlyKeys {

        @Test
        void resourceOnly_createsKeyWithNullSubject() {
            SubjectResourceKey key = SubjectResourceKey.resourceOnly("Encounter/123");

            assertNull(key.subjectId());
            assertEquals("Encounter/123", key.resourceValue());
            assertFalse(key.hasSubject());
        }

        @Test
        void resourceOnly_keysWithSameResourceAreEqual() {
            SubjectResourceKey key1 = SubjectResourceKey.resourceOnly("Encounter/123");
            SubjectResourceKey key2 = SubjectResourceKey.resourceOnly("Encounter/123");

            assertEquals(key1, key2);
            assertEquals(key1.hashCode(), key2.hashCode());
        }

        @Test
        void resourceOnly_keysWithDifferentResourcesAreNotEqual() {
            SubjectResourceKey key1 = SubjectResourceKey.resourceOnly("Encounter/123");
            SubjectResourceKey key2 = SubjectResourceKey.resourceOnly("Encounter/456");

            assertNotEquals(key1, key2);
        }

        @Test
        void resourceOnly_deduplicatesInSet() {
            // For FHIR resources, same resource ID should deduplicate (globally unique)
            Set<SubjectResourceKey> set = new HashSet<>();
            set.add(SubjectResourceKey.resourceOnly("Encounter/123"));
            set.add(SubjectResourceKey.resourceOnly("Encounter/123"));
            set.add(SubjectResourceKey.resourceOnly("Encounter/456"));

            assertEquals(2, set.size(), "Same resource IDs should deduplicate");
        }

        @Test
        void resourceOnly_throwsOnNullResourceValue() {
            assertThrows(NullPointerException.class, () -> SubjectResourceKey.resourceOnly(null));
        }
    }

    @Nested
    class SubjectQualifiedKeys {

        @Test
        void of_createsKeyWithSubjectAndResource() {
            SubjectResourceKey key = SubjectResourceKey.of("Patient/A", "2024-01-01");

            assertEquals("Patient/A", key.subjectId());
            assertEquals("2024-01-01", key.resourceValue());
            assertTrue(key.hasSubject());
        }

        @Test
        void of_keysWithSameSubjectAndValueAreEqual() {
            SubjectResourceKey key1 = SubjectResourceKey.of("Patient/A", "2024-01-01");
            SubjectResourceKey key2 = SubjectResourceKey.of("Patient/A", "2024-01-01");

            assertEquals(key1, key2);
            assertEquals(key1.hashCode(), key2.hashCode());
        }

        @Test
        void of_keysWithDifferentSubjectsSameValueAreNotEqual() {
            // This is the critical behavior that fixes the deduplication bug
            SubjectResourceKey key1 = SubjectResourceKey.of("Patient/A", "2024-01-01");
            SubjectResourceKey key2 = SubjectResourceKey.of("Patient/B", "2024-01-01");

            assertNotEquals(key1, key2, "Same value for different subjects should NOT be equal");
        }

        @Test
        void of_keysWithSameSubjectDifferentValuesAreNotEqual() {
            SubjectResourceKey key1 = SubjectResourceKey.of("Patient/A", "2024-01-01");
            SubjectResourceKey key2 = SubjectResourceKey.of("Patient/A", "2024-01-02");

            assertNotEquals(key1, key2);
        }

        @Test
        void of_throwsOnNullSubjectId() {
            assertThrows(NullPointerException.class, () -> SubjectResourceKey.of(null, "2024-01-01"));
        }

        @Test
        void of_throwsOnNullResourceValue() {
            assertThrows(NullPointerException.class, () -> SubjectResourceKey.of("Patient/A", null));
        }
    }

    /**
     * Tests that validate the fix for the stratifier deduplication bug.
     *
     * <p>The bug occurred when multiple patients had the same primitive value (like a Date)
     * in their population. Using Set<String> with just the value would deduplicate,
     * losing count of items belonging to different patients.
     */
    @Nested
    class StratifierDeduplicationBugFix {

        @Test
        void primitiveValues_preservedAcrossMultipleSubjects() {
            // Scenario: 2 patients each have 5 dates, some dates are the same
            // Expected: All 10 entries should be preserved, not deduplicated to unique dates
            Set<SubjectResourceKey> set = new HashSet<>();

            // Patient A's dates
            set.add(SubjectResourceKey.of("Patient/A", "2024-01-01"));
            set.add(SubjectResourceKey.of("Patient/A", "2024-01-02"));
            set.add(SubjectResourceKey.of("Patient/A", "2024-01-03"));
            set.add(SubjectResourceKey.of("Patient/A", "2024-01-04"));
            set.add(SubjectResourceKey.of("Patient/A", "2024-01-05"));

            // Patient B's dates (same values as Patient A)
            set.add(SubjectResourceKey.of("Patient/B", "2024-01-01"));
            set.add(SubjectResourceKey.of("Patient/B", "2024-01-02"));
            set.add(SubjectResourceKey.of("Patient/B", "2024-01-03"));
            set.add(SubjectResourceKey.of("Patient/B", "2024-01-04"));
            set.add(SubjectResourceKey.of("Patient/B", "2024-01-05"));

            assertEquals(
                    10,
                    set.size(),
                    "All 10 dates should be preserved (5 per patient), not deduplicated to 5 unique dates");
        }

        @Test
        void fhirResources_correctlyDeduplicate() {
            // Scenario: FHIR resources have globally unique IDs, so deduplication is correct
            Set<SubjectResourceKey> set = new HashSet<>();

            // Same encounter referenced by different code paths should deduplicate
            set.add(SubjectResourceKey.resourceOnly("Encounter/123"));
            set.add(SubjectResourceKey.resourceOnly("Encounter/123"));
            set.add(SubjectResourceKey.resourceOnly("Encounter/456"));

            assertEquals(2, set.size(), "FHIR resources should deduplicate by ID");
        }

        @Test
        void intersectionWorksCorrectly_forPrimitiveTypes() {
            // Stratum keys (from stratifier evaluation)
            Set<SubjectResourceKey> stratumKeys = new HashSet<>();
            stratumKeys.add(SubjectResourceKey.of("Patient/A", "2024-01-01"));
            stratumKeys.add(SubjectResourceKey.of("Patient/A", "2024-01-02"));
            stratumKeys.add(SubjectResourceKey.of("Patient/B", "2024-01-01"));
            stratumKeys.add(SubjectResourceKey.of("Patient/B", "2024-01-02"));

            // Population keys (from population evaluation)
            Set<SubjectResourceKey> populationKeys = new HashSet<>();
            populationKeys.add(SubjectResourceKey.of("Patient/A", "2024-01-01"));
            populationKeys.add(SubjectResourceKey.of("Patient/A", "2024-01-02"));
            populationKeys.add(SubjectResourceKey.of("Patient/A", "2024-01-03")); // Not in stratum
            populationKeys.add(SubjectResourceKey.of("Patient/B", "2024-01-01"));
            // Patient/B 2024-01-02 NOT in population

            // Intersection
            long intersectionCount =
                    stratumKeys.stream().filter(populationKeys::contains).count();

            assertEquals(3, intersectionCount, "Intersection should find 3 matches: A/01-01, A/01-02, B/01-01");
        }
    }

    @Nested
    class FromRowKey {

        @Test
        void fromRowKey_withPrimitiveBasis_includesSubject() {
            StratifierRowKey rowKey = StratifierRowKey.withInput("Patient/123", "2024-01-01");

            SubjectResourceKey result = SubjectResourceKey.fromRowKey(rowKey, true);

            assertEquals("Patient/123", result.subjectId());
            assertEquals("2024-01-01", result.resourceValue());
            assertTrue(result.hasSubject());
        }

        @Test
        void fromRowKey_withResourceBasis_excludesSubject() {
            StratifierRowKey rowKey = StratifierRowKey.withInput("Patient/123", "Encounter/456");

            SubjectResourceKey result = SubjectResourceKey.fromRowKey(rowKey, false);

            assertNull(result.subjectId());
            assertEquals("Encounter/456", result.resourceValue());
            assertFalse(result.hasSubject());
        }

        @Test
        void fromRowKey_throwsWhenNoInputParam() {
            StratifierRowKey rowKey = StratifierRowKey.subjectOnly("Patient/123");

            assertThrows(IllegalArgumentException.class, () -> SubjectResourceKey.fromRowKey(rowKey, true));
        }

        @Test
        void fromRowKey_primitiveKeys_differentSubjectsSameValue_areNotEqual() {
            StratifierRowKey rowKeyA = StratifierRowKey.withInput("Patient/A", "2024-01-01");
            StratifierRowKey rowKeyB = StratifierRowKey.withInput("Patient/B", "2024-01-01");

            SubjectResourceKey keyA = SubjectResourceKey.fromRowKey(rowKeyA, true);
            SubjectResourceKey keyB = SubjectResourceKey.fromRowKey(rowKeyB, true);

            assertNotEquals(
                    keyA, keyB, "Keys from different subjects with same value should NOT be equal for primitive basis");
        }

        @Test
        void fromRowKey_resourceKeys_sameResource_areEqual() {
            StratifierRowKey rowKeyA = StratifierRowKey.withInput("Patient/A", "Encounter/123");
            StratifierRowKey rowKeyB = StratifierRowKey.withInput("Patient/B", "Encounter/123");

            SubjectResourceKey keyA = SubjectResourceKey.fromRowKey(rowKeyA, false);
            SubjectResourceKey keyB = SubjectResourceKey.fromRowKey(rowKeyB, false);

            assertEquals(keyA, keyB, "Keys with same resource ID should be equal for resource basis (subject ignored)");
        }
    }
}
