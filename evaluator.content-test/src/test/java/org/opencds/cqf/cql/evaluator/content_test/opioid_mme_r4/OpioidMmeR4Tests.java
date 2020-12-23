package org.opencds.cqf.cql.evaluator.content_test.opioid_mme_r4;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.r4.model.Coding;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.guice.builder.BuilderModule;
import org.opencds.cqf.cql.evaluator.guice.cql2elm.Cql2ElmModule;
import org.opencds.cqf.cql.evaluator.guice.fhir.FhirModule;
import org.opencds.cqf.cql.evaluator.guice.library.LibraryModule;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class OpioidMmeR4Tests {

    private static LibraryProcessor libraryProcessor;
    private static FhirContext fhirContext;

    private static Endpoint libraryEndpoint;
    private static Endpoint terminologyEndpoint;
    
    private static VersionedIdentifier id;

    @BeforeClass
    public static void setup() {
        fhirContext = FhirContext.forR4();
        Injector injector = Guice.createInjector(
            new FhirModule(fhirContext),
            new Cql2ElmModule(), 
            new BuilderModule(), 
            new LibraryModule());

        libraryProcessor = injector.getInstance(LibraryProcessor.class);
        terminologyEndpoint = createEndpoint("vocabulary/valueset", Constants.HL7_FHIR_FILES);
        libraryEndpoint = createEndpoint("cql", Constants.HL7_CQL_FILES);

        id = new VersionedIdentifier().withId("MMECalculatorTests").withVersion("3.0.0");
    }

    private static Endpoint createEndpoint(String url, String type) {
        return new Endpoint().setAddress(getJarPath(url)).setConnectionType(new Coding().setCode(type));
    }

    private Parameters getParameters(String path) {
        IParser parser = path.endsWith(".json") ? fhirContext.newJsonParser() : fhirContext.newXmlParser();

        Parameters parameters = (Parameters) parser.parseResource(OpioidMmeR4Tests.class.getResourceAsStream(path));
        return this.clearParserData(parameters);
    }

    private String getSubject(Parameters parameters) {
        StringType subject = (StringType)parameters.getParameter("subject");

        return subject.getValue().replace("Patient/", "");
    }

    private static String getJarPath(String resourcePath) {
        try {
            return OpioidMmeR4Tests.class.getResource(resourcePath).toURI().toString();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> asSet(String... strings) {
        Set<String> set = new HashSet<>();
        for (String s : strings) {
            set.add(s);
        }

        return set;
    }

    private Parameters clearParserData(Parameters parameters) {
        parameters.setIdElement(null);
        parameters.clearUserData("ca.uhn.fhir.parser.BaseParser_RESOURCE_CREATED_BY_PARSER");
        return parameters;
    }

    @Test
    public void canInstantiate() {
        assertNotNull(libraryProcessor);
    }

    @Test
    public void patientMmeLessThan50() { 
        Parameters expected = getParameters("tests/MMECalculatorTests/patient-mme-less-than-fifty/Parameters-patient-mme-less-than-fifty-output.json");

        Endpoint dataEndpoint = createEndpoint("tests/MMECalculatorTests/patient-mme-less-than-fifty", Constants.HL7_FHIR_FILES);
        Parameters test = this.getParameters("tests/MMECalculatorTests/patient-mme-less-than-fifty/Parameters-patient-mme-less-than-fifty-input.json");


        Parameters actual = (Parameters)libraryProcessor.evaluate(id, 
        this.getSubject(test), null, libraryEndpoint, terminologyEndpoint, dataEndpoint, null, asSet("TotalMME"));

        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void patientMmeGreaterThan50() { 
        Parameters expected = this.getParameters("tests/MMECalculatorTests/patient-mme-greater-than-fifty/Parameters-patient-mme-greater-than-fifty-output.json");

        Endpoint dataEndpoint = createEndpoint("tests/MMECalculatorTests/patient-mme-greater-than-fifty", Constants.HL7_FHIR_FILES);
        Parameters test = this.getParameters("tests/MMECalculatorTests/patient-mme-greater-than-fifty/Parameters-patient-mme-greater-than-fifty-input.json");


        Parameters actual = (Parameters)libraryProcessor.evaluate(id, 
        this.getSubject(test), null, libraryEndpoint, terminologyEndpoint, dataEndpoint, null, asSet("TotalMME"));

        assertTrue(expected.equalsDeep(actual));
    }
}