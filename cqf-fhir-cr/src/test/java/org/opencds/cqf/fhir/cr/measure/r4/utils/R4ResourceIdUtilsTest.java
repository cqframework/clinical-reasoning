package org.opencds.cqf.fhir.cr.measure.r4.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class R4ResourceIdUtilsTest {

    @Test
    void stripPatientQualifier_withPatientPrefix_removesPrefix() {
        String result = R4ResourceIdUtils.stripPatientQualifier("Patient/123");
        assertThat(result, equalTo("123"));
    }

    @Test
    void stripPatientQualifier_withoutPatientPrefix_returnsUnchanged() {
        String result = R4ResourceIdUtils.stripPatientQualifier("123");
        assertThat(result, equalTo("123"));
    }

    @Test
    void stripPatientQualifier_withDifferentPrefix_returnsUnchanged() {
        String result = R4ResourceIdUtils.stripPatientQualifier("Practitioner/123");
        assertThat(result, equalTo("Practitioner/123"));
    }

    @Test
    void stripPatientQualifier_withEmptyString_returnsEmptyString() {
        String result = R4ResourceIdUtils.stripPatientQualifier("");
        assertThat(result, equalTo(""));
    }

    @Test
    void stripPatientQualifier_withMultiplePatientOccurrences_removesAllOccurrences() {
        String result = R4ResourceIdUtils.stripPatientQualifier("Patient/Patient/123");
        assertThat(result, equalTo("123"));
    }

    @ParameterizedTest
    @CsvSource({
        "Patient/123, Patient, 123",
        "Practitioner/456, Practitioner, 456",
        "Organization/789, Organization, 789",
        "Encounter/abc, Encounter, abc",
        "Observation/xyz-123, Observation, xyz-123"
    })
    void stripSpecificResourceQualifier_withMatchingPrefix_removesPrefix(
            String input, String resourceTypeName, String expected) {
        ResourceType resourceType = ResourceType.fromCode(resourceTypeName);
        String result = R4ResourceIdUtils.stripSpecificResourceQualifier(input, resourceType);
        assertThat(result, equalTo(expected));
    }

    @Test
    void stripSpecificResourceQualifier_withoutMatchingPrefix_returnsUnchanged() {
        String result = R4ResourceIdUtils.stripSpecificResourceQualifier("Patient/123", ResourceType.Practitioner);
        assertThat(result, equalTo("Patient/123"));
    }

    @Test
    void stripSpecificResourceQualifier_withNoPrefix_returnsUnchanged() {
        String result = R4ResourceIdUtils.stripSpecificResourceQualifier("123", ResourceType.Patient);
        assertThat(result, equalTo("123"));
    }

    @Test
    void stripSpecificResourceQualifier_withEmptyString_returnsEmptyString() {
        String result = R4ResourceIdUtils.stripSpecificResourceQualifier("", ResourceType.Patient);
        assertThat(result, equalTo(""));
    }

    @Test
    void stripSpecificResourceQualifier_withMultipleOccurrences_removesAllOccurrences() {
        String result = R4ResourceIdUtils.stripSpecificResourceQualifier("Patient/Patient/123", ResourceType.Patient);
        assertThat(result, equalTo("123"));
    }

    @Test
    void stripAnyResourceQualifier_withNull_returnsNull() {
        assertNull(R4ResourceIdUtils.stripAnyResourceQualifier(null));
    }

    @Test
    void stripAnyResourceQualifier_withPatientPrefix_removesPrefix() {
        String result = R4ResourceIdUtils.stripAnyResourceQualifier("Patient/123");
        assertThat(result, equalTo("123"));
    }

    @Test
    void stripAnyResourceQualifier_withPractitionerPrefix_removesPrefix() {
        String result = R4ResourceIdUtils.stripAnyResourceQualifier("Practitioner/456");
        assertThat(result, equalTo("456"));
    }

    @Test
    void stripAnyResourceQualifier_withNoSlash_returnsUnchanged() {
        String result = R4ResourceIdUtils.stripAnyResourceQualifier("123");
        assertThat(result, equalTo("123"));
    }

    @Test
    void stripAnyResourceQualifier_withEmptyString_returnsEmptyString() {
        String result = R4ResourceIdUtils.stripAnyResourceQualifier("");
        assertThat(result, equalTo(""));
    }

    @Test
    void stripAnyResourceQualifier_withMultipleSlashes_returnsFirstAfterSlash() {
        String result = R4ResourceIdUtils.stripAnyResourceQualifier("Patient/123/456");
        assertThat(result, equalTo("123"));
    }

    @Test
    void stripAnyResourceQualifier_withTrailingSlash_returnsResourceType() {
        String result = R4ResourceIdUtils.stripAnyResourceQualifier("Patient/");
        assertThat(result, equalTo("Patient"));
    }

    @Test
    void stripAnyResourceQualifier_withLeadingSlash_returnsAfterSlash() {
        String result = R4ResourceIdUtils.stripAnyResourceQualifier("/123");
        assertThat(result, equalTo("123"));
    }

    @ParameterizedTest
    @CsvSource({
        "Patient/123, 123",
        "Practitioner/abc-def, abc-def",
        "Organization/org1, org1",
        "123, 123",
        "'', ''",
        "Patient/123/456, 123"
    })
    void stripAnyResourceQualifier_variousInputs_returnsExpected(String input, String expected) {
        String result = R4ResourceIdUtils.stripAnyResourceQualifier(input);
        assertThat(result, equalTo(expected));
    }
}
