package org.opencds.cqf.fhir.cr.hapi.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ClasspathUtil;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

class HapiArtifactDiffProcessorTest {

    public HapiArtifactDiffProcessor artifactDiffProcessor;

    @Test
    void testArtifactDiffProcessor_compareComputable() {
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        artifactDiffProcessor = new HapiArtifactDiffProcessor(repository);

        Bundle sourceBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "ersd-small-active-bundle.json");
        Bundle targetBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "ersd-small-drafted-bundle.json");
        repository.transaction(sourceBundle);
        repository.transaction(targetBundle);
        Library source = sourceBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();
        Library target = targetBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();
        boolean compareComputable = true;
        boolean compareExecutable = false;

        Parameters diff = (Parameters)
                artifactDiffProcessor.getArtifactDiff(source, target, compareComputable, compareExecutable, null, null);
        Assertions.assertNotNull(diff);
        List<Parameters> nestedChanges = diff.getParameter().stream()
                .filter(p -> !p.getName().equals("operation"))
                .map(p -> (Parameters) p.getResource())
                .filter(Objects::nonNull)
                .filter(p -> !p.getParameter().isEmpty())
                .toList();
        assertEquals(6, nestedChanges.size());
        Parameters grouperChanges = diff.getParameter().stream()
                .filter(p -> p.getName().contains("/dxtc"))
                .map(p -> (Parameters) p.getResource())
                .findFirst()
                .get();
        List<Parameters.ParametersParameterComponent> deleteOperations =
                getOperationsByType(grouperChanges.getParameter(), "delete");
        List<Parameters.ParametersParameterComponent> insertOperations =
                getOperationsByType(grouperChanges.getParameter(), "insert");
        // delete 2 leafs and extensions
        assertEquals(5, deleteOperations.size());
        // there aren't actually 2 operations here
        assertEquals(2, insertOperations.size());
        String path1 = insertOperations.get(0).getPart().stream()
                .filter(p -> p.getName().equals("path"))
                .map(p -> ((StringType) p.getValue()).getValue())
                .findFirst()
                .get();
        String path2 = insertOperations.get(1).getPart().stream()
                .filter(p -> p.getName().equals("path"))
                .map(p -> ((StringType) p.getValue()).getValue())
                .findFirst()
                .get();
        // insert the new leaf; adding a node takes multiple operations if
        // the thing being added isn't a defined complex FHIR type
        assertEquals("ValueSet.compose.include", path1);
        assertEquals("ValueSet.compose.include[1].valueSet", path2);
    }

    @Test
    void testArtifactDiffProcessor_compareExecutable() {
        var repository = new InMemoryFhirRepository(FhirContext.forR4());
        artifactDiffProcessor = new HapiArtifactDiffProcessor(repository);

        Bundle sourceBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "ersd-small-active-bundle.json");
        Bundle targetBundle =
                ClasspathUtil.loadResource(FhirContext.forR4(), Bundle.class, "ersd-small-drafted-bundle.json");
        repository.transaction(sourceBundle);
        repository.transaction(targetBundle);
        Library source = sourceBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();
        Library target = targetBundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Library)
                .map(e -> (Library) e.getResource())
                .findFirst()
                .get();
        boolean compareComputable = false;
        boolean compareExecutable = true;

        Parameters diff = (Parameters)
                artifactDiffProcessor.getArtifactDiff(source, target, compareComputable, compareExecutable, null, null);
        Assertions.assertNotNull(diff);
        List<Parameters> nestedChanges = diff.getParameter().stream()
                .filter(p -> !p.getName().equals("operation"))
                .map(p -> (Parameters) p.getResource())
                .filter(Objects::nonNull)
                .filter(p -> !p.getParameter().isEmpty())
                .toList();
        assertEquals(6, nestedChanges.size());
        Parameters grouperChanges = diff.getParameter().stream()
                .filter(p -> p.getName().contains("/dxtc"))
                .map(p -> (Parameters) p.getResource())
                .findFirst()
                .get();
        List<Parameters.ParametersParameterComponent> deleteOperations =
                getOperationsByType(grouperChanges.getParameter(), "delete");
        List<Parameters.ParametersParameterComponent> insertOperations =
                getOperationsByType(grouperChanges.getParameter(), "insert");

        // old codes removed
        assertEquals(32, deleteOperations.size());
        // new codes added
        assertEquals(40, insertOperations.size());
    }

    private List<Parameters.ParametersParameterComponent> getOperationsByType(
            List<Parameters.ParametersParameterComponent> parameters, String type) {
        return parameters.stream()
                .filter(p -> p.getName().equals("operation")
                        && p.getPart().stream()
                                .anyMatch(part -> part.getName().equals("type")
                                        && ((CodeType) part.getValue())
                                                .getCode()
                                                .equals(type)))
                .collect(Collectors.toList());
    }
}
