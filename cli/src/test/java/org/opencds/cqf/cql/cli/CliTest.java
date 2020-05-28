package org.opencds.cqf.cql.cli;

import org.junit.Test;
import org.opencds.cqf.cql.service.Parameters;
import org.opencds.cqf.cql.service.Response;
import org.opencds.cqf.cql.service.Service;

import java.util.EnumSet;

import static org.junit.Assert.assertNotNull;

public class CliTest {

    private Parameters parseParameters(String[] args) {
        Parameters params = new ArgumentProcessor().parseAndConvert(args);
        return params;
    }

    private Response evaluate(Parameters params) {
        Service service = new Service(EnumSet.of(Service.Options.EnableFileUri));
        Response response = service.evaluate(params);
        return response;
    }

    @Test
    public void testDstu3() {

    }

    @Test
    public void testR4() {
        String[] args = new String[]{
                "-lp=C:\\Users\\Bryn\\Documents\\Src\\SS\\CQL-Evaluator\\cli\\src\\test\\resources\\r4",
                "-ln=TestFHIR",
                "-m=FHIR=C:\\Users\\Bryn\\Documents\\Src\\SS\\CQL-Evaluator\\cli\\src\\test\\resources\\r4",
                "-t=C:\\Users\\Bryn\\Documents\\Src\\SS\\CQL-Evaluator\\cli\\src\\test\\resources\\r4\\vocabulary\\ValueSet",
                "-cPatient=example"
        };

        Parameters params = parseParameters(args);
        Response response = evaluate(params);
        assertNotNull(response);
    }

    @Test
    public void testUSCore() {

    }

    @Test
    public void testQICore() {

    }
}
