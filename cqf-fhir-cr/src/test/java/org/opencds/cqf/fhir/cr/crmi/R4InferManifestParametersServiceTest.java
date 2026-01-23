package org.opencds.cqf.fhir.cr.crmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

/**
 * Tests for R4InferManifestParametersService.
 * Verifies that the service correctly processes module-definition Libraries
 * with direct Library input.
 */
class R4InferManifestParametersServiceTest {

    private final FhirContext fhirContext = FhirContext.forR4Cached();

    @Test
    void inferManifestParameters_DirectLibrary_ReturnsManifestWithParameters() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create module-definition Library (not stored in repository)
        var moduleDefinition = new Library();
        moduleDefinition.setUrl("http://example.org/Library/module-def");
        moduleDefinition.setVersion("1.0.0");
        moduleDefinition.setName("ModuleDef");
        moduleDefinition.setStatus(Enumerations.PublicationStatus.ACTIVE);

        var typeCC = new org.hl7.fhir.r4.model.CodeableConcept();
        typeCC.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/library-type")
                .setCode("module-definition");
        moduleDefinition.setType(typeCC);

        // Add multiple dependencies
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://hl7.org/fhir/us/core/CodeSystem/us-core-race|6.1.0");

        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type|3.0.0");

        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/Library/helper|2.0.0");

        // Run service with direct Library parameter
        var service = new R4InferManifestParametersService(repository);
        var result = service.inferManifestParameters(moduleDefinition);

        // Verify result
        assertNotNull(result);
        assertEquals("asset-collection", result.getType().getCodingFirstRep().getCode());
        assertEquals("ModuleDefManifest", result.getName());

        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(3, parameters.getParameter().size());

        // Verify system-version for CodeSystem
        var systemVersionParam = parameters.getParameter().stream()
                .filter(p -> "system-version".equals(p.getName()))
                .findFirst();
        assertTrue(systemVersionParam.isPresent());
        assertEquals(
                "http://hl7.org/fhir/us/core/CodeSystem/us-core-race|6.1.0",
                systemVersionParam.get().getValue().primitiveValue());

        // Verify canonicalVersion for ValueSet
        var valueSetParam = parameters.getParameter().stream()
                .filter(p -> "canonicalVersion".equals(p.getName())
                        && p.getValue().primitiveValue().contains("ValueSet"))
                .findFirst();
        assertTrue(valueSetParam.isPresent());
        assertEquals(
                "http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type|3.0.0",
                valueSetParam.get().getValue().primitiveValue());

        // Verify canonicalVersion with resourceType extension for Library
        var libraryParam = parameters.getParameter().stream()
                .filter(p -> "canonicalVersion".equals(p.getName())
                        && p.getValue().primitiveValue().contains("Library"))
                .findFirst();
        assertTrue(libraryParam.isPresent());
        var libraryExt = libraryParam
                .get()
                .getExtensionByUrl("http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-resourceType");
        assertNotNull(libraryExt);
        assertEquals("Library", ((CodeType) libraryExt.getValue()).getCode());
    }

    @Test
    void inferManifestParameters_DirectLibrary_PreservesMetadata() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = new Library();
        moduleDefinition.setUrl("http://example.org/Library/module-def");
        moduleDefinition.setVersion("2.5.0");
        moduleDefinition.setName("MyCustomModule");
        moduleDefinition.setStatus(Enumerations.PublicationStatus.DRAFT);

        var typeCC = new org.hl7.fhir.r4.model.CodeableConcept();
        typeCC.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/library-type")
                .setCode("module-definition");
        moduleDefinition.setType(typeCC);

        var service = new R4InferManifestParametersService(repository);
        var result = service.inferManifestParameters(moduleDefinition);

        assertEquals("http://example.org/Library/module-def", result.getUrl());
        assertEquals("2.5.0", result.getVersion());
        assertEquals("MyCustomModuleManifest", result.getName());
        assertEquals(Enumerations.PublicationStatus.DRAFT, result.getStatus());
    }
}
