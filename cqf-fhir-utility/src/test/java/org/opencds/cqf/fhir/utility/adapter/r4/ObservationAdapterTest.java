package org.opencds.cqf.fhir.utility.adapter.r4;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

class ObservationAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var library = new Library();
        assertThrows(IllegalArgumentException.class, () -> new ObservationAdapter(library));
    }

    @Test
    void adapter_get_and_set() {
        var obs = new Observation();
        obs.setStatus(ObservationStatus.FINAL);
        var adapter = (ObservationAdapter) adapterFactory.createResource(obs);
        assertEquals("final", adapter.getStatus());
        adapter.setStatus("amended");
        assertEquals("amended", adapter.getStatus());
        assertNull(adapter.getCode());
        adapter.setCode(new CodeableConcept().addCoding(new Coding("test.com", "test", "test")));
        assertInstanceOf(CodeableConcept.class, adapter.get().getCode());
        assertEquals("test", adapter.getCode().getCodingFirstRep().getCode());
        assertEquals("test.com", adapter.getCode().getCodingFirstRep().getSystem());
        var basedOn = new Reference("basedOn");
        var partOf = new Reference("partOf");
        var derivedFrom = new Reference("derivedFrom");
        var category = new CodeableConcept().addCoding(new Coding("test.com", "test", "Test"));
        var subject = new Reference("subject");
        var encounter = new Reference("encounter");
        var performer = new Reference("performer");
        var value = new BooleanType(true);
        adapter.setBasedOn(singletonList(basedOn))
                .setPartOf(singletonList(partOf))
                .setDerivedFrom(singletonList(derivedFrom))
                .setCategory(singletonList(category))
                .setSubject(subject)
                .setEncounter(encounter)
                .setPerformer(singletonList(performer))
                .setValue(value);
        assertEquals(basedOn, obs.getBasedOnFirstRep());
        assertEquals(partOf, obs.getPartOfFirstRep());
        assertEquals(derivedFrom, obs.getDerivedFromFirstRep());
        assertEquals(category, obs.getCategoryFirstRep());
        assertEquals(subject, obs.getSubject());
        assertEquals(encounter, obs.getEncounter());
        assertEquals(performer, obs.getPerformerFirstRep());
        assertEquals(value, obs.getValue());
    }
}
