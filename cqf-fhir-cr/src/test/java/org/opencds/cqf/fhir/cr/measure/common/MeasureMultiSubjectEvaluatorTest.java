package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;

class MeasureMultiSubjectEvaluatorTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4Cached();
    static final FhirModelResolver modelResolver = new R4FhirModelResolver();

    private static MeasureDef measureDefWith(SdeDef... sdes) {
        return new MeasureDef(
                new IdType(ResourceType.Measure.name(), "m1"), "http://test", null, List.of(), List.of(sdes));
    }

    @Test
    void emptyResults_notAccumulated() {
        var sde = new SdeDef("sde-1", new ConceptDef(List.of(), null), null);

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(sde));

        assertTrue(sde.getAccumulatedValues().isEmpty());
        assertTrue(sde.getAllEvaluatedResources().isEmpty());
    }

    @Test
    void singleSubject_singlePrimitiveValue() {
        var sde = new SdeDef("sde-1", new ConceptDef(List.of(), null), null);
        sde.putResult("Patient/p1", new org.opencds.cqf.cql.engine.runtime.String("male"), Set.of());

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(sde));

        assertEquals(1, sde.getAccumulatedValues().size());

        var entry = sde.getAccumulatedValues().entrySet().iterator().next();
        assertEquals("male", entry.getKey().getValueAsString());
        assertEquals(1L, entry.getValue());
    }

    @Test
    void multipleSubjects_sameValue() {
        var sde = new SdeDef("sde-1", new ConceptDef(List.of(), null), null);
        sde.putResult("Patient/p1", new org.opencds.cqf.cql.engine.runtime.String("male"), Set.of());
        sde.putResult("Patient/p2", new org.opencds.cqf.cql.engine.runtime.String("male"), Set.of());
        sde.putResult("Patient/p3", new org.opencds.cqf.cql.engine.runtime.String("male"), Set.of());

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(sde));

        assertEquals(1, sde.getAccumulatedValues().size());
        assertEquals(3L, sde.getAccumulatedValues().values().iterator().next());
    }

    @Test
    void multipleSubjects_differentValues() {
        var sde = new SdeDef("sde-1", new ConceptDef(List.of(), null), null);
        sde.putResult("Patient/p1", new org.opencds.cqf.cql.engine.runtime.String("male"), Set.of());
        sde.putResult("Patient/p2", new org.opencds.cqf.cql.engine.runtime.String("female"), Set.of());
        sde.putResult("Patient/p3", new org.opencds.cqf.cql.engine.runtime.String("male"), Set.of());

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(sde));

        assertEquals(2, sde.getAccumulatedValues().size());

        Map<StratumValueWrapper, Long> accumulated = sde.getAccumulatedValues();
        assertEquals(2L, accumulated.get(new StratumValueWrapper("male")));
        assertEquals(1L, accumulated.get(new StratumValueWrapper("female")));
    }

    @Test
    void resourceTypedValues_accumulated() {
        var patient = modelResolver.toCqlValue(new Patient().setId(new IdType("Patient", "patient1")), false);
        var sde = new SdeDef("sde-1", new ConceptDef(List.of(), null), null);
        sde.putResult("Patient/p1", patient, Set.of());
        sde.putResult("Patient/p2", patient, Set.of());

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(sde));

        assertEquals(1, sde.getAccumulatedValues().size());
        assertEquals(2L, sde.getAccumulatedValues().values().iterator().next());
    }

    @Test
    void nullValues_filteredOut() {
        var sde = new SdeDef("sde-1", new ConceptDef(List.of(), null), null);
        var list = new org.opencds.cqf.cql.engine.runtime.List(Arrays.asList(
                new org.opencds.cqf.cql.engine.runtime.String("male"),
                null,
                new org.opencds.cqf.cql.engine.runtime.String("female")));
        sde.putResult("Patient/p1", list, Set.of());

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(sde));

        // Only "male" and "female" should be accumulated (null filtered)
        assertEquals(2, sde.getAccumulatedValues().size());
    }

    @Test
    void evaluatedResources_aggregatedAcrossSubjects() {
        var res1 = modelResolver.toCqlValue(new Patient().setId(new IdType("Patient", "res1")), false);
        var res2 = modelResolver.toCqlValue(new Patient().setId(new IdType("Patient", "res2")), false);
        var res3 = modelResolver.toCqlValue(new Patient().setId(new IdType("Patient", "res3")), false);

        var sde = new SdeDef("sde-1", new ConceptDef(List.of(), null), null);
        sde.putResult("Patient/p1", new org.opencds.cqf.cql.engine.runtime.String("male"), Set.of(res1, res2));
        sde.putResult("Patient/p2", new org.opencds.cqf.cql.engine.runtime.String("female"), Set.of(res2, res3));

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(sde));

        // All unique evaluated resources should be aggregated
        assertEquals(3, sde.getAllEvaluatedResources().size());
        assertTrue(sde.getAllEvaluatedResources().contains(res1));
        assertTrue(sde.getAllEvaluatedResources().contains(res2));
        assertTrue(sde.getAllEvaluatedResources().contains(res3));
    }

    @Test
    void multipleSdeDefs_accumulatedIndependently() {
        var sde1 = new SdeDef("sde-race", new ConceptDef(List.of(), null), null);
        sde1.putResult("Patient/p1", new org.opencds.cqf.cql.engine.runtime.String("white"), Set.of());
        sde1.putResult("Patient/p2", new org.opencds.cqf.cql.engine.runtime.String("black"), Set.of());

        var sde2 = new SdeDef("sde-sex", new ConceptDef(List.of(), null), null);
        sde2.putResult("Patient/p1", new org.opencds.cqf.cql.engine.runtime.String("male"), Set.of());
        sde2.putResult("Patient/p2", new org.opencds.cqf.cql.engine.runtime.String("male"), Set.of());

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(sde1, sde2));

        // sde1: 2 distinct values
        assertEquals(2, sde1.getAccumulatedValues().size());

        // sde2: 1 value with count 2
        assertEquals(1, sde2.getAccumulatedValues().size());
        assertEquals(2L, sde2.getAccumulatedValues().values().iterator().next());
    }
}
