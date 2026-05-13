package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;

class MeasureMultiSubjectEvaluatorTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4Cached();

    private static MeasureDef measureDefWith(SdeDef... sdes) {
        return new MeasureDef(
                new IdType(ResourceType.Measure.name(), "m1"), "http://test", null, List.of(), List.of(sdes));
    }

    private static MeasureDef measureDefWith(GroupDef groupDef) {
        return new MeasureDef(
                new IdType(ResourceType.Measure.name(), "m1"), "http://test", null, List.of(groupDef), List.of());
    }

    private static CodeDef basisCode(String basis) {
        return new CodeDef("http://hl7.org/fhir/ValueSet/measure-population", basis);
    }

    private static ConceptDef textConcept(String text) {
        return new ConceptDef(List.of(), text);
    }

    private static PopulationDef initialPopulation(CodeDef basis) {
        return new PopulationDef(
                "initial-population",
                new ConceptDef(
                        List.of(new CodeDef(
                                "http://terminology.hl7.org/CodeSystem/measure-population", "initial-population")),
                        "Initial Population"),
                MeasurePopulationType.INITIALPOPULATION,
                "Initial Population Expression",
                basis,
                null);
    }

    private static Encounter encounter(String id) {
        var enc = new Encounter();
        enc.setId(new IdType("Encounter", id));
        return enc;
    }

    private static GroupDef cohortGroup(CodeDef basis, PopulationDef pop, StratifierDef stratifierDef) {
        return new GroupDef(
                "group-1",
                textConcept("group-1"),
                List.of(stratifierDef),
                List.of(pop),
                MeasureScoring.COHORT,
                false,
                new CodeDef("http://terminology.hl7.org/CodeSystem/measure-improvement-notation", "increase"),
                basis);
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
        sde.putResult("Patient/p1", "male", Set.of());

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(sde));

        assertEquals(1, sde.getAccumulatedValues().size());

        var entry = sde.getAccumulatedValues().entrySet().iterator().next();
        assertEquals("male", entry.getKey().getValueAsString());
        assertEquals(1L, entry.getValue());
    }

    @Test
    void multipleSubjects_sameValue() {
        var sde = new SdeDef("sde-1", new ConceptDef(List.of(), null), null);
        sde.putResult("Patient/p1", "male", Set.of());
        sde.putResult("Patient/p2", "male", Set.of());
        sde.putResult("Patient/p3", "male", Set.of());

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(sde));

        assertEquals(1, sde.getAccumulatedValues().size());
        assertEquals(3L, sde.getAccumulatedValues().values().iterator().next());
    }

    @Test
    void multipleSubjects_differentValues() {
        var sde = new SdeDef("sde-1", new ConceptDef(List.of(), null), null);
        sde.putResult("Patient/p1", "male", Set.of());
        sde.putResult("Patient/p2", "female", Set.of());
        sde.putResult("Patient/p3", "male", Set.of());

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(sde));

        assertEquals(2, sde.getAccumulatedValues().size());

        Map<StratumValueWrapper, Long> accumulated = sde.getAccumulatedValues();
        assertEquals(2L, accumulated.get(new StratumValueWrapper("male")));
        assertEquals(1L, accumulated.get(new StratumValueWrapper("female")));
    }

    @Test
    void resourceTypedValues_accumulated() {
        var patient = (Patient) new Patient().setId(new IdType("Patient", "patient1"));
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
        sde.putResult("Patient/p1", Arrays.asList("male", null, "female"), Set.of());

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(sde));

        // Only "male" and "female" should be accumulated (null filtered)
        assertEquals(2, sde.getAccumulatedValues().size());
    }

    @Test
    void evaluatedResources_aggregatedAcrossSubjects() {
        var res1 = (Patient) new Patient().setId(new IdType("Patient", "res1"));
        var res2 = (Patient) new Patient().setId(new IdType("Patient", "res2"));
        var res3 = (Patient) new Patient().setId(new IdType("Patient", "res3"));

        var sde = new SdeDef("sde-1", new ConceptDef(List.of(), null), null);
        sde.putResult("Patient/p1", "male", Set.of(res1, res2));
        sde.putResult("Patient/p2", "female", Set.of(res2, res3));

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
        sde1.putResult("Patient/p1", "white", Set.of());
        sde1.putResult("Patient/p2", "black", Set.of());

        var sde2 = new SdeDef("sde-sex", new ConceptDef(List.of(), null), null);
        sde2.putResult("Patient/p1", "male", Set.of());
        sde2.putResult("Patient/p2", "male", Set.of());

        MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(sde1, sde2));

        // sde1: 2 distinct values
        assertEquals(2, sde1.getAccumulatedValues().size());

        // sde2: 1 value with count 2
        assertEquals(1, sde2.getAccumulatedValues().size());
        assertEquals(2L, sde2.getAccumulatedValues().values().iterator().next());
    }

    /**
     * Direct unit coverage of the four #909 scenarios at the {@link MeasureMultiSubjectEvaluator}
     * level — see https://github.com/cqframework/clinical-reasoning/issues/909.
     *
     * <p>MMSE itself only sees cases 1, 2, and 4: case 3 (subject-basis + CQL function) is rejected
     * by {@link FunctionEvaluationHandler#cqlFunctionEvaluation} before MMSE runs. The case-3 test
     * here pins that contract by exercising MMSE with subject-basis + scalar inputs (the post-gate
     * state) and noting in javadoc where the actual rejection is exercised.
     */
    @Nested
    class Issue909 {

        /**
         * Case 1: non-subject (Encounter) basis + CQL function expression.
         *
         * <p>{@code FunctionEvaluationHandler.processNonSubValueStratifier} populates
         * {@code component.results} with a {@code Map<Encounter, value>} per subject. MMSE expands
         * that into one row per (subject, Encounter), then groups rows by value to form strata.
         */
        @Test
        void case1_nonSubjectBasis_functionResult_producesPerResourceStrata() {
            var basis = basisCode("Encounter");
            var enc1 = encounter("enc-1");
            var enc2 = encounter("enc-2");
            var enc3 = encounter("enc-3");

            var pop = initialPopulation(basis);
            pop.addResource("p1", enc1);
            pop.addResource("p1", enc2);
            pop.addResource("p2", enc3);

            var component =
                    new StratifierComponentDef("strat-1-c1", textConcept("Status"), "Encounter Status Stratifier");
            // Order matters for stratum identity assertions below.
            Map<Object, Object> p1Result = new LinkedHashMap<>();
            p1Result.put(enc1, "finished");
            p1Result.put(enc2, "in-progress");
            component.putResult("p1", p1Result, Set.of());
            component.putResult("p2", Map.of(enc3, "finished"), Set.of());

            var stratifierDef = new StratifierDef(
                    "stratifier-1",
                    textConcept("Encounter Status"),
                    "Encounter Status Stratifier",
                    MeasureStratifierType.NON_SUBJECT_VALUE,
                    List.of(component));
            var groupDef = cohortGroup(basis, pop, stratifierDef);

            MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(groupDef));

            // Two distinct function outputs → two strata.
            assertEquals(2, stratifierDef.getStratum().size());

            var byValue = indexStrataByValueText(stratifierDef);

            var finishedStratum = byValue.get("finished");
            assertNotNull(finishedStratum);
            // "finished" covers enc-1 (p1) and enc-3 (p2) → 2 Encounter resources.
            assertEquals(2, finishedStratum.getPopulationCount(pop));

            var inProgressStratum = byValue.get("in-progress");
            assertNotNull(inProgressStratum);
            // "in-progress" covers enc-2 (p1) only.
            assertEquals(1, inProgressStratum.getPopulationCount(pop));
        }

        /**
         * Case 2: non-subject (Encounter) basis + scalar (non-function) expression.
         *
         * <p>The fallback in {@link MeasureEvaluator#handleNonBooleanBasisComponent} stores a single
         * scalar value per subject. MMSE forms one stratum per scalar value and counts every Encounter
         * for each subject under that subject's stratum — the scalar is "applied per resource".
         *
         * <p>Includes a MEASUREOBSERVATION population to lock in the
         * {@code getResourcesForSubjects} fix on this branch. Without that fix MMSE writes
         * garbage {@code Map.toString()} keys into {@code resourceIdsForSubjectList} for
         * MEASUREOBSERVATION populations and downstream CV scoring produces a null measureScore
         * per stratum (see
         * {@code ContinuousVariableResourceMeasureObservationTest.continuousVariableObservationEncounterBasisNonFunctionStratifier}).
         */
        @Test
        void case2_nonSubjectBasis_scalarResult_appliesScalarPerResource() {
            var basis = basisCode("Encounter");
            var enc1 = encounter("enc-1");
            var enc2 = encounter("enc-2");
            var enc3 = encounter("enc-3");

            var ipPop = initialPopulation(basis);
            ipPop.addResource("p1", enc1);
            ipPop.addResource("p1", enc2);
            ipPop.addResource("p2", enc3);

            // MEASUREOBSERVATION population: subjectResources values are Set<Map<input, output>>,
            // exactly how FunctionEvaluationHandler.processMeasureObservation populates them.
            var measureObsPop = new PopulationDef(
                    "measure-observation",
                    new ConceptDef(
                            List.of(new CodeDef(
                                    "http://terminology.hl7.org/CodeSystem/measure-population", "measure-observation")),
                            "Measure Observation"),
                    MeasurePopulationType.MEASUREOBSERVATION,
                    "initial-population-MeasureObservation",
                    basis,
                    "initial-population",
                    ContinuousVariableObservationAggregateMethod.AVG,
                    null);
            Map<Object, Object> p1Observations = new LinkedHashMap<>();
            p1Observations.put(enc1, new QuantityDef(120.0));
            p1Observations.put(enc2, new QuantityDef(80.0));
            measureObsPop.addResource("p1", p1Observations);
            measureObsPop.addResource("p2", Map.of(enc3, new QuantityDef(60.0)));

            var component = new StratifierComponentDef("strat-1-c1", textConcept("Gender"), "Patient Gender");
            // Scalar value per subject — what MeasureEvaluator.handleNonBooleanBasisComponent writes.
            component.putResult("p1", "male", Set.of());
            component.putResult("p2", "female", Set.of());

            var stratifierDef = new StratifierDef(
                    "stratifier-1",
                    textConcept("Gender"),
                    "Patient Gender",
                    MeasureStratifierType.NON_SUBJECT_VALUE,
                    List.of(component));
            var groupDef = new GroupDef(
                    "group-1",
                    textConcept("group-1"),
                    List.of(stratifierDef),
                    List.of(ipPop, measureObsPop),
                    MeasureScoring.CONTINUOUSVARIABLE,
                    false,
                    new CodeDef("http://terminology.hl7.org/CodeSystem/measure-improvement-notation", "increase"),
                    basis);

            MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(groupDef));

            assertEquals(2, stratifierDef.getStratum().size());

            var byValue = indexStrataByValueText(stratifierDef);

            // p1 has 2 encounters, all under "male"; p2 has 1 encounter under "female".
            assertEquals(2, byValue.get("male").getPopulationCount(ipPop));
            assertEquals(1, byValue.get("female").getPopulationCount(ipPop));

            // The fix: MEASUREOBSERVATION resourceIdsForSubjectList must contain real Encounter IDs
            // (extracted from Map keys), not Map.toString() garbage. Pre-fix this list would contain
            // entries like "{Encounter/enc-1=QuantityDef[value=120.0]}".
            var maleObsResourceIds =
                    byValue.get("male").getStratumPopulation(measureObsPop).resourceIdsForSubjectList();
            assertTrue(
                    maleObsResourceIds.contains("Encounter/enc-1"),
                    "MEASUREOBSERVATION stratum population should hold real Encounter IDs but got: "
                            + maleObsResourceIds);
            assertTrue(maleObsResourceIds.contains("Encounter/enc-2"));
            var femaleObsResourceIds =
                    byValue.get("female").getStratumPopulation(measureObsPop).resourceIdsForSubjectList();
            assertTrue(femaleObsResourceIds.contains("Encounter/enc-3"));
        }

        /**
         * Case 3: subject (boolean) basis + CQL function expression must error per #909.
         *
         * <p>The rejection lives in
         * {@code FunctionEvaluationHandler.validateStratifierExpressionTypes} (line 170-198) and runs
         * before MMSE. MMSE itself performs no defensive validation — it operates on whatever
         * post-validation state it receives. The end-to-end rejection is asserted by
         * {@code MeasureStratifierTest.cohortBooleanValueStratFunctionStratifierInvalid}.
         *
         * <p>This test pins MMSE's contract: given the legal post-gate inputs for a subject-basis
         * stratifier (scalar value per subject, boolean basis), MMSE produces normal subject-level
         * strata — see case 4. Case 3's Map-shaped input never reaches here.
         */
        @Test
        void case3_subjectBasis_functionResult_isRejectedUpstreamSoMmseNeverSeesIt() {
            // Intentionally identical setup to case 4 — the point is that this is what MMSE actually
            // sees for subject-basis stratifiers. Any Map-shaped (function) value would have been
            // rejected by FunctionEvaluationHandler before reaching this code path.
            var basis = basisCode("boolean");

            var pop = initialPopulation(basis);
            pop.addResource("p1", Boolean.TRUE);
            pop.addResource("p2", Boolean.TRUE);

            var component = new StratifierComponentDef("strat-1-c1", textConcept("Gender"), "Gender Stratification");
            component.putResult("p1", "male", Set.of());
            component.putResult("p2", "female", Set.of());

            var stratifierDef = new StratifierDef(
                    "stratifier-1",
                    textConcept("Gender"),
                    "Gender Stratification",
                    MeasureStratifierType.VALUE,
                    List.of(component));
            var groupDef = cohortGroup(basis, pop, stratifierDef);

            MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(groupDef));

            // MMSE accepted the legal scalar input shape — see end-to-end test for rejection of
            // the function-shape variant.
            assertEquals(2, stratifierDef.getStratum().size());
        }

        /**
         * Case 4: subject (boolean) basis + scalar expression — normal subject-level stratification.
         *
         * <p>{@link MeasureEvaluator#handleBooleanBasisComponent} writes one scalar per subject. MMSE
         * forms one stratum per scalar value with subject (not resource) counts.
         */
        @Test
        void case4_subjectBasis_scalarResult_producesSubjectCountStrata() {
            var basis = basisCode("boolean");

            var pop = initialPopulation(basis);
            pop.addResource("p1", Boolean.TRUE);
            pop.addResource("p2", Boolean.TRUE);
            pop.addResource("p3", Boolean.TRUE);

            var component = new StratifierComponentDef("strat-1-c1", textConcept("Gender"), "Gender Stratification");
            component.putResult("p1", "male", Set.of());
            component.putResult("p2", "female", Set.of());
            component.putResult("p3", "male", Set.of());

            var stratifierDef = new StratifierDef(
                    "stratifier-1",
                    textConcept("Gender"),
                    "Gender Stratification",
                    MeasureStratifierType.VALUE,
                    List.of(component));
            var groupDef = cohortGroup(basis, pop, stratifierDef);

            MeasureMultiSubjectEvaluator.postEvaluationMultiSubject(FHIR_CONTEXT, measureDefWith(groupDef));

            assertEquals(2, stratifierDef.getStratum().size());

            var byValue = indexStrataByValueText(stratifierDef);
            // 2 male subjects, 1 female subject — boolean basis counts subjects.
            assertEquals(2, byValue.get("male").getPopulationCount(pop));
            assertEquals(1, byValue.get("female").getPopulationCount(pop));
        }

        private Map<String, StratumDef> indexStrataByValueText(StratifierDef stratifierDef) {
            Map<String, StratumDef> byValue = new LinkedHashMap<>();
            for (StratumDef stratum : stratifierDef.getStratum()) {
                // Each stratum has exactly one component value in these tests.
                String key = stratum.valueDefs().iterator().next().value().getValueAsString();
                byValue.put(key, stratum);
            }
            return byValue;
        }
    }
}
