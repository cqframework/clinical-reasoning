package org.opencds.cqf.fhir.cql.engine.parameters;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver.fhirModelNamespaceUri;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.elm.executing.EqualEvaluator;
import org.opencds.cqf.cql.engine.execution.EvaluationExpressionRef;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.runtime.Value;
import javax.xml.namespace.QName;

class CqlFhirParametersConverterTests {

    protected static CqlFhirParametersConverter cqlFhirParametersConverter;

    private static BooleanType nullValueMarker() {
        var nullValueMarker = new BooleanType((String) null);
        nullValueMarker.addExtension(
                "http://hl7.org/fhir/StructureDefinition/data-absent-reason", new CodeType("unknown"));
        return nullValueMarker;
    }

    private static BooleanType emptyListMarker() {
        var emptyListMarker = new BooleanType((String) null);
        emptyListMarker.addExtension("http://hl7.org/fhir/StructureDefinition/cqf-isEmptyList", new BooleanType(true));
        return emptyListMarker;
    }

    @BeforeAll
    static void setup() {
        var fhirContext = FhirContext.forCached(FhirVersionEnum.R4);

        var adapterFactory = new org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory();
        var fhirTypeConverter =
                new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());

        cqlFhirParametersConverter = new CqlFhirParametersConverter(fhirContext, adapterFactory, fhirTypeConverter);
    }

    @Test
    void evaluationResultToParameters() {
        var expected = new Parameters();
        expected.addParameter().setName("Patient").setResource(new Patient());
        expected.addParameter().setName("Numerator").setValue(new BooleanType(true));

        var testData = new EvaluationResult();
        testData.set(new EvaluationExpressionRef("Patient"), new ExpressionResult(new ClassInstance(new QName(fhirModelNamespaceUri,"Patient"), Map.of()), null));
        testData.set(new EvaluationExpressionRef("Numerator"), new ExpressionResult(new org.opencds.cqf.cql.engine.runtime.Boolean(true), null));

        var actual = (Parameters) cqlFhirParametersConverter.toFhirParameters(testData);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    void evaluationResultToEmptyListParameters() {
        var expected = new Parameters();
        expected.addParameter().setName("Patient").setResource(new Patient());
        expected.addParameter().setName("Encounters").setValue(emptyListMarker());

        var testData = new EvaluationResult();
        testData.set(new EvaluationExpressionRef("Patient"), new ExpressionResult(new ClassInstance(new QName(fhirModelNamespaceUri,"Patient"), Map.of()), null));
        testData.set(new EvaluationExpressionRef("Encounters"), new ExpressionResult(new org.opencds.cqf.cql.engine.runtime.List(emptyList()), null));

        Parameters actual = (Parameters) cqlFhirParametersConverter.toFhirParameters(testData);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    void evaluationResultsWithListContainingNullValue() {
        var expected = new Parameters();
        expected.addParameter().setName("NullInList").setValue(nullValueMarker());
        expected.addParameter().setName("NullInList").setValue(new IntegerType(5));

        var testList = new ArrayList<Value>();
        testList.add(null);
        testList.add(new org.opencds.cqf.cql.engine.runtime.Integer(5));

        var cqlList = new org.opencds.cqf.cql.engine.runtime.List(testList);

        var testData = new EvaluationResult();
        testData.set(new EvaluationExpressionRef("NullInList"), new ExpressionResult(cqlList, null));

        var actual = (Parameters) cqlFhirParametersConverter.toFhirParameters(testData);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    void evaluationResultNullParameters() {
        var expected = new Parameters();
        expected.addParameter().setName("Patient").setResource(new Patient());
        expected.addParameter().setName("Null").setValue(nullValueMarker());

        var testData = new EvaluationResult();
        testData.set(new EvaluationExpressionRef("Patient"), new ExpressionResult(new ClassInstance(new QName(fhirModelNamespaceUri,"Patient"), Map.of()), null));
        testData.set(new EvaluationExpressionRef("Null"), new ExpressionResult(null, null));

        var actual = (Parameters) cqlFhirParametersConverter.toFhirParameters(testData);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    void fhirParametersToCqlParameters() {
        var expected = new HashMap<String, Value>();
        expected.put("Measurement Period", new Interval(new Date("2020-01-01"), true, new Date("2021-01-01"), true));
        expected.put("Product Line", new org.opencds.cqf.cql.engine.runtime.String("Medicare"));

        var testData = new Parameters();
        testData.addParameter().setName("Product Line").setValue(new StringType("Medicare"));

        var testPeriod = new Period();
        testPeriod.setStartElement(new DateTimeType("2020-01-01"));
        testPeriod.setEndElement(new DateTimeType("2021-01-01"));

        testData.addParameter().setName("Measurement Period").setValue(testPeriod);

        var actual = cqlFhirParametersConverter.toCqlParameters(testData);

        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());

        assertEquals(expected.get("Product Line"), actual.get("Product Line"));

        assertTrue(EqualEvaluator.equal(expected.get("Measurement Period"), actual.get("Measurement Period")).getValue());
    }

    @Test
    void fhirParametersListToCqlParameters() {
        var testData = new Parameters();
        testData.addParameter().setName("%encounters").setResource(new Encounter().setId("1"));
        testData.addParameter().setName("%encounters").setResource(new Encounter().setId("2"));

        var actual = cqlFhirParametersConverter.toCqlParameters(testData);

        assertEquals(1, actual.size());
        assertTrue(actual.containsKey("%encounters"));

        var value = actual.get("%encounters");

        assertInstanceOf(org.opencds.cqf.cql.engine.runtime.List.class, value);

        var encounters = StreamSupport.stream(((org.opencds.cqf.cql.engine.runtime.List) value).getValue().spliterator(), false).toList();

        assertEquals(2, encounters.size());
    }

    @Test
    void maxParameterDefinitionCreatesList() {
        var testData = new Parameters();
        var ppc = testData.addParameter();
        ppc.setName("%encounters").setResource(new Encounter().setId("1"));
        ppc.addExtension(
                "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition",
                new ParameterDefinition().setMax("*").setName("%encounters"));

        var actual = cqlFhirParametersConverter.toCqlParameters(testData);

        assertEquals(1, actual.size());
        assertTrue(actual.containsKey("%encounters"));

        var value = actual.get("%encounters");

        assertInstanceOf(org.opencds.cqf.cql.engine.runtime.List.class, value);

        var encounters = StreamSupport.stream(((org.opencds.cqf.cql.engine.runtime.List) value).getValue().spliterator(), false).toList();

        assertEquals(1, encounters.size());
    }

    @Test
    void minParameterDefinitionCreatesList() {
        var testData = new Parameters();
        var ppc = testData.addParameter();

        // This is technically still an invalid state since there's only one value and the parameter
        // definition
        // requires 2, but that type of validation would happen at the FHIR API level.
        ppc.setName("%encounters").setResource(new Encounter().setId("1"));
        ppc.addExtension(
                "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition",
                new ParameterDefinition().setMin(2).setName("%encounters"));

        var actual = cqlFhirParametersConverter.toCqlParameters(testData);

        assertEquals(1, actual.size());
        assertTrue(actual.containsKey("%encounters"));

        var value = actual.get("%encounters");

        assertInstanceOf(org.opencds.cqf.cql.engine.runtime.List.class, value);

        var encounters = StreamSupport.stream(((org.opencds.cqf.cql.engine.runtime.List) value).getValue().spliterator(), false).toList();

        assertEquals(1, encounters.size());
    }

    @Test
    void invalidParameterDefinitionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            var testData = new Parameters();
            var ppc = testData.addParameter();

            testData.addParameter().setName("%encounters").setResource(new Encounter().setId("1"));

            // This is a case where we'd expect a single value as a parameters, but two are passed.
            ppc.setName("%encounters").setResource(new Encounter().setId("2"));
            ppc.addExtension(
                    "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition",
                    new ParameterDefinition().setMax("1").setName("%encounters"));

            cqlFhirParametersConverter.toCqlParameters(testData);
        });
    }

    @Test
    void TestParameterDefinitionCreatesList() {
        var testData = new Parameters();
        var ppc = testData.addParameter();
        ppc.setName("%encounters");
        ppc.addExtension(
                "http://hl7.org/fhir/uv/cpg/StructureDefinition/cpg-parameterDefinition",
                new ParameterDefinition().setMax("*").setName("%encounters").setType("Encounter"));

        var actual = cqlFhirParametersConverter.toCqlParameters(testData);

        assertEquals(1, actual.size());
        assertTrue(actual.containsKey("%encounters"));

        var value = actual.get("%encounters");

        assertInstanceOf(org.opencds.cqf.cql.engine.runtime.List.class, value);

        var encounters = StreamSupport.stream(((org.opencds.cqf.cql.engine.runtime.List) value).getValue().spliterator(), false).toList();

        assertEquals(0, encounters.size());
    }
}
