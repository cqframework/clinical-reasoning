package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;

public class ParametersTest {

    @Test
    void removeWithOnePart() {
        var parameters = new Parameters();
        var ppc = parameters.addParameter().setName("one");
        ppc.addPart().setValue(new StringType("valueOne"));

        org.opencds.cqf.fhir.utility.Parameters.removeParameter(parameters, "one");

        assertTrue(parameters.isEmpty());
    }

    @Test
    void removeWithTwoParts() {
        var parameters = new Parameters();
        var ppc = parameters.addParameter().setName("one");
        ppc.addPart().setValue(new StringType("valueOne"));
        ppc.addPart().setValue(new StringType("valueTwo"));

        org.opencds.cqf.fhir.utility.Parameters.removeParameter(parameters, "one");

        assertTrue(parameters.isEmpty());
    }

    @Test
    void nonexistentNameDoesNotModify() {
        var parameters = new Parameters();
        var ppc = parameters.addParameter().setName("two");
        ppc.addPart().setValue(new StringType("value"));

        org.opencds.cqf.fhir.utility.Parameters.removeParameter(parameters, "one");

        assertNotNull(parameters.getParameter("two"));
    }

    @Test
    void removeWithValue() {
        var parameters = new Parameters();
        var ppc = parameters.addParameter().setName("one");
        ppc.setValue(new StringType("value"));

        org.opencds.cqf.fhir.utility.Parameters.removeParameter(parameters, "one");

        assertTrue(parameters.isEmpty());
    }

    @Test
    void removeWithResource() {
        var parameters = new Parameters();
        var ppc = parameters.addParameter().setName("one");
        ppc.setResource(new Encounter().setId("123"));

        org.opencds.cqf.fhir.utility.Parameters.removeParameter(parameters, "one");

        assertTrue(parameters.isEmpty());
    }
}
