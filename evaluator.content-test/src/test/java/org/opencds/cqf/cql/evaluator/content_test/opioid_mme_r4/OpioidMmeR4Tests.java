package org.opencds.cqf.cql.evaluator.content_test.opioid_mme_r4;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.dagger.DaggerCqlEvaluatorComponent;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class OpioidMmeR4Tests {

    private LibraryProcessor libraryProcessor;
    private FhirContext fhirContext;

    private Endpoint libraryEndpoint;
    private Endpoint terminologyEndpoint;
    
    private VersionedIdentifier id;

    @BeforeClass
    public void setup() {
        this.fhirContext = FhirContext.forCached(FhirVersionEnum.R4);

        this.libraryProcessor = DaggerCqlEvaluatorComponent.builder().fhirContext(fhirContext).build().createLibraryProcessor();
        this.terminologyEndpoint = createEndpoint("vocabulary/valueset", Constants.HL7_FHIR_FILES);
        this.libraryEndpoint = createEndpoint("cql", Constants.HL7_CQL_FILES);

        this.id = new VersionedIdentifier().withId("MMECalculatorTests").withVersion("3.0.0");
    }

    private Endpoint createEndpoint(String url, String type) {
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