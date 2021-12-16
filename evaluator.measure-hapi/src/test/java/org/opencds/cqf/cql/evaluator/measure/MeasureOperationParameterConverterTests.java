package org.opencds.cqf.cql.evaluator.measure;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class MeasureOperationParameterConverterTests {

    static MeasureOperationParameterConverter measureOperationParameterConverter;
    
    @BeforeClass
    public void setup() {
        FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
        AdapterFactory adapterFactory = new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory();
        FhirTypeConverter fhirTypeConverter = new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());

        measureOperationParameterConverter = new MeasureOperationParameterConverter(adapterFactory, fhirTypeConverter);

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAddProductLine() {
        Parameters parameters = new Parameters();

        measureOperationParameterConverter.addProductLine(parameters, "Medicare");

        ParametersParameterComponent ppc = parameters.getParameter().stream().filter(x -> x.getName().equals("Product Line")).findFirst().get();
        assertNotNull(ppc);

        IPrimitiveType<String> actual = (IPrimitiveType<String>)ppc.getValue();
        assertNotNull(actual);

        assertTrue(actual.getValue().equals("Medicare"));
    }

    @Test
    public void testNullProductLine() {
        Parameters parameters = new Parameters();

        measureOperationParameterConverter.addProductLine(parameters, null);

        long actualCount = parameters.getParameter().stream().filter(x -> x.getName().equals("Product Line")).count();
        assertEquals(0, actualCount);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOverrideProductLine() {
        Parameters parameters = new Parameters();
        parameters.addParameter("Product Line", "Bubba");

        measureOperationParameterConverter.addProductLine(parameters, "Medicare");

        long actualCount = parameters.getParameter().stream().filter(x -> x.getName().equals("Product Line")).count();
        assertEquals(1, actualCount);

        ParametersParameterComponent ppc = parameters.getParameter().stream().filter(x -> x.getName().equals("Product Line")).findFirst().get();
        assertNotNull(ppc);

        IPrimitiveType<String> actualValue = (IPrimitiveType<String>)ppc.getValue();
        assertNotNull(actualValue);

        assertTrue(actualValue.getValue().equals("Medicare"));
    }

    @Test
    public void testAddMeasurementPeriod() {
        Parameters parameters = new Parameters();

        Period expected = new Period();
        expected.setStartElement(new DateTimeType("2019-01-01"));
        expected.setEndElement(new DateTimeType("2020-01-01"));

        measureOperationParameterConverter.addMeasurementPeriod(parameters, "2019-01-01", "2020-01-01");

        ParametersParameterComponent ppc = parameters.getParameter().stream().filter(x -> x.getName().equals("Measurement Period")).findFirst().get();
        assertNotNull(ppc);

        Period actual = (Period)ppc.getValue();
        assertNotNull(actual);


        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void testOverrideMeasurementPeriod() {
        Parameters parameters = new Parameters();

        Period initial = new Period();
        initial.setStartElement(new DateTimeType("2000-01-01"));
        initial.setEndElement(new DateTimeType("2001-01-01"));

        parameters.addParameter().setName("Measurement Period").setValue(initial);

        Period expected = new Period();
        expected.setStartElement(new DateTimeType("2019-01-01"));
        expected.setEndElement(new DateTimeType("2020-01-01"));

        measureOperationParameterConverter.addMeasurementPeriod(parameters, "2019-01-01", "2020-01-01");

        long actualCount = parameters.getParameter().stream().filter(x -> x.getName().equals("Measurement Period")).count();
        assertEquals(1, actualCount);

        ParametersParameterComponent ppc = parameters.getParameter().stream().filter(x -> x.getName().equals("Measurement Period")).findFirst().get();
        assertNotNull(ppc);

        Period actual = (Period)ppc.getValue();
        assertNotNull(actual);


        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void testNullMeasurementPeriod() {

        Parameters parameters = new Parameters();

        measureOperationParameterConverter.addMeasurementPeriod(parameters, null, null);

        long actualCount = parameters.getParameter().stream().filter(x -> x.getName().equals("Measurement Period")).count();
        assertEquals(0, actualCount);
    }
}
