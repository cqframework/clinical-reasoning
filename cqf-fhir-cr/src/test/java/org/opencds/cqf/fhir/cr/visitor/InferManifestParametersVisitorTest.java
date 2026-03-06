package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Constants;
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

    private Library createModuleDefinition() {
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

        return moduleDefinition;
    }

    @Test
    void inferManifestParameters_CodeSystemDependency_CreatesSystemVersionParameter() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();

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

        // Verify cqf-expansionParameters extension
        assertExpansionParametersExtension(result);
    }

    @Test
    void inferManifestParameters_ValueSetDependency_CreatesCanonicalVersionParameter() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();

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

        assertExpansionParametersExtension(result);
    }

    @Test
    void inferManifestParameters_NonTerminologyDependency_IsSkipped() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();

        // Add Library dependency (non-terminology resource)
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/Library/helper|2.0.0");

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify no contained parameters — non-terminology resources are skipped
        assertTrue(result.getContained().isEmpty());
        // No expansion parameters extension when no parameters
        assertTrue(result.getExtensionsByUrl(Constants.CQF_EXPANSION_PARAMETERS).isEmpty());
    }

    @Test
    void inferManifestParameters_MultipleDependencies_CreatesMultipleParameters() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();

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

        // Verify only terminology resources produce parameters (Library and Measure are skipped)
        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(2, parameters.getParameter().size());

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
                .filter(p -> "canonicalVersion".equals(p.getName()))
                .findFirst();
        assertTrue(valueSetParam.isPresent());
        assertEquals(
                "http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type|3.0.0",
                valueSetParam.get().getValue().primitiveValue());

        assertExpansionParametersExtension(result);
    }

    @Test
    void inferManifestParameters_EmptyRelatedArtifacts_CreatesManifestWithoutParameters() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify result is a manifest Library without contained parameters
        assertEquals("asset-collection", result.getType().getCodingFirstRep().getCode());
        assertTrue(result.getContained().isEmpty());
    }

    @Test
    void inferManifestParameters_ManifestMetadata() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();
        moduleDefinition.setName("MyModuleDef");

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify URL and version are copied from input
        assertEquals("http://example.org/Library/module-def", result.getUrl());
        assertEquals("1.0.0", result.getVersion());
        assertEquals("MyModuleDefManifest", result.getName());

        // Manifest is a generated artifact: status=draft, experimental=true, date=now
        assertEquals(Enumerations.PublicationStatus.DRAFT, result.getStatus());
        assertTrue(result.getExperimental());
        assertNotNull(result.getDate());
    }

    @Test
    void inferManifestParameters_RelatedArtifactWithoutResource_SkipsParameter() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();

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

    @Test
    void inferManifestParameters_ExternalCodeSystemWithExtension_CreatesSystemVersionParameter() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();

        // Add external CodeSystem dependency with cqf-resourceType extension on the resource element
        var ra = moduleDefinition.addRelatedArtifact();
        ra.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
        var resourceElement = new CanonicalType("https://www.usps.com/|1.0.0");
        resourceElement.addExtension(Constants.CQF_RESOURCETYPE, new CodeType("CodeSystem"));
        ra.setResourceElement(resourceElement);

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify Parameters are contained
        assertEquals(1, result.getContained().size());
        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());

        // Verify system-version parameter for external CodeSystem
        var param = parameters.getParameter().get(0);
        assertEquals("system-version", param.getName());
        assertEquals("https://www.usps.com/|1.0.0", param.getValue().primitiveValue());
    }

    @Test
    void inferManifestParameters_ExtensionTakesPrecedenceOverUrlParsing() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();

        // Add dependency where URL path says "ValueSet" but extension says "CodeSystem"
        var ra = moduleDefinition.addRelatedArtifact();
        ra.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
        var resourceElement = new CanonicalType("http://example.org/ValueSet/actually-a-codesystem|1.0.0");
        resourceElement.addExtension(Constants.CQF_RESOURCETYPE, new CodeType("CodeSystem"));
        ra.setResourceElement(resourceElement);

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify extension-based CodeSystem type takes precedence
        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        var param = parameters.getParameter().get(0);
        assertEquals("system-version", param.getName());
        assertEquals(
                "http://example.org/ValueSet/actually-a-codesystem|1.0.0",
                param.getValue().primitiveValue());
    }

    @Test
    void inferManifestParameters_OnlyKeyDependsOnIncluded() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();

        // Add depends-on with "key" role — should produce parameter
        var keyRa = moduleDefinition.addRelatedArtifact();
        keyRa.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
        keyRa.setResource("http://example.org/CodeSystem/key-cs|1.0.0");
        keyRa.addExtension(Constants.CRMI_DEPENDENCY_ROLE, new CodeType("key"));

        // Add depends-on with "default" role only — should be skipped
        var defaultRa = moduleDefinition.addRelatedArtifact();
        defaultRa.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
        defaultRa.setResource("http://example.org/ValueSet/default-vs|2.0.0");
        defaultRa.addExtension(Constants.CRMI_DEPENDENCY_ROLE, new CodeType("default"));

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify only the "key" dependency produced a parameter
        assertEquals(1, result.getContained().size());
        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        assertEquals("system-version", parameters.getParameter().get(0).getName());
        assertEquals(
                "http://example.org/CodeSystem/key-cs|1.0.0",
                parameters.getParameter().get(0).getValue().primitiveValue());
    }

    @Test
    void inferManifestParameters_NoDependencyRoleExtension_BackwardCompatible() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();

        // Add depends-on with NO crmi-dependencyRole extension — backward compat: should still produce parameter
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/ValueSet/no-role-vs|1.0.0");

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify parameter was created (backward compat)
        assertEquals(1, result.getContained().size());
        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        assertEquals("canonicalVersion", parameters.getParameter().get(0).getName());
        assertEquals(
                "http://example.org/ValueSet/no-role-vs|1.0.0",
                parameters.getParameter().get(0).getValue().primitiveValue());
    }

    @Test
    void inferManifestParameters_ComposedOfPropagated() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();

        // Add a composed-of RA for an IG
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .setResource("http://example.org/ImplementationGuide/my-ig|1.0.0")
                .setDisplay("My IG");

        // Also add a depends-on RA for a CodeSystem
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/CodeSystem/test-cs|1.0.0");

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify the manifest has the composed-of RA
        var composedOfRas = result.getRelatedArtifact().stream()
                .filter(ra -> ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .toList();
        assertEquals(1, composedOfRas.size());
        assertEquals(
                "http://example.org/ImplementationGuide/my-ig|1.0.0",
                composedOfRas.get(0).getResource());
        assertEquals("My IG", composedOfRas.get(0).getDisplay());

        // Verify parameters were also created for the depends-on entry
        assertEquals(1, result.getContained().size());
        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        assertEquals("system-version", parameters.getParameter().get(0).getName());
    }

    @Test
    void inferManifestParameters_ComposedOfNotInExpansionParams() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();

        // Add ONLY a composed-of RA (no depends-on)
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .setResource("http://example.org/ImplementationGuide/my-ig|1.0.0");

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify no contained parameters (composed-of should not produce expansion parameters)
        assertTrue(result.getContained().isEmpty());

        // But composed-of should still appear as a relatedArtifact
        assertEquals(1, result.getRelatedArtifact().size());
        assertEquals(
                RelatedArtifact.RelatedArtifactType.COMPOSEDOF,
                result.getRelatedArtifact().get(0).getType());
    }

    @Test
    void inferManifestParameters_ExpansionParametersExtension() {
        var repository = new InMemoryFhirRepository(fhirContext);

        var moduleDefinition = createModuleDefinition();

        // Add a CodeSystem dependency to ensure parameters are generated
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/CodeSystem/test-cs|1.0.0");

        // Run $infer-manifest-parameters
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (Library) visitor.visit(adapter, null);

        // Verify cqf-expansionParameters extension
        assertExpansionParametersExtension(result);
    }

    private void assertExpansionParametersExtension(Library manifest) {
        var extList = manifest.getExtensionsByUrl(Constants.CQF_EXPANSION_PARAMETERS);
        assertEquals(1, extList.size(), "Should have exactly one cqf-expansionParameters extension");
        Extension ext = extList.get(0);
        assertTrue(ext.getValue() instanceof Reference, "Extension value should be a Reference");
        assertEquals("#expansion-parameters", ((Reference) ext.getValue()).getReference());
    }

    // ---- DSTU3 tests ----

    @Test
    void inferManifestParameters_Dstu3_CodeSystemDependency_CreatesSystemVersionParameter() {
        var dstu3Context = ca.uhn.fhir.context.FhirContext.forDstu3Cached();
        var repository = new InMemoryFhirRepository(dstu3Context);

        var moduleDefinition = createDstu3ModuleDefinition();
        moduleDefinition
                .addRelatedArtifact()
                .setType(org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource(new org.hl7.fhir.dstu3.model.Reference("http://example.org/CodeSystem/test-cs|1.0.0"));

        var visitor = new InferManifestParametersVisitor(repository);
        var dstu3AdapterFactory = new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();
        var adapter = dstu3AdapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (org.hl7.fhir.dstu3.model.Library) visitor.visit(adapter, null);

        assertEquals("asset-collection", result.getType().getCodingFirstRep().getCode());
        assertEquals("ModuleDefManifest", result.getName());
        assertEquals(1, result.getContained().size());

        var parameters =
                (org.hl7.fhir.dstu3.model.Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        assertEquals("system-version", parameters.getParameter().get(0).getName());
    }

    @Test
    void inferManifestParameters_Dstu3_ValueSetDependency_CreatesCanonicalVersionParameter() {
        var dstu3Context = ca.uhn.fhir.context.FhirContext.forDstu3Cached();
        var repository = new InMemoryFhirRepository(dstu3Context);

        var moduleDefinition = createDstu3ModuleDefinition();
        moduleDefinition
                .addRelatedArtifact()
                .setType(org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource(new org.hl7.fhir.dstu3.model.Reference("http://example.org/ValueSet/test-vs|2.0.0"));

        var visitor = new InferManifestParametersVisitor(repository);
        var dstu3AdapterFactory = new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();
        var adapter = dstu3AdapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (org.hl7.fhir.dstu3.model.Library) visitor.visit(adapter, null);

        var parameters =
                (org.hl7.fhir.dstu3.model.Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        assertEquals("canonicalVersion", parameters.getParameter().get(0).getName());
    }

    @Test
    void inferManifestParameters_Dstu3_EmptyRelatedArtifacts_NoParameters() {
        var dstu3Context = ca.uhn.fhir.context.FhirContext.forDstu3Cached();
        var repository = new InMemoryFhirRepository(dstu3Context);

        var moduleDefinition = createDstu3ModuleDefinition();

        var visitor = new InferManifestParametersVisitor(repository);
        var dstu3AdapterFactory = new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();
        var adapter = dstu3AdapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (org.hl7.fhir.dstu3.model.Library) visitor.visit(adapter, null);

        assertEquals("asset-collection", result.getType().getCodingFirstRep().getCode());
        assertTrue(result.getContained().isEmpty());
    }

    @Test
    void inferManifestParameters_Dstu3_ComposedOfPropagated() {
        var dstu3Context = ca.uhn.fhir.context.FhirContext.forDstu3Cached();
        var repository = new InMemoryFhirRepository(dstu3Context);

        var moduleDefinition = createDstu3ModuleDefinition();
        moduleDefinition
                .addRelatedArtifact()
                .setType(org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .setResource(new org.hl7.fhir.dstu3.model.Reference("http://example.org/ImplementationGuide/ig|1.0.0"));
        moduleDefinition
                .addRelatedArtifact()
                .setType(org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource(new org.hl7.fhir.dstu3.model.Reference("http://example.org/CodeSystem/test-cs|1.0.0"));

        var visitor = new InferManifestParametersVisitor(repository);
        var dstu3AdapterFactory = new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();
        var adapter = dstu3AdapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (org.hl7.fhir.dstu3.model.Library) visitor.visit(adapter, null);

        var composedOf = result.getRelatedArtifact().stream()
                .filter(ra -> ra.getType() == org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .toList();
        assertEquals(1, composedOf.size());
        assertEquals(1, result.getContained().size());
    }

    @Test
    void inferManifestParameters_Dstu3_KeyDependencyRoleFilter() {
        var dstu3Context = ca.uhn.fhir.context.FhirContext.forDstu3Cached();
        var repository = new InMemoryFhirRepository(dstu3Context);

        var moduleDefinition = createDstu3ModuleDefinition();

        // Add depends-on with "key" role
        var keyRa = moduleDefinition.addRelatedArtifact();
        keyRa.setType(org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
        keyRa.setResource(new org.hl7.fhir.dstu3.model.Reference("http://example.org/CodeSystem/key-cs|1.0.0"));
        keyRa.addExtension(Constants.CRMI_DEPENDENCY_ROLE, new org.hl7.fhir.dstu3.model.CodeType("key"));

        // Add depends-on with "default" role only
        var defaultRa = moduleDefinition.addRelatedArtifact();
        defaultRa.setType(org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
        defaultRa.setResource(new org.hl7.fhir.dstu3.model.Reference("http://example.org/ValueSet/default-vs|2.0.0"));
        defaultRa.addExtension(Constants.CRMI_DEPENDENCY_ROLE, new org.hl7.fhir.dstu3.model.CodeType("default"));

        var visitor = new InferManifestParametersVisitor(repository);
        var dstu3AdapterFactory = new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();
        var adapter = dstu3AdapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (org.hl7.fhir.dstu3.model.Library) visitor.visit(adapter, null);

        var parameters =
                (org.hl7.fhir.dstu3.model.Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        assertEquals("system-version", parameters.getParameter().get(0).getName());
    }

    @Test
    void inferManifestParameters_Dstu3_NullName_UsesDefault() {
        var dstu3Context = ca.uhn.fhir.context.FhirContext.forDstu3Cached();
        var repository = new InMemoryFhirRepository(dstu3Context);

        var moduleDefinition = createDstu3ModuleDefinition();
        moduleDefinition.setName(null); // null name

        var visitor = new InferManifestParametersVisitor(repository);
        var dstu3AdapterFactory = new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();
        var adapter = dstu3AdapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (org.hl7.fhir.dstu3.model.Library) visitor.visit(adapter, null);

        assertEquals("Manifest", result.getName());
    }

    private org.hl7.fhir.dstu3.model.Library createDstu3ModuleDefinition() {
        var lib = new org.hl7.fhir.dstu3.model.Library();
        lib.setUrl("http://example.org/Library/module-def");
        lib.setVersion("1.0.0");
        lib.setName("ModuleDef");
        lib.setStatus(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.ACTIVE);
        var typeCC = new org.hl7.fhir.dstu3.model.CodeableConcept();
        typeCC.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/library-type")
                .setCode("module-definition");
        lib.setType(typeCC);
        return lib;
    }

    // ---- R5 tests ----

    @Test
    void inferManifestParameters_R5_CodeSystemDependency_CreatesSystemVersionParameter() {
        var r5Context = ca.uhn.fhir.context.FhirContext.forR5Cached();
        var repository = new InMemoryFhirRepository(r5Context);

        var moduleDefinition = createR5ModuleDefinition();
        moduleDefinition
                .addRelatedArtifact()
                .setType(org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/CodeSystem/test-cs|1.0.0");

        var visitor = new InferManifestParametersVisitor(repository);
        var r5AdapterFactory = new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();
        var adapter = r5AdapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (org.hl7.fhir.r5.model.Library) visitor.visit(adapter, null);

        assertEquals("asset-collection", result.getType().getCodingFirstRep().getCode());
        assertEquals("ModuleDefManifest", result.getName());
        assertEquals(1, result.getContained().size());

        var parameters =
                (org.hl7.fhir.r5.model.Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        assertEquals("system-version", parameters.getParameter().get(0).getName());
    }

    @Test
    void inferManifestParameters_R5_ValueSetDependency_CreatesCanonicalVersionParameter() {
        var r5Context = ca.uhn.fhir.context.FhirContext.forR5Cached();
        var repository = new InMemoryFhirRepository(r5Context);

        var moduleDefinition = createR5ModuleDefinition();
        moduleDefinition
                .addRelatedArtifact()
                .setType(org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/ValueSet/test-vs|2.0.0");

        var visitor = new InferManifestParametersVisitor(repository);
        var r5AdapterFactory = new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();
        var adapter = r5AdapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (org.hl7.fhir.r5.model.Library) visitor.visit(adapter, null);

        var parameters =
                (org.hl7.fhir.r5.model.Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        assertEquals("canonicalVersion", parameters.getParameter().get(0).getName());
    }

    @Test
    void inferManifestParameters_R5_EmptyRelatedArtifacts_NoParameters() {
        var r5Context = ca.uhn.fhir.context.FhirContext.forR5Cached();
        var repository = new InMemoryFhirRepository(r5Context);

        var moduleDefinition = createR5ModuleDefinition();

        var visitor = new InferManifestParametersVisitor(repository);
        var r5AdapterFactory = new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();
        var adapter = r5AdapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (org.hl7.fhir.r5.model.Library) visitor.visit(adapter, null);

        assertEquals("asset-collection", result.getType().getCodingFirstRep().getCode());
        assertTrue(result.getContained().isEmpty());
    }

    @Test
    void inferManifestParameters_R5_ComposedOfPropagated() {
        var r5Context = ca.uhn.fhir.context.FhirContext.forR5Cached();
        var repository = new InMemoryFhirRepository(r5Context);

        var moduleDefinition = createR5ModuleDefinition();
        moduleDefinition
                .addRelatedArtifact()
                .setType(org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .setResource("http://example.org/ImplementationGuide/ig|1.0.0");
        moduleDefinition
                .addRelatedArtifact()
                .setType(org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/CodeSystem/test-cs|1.0.0");

        var visitor = new InferManifestParametersVisitor(repository);
        var r5AdapterFactory = new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();
        var adapter = r5AdapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (org.hl7.fhir.r5.model.Library) visitor.visit(adapter, null);

        var composedOf = result.getRelatedArtifact().stream()
                .filter(ra -> ra.getType() == org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.COMPOSEDOF)
                .toList();
        assertEquals(1, composedOf.size());
        assertEquals(1, result.getContained().size());
    }

    @Test
    void inferManifestParameters_R5_KeyDependencyRoleFilter() {
        var r5Context = ca.uhn.fhir.context.FhirContext.forR5Cached();
        var repository = new InMemoryFhirRepository(r5Context);

        var moduleDefinition = createR5ModuleDefinition();

        // Add depends-on with "key" role
        var keyRa = moduleDefinition.addRelatedArtifact();
        keyRa.setType(org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
        keyRa.setResource("http://example.org/CodeSystem/key-cs|1.0.0");
        keyRa.addExtension(Constants.CRMI_DEPENDENCY_ROLE, new org.hl7.fhir.r5.model.CodeType("key"));

        // Add depends-on with "default" role only
        var defaultRa = moduleDefinition.addRelatedArtifact();
        defaultRa.setType(org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
        defaultRa.setResource("http://example.org/ValueSet/default-vs|2.0.0");
        defaultRa.addExtension(Constants.CRMI_DEPENDENCY_ROLE, new org.hl7.fhir.r5.model.CodeType("default"));

        var visitor = new InferManifestParametersVisitor(repository);
        var r5AdapterFactory = new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();
        var adapter = r5AdapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (org.hl7.fhir.r5.model.Library) visitor.visit(adapter, null);

        var parameters =
                (org.hl7.fhir.r5.model.Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        assertEquals("system-version", parameters.getParameter().get(0).getName());
    }

    @Test
    void inferManifestParameters_R5_ResourceTypeExtension() {
        var r5Context = ca.uhn.fhir.context.FhirContext.forR5Cached();
        var repository = new InMemoryFhirRepository(r5Context);

        var moduleDefinition = createR5ModuleDefinition();

        // Add dependency with cqf-resourceType extension
        var ra = moduleDefinition.addRelatedArtifact();
        ra.setType(org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.DEPENDSON);
        var resourceElement = new org.hl7.fhir.r5.model.CanonicalType("https://www.usps.com/|1.0.0");
        resourceElement.addExtension(Constants.CQF_RESOURCETYPE, new org.hl7.fhir.r5.model.CodeType("CodeSystem"));
        ra.setResourceElement(resourceElement);

        var visitor = new InferManifestParametersVisitor(repository);
        var r5AdapterFactory = new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();
        var adapter = r5AdapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (org.hl7.fhir.r5.model.Library) visitor.visit(adapter, null);

        var parameters =
                (org.hl7.fhir.r5.model.Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        assertEquals("system-version", parameters.getParameter().get(0).getName());
    }

    @Test
    void inferManifestParameters_R5_NullName_UsesDefault() {
        var r5Context = ca.uhn.fhir.context.FhirContext.forR5Cached();
        var repository = new InMemoryFhirRepository(r5Context);

        var moduleDefinition = createR5ModuleDefinition();
        moduleDefinition.setName(null);

        var visitor = new InferManifestParametersVisitor(repository);
        var r5AdapterFactory = new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();
        var adapter = r5AdapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        var result = (org.hl7.fhir.r5.model.Library) visitor.visit(adapter, null);

        assertEquals("Manifest", result.getName());
    }

    private org.hl7.fhir.r5.model.Library createR5ModuleDefinition() {
        var lib = new org.hl7.fhir.r5.model.Library();
        lib.setUrl("http://example.org/Library/module-def");
        lib.setVersion("1.0.0");
        lib.setName("ModuleDef");
        lib.setStatus(org.hl7.fhir.r5.model.Enumerations.PublicationStatus.ACTIVE);
        var typeCC = new org.hl7.fhir.r5.model.CodeableConcept();
        typeCC.addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/library-type")
                .setCode("module-definition");
        lib.setType(typeCC);
        return lib;
    }
}
