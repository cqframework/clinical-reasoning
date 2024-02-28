package org.opencds.cqf.fhir.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Paths;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

class LibraryEngineTests {

    @Test
    void testFhirPath() {
        var patientId = "Patient/Patient1";
        var repository =
                new IgRepository(FhirContext.forR4Cached(), Paths.get(getResourcePath(LibraryEngineTests.class)));
        var libraryEngine = new LibraryEngine(repository, EvaluationSettings.getDefault());

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
    void testFhirPathWithResource() {
        var patientId = "Patient/Patient1";
        var repository =
                new IgRepository(FhirContext.forR4Cached(), Paths.get(getResourcePath(LibraryEngineTests.class)));
        var libraryEngine = new LibraryEngine(repository, EvaluationSettings.getDefault());

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
}
