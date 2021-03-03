package org.opencds.cqf.cql.evaluator.library;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
public class CqlFhirParametersConverterTests {
    

    protected static CqlFhirParametersConverter cqlFhirParametersConverter;
    
    @BeforeClass
    public void setup() {
        FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);

        AdapterFactory adapterFactory = new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory();
        FhirTypeConverter fhirTypeConverter = new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());

        cqlFhirParametersConverter = new CqlFhirParametersConverter(fhirContext, adapterFactory, fhirTypeConverter);

    }

    @Test
    public void TestEvaluationResultToParameters() {
        Parameters expected = new Parameters();
        expected.addParameter().setName("Patient").setResource(new Patient());
        expected.addParameter().setName("Numerator").setValue(new BooleanType(true));

        EvaluationResult testData = new EvaluationResult();
        testData.expressionResults.put("Patient", new Patient());
        testData.expressionResults.put("Numerator", true);

        Parameters actual = (Parameters)cqlFhirParametersConverter.toFhirParameters(testData);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void TestFhirParametersToCqlParameters() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("Measurement Period", new Interval(new Date("2020-01-01"), true, new Date("2021-01-01"), true));
        expected.put("Product Line", "Medicare");

        Parameters testData = new Parameters();
        testData.addParameter().setName("Product Line").setValue(new StringType("Medicare"));

        Period testPeriod = new Period();
        testPeriod.setStartElement(new DateTimeType("2020-01-01"));
        testPeriod.setEndElement(new DateTimeType("2021-01-01"));

        testData.addParameter().setName("Measurement Period").setValue(testPeriod);

        Map<String, Object> actual = cqlFhirParametersConverter.toCqlParameters(testData);

        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());

        assertEquals(expected.get("Product Line"), actual.get("Product Line"));

        assertTrue(((Interval)expected.get("Measurement Period")).equal(actual.get("Measurement Period")));
    }
}
