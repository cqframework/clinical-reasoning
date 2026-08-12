package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Observation.ObservationStatus;
import org.hl7.fhir.dstu3.model.Reference;
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
        var category = new CodeableConcept().addCoding(new Coding("test.com", "test", "Test"));
        var subject = new Reference("subject");
        var performer = new Reference("performer");
        var value = new BooleanType(true);
        adapter.setBasedOn(singletonList(basedOn))
                .setCategory(singletonList(category))
                .setSubject(subject)
                .setPerformer(singletonList(performer))
                .setValue(value);
        assertEquals(basedOn, obs.getBasedOnFirstRep());
        assertEquals(category, obs.getCategoryFirstRep());
        assertEquals(subject, obs.getSubject());
        assertEquals(performer, obs.getPerformerFirstRep());
        assertEquals(value, obs.getValue());
    }
}
