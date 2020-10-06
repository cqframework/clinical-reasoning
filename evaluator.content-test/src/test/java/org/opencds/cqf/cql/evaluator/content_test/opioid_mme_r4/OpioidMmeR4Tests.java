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
import org.opencds.cqf.cql.evaluator.guice.fhir.FhirModule;
import org.opencds.cqf.cql.evaluator.guice.library.LibraryModule;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class OpioidMmeR4Tests {

    private LibraryProcessor libraryProcessor;
    private FhirContext fhirContext;

    private Endpoint libraryEndpoint;
    private Endpoint terminologyEndpoint;
    
    private VersionedIdentifier id;

    @BeforeClass
    public void setup() {
        this.fhirContext = FhirContext.forR4();
        Injector injector = Guice.createInjector(new FhirModule(fhirContext), new BuilderModule(), new LibraryModule());

        this.libraryProcessor = injector.getInstance(LibraryProcessor.class);
        this.terminologyEndpoint = this.createEndpoint("vocabulary/valueset", Constants.HL7_FHIR_FILES);
        this.libraryEndpoint = this.createEndpoint("cql", Constants.HL7_CQL_FILES);

        this.id = new VersionedIdentifier().withId("MMECalculatorTests").withVersion("3.0.0");
    }

    private Endpoint createEndpoint(String url, String type) {
        return new Endpoint().setAddress(this.getJarPath(url)).setConnectionType(new Coding().setCode(type));
    }

    private Parameters getParameters(String path) {
        IParser parser = path.endsWith(".json") ? this.fhirContext.newJsonParser() : this.fhirContext.newXmlParser();

        Parameters parameters = (Parameters) parser.parseResource(OpioidMmeR4Tests.class.getResourceAsStream(path));
        return this.clearParserData(parameters);
    }

    private String getSubject(Parameters parameters) {
        StringType subject = (StringType)parameters.getParameter("subject");

        return subject.getValue().replace("Patient/", "");
    }

    private String getJarPath(String resourcePath) {
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
        assertNotNull(this.libraryProcessor);
    }

    @Test
    public void patientMmeLessThan50() { 
        Parameters expected = this.getParameters("tests/MMECalculatorTests/patient-mme-less-than-fifty/Parameters-patient-mme-less-than-fifty-output.json");

        Endpoint dataEndpoint = this.createEndpoint("tests/MMECalculatorTests/patient-mme-less-than-fifty", Constants.HL7_FHIR_FILES);
        Parameters test = this.getParameters("tests/MMECalculatorTests/patient-mme-less-than-fifty/Parameters-patient-mme-less-than-fifty-input.json");


        Parameters actual = (Parameters)this.libraryProcessor.evaluate(id, 
        this.getSubject(test), null, libraryEndpoint, terminologyEndpoint, dataEndpoint, null, asSet("TotalMME"));

        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    public void patientMmeGreaterThan50() { 
        Parameters expected = this.getParameters("tests/MMECalculatorTests/patient-mme-greater-than-fifty/Parameters-patient-mme-greater-than-fifty-output.json");

        Endpoint dataEndpoint = this.createEndpoint("tests/MMECalculatorTests/patient-mme-greater-than-fifty", Constants.HL7_FHIR_FILES);
        Parameters test = this.getParameters("tests/MMECalculatorTests/patient-mme-greater-than-fifty/Parameters-patient-mme-greater-than-fifty-input.json");


        Parameters actual = (Parameters)this.libraryProcessor.evaluate(id, 
        this.getSubject(test), null, libraryEndpoint, terminologyEndpoint, dataEndpoint, null, asSet("TotalMME"));

        assertTrue(expected.equalsDeep(actual));
    }
}