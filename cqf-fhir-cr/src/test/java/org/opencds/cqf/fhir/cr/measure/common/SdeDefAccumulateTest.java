package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.cql.engine.runtime.Value;

/**
 * {@link SdeDef#accumulate()} merges every subject's evaluated resources into one set. That set is
 * keyed by resource identity rather than by {@link Value#hashCode}, which for a
 * {@link ClassInstance} is a recursive walk of the whole element tree.
 *
 * <p>The retrieve path builds a fresh {@code ClassInstance} per retrieve, so the same record
 * reaches this method as several structurally-equal-but-distinct instances. They must collapse to
 * one entry, and they must do so without hashing the graph.
 */
class SdeDefAccumulateTest {

    private static final String FHIR_NS = "http://hl7.org/fhir";

    /** As {@link #resource}, plus one extra element so two instances differ structurally. */
    private static ClassInstance resource(String type, String id, String statusValue) {
        var instance = resource(type, id);
        instance.getElements().put("status", new org.opencds.cqf.cql.engine.runtime.String(statusValue));
        return instance;
    }

    /** A FHIR-namespaced {@link ClassInstance}, the shape a retrieve produces. */
    private static ClassInstance resource(String type, String id) {
        Map<String, Value> idElements = new LinkedHashMap<>();
        idElements.put("value", new org.opencds.cqf.cql.engine.runtime.String(id));
        Map<String, Value> elements = new LinkedHashMap<>();
        elements.put("id", new ClassInstance(new QName(FHIR_NS, "id", "FHIR"), idElements));
        return new ClassInstance(new QName(FHIR_NS, type, "FHIR"), elements);
    }

    private static SdeDef sdeDef() {
        return new SdeDef("sde-1", null, "SDE Sex");
    }

    @Test
    void collapsesSeparateInstancesOfTheSameResource() {
        var firstRetrieve = resource("Encounter", "enc-1");
        var secondRetrieve = resource("Encounter", "enc-1");

        // Precondition: distinct objects standing for one record, or this proves nothing.
        assertNotSame(firstRetrieve, secondRetrieve);

        var sde = sdeDef();
        sde.putResult("Patient/1", "SDE Sex", null, Set.of(firstRetrieve));
        sde.putResult("Patient/2", "SDE Sex", null, Set.of(secondRetrieve));

        sde.accumulate();

        assertEquals(
                1,
                sde.getAllEvaluatedResources().size(),
                "one record retrieved twice should be one evaluated resource");
    }

    @Test
    void collapsesOneRecordArrivingWithDifferentContent() {
        // The case that separates resource identity from structural equality: the same record read
        // twice, differing in some element — a status that moved on, a bumped versionId. Deep
        // equality calls these two records and keeps both; identity calls them one.
        var earlier = resource("Encounter", "enc-1", "planned");
        var later = resource("Encounter", "enc-1", "finished");

        assertNotEquals(earlier, later, "fixture should differ structurally");

        var sde = sdeDef();
        sde.putResult("Patient/1", "SDE Sex", null, Set.of(earlier));
        sde.putResult("Patient/2", "SDE Sex", null, Set.of(later));

        sde.accumulate();

        assertEquals(
                1,
                sde.getAllEvaluatedResources().size(),
                "one record is one evaluated resource regardless of which read arrived");
    }

    @Test
    void keepsDistinctResources() {
        var sde = sdeDef();
        sde.putResult("Patient/1", "SDE Sex", null, Set.of(resource("Encounter", "enc-1")));
        sde.putResult("Patient/2", "SDE Sex", null, Set.of(resource("Encounter", "enc-2")));

        sde.accumulate();

        assertEquals(2, sde.getAllEvaluatedResources().size());
    }

    @Test
    void keepsResourcesOfDifferentTypesSharingAnId() {
        // Identity is (resource type, logical id), not the id alone — Encounter/1 and Observation/1
        // are different records.
        var sde = sdeDef();
        sde.putResult("Patient/1", "SDE Sex", null, Set.of(resource("Encounter", "shared-id")));
        sde.putResult("Patient/2", "SDE Sex", null, Set.of(resource("Observation", "shared-id")));

        sde.accumulate();

        assertEquals(2, sde.getAllEvaluatedResources().size());
    }

    @Test
    void mergesAcrossManySubjectsRetrievingTheSameRecord() {
        // The shape that made this expensive: one record reached by many subjects, each carrying
        // its own instance of it.
        var sde = sdeDef();
        for (int subject = 0; subject < 50; subject++) {
            sde.putResult("Patient/" + subject, "SDE Sex", null, Set.of(resource("Encounter", "enc-1")));
        }

        sde.accumulate();

        assertEquals(1, sde.getAllEvaluatedResources().size());
    }

    @Test
    void accumulatingNothingYieldsAnEmptySet() {
        var sde = sdeDef();

        sde.accumulate();

        assertTrue(sde.getAllEvaluatedResources().isEmpty());
    }
}
