package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
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

    private Library runInferManifestParameters(Library moduleDefinition) {
        var repository = new InMemoryFhirRepository(fhirContext);
        var visitor = new InferManifestParametersVisitor(repository);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(moduleDefinition);
        return (Library) visitor.visit(adapter, null);
    }

    @Test
    void inferManifestParameters_CodeSystemDependency_CreatesSystemVersionParameter() {
        var moduleDefinition = createModuleDefinition();
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://hl7.org/fhir/us/core/CodeSystem/us-core-race|6.1.0");

        var result = runInferManifestParameters(moduleDefinition);

        assertEquals("asset-collection", result.getType().getCodingFirstRep().getCode());
        assertEquals("ModuleDefManifest", result.getName());

        var parameters = (Parameters) result.getContained().get(0);
        assertEquals("expansion-parameters", parameters.getId());
        assertEquals(1, parameters.getParameter().size());

        var param = parameters.getParameter().get(0);
        assertEquals("system-version", param.getName());
        assertEquals(
                "http://hl7.org/fhir/us/core/CodeSystem/us-core-race|6.1.0",
                param.getValue().primitiveValue());
    }

    @Test
    void inferManifestParameters_CodeSystemDependency_HasCqfResourceTypeExtension() {
        var moduleDefinition = createModuleDefinition();
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://hl7.org/fhir/us/core/CodeSystem/us-core-race|6.1.0");

        var result = runInferManifestParameters(moduleDefinition);
        var parameters = (Parameters) result.getContained().get(0);
        var param = parameters.getParameter().get(0);

        // system-version parameters should also have cqf-resourceType extension
        var ext = param.getExtensionByUrl(Constants.CQF_RESOURCETYPE);
        assertNotNull(ext, "system-version parameter should have cqf-resourceType extension");
        assertEquals("CodeSystem", ((CodeType) ext.getValue()).getCode());
    }

    @Test
    void inferManifestParameters_ValueSetDependency_CreatesCanonicalVersionParameter() {
        var moduleDefinition = createModuleDefinition();
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type|3.0.0");

        var result = runInferManifestParameters(moduleDefinition);

        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        var param = parameters.getParameter().get(0);
        assertEquals("canonicalVersion", param.getName());
        assertEquals(
                "http://hl7.org/fhir/uv/sdc/ValueSet/sdc-question-type|3.0.0",
                param.getValue().primitiveValue());

        var ext = param.getExtensionByUrl(Constants.CQF_RESOURCETYPE);
        assertNotNull(ext, "canonicalVersion parameter should have cqf-resourceType extension");
        assertEquals("ValueSet", ((CodeType) ext.getValue()).getCode());
    }

    @Test
    void inferManifestParameters_OtherResourceDependency_CreatesCanonicalVersionParameterWithExtension() {
        var moduleDefinition = createModuleDefinition();
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/Library/helper|2.0.0");

        var result = runInferManifestParameters(moduleDefinition);

        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        var param = parameters.getParameter().get(0);
        assertEquals("canonicalVersion", param.getName());
        assertEquals("http://example.org/Library/helper|2.0.0", param.getValue().primitiveValue());

        var ext = param.getExtensionByUrl(Constants.CQF_RESOURCETYPE);
        assertNotNull(ext);
        assertEquals("Library", ((CodeType) ext.getValue()).getCode());
    }

    @Test
    void inferManifestParameters_NonStandardUrlWithCqfResourceTypeExtension_UsesExtensionValue() {
        // External CodeSystem like http://www.ada.org/cdt has a non-standard URL.
        // When the relatedArtifact has a cqf-resourceType extension, it should be used.
        var moduleDefinition = createModuleDefinition();
        var ra = moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://www.ada.org/cdt");
        ra.addExtension(new Extension()
                .setUrl(Constants.CQF_RESOURCETYPE)
                .setValue(new CodeType("CodeSystem")));

        var result = runInferManifestParameters(moduleDefinition);

        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());

        var param = parameters.getParameter().get(0);
        // Should be system-version since the extension says CodeSystem
        assertEquals("system-version", param.getName());
        assertEquals("http://www.ada.org/cdt", param.getValue().primitiveValue());
    }

    @Test
    void inferManifestParameters_NonStandardUrlWithoutExtension_DefaultsToCodeSystem() {
        // External CodeSystem with non-standard URL and NO extension.
        // Resource type cannot be inferred from URL, so it defaults to CodeSystem.
        var moduleDefinition = createModuleDefinition();
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://www.ada.org/cdt");

        var result = runInferManifestParameters(moduleDefinition);

        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());

        var param = parameters.getParameter().get(0);
        assertEquals("system-version", param.getName());
        assertEquals("http://www.ada.org/cdt", param.getValue().primitiveValue());

        var ext = param.getExtensionByUrl(Constants.CQF_RESOURCETYPE);
        assertNotNull(ext, "defaulted parameter should have cqf-resourceType extension");
        assertEquals("CodeSystem", ((CodeType) ext.getValue()).getCode());
    }

    @Test
    void inferManifestParameters_UnversionedCodeSystem_SystemVersionWithoutPipeVersion() {
        // External CodeSystem without version - should produce system-version without |version
        var moduleDefinition = createModuleDefinition();
        var ra = moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://snomed.info/sct");
        ra.addExtension(new Extension()
                .setUrl(Constants.CQF_RESOURCETYPE)
                .setValue(new CodeType("CodeSystem")));

        var result = runInferManifestParameters(moduleDefinition);

        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());

        var param = parameters.getParameter().get(0);
        assertEquals("system-version", param.getName());
        // No pipe-version: signals consumer must resolve version
        assertEquals("http://snomed.info/sct", param.getValue().primitiveValue());
    }

    @Test
    void inferManifestParameters_UnversionedValueSet_CanonicalVersionWithoutPipeVersion() {
        // External ValueSet without version - should produce canonicalVersion without |version
        var moduleDefinition = createModuleDefinition();
        var ra = moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://hl7.org/fhir/ValueSet/observation-codes");
        ra.addExtension(new Extension()
                .setUrl(Constants.CQF_RESOURCETYPE)
                .setValue(new CodeType("ValueSet")));

        var result = runInferManifestParameters(moduleDefinition);

        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());

        var param = parameters.getParameter().get(0);
        assertEquals("canonicalVersion", param.getName());
        assertEquals("http://hl7.org/fhir/ValueSet/observation-codes", param.getValue().primitiveValue());

        var ext = param.getExtensionByUrl(Constants.CQF_RESOURCETYPE);
        assertNotNull(ext, "canonicalVersion parameter should have cqf-resourceType extension");
        assertEquals("ValueSet", ((CodeType) ext.getValue()).getCode());
    }

    @Test
    void inferManifestParameters_MultipleDependencies_CreatesMultipleParameters() {
        var moduleDefinition = createModuleDefinition();

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
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/Measure/quality-measure|1.0.0");

        var result = runInferManifestParameters(moduleDefinition);

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

        // Verify canonicalVersion with cqf-resourceType extension for Library
        var libraryParam = parameters.getParameter().stream()
                .filter(p -> "canonicalVersion".equals(p.getName())
                        && p.getValue().primitiveValue().contains("Library"))
                .findFirst();
        assertTrue(libraryParam.isPresent());
        var libraryExt = libraryParam.get().getExtensionByUrl(Constants.CQF_RESOURCETYPE);
        assertNotNull(libraryExt);
        assertEquals("Library", ((CodeType) libraryExt.getValue()).getCode());

        // Verify canonicalVersion with cqf-resourceType extension for Measure
        var measureParam = parameters.getParameter().stream()
                .filter(p -> "canonicalVersion".equals(p.getName())
                        && p.getValue().primitiveValue().contains("Measure"))
                .findFirst();
        assertTrue(measureParam.isPresent());
        var measureExt = measureParam.get().getExtensionByUrl(Constants.CQF_RESOURCETYPE);
        assertNotNull(measureExt);
        assertEquals("Measure", ((CodeType) measureExt.getValue()).getCode());
    }

    @Test
    void inferManifestParameters_EmptyRelatedArtifacts_CreatesManifestWithoutParameters() {
        var moduleDefinition = createModuleDefinition();

        var result = runInferManifestParameters(moduleDefinition);

        assertEquals("asset-collection", result.getType().getCodingFirstRep().getCode());
        assertTrue(result.getContained().isEmpty());
    }

    @Test
    void inferManifestParameters_PreservesMetadata() {
        var moduleDefinition = createModuleDefinition();
        moduleDefinition.setName("MyModuleDef");

        var result = runInferManifestParameters(moduleDefinition);

        assertEquals("http://example.org/Library/module-def", result.getUrl());
        assertEquals("1.0.0", result.getVersion());
        assertEquals("MyModuleDefManifest", result.getName());
        assertEquals(Enumerations.PublicationStatus.ACTIVE, result.getStatus());
    }

    @Test
    void inferManifestParameters_ValueSetWithDisplay_CreatesExtensions() {
        var moduleDefinition = createModuleDefinition();
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://hl7.org/fhir/ValueSet/administrative-gender|4.0.1")
                .setDisplay("Administrative Gender");

        var result = runInferManifestParameters(moduleDefinition);

        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        var param = parameters.getParameter().get(0);
        assertEquals("canonicalVersion", param.getName());
        assertEquals(
                "http://hl7.org/fhir/ValueSet/administrative-gender|4.0.1",
                param.getValue().primitiveValue());

        var cqfResourceTypeExt = param.getExtensionByUrl(Constants.CQF_RESOURCETYPE);
        assertNotNull(cqfResourceTypeExt, "cqf-resourceType extension should be present");
        assertEquals("ValueSet", ((CodeType) cqfResourceTypeExt.getValue()).getCode());

        var displayExt = param.getExtensionByUrl(Constants.DISPLAY_EXTENSION);
        assertNotNull(displayExt, "display extension should be present");
        assertEquals("Administrative Gender", displayExt.getValue().primitiveValue());
    }

    @Test
    void inferManifestParameters_RelatedArtifactWithoutResource_SkipsParameter() {
        var moduleDefinition = createModuleDefinition();

        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setDisplay("Some documentation");
        moduleDefinition
                .addRelatedArtifact()
                .setType(RelatedArtifact.RelatedArtifactType.DEPENDSON)
                .setResource("http://example.org/CodeSystem/valid|1.0.0");

        var result = runInferManifestParameters(moduleDefinition);

        var parameters = (Parameters) result.getContained().get(0);
        assertEquals(1, parameters.getParameter().size());
        assertEquals(
                "http://example.org/CodeSystem/valid|1.0.0",
                parameters.getParameter().get(0).getValue().primitiveValue());
    }
}
