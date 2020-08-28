package org.opencds.cqf.cql.evaluator.library;

import java.io.File;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.opencds.cqf.cql.evaluator.builder.api.Constants;
import org.opencds.cqf.cql.evaluator.builder.di.EvaluatorModule;
import org.opencds.cqf.cql.evaluator.builder.di.FhirContextModule;
import org.opencds.cqf.cql.evaluator.library.di.LibraryModule;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class LibraryEvaluatorTests {

    private static Injector injector;

    @BeforeClass
    public void SetupDI() {
        injector = Guice.createInjector(
            new FhirContextModule(FhirVersionEnum.R4),
            new EvaluatorModule(),
            new LibraryModule());
    }

    @Test
    public void TestEndToEnd() {

        org.opencds.cqf.cql.evaluator.library.api.LibraryEvaluator evaluator = 
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