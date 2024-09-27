package org.opencds.cqf.fhir.utility;

import static org.junit.Assert.assertTrue;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;

public class ParametersTest {

    @Test
    public void removeWithOnePart() {
        var parameters = new Parameters();
        var ppc = parameters.addParameter().setName("one");
        ppc.addPart().setValue(new StringType("valueOne"));

        org.opencds.cqf.fhir.utility.Parameters.removeParameter(parameters, "one");

        assertTrue(parameters.isEmpty());
    }

    @Test
    public void removeWithTwoParts() {
        var parameters = new Parameters();
        var ppc = parameters.addParameter().setName("one");
        ppc.addPart().setValue(new StringType("valueOne"));
        ppc.addPart().setValue(new StringType("valueTwo"));

        org.opencds.cqf.fhir.utility.Parameters.removeParameter(parameters, "bubba");

        assertTrue(parameters.isEmpty());
    }

    @Test
    public void nonexistentNameDoesNotModify() {
        var parameters = new Parameters();
        var ppc = parameters.addParameter().setName("two");
        ppc.addPart().setValue(new StringType("value"));

        org.opencds.cqf.fhir.utility.Parameters.removeParameter(parameters, "one");

        assertTrue(parameters.getParameter("two") != null);
    }

    @Test
    public void removeWithValue() {
        var parameters = new Parameters();
        var ppc = parameters.addParameter().setName("one");
        ppc.setValue(new StringType("value"));

        org.opencds.cqf.fhir.utility.Parameters.removeParameter(parameters, "one");

        assertTrue(parameters.isEmpty());
    }

    @Test
    public void removeWithResource() {
        var parameters = new Parameters();
        var ppc = parameters.addParameter().setName("one");
        ppc.setResource(new Encounter().setId("123"));

        org.opencds.cqf.fhir.utility.Parameters.removeParameter(parameters, "one");

        assertTrue(parameters.isEmpty());
    }
}
