package org.opencds.cqf.fhir.cql;

import static kotlinx.io.CoreKt.buffered;
import static kotlinx.io.JvmCoreKt.asSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import kotlinx.io.Source;
import org.cqframework.cql.cql2elm.LibraryContentType;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

class LibraryEngineTests {

    IRepository repository;
    LibraryEngine libraryEngine;

    @BeforeEach
    public void beforeEach() {
        var path = Path.of(getResourcePath(LibraryEngineTests.class) + "/org/opencds/cqf/fhir/cql");
        repository = new IgRepository(FhirContext.forR4Cached(), path);
        libraryEngine = new LibraryEngine(repository, EvaluationSettings.getDefault());
    }

    @Test
    void fhirPath() {
        var patientId = "Patient/Patient1";
        var params = parameters();
        params.addParameter(part("%subject", new Patient().addName(new HumanName().addGiven("Alice"))));
        params.addParameter(part("%practitioner", new Practitioner().addName(new HumanName().addGiven("Michael"))));
        var expression = new CqfExpression(
                "text/fhirpath",
                "'Greeting: Hello! ' + %subject.name.given.first() + ' Message: Test message Practitioner: ' + %practitioner.name.given.first()",
                null);

        var result = libraryEngine.resolveExpression(patientId, expression, params, null, null, null, null);
        assertEquals(
                "Greeting: Hello! Alice Message: Test message Practitioner: Michael",
                ((StringType) result.get(0)).getValue());

        var expression2 = new CqfExpression(
                "text/fhirpath", "'Provide discharge instructions for ' + %subject.name.given.first()", null);
        var result2 = libraryEngine.resolveExpression(patientId, expression2, params, null, null, null, null);
        assertEquals("Provide discharge instructions for Alice", ((StringType) result2.get(0)).getValue());
    }

    @Test
    void fhirPathWithResource() {
        var patientId = "Patient/Patient1";
        var params = parameters();
        params.addParameter(part("%subject", new Patient().addName(new HumanName().addGiven("Alice"))));
        params.addParameter(part("%practitioner", new Practitioner().addName(new HumanName().addGiven("Michael"))));
        var expression = new CqfExpression("text/fhirpath", "%resource.code", null);

        var task = new Task().setCode(new CodeableConcept(new Coding("test-system", "test-code", null)));

        var result = libraryEngine.resolveExpression(patientId, expression, params, null, null, task, null);
        assertEquals(
                "test-system",
                ((CodeableConcept) result.get(0)).getCodingFirstRep().getSystem());
        assertEquals(
                "test-code",
                ((CodeableConcept) result.get(0)).getCodingFirstRep().getCode());
    }

    @Test
    void fhirPathWithContextAndResource() {
        var patientId = "Patient/Patient1";
        var patient = new Patient().addName(new HumanName().addGiven("Alice")).setId(patientId);
        var encounter = new Encounter()
                .setSubject(new Reference(patient.getIdElement()))
                .setStatus(EncounterStatus.FINISHED);
        var params = parameters();
        params.addParameter(part("%subject", patient));
        params.addParameter(part("%practitioner", new Practitioner().addName(new HumanName().addGiven("Michael"))));
        var expression = new CqfExpression(
                "text/fhirpath",
                "'Encounter: ' + %context.status + ' ' + %resource.code.coding[0].system + ' ' + %resource.code.coding[0].code",
                null);

        var task = new Task().setCode(new CodeableConcept(new Coding("test-system", "test-code", null)));

        var result = libraryEngine.resolveExpression(patientId, expression, params, null, null, encounter, task);
        assertNotNull(result);
        assertEquals("Encounter: finished test-system test-code", ((StringType) result.get(0)).getValueAsString());
    }

    @Test
    void expressionWithLibraryReference() {
        var patientId = "Patient/Patient1";
        var expression = new CqfExpression(
                "text/cql",
                "TestLibrary.testExpression",
                Map.of("TestLibrary", "http://fhir.test/Library/TestLibrary"));
        var result = libraryEngine.resolveExpression(patientId, expression, null, null, null, null, null);
        assertEquals("I am a test", ((StringType) result.get(0)).getValue());
    }

    String libraryCql = """
            library MyLibrary version '1.0.0'

            using FHIR version '4.0.1'

            include FHIRHelpers version '4.0.1' called FHIRHelpers

            context Patient

            define "MyNameReturner":
             Patient.name.given""";

    @Test
    void expressionWithLibraryResourceProvider() {

        var libraryResourceProvider = new ArrayList<LibrarySourceProvider>();
        libraryResourceProvider.add(new LibrarySourceProvider() {
            @Override
            public Source getLibrarySource(VersionedIdentifier libraryIdentifier) {
                return null;
            }

            @Override
            public Source getLibraryContent(VersionedIdentifier libraryIdentifier, LibraryContentType type) {
                if ("MyLibrary".equals(libraryIdentifier.getId()))
                    return buffered(asSource(new ByteArrayInputStream(libraryCql.getBytes(StandardCharsets.UTF_8))));
                else return LibrarySourceProvider.DefaultImpls.getLibraryContent(this, libraryIdentifier, type);
            }
        });
        var evaluationSettings = EvaluationSettings.getDefault().withLibrarySourceProviders(libraryResourceProvider);

        libraryEngine = new LibraryEngine(repository, evaluationSettings);
        repository.create(new Patient().addName(new HumanName().addGiven("me")).setId("Patient/Patient1"));
        var patientId = "Patient/Patient1";
        var expression = new CqfExpression(
                "text/cql", "MyLibrary.MyNameReturner", Map.of("MyLibrary", "http://fhir.test/Library/MyLibrary"));
        var result = libraryEngine.resolveExpression(patientId, expression, null, null, null, null, null);
        assertEquals("me", ((StringType) result.get(0)).getValue());
    }

    @Test
    void multipleExpressions() {
        var patientId = "Patient/Patient1";
        var patient = new Patient().addName(new HumanName().addGiven("Alice")).setId(patientId);
        var codeableConcept1 = new CodeableConcept().setText("TestText");
        var condition1 = new Condition()
                .setSubject(new Reference(patient.getIdElement()))
                .setCode(codeableConcept1);
        var codeableConcept2 =
                new CodeableConcept().addCoding(new Coding().setCode("TestCode").setDisplay("TestDisplay"));
        var condition2 = new Condition()
                .setSubject(new Reference(patient.getIdElement()))
                .setCode(codeableConcept2);
        var params1 = parameters();
        params1.addParameter(part("%subject", patient));
        params1.addParameter(part("%TestExpression", condition1));
        var params2 = parameters();
        params2.addParameter(part("%subject", patient));
        params2.addParameter(part("%TestExpression", condition2));
        var expression1 = new CqfExpression("text/cql-expression", "%TestExpression.code", null);
        var expression2 = new CqfExpression("text/cql-expression", "%TestExpression.code.coding.display", null);

        var result1 = libraryEngine.resolveExpression(patientId, expression1, params1, null, null, null, null);
        var result2 = libraryEngine.resolveExpression(patientId, expression2, params1, null, null, null, null);
        var result3 = libraryEngine.resolveExpression(patientId, expression1, params2, null, null, null, null);
        var result4 = libraryEngine.resolveExpression(patientId, expression2, params2, null, null, null, null);
        assertEquals(codeableConcept1, result1.get(0));
        assertNull(((BooleanType) result2.get(0)).getValue());
        assertEquals(codeableConcept2, result3.get(0));
        assertEquals(codeableConcept2.getCodingFirstRep().getDisplay(), ((StringType) result4.get(0)).getValue());
    }
}
