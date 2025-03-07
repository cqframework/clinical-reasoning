package org.opencds.cqf.fhir.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.cqframework.cql.cql2elm.LibraryContentType;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
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
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

class LibraryEngineTests {

    Repository repository;
    LibraryEngine libraryEngine;

    @BeforeEach
    public void beforeEach() {
        repository = new IgRepository(FhirContext.forR4Cached(), Paths.get(getResourcePath(LibraryEngineTests.class)));
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

        var result = libraryEngine.resolveExpression(patientId, expression, params, null, null, null);
        assertEquals(
                "Greeting: Hello! Alice Message: Test message Practitioner: Michael",
                ((StringType) result.get(0)).getValue());

        var expression2 = new CqfExpression(
                "text/fhirpath", "'Provide discharge instructions for ' + %subject.name.given.first()", null);
        var result2 = libraryEngine.resolveExpression(patientId, expression2, params, null, null, null);
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

        var result = libraryEngine.resolveExpression(patientId, expression, params, null, task, null);
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

        var result = libraryEngine.resolveExpression(patientId, expression, params, null, encounter, task);
        assertNotNull(result);
        assertEquals("Encounter: finished test-system test-code", ((StringType) result.get(0)).getValueAsString());
    }

    @Test
    void expressionWithLibraryReference() {
        var patientId = "Patient/Patient1";
        var expression =
                new CqfExpression("text/cql", "TestLibrary.testExpression", "http://fhir.test/Library/TestLibrary");
        var result = libraryEngine.resolveExpression(patientId, expression, null, null, null, null);
        assertEquals("I am a test", ((StringType) result.get(0)).getValue());
    }

    String libraryCql = "library MyLibrary version '1.0.0'\n"
            + "\n"
            + "using FHIR version '4.0.1'\n"
            + "\n"
            + "include FHIRHelpers version '4.0.1' called FHIRHelpers\n"
            + "\n"
            + "context Patient\n"
            + "\n"
            + "define \"MyNameReturner\":\n"
            + " Patient.name.given";

    @Test
    void expressionWithLibraryResourceProvider() {

        var libraryResourceProvider = new ArrayList<LibrarySourceProvider>();
        libraryResourceProvider.add(new LibrarySourceProvider() {
            @Override
            public InputStream getLibrarySource(VersionedIdentifier libraryIdentifier) {
                return null;
            }

            @Override
            public InputStream getLibraryContent(VersionedIdentifier libraryIdentifier, LibraryContentType type) {
                if ("MyLibrary".equals(libraryIdentifier.getId()))
                    return new ByteArrayInputStream(libraryCql.getBytes(StandardCharsets.UTF_8));
                else return LibrarySourceProvider.super.getLibraryContent(libraryIdentifier, type);
            }
        });
        var evaluationSettings = EvaluationSettings.getDefault().withLibrarySourceProviders(libraryResourceProvider);

        libraryEngine = new LibraryEngine(repository, evaluationSettings);
        repository.create(new Patient().addName(new HumanName().addGiven("me")).setId("Patient/Patient1"));
        var patientId = "Patient/Patient1";
        var expression =
                new CqfExpression("text/cql", "MyLibrary.MyNameReturner", "http://fhir.test/Library/MyLibrary");
        var result = libraryEngine.resolveExpression(patientId, expression, null, null, null, null);
        assertEquals(((StringType) result.get(0)).getValue(), "me");
    }
}
