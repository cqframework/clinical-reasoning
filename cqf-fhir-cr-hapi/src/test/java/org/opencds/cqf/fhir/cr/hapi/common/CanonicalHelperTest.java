package org.opencds.cqf.fhir.cr.hapi.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.UrlType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CanonicalHelperTest {

    @Nested
    @DisplayName("Logic for determining the canonical string")
    class CanonicalStringLogicTests {

        @Test
        @DisplayName("Should use 'canonical' parameter when it is provided")
        void getCanonicalType_usesCanonicalWhenProvided() {
            // Arrange
            var canonical = "http://example.com/canonical/provided";
            var url = "http://example.com/url";
            var version = "1.0";

            // Act
            var result = CanonicalHelper.getCanonicalType(FhirVersionEnum.R4, canonical, url, version);

            // Assert
            assertNotNull(result);
            assertEquals(canonical, result.getValue());
        }

        @Test
        @DisplayName("Should combine 'url' and 'version' when 'canonical' is null")
        void getCanonicalType_usesUrlAndVersionWhenCanonicalIsNull() {
            // Arrange
            var url = "http://example.com/structuredefinition/patient";
            var version = "2.1.0";
            var expected = "http://example.com/structuredefinition/patient|2.1.0";

            // Act
            var result = CanonicalHelper.getCanonicalType(FhirVersionEnum.R4, null, url, version);

            // Assert
            assertNotNull(result);
            assertEquals(expected, result.getValue());
        }

        @Test
        @DisplayName("Should use only 'url' when 'canonical' and 'version' are null")
        void getCanonicalType_usesUrlOnlyWhenCanonicalAndVersionAreNull() {
            // Arrange
            var url = "http://example.com/structuredefinition/observation";

            // Act
            var result = CanonicalHelper.getCanonicalType(FhirVersionEnum.R4, null, url, null);

            // Assert
            assertNotNull(result);
            assertEquals(url, result.getValue());
        }

        @Test
        @DisplayName("Should return null if all inputs resolve to null")
        void getCanonicalType_returnsNullWhenAllInputsAreNull() {
            // Act
            var result = CanonicalHelper.getCanonicalType(FhirVersionEnum.R4, (String) null, null, null);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("Should accept Parameter components as arguments")
        void getCanonicalType_acceptsParameterComponents() {
            // Arrange
            var canonical = "http://example.com/canonical/provided";
            var url = "http://example.com/url";
            var version = "1.0";
            var canonicalParam =
                    new ParametersParameterComponent().setName("canonical").setValue(new CanonicalType(canonical));
            var urlParam = new ParametersParameterComponent().setName("url").setValue(new UrlType(url));
            var versionParam = new StringType(version);
            // Act
            var result = CanonicalHelper.getCanonicalType(FhirVersionEnum.R4, canonicalParam, urlParam, versionParam);
            // Assert
            assertNotNull(result);
            assertEquals(canonical, result.getValue());
        }
    }

    @Nested
    @DisplayName("FHIR version specific Primitive creation")
    class PrimitiveCreationLogicTests {

        @Test
        @DisplayName("Should return a DSTU3 StringType for DSTU3 version")
        void newCanonicalType_returnsDstu3StringType() {
            // Act
            var result = CanonicalHelper.getCanonicalType(FhirVersionEnum.DSTU3, "test", "test", null);

            // Assert
            assertNotNull(result);
            assertInstanceOf(StringType.class, result);
            assertEquals("test", result.getValue());
        }

        @Test
        @DisplayName("Should return an R4 CanonicalType for R4 version")
        void newCanonicalType_returnsR4CanonicalType() {
            // Act
            var result = CanonicalHelper.getCanonicalType(FhirVersionEnum.R4, "test", "test", null);

            // Assert
            assertNotNull(result);
            assertInstanceOf(CanonicalType.class, result);
            assertEquals("test", result.getValue());
        }

        @Test
        @DisplayName("Should return an R5 CanonicalType for R5 version")
        void newCanonicalType_returnsR5CanonicalType() {
            // Act
            var result = CanonicalHelper.getCanonicalType(FhirVersionEnum.R5, "test", "test", null);

            // Assert
            assertNotNull(result);
            assertInstanceOf(org.hl7.fhir.r5.model.CanonicalType.class, result);
            assertEquals("test", result.getValue());
        }

        @ParameterizedTest
        @EnumSource(
                value = FhirVersionEnum.class,
                names = {"DSTU2", "DSTU2_HL7ORG", "DSTU2_1", "R4B"})
        @DisplayName("Should return null for unsupported FHIR versions")
        void newCanonicalType_returnsNullForUnsupportedVersions(FhirVersionEnum version) {
            // Act
            var result = CanonicalHelper.getCanonicalType(version, "some-canonical", "some-url", "1.0");

            // Assert
            assertNull(result);
        }
    }
}
