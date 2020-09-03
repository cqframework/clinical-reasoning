package org.opencds.cqf.cql.evaluator.library;

import java.io.File;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.BuilderModule;
import org.opencds.cqf.cql.evaluator.fhir.FhirModule;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
public class LibraryEvaluatorTests {

    private static Injector injector;

    @BeforeClass
    public void SetupDI() {
        injector = Guice.createInjector(
            new FhirModule(FhirContext.forR4()),
            new BuilderModule(),
            new LibraryModule());
    }

    @Test
    public void TestEndToEnd() {

        LibraryEvaluator evaluator = 
            injector.getInstance(LibraryEvaluator.class);
            
        FhirContext context = injector.getInstance(FhirContext.class);

        Endpoint endpoint = new Endpoint()
            .setAddress(new File("evaluator.library/src/test/resources/r4").getAbsolutePath().toString())
            .setConnectionType(new Coding().setCode(Constants.HL7_FHIR_FILES));

        IBaseParameters result = evaluator
            .evaluate(new VersionedIdentifier().withId("EXM125").withVersion("8.0.000"), 
            "Patient", 
            "numer-EXM125", 
            null, 
            null, 
            null, 
            endpoint, 
            endpoint, 
            endpoint,
            null, 
            null, 
            null);

        System.out.println(context.newJsonParser().encodeResourceToString(result));
    }
    
}