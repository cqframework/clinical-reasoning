package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.runtime.Code;

class StratumValueWrapperTest {

    // ==================== R4 Tests ====================

    @Nested
    class R4 {

        @Test
        void codingGetKey() {
            var coding = new org.hl7.fhir.r4.model.Coding()
                    .setSystem("http://example.com")
                    .setCode("ABC")
                    .setDisplay("Alpha");
            var wrapper = new StratumValueWrapper(coding);
            assertEquals("coding-ABC", wrapper.getKey());
        }

        @Test
        void codingGetDescription_withDisplay() {
            var coding = new org.hl7.fhir.r4.model.Coding().setCode("ABC").setDisplay("Alpha");
            var wrapper = new StratumValueWrapper(coding);
            assertEquals("Alpha", wrapper.getDescription());
        }

        @Test
        void codingGetDescription_withoutDisplay() {
            var coding = new org.hl7.fhir.r4.model.Coding().setCode("ABC");
            var wrapper = new StratumValueWrapper(coding);
            assertEquals("ABC", wrapper.getDescription());
        }

        @Test
        void codingGetValueAsString() {
            var coding = new org.hl7.fhir.r4.model.Coding().setCode("ABC").setDisplay("Alpha");
            var wrapper = new StratumValueWrapper(coding);
            assertEquals("ABC", wrapper.getValueAsString());
        }

        @Test
        void codeableConceptGetKey() {
            var cc = new org.hl7.fhir.r4.model.CodeableConcept()
                    .addCoding(new org.hl7.fhir.r4.model.Coding().setCode("XYZ"));
            var wrapper = new StratumValueWrapper(cc);
            assertEquals("codeable-concept-XYZ", wrapper.getKey());
        }

        @Test
        void codeableConceptGetDescription_withDisplay() {
            var cc = new org.hl7.fhir.r4.model.CodeableConcept()
                    .addCoding(new org.hl7.fhir.r4.model.Coding().setCode("XYZ").setDisplay("X-ray"));
            var wrapper = new StratumValueWrapper(cc);
            assertEquals("X-ray", wrapper.getDescription());
        }

        @Test
        void codeableConceptGetDescription_withoutDisplay() {
            var cc = new org.hl7.fhir.r4.model.CodeableConcept()
                    .addCoding(new org.hl7.fhir.r4.model.Coding().setCode("XYZ"));
            var wrapper = new StratumValueWrapper(cc);
            assertEquals("XYZ", wrapper.getDescription());
        }

        @Test
        void codeableConceptGetValueAsString() {
            var cc = new org.hl7.fhir.r4.model.CodeableConcept()
                    .addCoding(new org.hl7.fhir.r4.model.Coding().setCode("XYZ").setDisplay("X-ray"));
            var wrapper = new StratumValueWrapper(cc);
            assertEquals("XYZ", wrapper.getValueAsString());
        }

        @Test
        void identifierGetKey() {
            var id = new org.hl7.fhir.r4.model.Identifier().setValue("ID-123");
            var wrapper = new StratumValueWrapper(id);
            assertEquals("ID-123", wrapper.getKey());
        }

        @Test
        void identifierGetDescription() {
            var id = new org.hl7.fhir.r4.model.Identifier().setValue("ID-123");
            var wrapper = new StratumValueWrapper(id);
            assertEquals("ID-123", wrapper.getDescription());
        }

        @Test
        void identifierGetValueAsString() {
            var id = new org.hl7.fhir.r4.model.Identifier().setValue("ID-123");
            var wrapper = new StratumValueWrapper(id);
            assertEquals("ID-123", wrapper.getValueAsString());
        }

        @Test
        void resourceGetKey() {
            var patient = new org.hl7.fhir.r4.model.Patient();
            patient.setId("Patient/123");
            var wrapper = new StratumValueWrapper(patient);
            assertEquals("Patient/123", wrapper.getKey());
        }

        @Test
        void resourceGetDescription() {
            var patient = new org.hl7.fhir.r4.model.Patient();
            patient.setId("Patient/456");
            var wrapper = new StratumValueWrapper(patient);
            assertEquals("Patient/456", wrapper.getDescription());
        }

        @Test
        void resourceGetValueAsString() {
            var patient = new org.hl7.fhir.r4.model.Patient();
            patient.setId("Patient/789");
            var wrapper = new StratumValueWrapper(patient);
            assertEquals("Patient/789", wrapper.getValueAsString());
        }

        @Test
        void codingWithNullCodeProducesKey() {
            var coding = new org.hl7.fhir.r4.model.Coding();
            var wrapper = new StratumValueWrapper(coding);
            assertEquals("coding-null", wrapper.getKey());
        }

        @Test
        void getValueClassForCoding() {
            var wrapper = new StratumValueWrapper(new org.hl7.fhir.r4.model.Coding().setCode("ABC"));
            assertEquals(org.hl7.fhir.r4.model.Coding.class, wrapper.getValueClass());
        }

        @Test
        void getValueReturnsSameObject() {
            var coding = new org.hl7.fhir.r4.model.Coding().setCode("ABC");
            var wrapper = new StratumValueWrapper(coding);
            assertEquals(coding, wrapper.getValue());
        }

        @Test
        void iterableGetValueAsString() {
            var list = List.of(
                    new org.hl7.fhir.r4.model.Coding().setCode("A"), new org.hl7.fhir.r4.model.Coding().setCode("B"));
            var wrapper = new StratumValueWrapper(list);
            assertEquals("A,B", wrapper.getValueAsString());
        }

        @Test
        void iterableWithPrimitiveTypes() {
            var list = new ArrayList<>();
            list.add(new org.hl7.fhir.r4.model.StringType("hello"));
            list.add(new org.hl7.fhir.r4.model.IntegerType(42));
            var wrapper = new StratumValueWrapper(list);
            assertEquals("hello,42", wrapper.getValueAsString());
        }

        @Test
        void stringTypeGetKey() {
            var wrapper = new StratumValueWrapper(new org.hl7.fhir.r4.model.StringType("hello"));
            assertEquals("primitive-hello", wrapper.getKey());
        }

        @Test
        void integerTypeGetKey() {
            var wrapper = new StratumValueWrapper(new org.hl7.fhir.r4.model.IntegerType(42));
            assertEquals("primitive-42", wrapper.getKey());
        }

        @Test
        void booleanTypeGetKey() {
            var wrapper = new StratumValueWrapper(new org.hl7.fhir.r4.model.BooleanType(true));
            assertEquals("primitive-true", wrapper.getKey());
        }

        @Test
        void primitiveGetDescription() {
            var wrapper = new StratumValueWrapper(new org.hl7.fhir.r4.model.StringType("hello"));
            assertEquals("hello", wrapper.getDescription());
        }

        @Test
        void primitiveGetValueAsString() {
            var wrapper = new StratumValueWrapper(new org.hl7.fhir.r4.model.IntegerType(42));
            assertEquals("42", wrapper.getValueAsString());
        }

        @Test
        void equalsWithSameValue() {
            var w1 = new StratumValueWrapper(new org.hl7.fhir.r4.model.Coding().setCode("ABC"));
            var w2 = new StratumValueWrapper(new org.hl7.fhir.r4.model.Coding().setCode("ABC"));
            assertEquals(w1, w2);
            assertEquals(w1.hashCode(), w2.hashCode());
        }

        @Test
        void equalsWithDifferentValues() {
            var w1 = new StratumValueWrapper(new org.hl7.fhir.r4.model.Coding().setCode("ABC"));
            var w2 = new StratumValueWrapper(new org.hl7.fhir.r4.model.Coding().setCode("DEF"));
            assertNotEquals(w1, w2);
        }
    }

    // ==================== DSTU3 Tests ====================

    @Nested
    class Dstu3 {

        @Test
        void codingGetKey() {
            var coding = new org.hl7.fhir.dstu3.model.Coding()
                    .setSystem("http://example.com")
                    .setCode("ABC")
                    .setDisplay("Alpha");
            var wrapper = new StratumValueWrapper(coding);
            assertEquals("coding-ABC", wrapper.getKey());
        }

        @Test
        void codingGetDescription_withDisplay() {
            var coding = new org.hl7.fhir.dstu3.model.Coding().setCode("ABC").setDisplay("Alpha");
            var wrapper = new StratumValueWrapper(coding);
            assertEquals("Alpha", wrapper.getDescription());
        }

        @Test
        void codingGetDescription_withoutDisplay() {
            var coding = new org.hl7.fhir.dstu3.model.Coding().setCode("ABC");
            var wrapper = new StratumValueWrapper(coding);
            assertEquals("ABC", wrapper.getDescription());
        }

        @Test
        void codeableConceptGetKey() {
            var cc = new org.hl7.fhir.dstu3.model.CodeableConcept()
                    .addCoding(new org.hl7.fhir.dstu3.model.Coding().setCode("XYZ"));
            var wrapper = new StratumValueWrapper(cc);
            assertEquals("codeable-concept-XYZ", wrapper.getKey());
        }

        @Test
        void codeableConceptGetDescription_withDisplay() {
            var cc = new org.hl7.fhir.dstu3.model.CodeableConcept()
                    .addCoding(
                            new org.hl7.fhir.dstu3.model.Coding().setCode("XYZ").setDisplay("X-ray"));
            var wrapper = new StratumValueWrapper(cc);
            assertEquals("X-ray", wrapper.getDescription());
        }

        @Test
        void identifierGetKey() {
            var id = new org.hl7.fhir.dstu3.model.Identifier().setValue("ID-123");
            var wrapper = new StratumValueWrapper(id);
            assertEquals("ID-123", wrapper.getKey());
        }

        @Test
        void resourceGetKey() {
            var patient = new org.hl7.fhir.dstu3.model.Patient();
            patient.setId("Patient/123");
            var wrapper = new StratumValueWrapper(patient);
            assertEquals("Patient/123", wrapper.getKey());
        }

        @Test
        void crossVersionEqualityWithR4() {
            var dstu3Coding = new org.hl7.fhir.dstu3.model.Coding().setCode("ABC");
            var r4Coding = new org.hl7.fhir.r4.model.Coding().setCode("ABC");
            var w1 = new StratumValueWrapper(dstu3Coding);
            var w2 = new StratumValueWrapper(r4Coding);
            assertEquals(w1, w2);
            assertEquals(w1.hashCode(), w2.hashCode());
        }
    }

    // ==================== R5 Tests ====================

    @Nested
    class R5 {

        @Test
        void codingGetKey() {
            var coding = new org.hl7.fhir.r5.model.Coding()
                    .setSystem("http://example.com")
                    .setCode("ABC")
                    .setDisplay("Alpha");
            var wrapper = new StratumValueWrapper(coding);
            assertEquals("coding-ABC", wrapper.getKey());
        }

        @Test
        void codingGetDescription_withDisplay() {
            var coding = new org.hl7.fhir.r5.model.Coding().setCode("ABC").setDisplay("Alpha");
            var wrapper = new StratumValueWrapper(coding);
            assertEquals("Alpha", wrapper.getDescription());
        }

        @Test
        void codingGetDescription_withoutDisplay() {
            var coding = new org.hl7.fhir.r5.model.Coding().setCode("ABC");
            var wrapper = new StratumValueWrapper(coding);
            assertEquals("ABC", wrapper.getDescription());
        }

        @Test
        void codeableConceptGetKey() {
            var cc = new org.hl7.fhir.r5.model.CodeableConcept()
                    .addCoding(new org.hl7.fhir.r5.model.Coding().setCode("XYZ"));
            var wrapper = new StratumValueWrapper(cc);
            assertEquals("codeable-concept-XYZ", wrapper.getKey());
        }

        @Test
        void codeableConceptGetDescription_withDisplay() {
            var cc = new org.hl7.fhir.r5.model.CodeableConcept()
                    .addCoding(new org.hl7.fhir.r5.model.Coding().setCode("XYZ").setDisplay("X-ray"));
            var wrapper = new StratumValueWrapper(cc);
            assertEquals("X-ray", wrapper.getDescription());
        }

        @Test
        void identifierGetKey() {
            var id = new org.hl7.fhir.r5.model.Identifier().setValue("ID-123");
            var wrapper = new StratumValueWrapper(id);
            assertEquals("ID-123", wrapper.getKey());
        }

        @Test
        void resourceGetKey() {
            var patient = new org.hl7.fhir.r5.model.Patient();
            patient.setId("Patient/123");
            var wrapper = new StratumValueWrapper(patient);
            assertEquals("Patient/123", wrapper.getKey());
        }

        @Test
        void crossVersionEqualityWithR4() {
            var r5Coding = new org.hl7.fhir.r5.model.Coding().setCode("ABC");
            var r4Coding = new org.hl7.fhir.r4.model.Coding().setCode("ABC");
            var w1 = new StratumValueWrapper(r5Coding);
            var w2 = new StratumValueWrapper(r4Coding);
            assertEquals(w1, w2);
            assertEquals(w1.hashCode(), w2.hashCode());
        }
    }

    // ==================== Version-agnostic Tests ====================

    @Nested
    class VersionAgnostic {

        @Test
        void cqlCodeGetKey() {
            var code = new Code().withCode("CQL1");
            var wrapper = new StratumValueWrapper(code);
            assertEquals("code-CQL1", wrapper.getKey());
        }

        @Test
        void cqlCodeGetDescription_withDisplay() {
            var code = new Code().withCode("CQL1").withDisplay("CQL Display");
            var wrapper = new StratumValueWrapper(code);
            assertEquals("CQL Display", wrapper.getDescription());
        }

        @Test
        void cqlCodeGetDescription_withoutDisplay() {
            var code = new Code().withCode("CQL1");
            var wrapper = new StratumValueWrapper(code);
            assertEquals("CQL1", wrapper.getDescription());
        }

        @Test
        void cqlCodeGetValueAsString() {
            var code = new Code().withCode("CQL1");
            var wrapper = new StratumValueWrapper(code);
            assertEquals("CQL1", wrapper.getValueAsString());
        }

        @Test
        void enumGetKey() {
            var wrapper = new StratumValueWrapper(MeasureScoring.PROPORTION);
            assertEquals("enum-PROPORTION", wrapper.getKey());
        }

        @Test
        void enumGetDescription() {
            var wrapper = new StratumValueWrapper(MeasureScoring.PROPORTION);
            assertEquals("PROPORTION", wrapper.getDescription());
        }

        @Test
        void enumGetValueAsString() {
            var wrapper = new StratumValueWrapper(MeasureScoring.PROPORTION);
            assertEquals("PROPORTION", wrapper.getValueAsString());
        }

        @Test
        void nullGetKey() {
            var wrapper = new StratumValueWrapper(null);
            assertEquals("null", wrapper.getKey());
        }

        @Test
        void nullGetDescription() {
            var wrapper = new StratumValueWrapper(null);
            assertEquals("null", wrapper.getDescription());
        }

        @Test
        void nullGetValueAsString() {
            var wrapper = new StratumValueWrapper(null);
            assertEquals("null", wrapper.getValueAsString());
        }

        @Test
        void nullGetValueClass() {
            var wrapper = new StratumValueWrapper(null);
            assertEquals(String.class, wrapper.getValueClass());
        }

        @Test
        void emptyListGetKey() {
            var wrapper = new StratumValueWrapper(Collections.emptyList());
            assertEquals("empty", wrapper.getKey());
        }

        @Test
        void emptyListGetDescription() {
            var wrapper = new StratumValueWrapper(Collections.emptyList());
            assertEquals("empty", wrapper.getDescription());
        }

        @Test
        void emptyListGetValueAsString() {
            var wrapper = new StratumValueWrapper(Collections.emptyList());
            assertEquals("empty", wrapper.getValueAsString());
        }

        @Test
        void plainObjectGetKey() {
            var wrapper = new StratumValueWrapper("plainString");
            assertEquals("plainString", wrapper.getKey());
        }

        @Test
        void plainObjectGetDescription() {
            var wrapper = new StratumValueWrapper("plainString");
            assertEquals("plainString", wrapper.getDescription());
        }

        @Test
        void getValueClassForString() {
            var wrapper = new StratumValueWrapper("hello");
            assertEquals(String.class, wrapper.getValueClass());
        }

        @Test
        void equalsWithNulls() {
            var w1 = new StratumValueWrapper(null);
            var w2 = new StratumValueWrapper(null);
            assertEquals(w1, w2);
        }

        @Test
        void notEqualToNull() {
            var w1 = new StratumValueWrapper(new org.hl7.fhir.r4.model.Coding().setCode("ABC"));
            assertNotEquals(null, w1);
        }

        @Test
        void notEqualWhenOneValueNull() {
            var w1 = new StratumValueWrapper(null);
            var w2 = new StratumValueWrapper(new org.hl7.fhir.r4.model.Coding().setCode("ABC"));
            assertNotEquals(w1, w2);
        }

        @Test
        void toStringContainsClassName() {
            var wrapper = new StratumValueWrapper("test");
            assertEquals("StratumValueWrapper[value=test]", wrapper.toString());
        }

        @Test
        void crossVersionCodeableConceptEquality() {
            var dstu3Cc = new org.hl7.fhir.dstu3.model.CodeableConcept()
                    .addCoding(new org.hl7.fhir.dstu3.model.Coding().setCode("XYZ"));
            var r4Cc = new org.hl7.fhir.r4.model.CodeableConcept()
                    .addCoding(new org.hl7.fhir.r4.model.Coding().setCode("XYZ"));
            var r5Cc = new org.hl7.fhir.r5.model.CodeableConcept()
                    .addCoding(new org.hl7.fhir.r5.model.Coding().setCode("XYZ"));

            var w1 = new StratumValueWrapper(dstu3Cc);
            var w2 = new StratumValueWrapper(r4Cc);
            var w3 = new StratumValueWrapper(r5Cc);

            assertEquals(w1, w2);
            assertEquals(w2, w3);
            assertEquals(w1, w3);
        }
    }
}
