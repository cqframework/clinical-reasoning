// package org.opencds.cqf.fhir.cr.measure.common;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertNull;
// import static org.junit.jupiter.api.Assertions.assertSame;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.junit.jupiter.api.Assertions.assertTrue;
//
// import java.math.BigDecimal;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import java.util.stream.StreamSupport;
// import org.hl7.fhir.r4.model.Encounter;
// import org.hl7.fhir.r4.model.Patient;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.Arguments;
// import org.junit.jupiter.params.provider.MethodSource;
// import org.opencds.cqf.cql.engine.execution.EvaluationExpressionRef;
// import org.opencds.cqf.cql.engine.execution.EvaluationResult;
// import org.opencds.cqf.cql.engine.execution.ExpressionResult;
// import org.opencds.cqf.cql.engine.fhir.model.FhirModelResolver;
// import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
// import org.opencds.cqf.cql.engine.runtime.String;
// import org.opencds.cqf.cql.engine.runtime.Value;
//
// class CqlExpressionValueTest {
//    static final FhirModelResolver modelResolver = new R4FhirModelResolver();
//
//    @Test
//    void of_nullExpressionResult_returnsEmpty() {
//        CqlExpressionValue wrapper = CqlExpressionValue.of(null, null);
//
//        assertTrue(wrapper.isNull());
//        assertTrue(wrapper.isEmpty());
//        assertSame(CqlExpressionValue.empty(), wrapper);
//        assertEquals(Set.of(), wrapper.evaluatedResources());
//    }
//
//    @Test
//    void of_expressionResult_propagatesValueAndResources() {
//        var patient = modelResolver.toCqlValue(new Patient().setId("p1"), false);
//        Set<Value> resources = new HashSet<>(List.of(patient));
//        ExpressionResult result = new ExpressionResult(patient, resources);
//
//        CqlExpressionValue wrapper = CqlExpressionValue.of(result);
//
//        assertSame(patient, wrapper.raw());
//        assertEquals(resources, wrapper.evaluatedResources());
//    }
//
//    @Test
//    void of_expressionResultWithNullResources_substitutesEmptySet() {
//        ExpressionResult result = new ExpressionResult(new String("v"), null);
//
//        CqlExpressionValue wrapper = CqlExpressionValue.of(result);
//
//        assertEquals(Set.of(), wrapper.evaluatedResources());
//    }
//
//    @Test
//    void ofRaw_acceptsNullResources() {
//        CqlExpressionValue wrapper = CqlExpressionValue.ofRaw(42, null);
//
//        assertEquals(42, wrapper.raw());
//        assertEquals(Set.of(), wrapper.evaluatedResources());
//    }
//
//    // -- predicates --------------------------------------------------------------
//
//    @Test
//    void isBooleanAndIsTrue_onlyForBooleanRawValues() {
//        assertTrue(CqlExpressionValue.ofRaw(true, null).isBoolean());
//        assertTrue(CqlExpressionValue.ofRaw(true, null).isTrue());
//        assertTrue(CqlExpressionValue.ofRaw(false, null).isBoolean());
//        assertFalse(CqlExpressionValue.ofRaw(false, null).isTrue());
//        assertFalse(CqlExpressionValue.ofRaw("string", null).isBoolean());
//        assertFalse(CqlExpressionValue.ofRaw(null, null).isTrue());
//    }
//
//    @Test
//    void isIterable_trueForCollectionsAndOtherIterables() {
//        assertTrue(CqlExpressionValue.ofRaw(List.of(1, 2), null).isIterable());
//        assertTrue(CqlExpressionValue.ofRaw(Set.of(1, 2), null).isIterable());
//        assertFalse(CqlExpressionValue.ofRaw("string", null).isIterable());
//        assertFalse(CqlExpressionValue.ofRaw(null, null).isIterable());
//    }
//
//    @ParameterizedTest
//    @MethodSource("emptyValues")
//    void isEmpty_recognizesNullEmptyIterableEmptyMap(Object raw) {
//        assertTrue(CqlExpressionValue.ofRaw(raw, null).isEmpty());
//    }
//
//    static java.util.stream.Stream<Object> emptyValues() {
//        return java.util.stream.Stream.of(null, Collections.emptyList(), Collections.emptySet(), Map.of());
//    }
//
//    @Test
//    void isEmpty_falseForNonEmptyContainersAndScalars() {
//        assertFalse(CqlExpressionValue.ofRaw(List.of(1), null).isEmpty());
//        assertFalse(CqlExpressionValue.ofRaw(Map.of("k", "v"), null).isEmpty());
//        assertFalse(CqlExpressionValue.ofRaw("anything", null).isEmpty());
//        assertFalse(CqlExpressionValue.ofRaw(false, null).isEmpty());
//    }
//
//    // -- asBoolean ---------------------------------------------------------------
//
//    @Test
//    void asBoolean_presentOnlyForBooleanValues() {
//        assertEquals(
//                java.util.Optional.of(true),
//                CqlExpressionValue.ofRaw(true, null).asBoolean());
//        assertEquals(
//                java.util.Optional.of(false),
//                CqlExpressionValue.ofRaw(false, null).asBoolean());
//        assertEquals(
//                java.util.Optional.empty(),
//                CqlExpressionValue.ofRaw("not-bool", null).asBoolean());
//        assertEquals(
//                java.util.Optional.empty(), CqlExpressionValue.ofRaw(null, null).asBoolean());
//    }
//
//    // -- asIterable / asIterableOrNull ------------------------------------------
//
//    @ParameterizedTest
//    @MethodSource("asIterableCases")
//    void asIterable_normalizesAllShapes(Object raw, List<Object> expected) {
//        Iterable<Object> actual = CqlExpressionValue.ofRaw(raw, null).asIterable();
//        assertEquals(expected, toList(actual));
//    }
//
//    static java.util.stream.Stream<Arguments> asIterableCases() {
//        Patient patient = new Patient();
//        patient.setId("p1");
//        Encounter encounter = new Encounter();
//        encounter.setId("e1");
//        return java.util.stream.Stream.of(
//                Arguments.of(null, List.of()),
//                Arguments.of(true, List.of(true)),
//                Arguments.of(false, List.of(false)),
//                Arguments.of(List.of(), List.of()),
//                Arguments.of(List.of(patient), List.of(patient)),
//                Arguments.of(List.of(patient, encounter), List.of(patient, encounter)),
//                Arguments.of("string", List.of("string")),
//                Arguments.of(BigDecimal.ONE, List.of(BigDecimal.ONE)),
//                Arguments.of(42, List.of(42)),
//                Arguments.of(Map.of("k", "v"), List.of(Map.of("k", "v"))));
//    }
//
//    @Test
//    void asIterable_passesThroughIterableInstance() {
//        ArrayList<Object> source = new ArrayList<>(List.of("a", "b"));
//        Iterable<Object> result = CqlExpressionValue.ofRaw(source, null).asIterable();
//
//        // Same iterable instance is returned (no copying)
//        assertSame(source, result);
//    }
//
//    @Test
//    void asIterableOrNull_preservesNullForTrueNullValue() {
//        assertNull(CqlExpressionValue.ofRaw(null, null).asIterableOrNull());
//    }
//
//    @Test
//    void asIterableOrNull_normalizesScalarToSingletonList() {
//        Iterable<Object> result = CqlExpressionValue.ofRaw("scalar", null).asIterableOrNull();
//
//        assertNotNull(result);
//        assertEquals(List.of("scalar"), toList(result));
//    }
//
//    @Test
//    void asIterableOrNull_passesThroughIterable() {
//        List<Object> source = List.of("a", "b");
//        assertSame(source, CqlExpressionValue.ofRaw(source, null).asIterableOrNull());
//    }
//
//    // -- resolveForPopulation ----------------------------------------------------
//
//    @Test
//    void resolveForPopulation_nullValueReturnsEmpty() {
//        EvaluationResult evaluationResult = new EvaluationResult();
//
//        Iterable<Object> result =
//                CqlExpressionValue.ofRaw(null, null).resolveForPopulation("Patient", evaluationResult);
//
//        assertEquals(List.of(), toList(result));
//    }
//
//    @Test
//    void resolveForPopulation_falseReturnsEmpty() {
//        EvaluationResult evaluationResult = new EvaluationResult();
//        var patient = modelResolver.toCqlValue(new Patient(), false);
//        evaluationResult.set(new EvaluationExpressionRef("Patient"), new ExpressionResult(patient, Set.of()));
//
//        Iterable<Object> result =
//                CqlExpressionValue.ofRaw(false, null).resolveForPopulation("Patient", evaluationResult);
//
//        assertEquals(List.of(), toList(result));
//    }
//
//    @Test
//    void resolveForPopulation_trueLooksUpSubjectContextValue() {
//        var patient = modelResolver.toCqlValue(new Patient().setId("p1"), false);
//        EvaluationResult evaluationResult = new EvaluationResult();
//        evaluationResult.set(new EvaluationExpressionRef("Patient"), new ExpressionResult(patient, Set.of()));
//
//        Iterable<Object> result =
//                CqlExpressionValue.ofRaw(true, null).resolveForPopulation("Patient", evaluationResult);
//
//        List<Object> resolved = toList(result);
//        assertEquals(1, resolved.size());
//        assertSame(patient, resolved.get(0));
//    }
//
//    @Test
//    void resolveForPopulation_trueButNoSubjectResultThrows() {
//        EvaluationResult evaluationResult = new EvaluationResult();
//
//        final CqlExpressionValue cqlExpressionValue = CqlExpressionValue.ofRaw(true, null);
//
//        CqlExpressionValueException ex = assertThrows(
//                CqlExpressionValueException.class,
//                () -> cqlExpressionValue.resolveForPopulation("Patient", evaluationResult));
//
//        assertTrue(ex.getMessage().contains("Patient"));
//    }
//
//    @Test
//    void resolveForPopulation_iterableReturnedAsIs() {
//        Patient p1 = new Patient();
//        p1.setId("p1");
//        Patient p2 = new Patient();
//        p2.setId("p2");
//        List<Object> source = List.of(p1, p2);
//
//        Iterable<Object> result =
//                CqlExpressionValue.ofRaw(source, null).resolveForPopulation("Patient", new EvaluationResult());
//
//        assertSame(source, result);
//    }
//
//    @Test
//    void resolveForPopulation_scalarWrappedInSingletonList() {
//        Encounter encounter = new Encounter();
//        encounter.setId("e1");
//
//        Iterable<Object> result =
//                CqlExpressionValue.ofRaw(encounter, null).resolveForPopulation("Patient", new EvaluationResult());
//
//        assertEquals(List.of(encounter), toList(result));
//    }
//
//    // -- isMap / asMap -----------------------------------------------------------
//
//    @Test
//    void isMap_trueOnlyForMapValues() {
//        assertTrue(CqlExpressionValue.ofRaw(Map.of("k", "v"), null).isMap());
//        assertTrue(CqlExpressionValue.ofRaw(Map.of(), null).isMap());
//        assertFalse(CqlExpressionValue.ofRaw(List.of(), null).isMap());
//        assertFalse(CqlExpressionValue.ofRaw("string", null).isMap());
//        assertFalse(CqlExpressionValue.ofRaw(null, null).isMap());
//    }
//
//    @Test
//    void asMap_emptyOptionalForNonMapInputs() {
//        assertEquals(
//                java.util.Optional.empty(), CqlExpressionValue.ofRaw(null, null).asMap());
//        assertEquals(
//                java.util.Optional.empty(),
//                CqlExpressionValue.ofRaw("scalar", null).asMap());
//        assertEquals(
//                java.util.Optional.empty(),
//                CqlExpressionValue.ofRaw(List.of(1, 2), null).asMap());
//    }
//
//    @Test
//    void asMap_returnsTypedMapForMapInputs() {
//        Patient patient = new Patient();
//        patient.setId("p1");
//        Map<Object, Object> source = new HashMap<>();
//        source.put(patient, 42);
//
//        java.util.Optional<Map<Object, Object>> opt =
//                CqlExpressionValue.ofRaw(source, null).asMap();
//
//        assertTrue(opt.isPresent());
//        assertSame(source, opt.get());
//        assertEquals(42, opt.get().get(patient));
//    }
//
//    @Test
//    void asMap_emptyMapInputYieldsEmptyMap() {
//        java.util.Optional<Map<Object, Object>> opt =
//                CqlExpressionValue.ofRaw(Map.of(), null).asMap();
//
//        assertTrue(opt.isPresent());
//        assertTrue(opt.get().isEmpty());
//    }
//
//    // -- asObservationAccumulator ------------------------------------------------
//
//    @Test
//    void asObservationAccumulator_emptyOptionalForNonAccumulatorInputs() {
//        assertEquals(
//                java.util.Optional.empty(), CqlExpressionValue.ofRaw(null, null).asObservationAccumulator());
//        assertEquals(
//                java.util.Optional.empty(),
//                CqlExpressionValue.ofRaw("scalar", null).asObservationAccumulator());
//        assertEquals(
//                java.util.Optional.empty(),
//                CqlExpressionValue.ofRaw(Map.of("k", "v"), null).asObservationAccumulator());
//        assertEquals(
//                java.util.Optional.empty(),
//                CqlExpressionValue.ofRaw(List.of(), null).asObservationAccumulator());
//    }
//
//    @Test
//    void asObservationAccumulator_returnsAccumulatorWhenWrapped() {
//        Encounter enc = new Encounter();
//        enc.setId("Encounter/1");
//        ObservationAccumulator acc =
//                new ObservationAccumulator(List.of(new ObservationEntry(enc, new QuantityDef(42.0))));
//
//        java.util.Optional<ObservationAccumulator> opt =
//                CqlExpressionValue.ofRaw(acc, null).asObservationAccumulator();
//
//        assertTrue(opt.isPresent());
//        assertSame(acc, opt.get());
//        assertEquals(1, opt.get().size());
//        assertSame(enc, opt.get().entries().get(0).inputResource());
//    }
//
//    @Test
//    void asObservationAccumulator_emptyAccumulatorYieldsEmptyAccumulator() {
//        ObservationAccumulator empty = new ObservationAccumulator(List.of());
//
//        java.util.Optional<ObservationAccumulator> opt =
//                CqlExpressionValue.ofRaw(empty, null).asObservationAccumulator();
//
//        assertTrue(opt.isPresent());
//        assertTrue(opt.get().isEmpty());
//        assertEquals(0, opt.get().size());
//    }
//
//    // -- asFunctionResultAccumulator ---------------------------------------------
//
//    @Test
//    void asFunctionResultAccumulator_emptyOptionalForNonAccumulatorInputs() {
//        assertEquals(
//                java.util.Optional.empty(), CqlExpressionValue.ofRaw(null, null).asFunctionResultAccumulator());
//        assertEquals(
//                java.util.Optional.empty(),
//                CqlExpressionValue.ofRaw("scalar", null).asFunctionResultAccumulator());
//        assertEquals(
//                java.util.Optional.empty(),
//                CqlExpressionValue.ofRaw(Map.of("k", "v"), null).asFunctionResultAccumulator());
//        assertEquals(
//                java.util.Optional.empty(),
//                CqlExpressionValue.ofRaw(List.of(), null).asFunctionResultAccumulator());
//    }
//
//    @Test
//    void asFunctionResultAccumulator_returnsAccumulatorWhenWrapped() {
//        Encounter enc = new Encounter();
//        enc.setId("Encounter/1");
//        FunctionResultAccumulator acc =
//                new FunctionResultAccumulator(List.of(new FunctionResultEntry(enc, "stratum-value")));
//
//        java.util.Optional<FunctionResultAccumulator> opt =
//                CqlExpressionValue.ofRaw(acc, null).asFunctionResultAccumulator();
//
//        assertTrue(opt.isPresent());
//        assertSame(acc, opt.get());
//        assertEquals(1, opt.get().size());
//        assertSame(enc, opt.get().entries().get(0).input());
//        assertEquals("stratum-value", opt.get().entries().get(0).output());
//    }
//
//    @Test
//    void asFunctionResultAccumulator_isNotConfusedWithObservationAccumulator() {
//        ObservationAccumulator obsAcc =
//                new ObservationAccumulator(List.of(new ObservationEntry("k", new QuantityDef(1.0))));
//
//        // Same wrapper held only as the OTHER accumulator type returns the right narrowing
//        CqlExpressionValue wrapper = CqlExpressionValue.ofRaw(obsAcc, null);
//        assertTrue(wrapper.asObservationAccumulator().isPresent());
//        assertEquals(java.util.Optional.empty(), wrapper.asFunctionResultAccumulator());
//    }
//
//    // -- valueAsSet --------------------------------------------------------------
//
//    @Test
//    void valueAsSet_nullValueYieldsEmptySet() {
//        Set<Object> set = CqlExpressionValue.ofRaw(null, null).valueAsSet();
//
//        assertTrue(set instanceof HashSetForFhirResourcesAndCqlTypes);
//        assertTrue(set.isEmpty());
//    }
//
//    @Test
//    void valueAsSet_scalarYieldsSingletonSet() {
//        Patient patient = new Patient();
//        patient.setId("p1");
//
//        Set<Object> set = CqlExpressionValue.ofRaw(patient, null).valueAsSet();
//
//        assertTrue(set instanceof HashSetForFhirResourcesAndCqlTypes);
//        assertEquals(1, set.size());
//        assertTrue(set.contains(patient));
//    }
//
//    @Test
//    void valueAsSet_iterableFlattensIntoSet() {
//        Patient p1 = new Patient();
//        p1.setId("p1");
//        Patient p2 = new Patient();
//        p2.setId("p2");
//
//        Set<Object> set = CqlExpressionValue.ofRaw(List.of(p1, p2), null).valueAsSet();
//
//        assertTrue(set instanceof HashSetForFhirResourcesAndCqlTypes);
//        assertEquals(2, set.size());
//    }
//
//    // -- nonNullValues -----------------------------------------------------------
//
//    @Test
//    void nonNullValues_nullValueYieldsEmptyList() {
//        assertEquals(List.of(), CqlExpressionValue.ofRaw(null, null).nonNullValues());
//    }
//
//    @Test
//    void nonNullValues_scalarYieldsSingletonList() {
//        assertEquals(List.of("v"), CqlExpressionValue.ofRaw("v", null).nonNullValues());
//    }
//
//    @Test
//    void nonNullValues_iterableFiltersOutNullElements() {
//        ArrayList<Object> source = new ArrayList<>();
//        source.add("a");
//        source.add(null);
//        source.add("b");
//        source.add(null);
//
//        assertEquals(List.of("a", "b"), CqlExpressionValue.ofRaw(source, null).nonNullValues());
//    }
//
//    @Test
//    void nonNullValues_emptyIterableYieldsEmptyList() {
//        assertEquals(List.of(), CqlExpressionValue.ofRaw(List.of(), null).nonNullValues());
//    }
//
//    // -- evaluatedResources / raw ------------------------------------------------
//
//    @Test
//    void evaluatedResources_returnsTheBackingSet() {
//        Set<Value> resources = new HashSet<>(List.of(new String("r1"), new String("r2")));
//        CqlExpressionValue wrapper = CqlExpressionValue.ofRaw("v", resources);
//
//        assertSame(resources, wrapper.evaluatedResources());
//    }
//
//    @Test
//    void raw_returnsUnderlyingObject() {
//        Map<Object, Object> accumulator = new HashMap<>();
//        accumulator.put("k", 1);
//
//        CqlExpressionValue wrapper = CqlExpressionValue.ofRaw(accumulator, null);
//
//        assertSame(accumulator, wrapper.raw());
//    }
//
//    private static List<Object> toList(Iterable<Object> it) {
//        return StreamSupport.stream(it.spliterator(), false).toList();
//    }
// }
