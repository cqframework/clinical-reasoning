package org.opencds.cqf.fhir.utility.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;

class BuilderTest {

    // -- CodingSettings --

    @Test
    void codingSettingsWithDisplay() {
        var cs = new CodingSettings("sys", "code", "display");
        assertEquals("sys", cs.getSystem());
        assertEquals("code", cs.getCode());
        assertEquals("display", cs.getDisplay());
    }

    @Test
    void codingSettingsWithoutDisplay() {
        var cs = new CodingSettings("sys", "code");
        assertEquals("sys", cs.getSystem());
        assertEquals("code", cs.getCode());
    }

    // -- NarrativeSettings --

    @Test
    void narrativeSettingsDefaults() {
        var ns = new NarrativeSettings("<div>text</div>");
        assertEquals("<div>text</div>", ns.getText());
        assertEquals("generated", ns.getStatus());
    }

    @Test
    void narrativeSettingsCustomStatus() {
        var ns = new NarrativeSettings("<div>text</div>", "additional");
        assertEquals("additional", ns.getStatus());
    }

    // -- CodeableConceptSettings --

    @Test
    void codeableConceptSettings() {
        var ccs = new CodeableConceptSettings();
        ccs.add("sys1", "code1").add("sys2", "code2", "display2");
        assertEquals(2, ccs.getCodingSettings().size());
        assertEquals(2, ccs.getCodingSettingsArray().length);
    }

    // -- BaseResourceBuilder --

    @Test
    void ensurePatientReference() {
        assertEquals("Patient/123", BaseResourceBuilder.ensurePatientReference("123"));
        assertEquals("Patient/123", BaseResourceBuilder.ensurePatientReference("Patient/123"));
        assertNull(BaseResourceBuilder.ensurePatientReference(null));
        assertEquals("", BaseResourceBuilder.ensurePatientReference(""));
    }

    @Test
    void ensureOrganizationReference() {
        assertEquals("Organization/123", BaseResourceBuilder.ensureOrganizationReference("123"));
        assertEquals("Organization/123", BaseResourceBuilder.ensureOrganizationReference("Organization/123"));
    }

    // -- BundleBuilder (R4) --

    @Test
    void bundleBuilderR4() {
        var bundle = new BundleBuilder<>(org.hl7.fhir.r4.model.Bundle.class, "test-id", "COLLECTION")
                .withTimestamp(new Date())
                .build();
        assertNotNull(bundle);
        assertEquals("test-id", bundle.getIdElement().getIdPart());
        assertEquals(org.hl7.fhir.r4.model.Bundle.BundleType.COLLECTION, bundle.getType());
    }

    @Test
    void bundleBuilderDstu3() {
        var bundle = new BundleBuilder<>(org.hl7.fhir.dstu3.model.Bundle.class, "test-id")
                .withType("COLLECTION")
                .build();
        assertNotNull(bundle);
        assertEquals(org.hl7.fhir.dstu3.model.Bundle.BundleType.COLLECTION, bundle.getType());
    }

    @Test
    void bundleBuilderR5() {
        var bundle = new BundleBuilder<>(org.hl7.fhir.r5.model.Bundle.class)
                .withType("COLLECTION")
                .withId("test-r5")
                .build();
        assertNotNull(bundle);
    }

    @Test
    void bundleBuilderNoTypeThrows() {
        var builder = new BundleBuilder<>(org.hl7.fhir.r4.model.Bundle.class);
        assertThrows(NullPointerException.class, builder::build);
    }

    // -- BundleBuilder with profile and identifier --

    @Test
    void bundleBuilderWithProfileAndIdentifier() {
        var bundle = new BundleBuilder<>(org.hl7.fhir.r4.model.Bundle.class, "test-id")
                .withType("COLLECTION")
                .withProfile("http://example.org/profile")
                .withIdentifier(new ImmutablePair<>("sys", "val"))
                .build();
        assertNotNull(bundle);
        assertTrue(bundle.getMeta().getProfile().stream()
                .anyMatch(p -> p.getValue().equals("http://example.org/profile")));
    }

    // -- DetectedIssueBuilder --

    @Test
    void detectedIssueBuilderR4() {
        var code = new CodeableConceptSettings().add("sys", "code", "display");
        var issue = new DetectedIssueBuilder<>(org.hl7.fhir.r4.model.DetectedIssue.class, "test-id", "FINAL", "detail")
                .withCode(code)
                .withPatient("Patient/123")
                .withEvidenceDetail("extra-detail")
                .build();
        assertNotNull(issue);
        assertEquals(org.hl7.fhir.r4.model.DetectedIssue.DetectedIssueStatus.FINAL, issue.getStatus());
    }

    @Test
    void detectedIssueBuilderDstu3() {
        var code = new CodeableConceptSettings().add("sys", "code", "display");
        var issue = new DetectedIssueBuilder<>(org.hl7.fhir.dstu3.model.DetectedIssue.class)
                .withStatus("FINAL")
                .withCode(code)
                .withPatient("123")
                .withEvidenceDetail("detail")
                .build();
        assertNotNull(issue);
    }

    @Test
    void detectedIssueBuilderR5() {
        var code = new CodeableConceptSettings().add("sys", "code", "display");
        var issue = new DetectedIssueBuilder<>(org.hl7.fhir.r5.model.DetectedIssue.class)
                .withStatus("FINAL")
                .withCode(code)
                .withEvidenceDetail("detail")
                .build();
        assertNotNull(issue);
    }

    // -- CompositionBuilder --

    @Test
    void compositionBuilderR4() {
        var type = new CodeableConceptSettings().add("sys", "code", "display");
        var comp = new CompositionBuilder<>(
                        org.hl7.fhir.r4.model.Composition.class, "test-id", type, "FINAL", "Practitioner/1", "Title")
                .withDate(new Date())
                .withSubject("Patient/123")
                .withCustodian("Organization/456")
                .build();
        assertNotNull(comp);
        assertEquals("Title", comp.getTitle());
    }

    @Test
    void compositionBuilderDstu3() {
        var type = new CodeableConceptSettings().add("sys", "code", "display");
        var comp = new CompositionBuilder<>(org.hl7.fhir.dstu3.model.Composition.class)
                .withType(type)
                .withStatus("FINAL")
                .withAuthor("Practitioner/1")
                .withTitle("Title")
                .withSubject("123")
                .withCustodian("456")
                .build();
        assertNotNull(comp);
    }

    @Test
    void compositionBuilderR5() {
        var type = new CodeableConceptSettings().add("sys", "code", "display");
        var comp = new CompositionBuilder<>(org.hl7.fhir.r5.model.Composition.class)
                .withType(type)
                .withStatus("FINAL")
                .withAuthor("Device/1")
                .withTitle("Title")
                .build();
        assertNotNull(comp);
    }

    // -- CompositionSectionComponentBuilder --

    @Test
    void compositionSectionBuilderR4() {
        var section = new CompositionSectionComponentBuilder<>(
                        org.hl7.fhir.r4.model.Composition.SectionComponent.class,
                        "sec-id",
                        "MeasureReport/1",
                        "entry-1")
                .withTitle("Section Title")
                .withText(new NarrativeSettings("<div>text</div>"))
                .withEntry("entry-2")
                .build();
        assertNotNull(section);
        assertEquals("Section Title", section.getTitle());
    }

    @Test
    void compositionSectionBuilderDstu3() {
        var section = new CompositionSectionComponentBuilder<>(
                        org.hl7.fhir.dstu3.model.Composition.SectionComponent.class)
                .withFocus("MeasureReport/1")
                .withEntry("entry-1")
                .withTitle("Title")
                .withText(new NarrativeSettings("<div>hi</div>"))
                .build();
        assertNotNull(section);
    }

    @Test
    void compositionSectionBuilderR5() {
        var section = new CompositionSectionComponentBuilder<>(org.hl7.fhir.r5.model.Composition.SectionComponent.class)
                .withFocus("MeasureReport/1")
                .withEntry("entry-1")
                .withTitle("Title")
                .withText(new NarrativeSettings("<div>hi</div>"))
                .build();
        assertNotNull(section);
    }

    // -- Extensions on builders --

    @Test
    void domainResourceBuilderWithExtension() {
        var extCc = new CodeableConceptSettings().add("ext-sys", "ext-code", "ext-display");
        var code = new CodeableConceptSettings().add("sys", "code", "display");
        var issue = new DetectedIssueBuilder<>(org.hl7.fhir.r4.model.DetectedIssue.class)
                .withStatus("FINAL")
                .withCode(code)
                .withEvidenceDetail("detail")
                .withExtension(new ImmutablePair<>("http://example.org/ext", extCc))
                .withModifierExtension(new ImmutablePair<>("http://example.org/mod-ext", extCc))
                .build();
        assertNotNull(issue);
        assertTrue(issue.hasExtension());
        assertTrue(issue.hasModifierExtension());
    }

    @Test
    void domainResourceBuilderWithExtensionDstu3() {
        var extCc = new CodeableConceptSettings().add("ext-sys", "ext-code", "ext-display");
        var code = new CodeableConceptSettings().add("sys", "code", "display");
        var issue = new DetectedIssueBuilder<>(org.hl7.fhir.dstu3.model.DetectedIssue.class)
                .withStatus("FINAL")
                .withCode(code)
                .withEvidenceDetail("detail")
                .withExtension(new ImmutablePair<>("http://example.org/ext", extCc))
                .withModifierExtension(new ImmutablePair<>("http://example.org/mod-ext", extCc))
                .build();
        assertNotNull(issue);
    }

    @Test
    void domainResourceBuilderWithExtensionR5() {
        var extCc = new CodeableConceptSettings().add("ext-sys", "ext-code", "ext-display");
        var code = new CodeableConceptSettings().add("sys", "code", "display");
        var issue = new DetectedIssueBuilder<>(org.hl7.fhir.r5.model.DetectedIssue.class)
                .withStatus("FINAL")
                .withCode(code)
                .withEvidenceDetail("detail")
                .withExtension(new ImmutablePair<>("http://example.org/ext", extCc))
                .withModifierExtension(new ImmutablePair<>("http://example.org/mod-ext", extCc))
                .build();
        assertNotNull(issue);
    }

    @Test
    void backboneElementBuilderWithExtension() {
        var extCc = new CodeableConceptSettings().add("ext-sys", "ext-code", "ext-display");
        var section = new CompositionSectionComponentBuilder<>(org.hl7.fhir.r4.model.Composition.SectionComponent.class)
                .withFocus("MeasureReport/1")
                .withEntry("entry-1")
                .withExtension(new ImmutablePair<>("http://example.org/ext", extCc))
                .withModifierExtension(new ImmutablePair<>("http://example.org/mod-ext", extCc))
                .build();
        assertNotNull(section);
    }

    @Test
    void backboneElementBuilderWithExtensionDstu3() {
        var extCc = new CodeableConceptSettings().add("ext-sys", "ext-code", "ext-display");
        var section = new CompositionSectionComponentBuilder<>(
                        org.hl7.fhir.dstu3.model.Composition.SectionComponent.class)
                .withFocus("MeasureReport/1")
                .withEntry("entry-1")
                .withExtension(new ImmutablePair<>("http://example.org/ext", extCc))
                .withModifierExtension(new ImmutablePair<>("http://example.org/mod-ext", extCc))
                .build();
        assertNotNull(section);
    }

    @Test
    void backboneElementBuilderWithExtensionR5() {
        var extCc = new CodeableConceptSettings().add("ext-sys", "ext-code", "ext-display");
        var section = new CompositionSectionComponentBuilder<>(org.hl7.fhir.r5.model.Composition.SectionComponent.class)
                .withFocus("MeasureReport/1")
                .withEntry("entry-1")
                .withExtension(new ImmutablePair<>("http://example.org/ext", extCc))
                .withModifierExtension(new ImmutablePair<>("http://example.org/mod-ext", extCc))
                .build();
        assertNotNull(section);
    }
}
