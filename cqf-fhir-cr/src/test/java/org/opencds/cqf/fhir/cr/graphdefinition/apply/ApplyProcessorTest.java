package org.opencds.cqf.fhir.cr.graphdefinition.apply;

import static org.hl7.fhir.r4.model.GraphDefinition.GraphDefinitionLinkComponent;
import static org.hl7.fhir.r4.model.GraphDefinition.GraphDefinitionLinkTargetComponent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.nio.file.Path;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class ApplyProcessorTest {
    private final Logger ourLog = LoggerFactory.getLogger(ApplyProcessorTest.class);

    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/shared";

    @Mock
    private ModelResolver modelResolver;

    private IRepository repository;
    private ApplyProcessor fixture;
    private FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private IParser jsonParser = getFhirContext().newJsonParser().setPrettyPrint(true);

    @BeforeEach
    void beforeEach() {
        repository = new IgRepository(getFhirContext(), getClassPath());
        fixture = new ApplyProcessor(repository, modelResolver, getFhirVersion());
    }

    @Test
    void testApply_whenInvokedWithInvalidFhirVersion_throwsUnsupportedOperationException() {
        ApplyRequest applyRequest = Mockito.mock(ApplyRequest.class);
        when(applyRequest.getFhirVersion()).thenReturn(FhirVersionEnum.R5);
        assertThrows(InvalidRequestException.class, () -> fixture.apply(applyRequest));
    }

    @Test
    void testApply_returnsDocumentBundleWithTimeStamp() {
        String id = "eras-postop";
        IdType graphDefinitionId = new IdType("GraphDefinition", id);

        ApplyRequestBuilder applyRequestBuilder = new ApplyRequestBuilder(repository, EvaluationSettings.getDefault());
        applyRequestBuilder.withSubject("Patient/time-zero");
        applyRequestBuilder.withPractitioner("Practitioner/ordering-md-1");
        applyRequestBuilder.withGraphDefinitionId(graphDefinitionId);

        ApplyRequest applyRequest = applyRequestBuilder.buildApplyRequest();

        Bundle bundle = (Bundle) fixture.apply(applyRequest);

        assertNotNull(bundle.getTimestamp());
        assertEquals(BundleType.DOCUMENT, bundle.getType());
    }

    @Test
    void testApply_firstEntryIsComposition() {
        String id = "eras-postop";
        IdType graphDefinitionId = new IdType("GraphDefinition", id);

        ApplyRequestBuilder applyRequestBuilder = new ApplyRequestBuilder(repository, EvaluationSettings.getDefault());
        applyRequestBuilder.withSubject("Patient/time-zero");
        applyRequestBuilder.withPractitioner("Practitioner/ordering-md-1");
        applyRequestBuilder.withGraphDefinitionId(graphDefinitionId);

        ApplyRequest applyRequest = applyRequestBuilder.buildApplyRequest();

        Bundle bundle = (Bundle) fixture.apply(applyRequest);

        // entry[0].type = Composition
        // entry[1].type = Patient
        // entry[2].type = Practitioner
        // entry[3].type = PractitionerRole
        assertEquals(4, bundle.getEntry().size());

        BundleEntryComponent entryFirstRep = bundle.getEntryFirstRep();
        assertTrue(entryFirstRep.getFullUrl().endsWith(id));

        Resource firstEntry = entryFirstRep.getResource();
        assertInstanceOf(Composition.class, firstEntry);

        printElement(bundle);
    }

    @Test
    void testFindPractitionerRolesFromPractitioner(){
        IdType practitionerId = new IdType("Practitioner", "ordering-md-1");
        List<IBaseResource> practitionerRoles = fixture.findPractitionerRoles(practitionerId);

        assertEquals(1, practitionerRoles.size());
    }

    @Test
    void testTransformExtensionToReference(){
        final String url = "someUrl";
        final String value = "someValue";
        final Extension originalExtension = new Extension(url, new StringType(value));

        final Reference reference = fixture.transformExtensionToReference(originalExtension);

        Extension wrappedExtension = reference.getExtensionFirstRep();

        assertTrue(wrappedExtension != originalExtension);
        assertEquals(url, wrappedExtension.getUrl());
        assertEquals(value, wrappedExtension.getValue().toString());
    }

    @Test
    void testTransformBackBoneElementsToSections_transformsValidLinks() {
        GraphDefinitionLinkTargetComponent target = new GraphDefinitionLinkTargetComponent();
        target.setType("GraphDefinition").addExtension(new Extension("http://example.org/ext1", new StringType("value1")));

        GraphDefinitionLinkComponent link = new GraphDefinitionLinkComponent()
            .setDescription("Valid Link")
            .addTarget(target);

        printElement(link);

        List<SectionComponent> sections = fixture.transformBackBoneElementsToSections(List.of(link));

        assertNotNull(sections);
        assertEquals(1, sections.size());

        SectionComponent parentSection = sections.get(0);

        assertEquals("Valid Link", parentSection.getTitle());

        assertEquals(1, parentSection.getSection().size());

        SectionComponent childSection = parentSection.getSection().get(0);

        assertEquals("http://example.org/ext1", childSection.getEntryFirstRep().getExtensionFirstRep().getUrl());
    }

    @Test
    void testTransformLinkToSection_transformsWithValidTargets() {
        GraphDefinitionLinkTargetComponent target1 = new GraphDefinitionLinkTargetComponent();
        target1.setType("GraphDefinition").addExtension(new Extension("http://example.org/ext1", new StringType("value1")));

        GraphDefinitionLinkTargetComponent target2 = new GraphDefinitionLinkTargetComponent();
        target2.setType("GraphDefinition").addExtension(new Extension("http://example.org/ext2", new StringType("value2")));

        var linkComponent = new GraphDefinitionLinkComponent()
                .setDescription("Test Link")
                .addTarget(target1)
                .addTarget(target2);

        printElement(linkComponent);

        SectionComponent section = fixture.transformLinkToSection(linkComponent);

        assertNotNull(section);
        assertEquals("Test Link", section.getTitle());
        assertEquals(2, section.getSection().size());
    }

    @Test
    void testTransformLinkToSection_skipsInvalidTargets() {
        GraphDefinitionLinkTargetComponent invalidTarget1 = new GraphDefinitionLinkTargetComponent();
        invalidTarget1.setType("Patient").addExtension(new Extension("http://example.org/ext1", new StringType("value1")));

        GraphDefinitionLinkTargetComponent invalidTarget2 = new GraphDefinitionLinkTargetComponent();
        invalidTarget2.addExtension(new Extension("http://example.org/ext1", new StringType("value1")));

        GraphDefinitionLinkTargetComponent invalidTarget3 = new GraphDefinitionLinkTargetComponent(); // No extensions

        var linkComponent = new GraphDefinitionLinkComponent()
            .addTarget(invalidTarget1)
            .addTarget(invalidTarget2)
            .addTarget(invalidTarget3);

        SectionComponent section = fixture.transformLinkToSection(linkComponent);

        assertNotNull(section);
        assertFalse(section.hasSection());
    }

    @Test
    void testTransformTargetToSection_transformsExtensionsToReferences() {
        var extension1 = new Extension("http://example.org/ext1", new StringType("value1"));
        var extension2 = new Extension("http://example.org/ext2", new StringType("value2"));

        var target = new GraphDefinitionLinkTargetComponent();
        target.addExtension(extension1);
        target.addExtension(extension2);

        SectionComponent section = fixture.transformTargetToSection(target);

        assertNotNull(section);
        assertEquals(2, section.getEntry().size());

        Reference ref1 = section.getEntry().get(0);
        Reference ref2 = section.getEntry().get(1);

        assertEquals("http://example.org/ext1", ref1.getExtensionFirstRep().getUrl());
        assertEquals("value1", ((StringType) ref1.getExtensionFirstRep().getValue()).getValue());

        assertEquals("http://example.org/ext2", ref2.getExtensionFirstRep().getUrl());
        assertEquals("value2", ((StringType) ref2.getExtensionFirstRep().getValue()).getValue());
    }

    private FhirContext getFhirContext() {
        return fhirContextR4;
    }

    private FhirVersionEnum getFhirVersion() {
        return getFhirContext().getVersion().getVersion();
    }

    private Path getClassPath() {
        String pathString = String.format("%s/%s/%s/eras",getResourcePath(this.getClass()), CLASS_PATH, getVersionPath());
        return Path.of(pathString);
    }

    private String getVersionPath(){
        switch (getFhirVersion()) {
            case R4:
                return "r4";
            default:
                throw new IllegalArgumentException(
                    "Unsupported FHIR version: " + getFhirVersion().getFhirVersionString());
        }
    }

    private void printElement(IBase element){
        String elementAsString = jsonParser.encodeToString(element);
        ourLog.info(elementAsString);
    }
}
