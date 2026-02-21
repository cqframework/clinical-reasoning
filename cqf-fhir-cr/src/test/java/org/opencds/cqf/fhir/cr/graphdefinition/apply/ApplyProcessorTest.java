package org.opencds.cqf.fhir.cr.graphdefinition.apply;

import static org.hl7.fhir.r4.model.GraphDefinition.GraphDefinitionLinkComponent;
import static org.hl7.fhir.r4.model.GraphDefinition.GraphDefinitionLinkTargetComponent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
import static org.opencds.cqf.fhir.utility.Constants.CPG_RELATED_SUMMARY_DEFINITION;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.nio.file.Path;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CanonicalType;
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

@SuppressWarnings("UnstableApiUsage")
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
        fixture = new ApplyProcessor(repository, modelResolver);
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
    void testFindPractitionerRolesFromPractitioner() {
        IdType practitionerId = new IdType("Practitioner", "ordering-md-1");
        List<IBaseResource> practitionerRoles = fixture.findPractitionerRoles(practitionerId);

        assertEquals(1, practitionerRoles.size());
    }

    @Test
    void testTransformExtensionToReference() {
        final String url = "someUrl";
        final String value = "someValue";
        final Extension originalExtension = new Extension(url, new StringType(value));

        final Reference reference = fixture.transformExtensionToReference(originalExtension);

        Extension wrappedExtension = reference.getExtensionFirstRep();

        assertNotSame(wrappedExtension, originalExtension);
        assertEquals(url, wrappedExtension.getUrl());
        assertEquals(value, wrappedExtension.getValue().toString());
    }

    @Test
    void testTransformBackBoneElementsToSections_transformsValidLinks() {
        GraphDefinitionLinkTargetComponent target = new GraphDefinitionLinkTargetComponent();
        target.setType("GraphDefinition")
                .addExtension(new Extension(CPG_RELATED_SUMMARY_DEFINITION, new CanonicalType("value1")));

        GraphDefinitionLinkComponent link =
                new GraphDefinitionLinkComponent().setDescription("Valid Link").addTarget(target);

        printElement(link);

        var applyRequest = Mockito.mock(ApplyRequest.class);
        List<SectionComponent> sections = fixture.transformBackBoneElementsToSections(applyRequest, List.of(link));

        assertNotNull(sections);
        assertEquals(1, sections.size());

        SectionComponent parentSection = sections.get(0);

        assertEquals("Valid Link", parentSection.getTitle());

        assertEquals(1, parentSection.getSection().size());

        SectionComponent childSection = parentSection.getSection().get(0);

        assertEquals(
                CPG_RELATED_SUMMARY_DEFINITION,
                childSection.getEntryFirstRep().getExtensionFirstRep().getUrl());
    }

    @Test
    void testTransformLinkToSection_returnsNullWithNoTarget() {
        var linkComponent = new GraphDefinitionLinkComponent().setDescription("Test Link");

        var applyRequest = Mockito.mock(ApplyRequest.class);
        SectionComponent section = fixture.transformLinkToSection(applyRequest, linkComponent);

        assertNull(section);
    }

    @Test
    void testTransformLinkToSection_transformsWithValidTargets() {
        GraphDefinitionLinkTargetComponent target1 = new GraphDefinitionLinkTargetComponent();
        target1.setType("GraphDefinition")
                .addExtension(new Extension("http://example.org/ext1", new StringType("value1")));

        GraphDefinitionLinkTargetComponent target2 = new GraphDefinitionLinkTargetComponent();
        target2.setType("GraphDefinition")
                .addExtension(new Extension("http://example.org/ext2", new StringType("value2")));

        var linkComponent = new GraphDefinitionLinkComponent()
                .setDescription("Test Link")
                .addTarget(target1)
                .addTarget(target2);

        printElement(linkComponent);

        var applyRequest = Mockito.mock(ApplyRequest.class);
        SectionComponent section = fixture.transformLinkToSection(applyRequest, linkComponent);

        assertNotNull(section);
        assertEquals("Test Link", section.getTitle());
        assertEquals(2, section.getSection().size());
    }

    @Test
    void testTransformLinkToSection_skipsInvalidTargets() {
        GraphDefinitionLinkTargetComponent invalidTarget1 = new GraphDefinitionLinkTargetComponent();
        invalidTarget1
                .setType("Patient")
                .addExtension(new Extension("http://example.org/ext1", new StringType("value1")));

        GraphDefinitionLinkTargetComponent invalidTarget2 = new GraphDefinitionLinkTargetComponent();
        invalidTarget2.addExtension(new Extension("http://example.org/ext1", new StringType("value1")));

        GraphDefinitionLinkTargetComponent invalidTarget3 = new GraphDefinitionLinkTargetComponent(); // No extensions

        var linkComponent = new GraphDefinitionLinkComponent()
                .addTarget(invalidTarget1)
                .addTarget(invalidTarget2)
                .addTarget(invalidTarget3);

        var applyRequest = Mockito.mock(ApplyRequest.class);
        SectionComponent section = fixture.transformLinkToSection(applyRequest, linkComponent);

        assertNotNull(section);
        assertFalse(section.hasSection());
    }

    @Test
    void testTransformTargetToSection_throwsWithProfileAndExtension() {
        var extension = new Extension(CPG_RELATED_SUMMARY_DEFINITION, new CanonicalType("value"));
        var target = new GraphDefinitionLinkTargetComponent();
        target.setType("GraphDefinition");
        target.addExtension(extension);
        target.setProfile("test");

        var applyRequest = Mockito.mock(ApplyRequest.class);
        assertThrows(UnprocessableEntityException.class, () -> fixture.transformTargetToSection(applyRequest, target));
    }

    @Test
    void testTransformTargetToSection_transformsExtensionsToReferences() {
        var extension1 = new Extension(CPG_RELATED_SUMMARY_DEFINITION, new CanonicalType("value1"));
        var extension2 = new Extension(CPG_RELATED_SUMMARY_DEFINITION, new CanonicalType("value2"));

        var target = new GraphDefinitionLinkTargetComponent();
        target.setType("GraphDefinition");
        target.addExtension(extension1);
        target.addExtension(extension2);

        var applyRequest = Mockito.mock(ApplyRequest.class);
        SectionComponent section = fixture.transformTargetToSection(applyRequest, target);

        assertNotNull(section);
        assertEquals(2, section.getEntry().size());

        Reference ref1 = section.getEntry().get(0);
        Reference ref2 = section.getEntry().get(1);

        assertEquals(CPG_RELATED_SUMMARY_DEFINITION, ref1.getExtensionFirstRep().getUrl());
        assertEquals("value1", ((CanonicalType) ref1.getExtensionFirstRep().getValue()).getValue());

        assertEquals(CPG_RELATED_SUMMARY_DEFINITION, ref2.getExtensionFirstRep().getUrl());
        assertEquals("value2", ((CanonicalType) ref2.getExtensionFirstRep().getValue()).getValue());
    }

    private FhirContext getFhirContext() {
        return fhirContextR4;
    }

    private FhirVersionEnum getFhirVersion() {
        return getFhirContext().getVersion().getVersion();
    }

    private Path getClassPath() {
        String pathString =
                String.format("%s/%s/%s/eras", getResourcePath(this.getClass()), CLASS_PATH, getVersionPath());
        return Path.of(pathString);
    }

    private String getVersionPath() {
        return switch (getFhirVersion()) {
            case R4 -> "r4";
            default ->
                throw new IllegalArgumentException(
                        "Unsupported FHIR version: " + getFhirVersion().getFhirVersionString());
        };
    }

    private void printElement(IBase element) {
        String elementAsString = jsonParser.encodeToString(element);
        ourLog.info(elementAsString);
    }
}
