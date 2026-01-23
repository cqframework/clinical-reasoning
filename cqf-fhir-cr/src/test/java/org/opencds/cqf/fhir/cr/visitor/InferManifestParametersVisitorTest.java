package org.opencds.cqf.fhir.cr.visitor;

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
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;

/**
 * Tests for the $infer-manifest-parameters operation.
 * Verifies that module-definition Libraries are correctly converted to manifest Libraries
 * with expansion parameters.
 */
class InferManifestParametersVisitorTest {

    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private final AdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void inferManifestParameters_CodeSystemDependency_CreatesSystemVersionParameter() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create module-definition Library with CodeSystem dependency
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

        // Add CodeSystem dependency
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://hl7.org/fhir/us/core/CodeSystem/us-core-race|6.1.0");

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify result is a manifest Library
        assertEquals("asset-collection", result.getType().getCodingFirstRep().getCode());
        assertEquals("ModuleDefManifest", result.getName());

        // Verify Parameters are contained
        assertEquals(1, result.getContained().size());
        var parameters = (Parameters) result.getContained().get(0);
        assertEquals("expansion-parameters", parameters.getId());

        // Verify system-version parameter
        assertEquals(1, parameters.getParameter().size());
        var param = parameters.getParameter().get(0);
        assertEquals("system-version", param.getName());
        assertEquals(
                "http://hl7.org/fhir/us/core/CodeSystem/us-core-race|6.1.0",
                param.getValue().primitiveValue());
    }

    @Test
    void inferManifestParameters_ValueSetDependency_CreatesCanonicalVersionParameter() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create module-definition Library with ValueSet dependency
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

        // Add ValueSet dependency
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type|3.0.0");

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify Parameters
        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        var param = parameters.getParameter().get(0);
        assertEquals("canonicalVersion", param.getName());
        assertEquals(
                "http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type|3.0.0",
                param.getValue().primitiveValue());
    }

    @Test
    void inferManifestParameters_OtherResourceDependency_CreatesCanonicalVersionParameterWithExtension() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create module-definition Library with Library dependency
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

        // Add Library dependency
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/Library/helper|2.0.0");

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify Parameters
        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        var param = parameters.getParameter().get(0);
        assertEquals("canonicalVersion", param.getName());
        assertEquals("http://example.org/Library/helper|2.0.0", param.getValue().primitiveValue());

        // Verify resourceType extension
        assertTrue(param.hasExtension());
        var ext = param.getExtensionByUrl("http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-resourceType");
        assertNotNull(ext);
        assertEquals("Library", ((CodeType) ext.getValue()).getCode());
    }

    @Test
    void inferManifestParameters_MultipleDependencies_CreatesMultipleParameters() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create module-definition Library with multiple dependencies
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

        // Add CodeSystem dependency
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://hl7.org/fhir/us/core/CodeSystem/us-core-race|6.1.0");

        // Add ValueSet dependency
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type|3.0.0");

        // Add Library dependency
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/Library/helper|2.0.0");

        // Add Measure dependency
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/Measure/quality-measure|1.0.0");

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify Parameters
        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(4, parameters.getParameter().size());

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

        // Verify canonicalVersion with resourceType extension for Measure
        var measureParam = parameters.getParameter().stream()
                .filter(p -> "canonicalVersion".equals(p.getName())
                        && p.getValue().primitiveValue().contains("Measure"))
                .findFirst();
        assertTrue(measureParam.isPresent());
        var measureExt = measureParam
                .get()
                .getExtensionByUrl("http://hl7.org/fhir/uv/crmi/StructureDefinition/crmi-resourceType");
        assertNotNull(measureExt);
        assertEquals("Measure", ((CodeType) measureExt.getValue()).getCode());
    }

    @Test
    void inferManifestParameters_EmptyRelatedArtifacts_CreatesManifestWithoutParameters() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create module-definition Library with no dependencies
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

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify result is a manifest Library without contained parameters
        assertEquals("asset-collection", result.getType().getCodingFirstRep().getCode());
        assertTrue(result.getContained().isEmpty());
    }

    @Test
    void inferManifestParameters_PreservesMetadata() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create module-definition Library
        var moduleDefinition = new Library();
        moduleDefinition.setUrl("http://example.org/Library/module-def");
        moduleDefinition.setVersion("1.0.0");
        moduleDefinition.setName("MyModuleDef");
        moduleDefinition.setStatus(Enumerations.PublicationStatus.ACTIVE);

        var typeCC = new org.hl7.fhir.r4.model.CodeableConcept();
        typeCC.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/library-type")
                .setCode("module-definition");
        moduleDefinition.setType(typeCC);

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify metadata is preserved
        assertEquals("http://example.org/Library/module-def", result.getUrl());
        assertEquals("1.0.0", result.getVersion());
        assertEquals("MyModuleDefManifest", result.getName());
        assertEquals(Enumerations.PublicationStatus.ACTIVE, result.getStatus());
    }

    @Test
    void inferManifestParameters_RelatedArtifactWithoutResource_SkipsParameter() {
        var repository = new InMemoryFhirRepository(fhirContext);

        // Create module-definition Library
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

        // Add relatedArtifact with NO resource (empty)
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setDisplay("Some documentation");

        // Also add a valid one
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/CodeSystem/valid|1.0.0");

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify only valid dependency created a parameter
        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        assertEquals(
                "http://example.org/CodeSystem/valid|1.0.0",
                parameters.getParameter().get(0).getValue().primitiveValue());
    }
}
