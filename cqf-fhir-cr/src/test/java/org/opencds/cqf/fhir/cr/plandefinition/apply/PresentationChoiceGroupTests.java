package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.questionnaire.generate.ItemGenerator;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

class PresentationChoiceGroupTests {
    @Test void refusesMalformedAnnotatedInputsBeforeGeneration() {
        var input = new DataRequirement(); input.addExtension(descriptor());
        var adapter = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4).createDataRequirement(input);
        assertThrows(IllegalArgumentException.class, () -> PresentationChoiceGroupProcessor.process(null, adapter, null, null));
        input.addProfile("https://example.org/a"); input.addExtension(descriptor());
        assertThrows(IllegalArgumentException.class, () -> PresentationChoiceGroupProcessor.process(null, adapter, null, null));
        var e = descriptor(); e.getExtensionByUrl("protocol").setValue(new StringType("1"));
        assertThrows(IllegalArgumentException.class, () -> PresentationChoiceGroupProcessor.collect(new Questionnaire(), e, p -> null));
    }
    private Extension descriptor() {
        var e = new Extension(PresentationChoiceGroupProcessor.URL);
        e.addExtension("protocol", new IntegerType(1));
        e.addExtension("id", new StringType("[\"https://example.org/Library/Owner\",\"1\",\"Symptoms\"]"));
        e.addExtension("name", new StringType("Symptoms"));
        e.addExtension("text", new StringType("Which symptoms apply?"));
        e.addExtension("columns", new IntegerType(2));
        e.addExtension("control", new CodeType("checkbox"));
        for (var profile : List.of("https://example.org/a", "https://example.org/b")) {
            var member = new Extension("member");
            member.addExtension("profile", new CanonicalType(profile));
            member.addExtension("text", new StringType(profile));
            e.addExtension(member);
        }
        return e;
    }
    private QuestionnaireItemComponent item(Questionnaire q, String profile) {
        var adapter = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4).createQuestionnaire(q);
        var id = ItemGenerator.nextRootLinkId(adapter.getItem());
        var item = new QuestionnaireItemComponent().setLinkId(id).setType(QuestionnaireItemType.BOOLEAN)
                .setDefinition(profile + "#Observation.value[x]");
        item.addExtension("http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-definitionExtract", new CanonicalType(profile));
        return item;
    }
    @Test void gathersOnceAndPreservesIndependentOriginalItems() {
        var q = new Questionnaire();
        var made = new ArrayList<QuestionnaireItemComponent>();
        PresentationChoiceGroupProcessor.collect(q, descriptor(), profile -> {
            var m = item(q, profile);
            m.addInitial().setValue(new BooleanType(made.isEmpty()));
            made.add(m);
            return m;
        });
        var wrapper = q.getItemFirstRep();
        assertEquals(QuestionnaireItemType.GROUP, wrapper.getType());
        assertFalse(wrapper.hasDefinition());
        assertFalse(wrapper.getRepeats());
        assertEquals(made, wrapper.getItem());
        assertEquals(List.of("1", "2"), made.stream().map(QuestionnaireItemComponent::getLinkId).toList());
        assertTrue(((BooleanType) made.get(0).getInitialFirstRep().getValue()).booleanValue());
        assertFalse(((BooleanType) made.get(1).getInitialFirstRep().getValue()).booleanValue());
        PresentationChoiceGroupProcessor.collect(q, descriptor().copy(), profile -> { fail("Must reuse original items"); return null; });
        assertEquals(1, q.getItem().size());
        assertEquals("3", item(q, "https://example.org/ordinary").getLinkId());
        assertEquals(2, IAdapterFactory.forFhirVersion(FhirVersionEnum.R4).createQuestionnaire(q).getAllItemDefinitions().size());
    }
    @Test void movesOnlyTopLevelOriginalRootsAndPreservesReferences() {
        var q = new Questionnaire();
        var old = item(q, "https://example.org/a");
        old.setLinkId("2");
        old.addEnableWhen().setQuestion("gate").setOperator(Questionnaire.QuestionnaireItemOperator.EQUAL).setAnswer(new BooleanType(true));
        q.addItem(old);
        PresentationChoiceGroupProcessor.collect(q, descriptor(), profile -> item(q, profile));
        assertSame(old, q.getItemFirstRep().getItemFirstRep());
        assertEquals("gate", old.getEnableWhenFirstRep().getQuestion());
        assertEquals("2", old.getLinkId());
    }
    @Test void refusesLossOfSuppliedConditionalParent() {
        var q = new Questionnaire();
        var parent = q.addItem().setLinkId("parent").setType(QuestionnaireItemType.GROUP);
        parent.addEnableWhen().setQuestion("gate").setOperator(Questionnaire.QuestionnaireItemOperator.EQUAL).setAnswer(new BooleanType(true));
        var old = item(q, "https://example.org/a"); parent.addItem(old);
        assertThrows(IllegalArgumentException.class, () -> PresentationChoiceGroupProcessor.collect(q, descriptor(), p -> item(q, p)));
        assertSame(old, parent.getItemFirstRep());
    }
    @Test void rejectsConflictingMetadataAndProfileOwnership() {
        var q = new Questionnaire();
        PresentationChoiceGroupProcessor.collect(q, descriptor(), p -> item(q, p));
        var conflicting = descriptor(); conflicting.getExtensionByUrl("text").setValue(new StringType("Different"));
        assertThrows(IllegalArgumentException.class, () -> PresentationChoiceGroupProcessor.collect(q, conflicting, p -> item(q, p)));
        var other = descriptor(); other.getExtensionByUrl("id").setValue(new StringType("other-owner"));
        assertThrows(IllegalArgumentException.class, () -> PresentationChoiceGroupProcessor.collect(q, other, p -> item(q, p)));
    }
    @Test void rejectsMissingAndDuplicateProfiles() {
        var q = new Questionnaire();
        assertThrows(IllegalArgumentException.class, () -> PresentationChoiceGroupProcessor.collect(q, descriptor(), p -> null));
        var e = descriptor(); e.addExtension(e.getExtensionsByUrl("member").get(0).copy());
        assertThrows(IllegalArgumentException.class, () -> PresentationChoiceGroupProcessor.collect(q, e, p -> item(q, p)));
    }
    @Test void allocatorChecksDescendantPrefixes() {
        var q = new Questionnaire();
        var root = q.addItem().setLinkId("wrapper").setType(QuestionnaireItemType.GROUP);
        root.addItem().setLinkId("2.1").setType(QuestionnaireItemType.BOOLEAN);
        assertEquals("3", item(q, "https://example.org/ordinary").getLinkId());
    }
}
