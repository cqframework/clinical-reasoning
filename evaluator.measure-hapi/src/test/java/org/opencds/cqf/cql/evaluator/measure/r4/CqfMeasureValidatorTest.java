package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirVersionEnum;

@Test(singleThreaded = true)
public class CqfMeasureValidatorTest {
    private CqfMeasureValidator validator;

    public CqfMeasureValidatorTest() {
        this.validator = new CqfMeasureValidator(FhirVersionEnum.R4);
    }

    @Test
    public void testValidMeasure() throws Exception {
        // This measure contains invalid expression.language values of cql.identifier.
        // The validator ignores these errors for backwards compatibility
        var measure = this.validator.parser.parseResource(this.getClass().getResourceAsStream("AntithromboticTherapyByEndofHospitalDay2FHIR4.json"));
        var result = this.validator.validate(measure);
        assertEquals(measure, result);
    }

    @Test
    public void testInvalidMeasure() throws Exception {
        // This measure is a copy of the previous measure with the publisher removed so it fails validation
        var measure = this.validator.parser.parseResource(this.getClass().getResourceAsStream("AntithromboticTherapyByEndofHospitalDay2FHIR4-invalid.json"));
        var result = this.validator.validate(measure);
        assertEquals(result.fhirType(), "OperationOutcome");
    }
}
