package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.context.FhirContext;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;

/**
 * Aggregates individual subject evaluation results into stratum definitions for measure reporting
 * across multiple subjects.
 *
 * <h2>Stratifier Set Intersection Rules</h2>
 *
 * <p>The stratifier intersection problem has four quadrants based on two dimensions:
 * <ol>
 *   <li><b>Stratifier Type</b>: CRITERIA vs VALUE (including NON_SUBJECT_VALUE)</li>
 *   <li><b>Population Basis</b>: Boolean vs any other FHIR Type (from fhir-types ValueSet)</li>
 * </ol>
 *
 * <p>Population basis can be ANY FHIR type from the
 * <a href="https://build.fhir.org/valueset-fhir-types.html">fhir-types ValueSet</a>, including:
 * <ul>
 *   <li><b>Primitive types</b>: boolean, string, integer, decimal, date, dateTime, etc.</li>
 *   <li><b>Complex types</b>: CodeableConcept, Coding, Identifier, Period, Quantity, etc.</li>
 *   <li><b>Resource types</b>: Encounter, Observation, Patient, Condition, etc.</li>
 * </ul>
 *
 * <p>The key distinction is between:
 * <ul>
 *   <li><b>Boolean basis</b>: Population membership is true/false per subject. Count = number of subjects.</li>
 *   <li><b>Non-boolean basis</b>: Population results are FHIR types (resources, complex types, primitives).
 *       Count = number of result items, with intersection based on the specific FHIR type's identity.</li>
 * </ul>
 *
 * <h3>Intersection Table</h3>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────────────────────┐
 * │                           STRATIFIER SET INTERSECTION RULES                                 │
 * ├─────────────────┬───────────────────────────────────────────────────────────────────────────┤
 * │                 │                        STRATIFIER TYPE                                    │
 * │ POPULATION      ├───────────────────────────────────┬───────────────────────────────────────┤
 * │ BASIS           │ CRITERIA                          │ VALUE / NON_SUBJECT_VALUE             │
 * ├─────────────────┼───────────────────────────────────┼───────────────────────────────────────┤
 * │                 │ Stratifier Result: Boolean        │ Stratifier Result: Scalar value       │
 * │ BOOLEAN         │ Criteria Key: Subject ID          │ Criteria Key: Subject ID              │
 * │                 │ Pop Result: Boolean per Subject   │ Pop Result: Boolean per Subject       │
 * │                 │                                   │                                       │
 * │                 │ INTERSECTION: Subject-based       │ INTERSECTION: Subject-based           │
 * │                 │ Count = subjects where            │ Count = subjects where                │
 * │                 │   strat_result ∩ pop_result       │   pop_result = TRUE (grouped by       │
 * │                 │   both TRUE (or matching sets)    │   stratifier value)                   │
 * ├─────────────────┼───────────────────────────────────┼───────────────────────────────────────┤
 * │                 │ Stratifier Result: FHIR types     │ Stratifier Result: Map&lt;FHIRType,Val&gt; │
 * │ NON-BOOLEAN     │ Criteria Key: FHIR type ID        │ Criteria Key: FHIR type ID (input)    │
 * │ (any FHIR type: │ Pop Result: FHIR types per Subj   │ Pop Result: FHIR types per Subject    │
 * │  Resource,      │                                   │ Display Value: Value (output)         │
 * │  Identifier,    │ INTERSECTION: Type-based          │                                       │
 * │  CodeableConcept│ Count = items where               │ INTERSECTION: Type-based              │
 * │  Period, etc.)  │   strat_items ∩ pop_items         │ Count = items where                   │
 * │                 │   (same item in both)             │   input_item ∩ pop_items              │
 * │                 │                                   │   (grouped by output value)           │
 * └─────────────────┴───────────────────────────────────┴───────────────────────────────────────┘
 * </pre>
 *
 * <h3>Non-Subject Value Stratifier (Key Case)</h3>
 * <p>For NON_SUBJECT_VALUE stratifiers with non-boolean population basis:
 * <ul>
 *   <li>CQL function takes each population result item as input, returns a value</li>
 *   <li>Criteria Key = normalized ID of the input item (e.g., "Encounter/123" for resources)</li>
 *   <li>Display Value = function output (e.g., "Finished", "P21Y--P41Y")</li>
 *   <li>Intersection: check if criteria key exists in population results</li>
 *   <li>Group rows by display value to form strata</li>
 * </ul>
 *
 * @see <a href="https://build.fhir.org/valueset-fhir-types.html">FHIR Types ValueSet</a>
 * @see <a href="https://build.fhir.org/clinicalreasoning-quality-reporting.html#stratification">
 *      FHIR Clinical Reasoning - Stratification</a>
 */
public class MeasureMultiSubjectEvaluator {

    private MeasureMultiSubjectEvaluator() {
        // static class
    }

    /**
     * Take the accumulated subject-by-subject evaluation results and use it to build StratumDefs
     * and StratumPopulationDefs
     *
     * @param fhirContext  FHIR context for FHIR version used
     * @param measureDef  to mutate post-evaluation with results of initial stratifier
     *                    subject-by-subject accumulations.
     *
     */
    public static void postEvaluationMultiSubject(FhirContext fhirContext, MeasureDef measureDef) {

        for (GroupDef groupDef : measureDef.groups()) {
            for (StratifierDef stratifierDef : groupDef.stratifiers()) {
                final List<StratumDef> stratumDefs;

                if (stratifierDef.isCriteriaStratifier()) {
                    stratumDefs = buildCriteriaStrata(fhirContext, stratifierDef, groupDef);
                } else {
                    stratumDefs = buildValueOrNonSubjectValueStrata(fhirContext, stratifierDef, groupDef);
                }

                stratifierDef.addAllStratum(stratumDefs);
            }
        }
    }

    private static List<StratumDef> buildCriteriaStrata(
            FhirContext fhirContext, StratifierDef stratifierDef, GroupDef groupDef) {

        return List.of(buildStratumDef(
                fhirContext,
                stratifierDef,
                // StratumValueDefs seem to be irrelevant for criteria based stratifiers
                Set.of(),
                // Patients seems to be irrelevant for criteria based stratifiers
                List.of(),
                // Row keys same as patients for non-component criteria stratifiers
                List.of(),
                groupDef));
    }

    /**
     * Builds a {@link StratumDef} for a single stratum by combining stratifier values,
     * subjects, row-level keys, and population definitions.
     *
     * <p>This method is the convergence point where:
     * <ul>
     *   <li>Component stratifier values ({@code values})</li>
     *   <li>The subjects that belong to the stratum ({@code subjectIds})</li>
     *   <li>Row-level alignment keys ({@code rowKeys})</li>
     *   <li>Population definitions ({@code populationDefs})</li>
     * </ul>
     * are combined into a fully populated {@link StratumDef}, including all
     * {@link StratumPopulationDef} instances.
     *
     * <h3>Row Keys</h3>
     *
     * <p>{@code rowKeys} represent the atomic units that make up a stratum.
     * Their meaning depends on the stratifier type:
     *
     * <h4>1. Subject-basis stratifiers (CRITERIA or VALUE)</h4>
     *
     * <p>Each row corresponds to a single subject.
     *
     * <pre>
     * rowKeys = [
     *   StratifierRowKey.subjectOnly("Patient/123"),
     *   StratifierRowKey.subjectOnly("Patient/456"),
     *   StratifierRowKey.subjectOnly("Patient/789")
     * ]
     * </pre>
     *
     * <ul>
     *   <li>Used for subject-based intersection</li>
     *   <li>Population counts are subject counts</li>
     *   <li>{@code subjectIds} and {@code rowKeys} usually contain the same values</li>
     * </ul>
     *
     * <h4>2. NON_SUBJECT_VALUE stratifiers (function results)</h4>
     *
     * <p>Each row corresponds to a specific input item for a subject
     * (e.g. Encounter, Observation, Procedure).
     *
     * <pre>
     * rowKeys = [
     *   StratifierRowKey.withInput("Patient/123", "Encounter/enc-1"),
     *   StratifierRowKey.withInput("Patient/123", "Encounter/enc-2"),
     *   StratifierRowKey.withInput("Patient/456", "Encounter/enc-9")
     * ]
     * </pre>
     *
     * <ul>
     *   <li>{@code subjectQualified()} returns the subject (e.g., "Patient/123")</li>
     *   <li>{@code inputParamId()} returns the input resource used by the stratifier function</li>
     *   <li>Intersection with populations is performed at the resource level</li>
     *   <li>Population counts are resource counts, not subject counts</li>
     * </ul>
     *
     * <p>In this case:
     * <ul>
     *   <li>{@code subjectIds = ["Patient/123", "Patient/456"]}</li>
     *   <li>{@code rowKeys.size()} may be greater than {@code subjectIds.size()}</li>
     * </ul>
     *
     * <h3>Population Handling</h3>
     *
     * <p>For each {@link PopulationDef}, this method:
     * <ul>
     *   <li>Builds a {@link StratumPopulationDef}</li>
     *   <li>Calculates subject or resource intersections as appropriate</li>
     *   <li>Derives population counts based on the measure population basis</li>
     * </ul>
     *
     * <h3>Measure Observation Optimization</h3>
     *
     * <p>If the group contains {@code MEASUREOBSERVATION} populations, this method
     * pre-computes a {@link MeasureObservationStratumCache} to efficiently link
     * numerator and denominator observations at the stratum level.
     *
     * @param fhirContext     the FHIR context for the evaluation
     * @param stratifierDef  the stratifier definition that produced this stratum
     * @param values         the set of stratifier component values defining this stratum
     * @param subjectIds     the distinct subjects included in this stratum
     * @param rowKeys        the row-level keys defining atomic stratum membership
     * @param groupDef       the group definition containing population basis and settings
     *
     * @return a fully constructed {@link StratumDef} with population results and metadata
     */
    private static StratumDef buildStratumDef(
            FhirContext fhirContext,
            StratifierDef stratifierDef,
            Set<StratumValueDef> values,
            List<String> subjectIds,
            List<StratifierRowKey> rowKeys,
            GroupDef groupDef) {

        // Build all stratum populations
        List<StratumPopulationDef> stratumPopulations = groupDef.populations().stream()
                .map(popDef ->
                        buildStratumPopulationDef(fhirContext, stratifierDef, popDef, subjectIds, rowKeys, groupDef))
                .toList();

        // Pre-compute measure observation cache if applicable
        MeasureObservationStratumCache observationCache =
                buildMeasureObservationCacheIfApplicable(groupDef, stratumPopulations);

        return new StratumDef(stratumPopulations, values, subjectIds, observationCache);
    }

    // Enhanced by Claude Sonnet 4.5 to calculate and populate all StratumPopulationDef fields
    private static StratumPopulationDef buildStratumPopulationDef(
            FhirContext fhirContext,
            StratifierDef stratifierDef,
            PopulationDef populationDef,
            List<String> subjectIds,
            List<StratifierRowKey> rowKeys,
            GroupDef groupDef) {
        // population subjectIds
        var popSubjectIds = populationDef.getSubjects().stream()
                .map(FhirResourceUtils::addPatientQualifier)
                .collect(Collectors.toUnmodifiableSet());
        // intersect stratum subjectIds and population subjectIds
        var qualifiedSubjectIdsCommonToPopulation = Sets.intersection(new HashSet<>(subjectIds), popSubjectIds);

        // Calculate intersection and resource IDs based on stratifier type and basis
        Set<Object> populationDefEvaluationResultIntersection;
        List<String> resourceIdsForSubjectList;

        // For criteria stratifiers, always calculate the intersection regardless of basis
        if (stratifierDef.isCriteriaStratifier()) {
            populationDefEvaluationResultIntersection =
                    calculateCriteriaStratifierIntersection(stratifierDef, populationDef);
        } else {
            populationDefEvaluationResultIntersection = Set.of();
        }

        if (groupDef.isBooleanBasis()) {
            // For boolean basis, we don't need resource IDs
            resourceIdsForSubjectList = List.of();
        } else {
            // For resource basis stratifiers, calculate resource IDs based on stratifier type
            resourceIdsForSubjectList =
                    getResourceIdsForValueStratifier(fhirContext, stratifierDef, rowKeys, groupDef, populationDef);
        }

        return new StratumPopulationDef(
                populationDef,
                qualifiedSubjectIdsCommonToPopulation,
                populationDefEvaluationResultIntersection,
                resourceIdsForSubjectList,
                stratifierDef.getStratifierType(),
                groupDef.getPopulationBasis());
    }

    /**
     * Build measure observation cache if this measure has MEASUREOBSERVATION populations
     * linked to NUMERATOR and DENOMINATOR populations (e.g., ratio measures with observations).
     * Returns null if not applicable or if lookups fail (preserves existing null behavior).
     * <p>
     * Added by Claude Sonnet 4.5 on 2025-12-05 for measure observation optimization.
     *
     * @param groupDef the group definition
     * @param stratumPopulations the list of stratum populations for this stratum
     * @return the cache or null if not applicable
     */
    private static MeasureObservationStratumCache buildMeasureObservationCacheIfApplicable(
            GroupDef groupDef, List<StratumPopulationDef> stratumPopulations) {

        // Only applicable for measures with observations
        if (!groupDef.hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
            return null;
        }

        // Get all MEASUREOBSERVATION populations
        List<PopulationDef> measureObservationPopulationDefs =
                groupDef.getPopulationDefs(MeasurePopulationType.MEASUREOBSERVATION);

        if (measureObservationPopulationDefs.isEmpty()) {
            return null;
        }

        // Find numerator and denominator observation PopulationDefs
        PopulationDef numObsPopDef = groupDef.findRatioObservationPopulationDef(
                measureObservationPopulationDefs, MeasurePopulationType.NUMERATOR);
        PopulationDef denObsPopDef = groupDef.findRatioObservationPopulationDef(
                measureObservationPopulationDefs, MeasurePopulationType.DENOMINATOR);

        if (numObsPopDef == null || denObsPopDef == null) {
            return null;
        }

        // Find corresponding StratumPopulationDefs
        StratumPopulationDef numObsStratumPop = stratumPopulations.stream()
                .filter(sp -> sp.populationDef() == numObsPopDef)
                .findFirst()
                .orElse(null);

        StratumPopulationDef denObsStratumPop = stratumPopulations.stream()
                .filter(sp -> sp.populationDef() == denObsPopDef)
                .findFirst()
                .orElse(null);

        // All-or-nothing: only create cache if both found
        if (numObsStratumPop == null || denObsStratumPop == null) {
            return null;
        }

        return new MeasureObservationStratumCache(numObsStratumPop, denObsStratumPop);
    }

    private static List<StratumDef> buildValueOrNonSubjectValueStrata(
            FhirContext fhirContext, StratifierDef stratifierDef, GroupDef groupDef) {

        final Table<StratifierRowKey, StratumValueWrapper, StratifierComponentDef> subjectResultTable =
                buildSubjectResultsTable(stratifierDef.components());

        // Stratifiers should be of the same basis as population
        // Split subjects by result values
        // ex. all Male Patients and all Female Patients

        var componentSubjects = groupSubjectsByValueDefSet(subjectResultTable);

        var stratumDefs = new ArrayList<StratumDef>();

        componentSubjects.forEach((valueSet, rowKeys) -> {
            // converts table into component value combinations
            // | Stratum   | Set<ValueDef>           | List<StratifierRowKey>                         |
            // | --------- | ----------------------- | ---------------------------------------------- |
            // | Stratum-1 | <'M','White>            | [subjectOnly(subject-a)]                       |
            // | Stratum-2 | <'F','hispanic/latino'> | [subjectOnly(subject-b)]                       |
            // | Stratum-3 | <'M','hispanic/latino'> | [subjectOnly(subject-c)]                       |
            // | Stratum-4 | <'F','black'>           | [subjectOnly(subject-d), subjectOnly(subject-e)]|
            //
            // For non-subject value stratifiers with function results:
            // | Stratum   | Set<ValueDef>           | List<StratifierRowKey>
            // |
            // | --------- | ----------------------- | ----------------------------------------------------------------
            // |
            // | Stratum-1 | <'35'>                  | [withInput(Patient/1, Encounter/a), withInput(Patient/2, Enc/b)]
            // |
            // | Stratum-2 | <'38'>                  | [withInput(Patient/3, Encounter/c)]
            // |

            // Extract subjects from row keys
            List<String> subjects = rowKeys.stream()
                    .map(StratifierRowKey::subjectOnlyKey)
                    .distinct()
                    .toList();

            stratumDefs.add(buildStratumDef(fhirContext, stratifierDef, valueSet, subjects, rowKeys, groupDef));
        });

        return stratumDefs;
    }

    /**
     * Builds a table mapping subjects (or subject|resource pairs) to their stratifier values.
     *
     * <p>Handles three cases:
     * <ul>
     *   <li><b>Map values (NON_SUBJECT_VALUE)</b>: CQL functions returning Map&lt;inputResource, outputValue&gt;</li>
     *   <li><b>Iterable values (multi-value)</b>: CQL expressions returning List of values per subject</li>
     *   <li><b>Scalar values (standard)</b>: Single value per subject</li>
     * </ul>
     *
     * <p>For non-subject value stratifiers, the CQL function returns Map&lt;inputResource, outputValue&gt;.
     * We expand this into multiple rows, one per input resource, using a composite row key.
     *
     * <p><b>Mixed function/scalar components:</b> When stratifiers mix function components (Map results)
     * and scalar components, the scalar values are expanded to match the function row keys.
     * This ensures all components can be properly aligned and grouped together.
     */
    private static Table<StratifierRowKey, StratumValueWrapper, StratifierComponentDef> buildSubjectResultsTable(
            List<StratifierComponentDef> componentDefs) {

        final Table<StratifierRowKey, StratumValueWrapper, StratifierComponentDef> subjectResultTable =
                HashBasedTable.create();

        // First pass: Collect all composite row keys (subject|resource) from function components
        // These are needed to expand scalar components to match function row keys
        final Map<String, Set<StratifierRowKey>> functionRowKeysBySubject = collectFunctionRowKeys(componentDefs);

        for (StratifierComponentDef componentDef : componentDefs) {
            for (StratumTableRow stratumTableRow : mapToListOfTableEntries(componentDef, functionRowKeysBySubject)) {
                subjectResultTable.put(
                        stratumTableRow.stratifierRowKey(), stratumTableRow.stratumValueWrapper(), componentDef);
            }
        }

        return subjectResultTable;
    }

    /**
     * Collects all composite row keys (subject|resource) from function components.
     *
     * <p>This is used to expand scalar components to match the function row keys when
     * stratifiers mix function and scalar components.
     *
     * @return Map from subject (e.g., "Patient/123") to set of composite row keys
     */
    private static Map<String, Set<StratifierRowKey>> collectFunctionRowKeys(
            List<StratifierComponentDef> componentDefs) {

        final Map<String, Set<StratifierRowKey>> functionRowKeysBySubject = new HashMap<>();

        for (StratifierComponentDef componentDef : componentDefs) {
            for (var entry : componentDef.getResults().entrySet()) {
                String subjectId = entry.getKey();
                CriteriaResult result = entry.getValue();
                Object rawValue = result == null ? null : result.rawValue();

                // Only process function results (Map values)
                if (rawValue instanceof Map<?, ?> functionResults) {
                    String qualifiedSubject = FhirResourceUtils.addPatientQualifier(subjectId);
                    Set<StratifierRowKey> rowKeys =
                            functionRowKeysBySubject.computeIfAbsent(qualifiedSubject, k -> new HashSet<>());

                    for (Object key : functionResults.keySet()) {
                        String normalizedKey = normalizeResourceKey(key);
                        rowKeys.add(StratifierRowKey.withInput(qualifiedSubject, normalizedKey));
                    }
                }
            }
        }

        return functionRowKeysBySubject;
    }

    private static List<StratumTableRow> mapToListOfTableEntries(
            StratifierComponentDef componentDef, Map<String, Set<StratifierRowKey>> functionRowKeysBySubject) {

        return componentDef.getResults().entrySet().stream()
                .map(entry -> mapToListOfTableEntries(entry.getKey(), entry.getValue(), functionRowKeysBySubject))
                .flatMap(Collection::stream)
                .toList();
    }

    private record StratumTableRow(StratifierRowKey stratifierRowKey, StratumValueWrapper stratumValueWrapper) {}

    private static List<StratumTableRow> mapToListOfTableEntries(
            String subjectId, CriteriaResult result, Map<String, Set<StratifierRowKey>> functionRowKeysBySubject) {

        final String qualifiedSubject = FhirResourceUtils.addPatientQualifier(subjectId);
        final Object rawValue = result == null ? null : result.rawValue();

        if (rawValue instanceof Map<?, ?> functionResults) {
            return addFunctionResultRows(qualifiedSubject, functionResults);

        } else if (rawValue instanceof Iterable<?> iterableValue) {
            return addIterableValueRows(qualifiedSubject, iterableValue);
        }

        // Scalar value: check if we need to expand to match function row keys
        Set<StratifierRowKey> functionRowKeys = functionRowKeysBySubject.get(qualifiedSubject);
        if (functionRowKeys != null && !functionRowKeys.isEmpty()) {
            // Expand scalar to match function row keys for this subject
            return expandScalarToMatchFunctionRowKeys(functionRowKeys, rawValue);
        }

        // No function row keys - use simple subject-only row key
        return List.of(addScalarValueRow(qualifiedSubject, rawValue));
    }

    /**
     * Expands a scalar value to match the row keys from function components.
     *
     * <p>When stratifiers mix function and scalar components, the scalar value applies
     * to all resources for that subject. This method creates one row per function row key,
     * all with the same scalar value.
     *
     * @param functionRowKeys the row keys from function components for this subject
     * @param scalarValue the scalar value to expand
     * @return list of table rows, one per function row key
     */
    private static List<StratumTableRow> expandScalarToMatchFunctionRowKeys(
            Set<StratifierRowKey> functionRowKeys, Object scalarValue) {

        StratumValueWrapper valueWrapper = new StratumValueWrapper(scalarValue);
        return functionRowKeys.stream()
                .map(rowKey -> new StratumTableRow(rowKey, valueWrapper))
                .toList();
    }

    /**
     * Adds rows for non-subject value stratifiers with function results (Map&lt;inputResource, outputValue&gt;).
     *
     * <p>For each entry in the map:
     * <ul>
     *   <li>Build composite row key: "Patient/xxx|Resource/yyy"</li>
     *   <li>The output value becomes the stratum value (what's displayed)</li>
     *   <li>Null values are allowed - they will be grouped into a special "null" stratum</li>
     * </ul>
     */
    private static List<StratumTableRow> addFunctionResultRows(String qualifiedSubject, Map<?, ?> functionResults) {

        return functionResults.entrySet().stream()
                .map(entry ->
                        // The output value becomes the stratum value (what's displayed)
                        // Null values are allowed - they will be grouped into a special "null" stratum
                        new StratumTableRow(
                                StratifierRowKey.withInput(
                                        qualifiedSubject,
                                        // Build composite row key: "Patient/xxx|Resource/yyy"
                                        normalizeResourceKey(entry.getKey())),
                                new StratumValueWrapper(entry.getValue())))
                .toList();
    }

    /**
     * Adds rows for Iterable values (e.g., List&lt;EncounterStatus&gt;) by expanding into multiple
     * rows.
     *
     * <p>Each element in the iterable creates its own row with a composite key, allowing
     * the subject to appear in multiple strata when the stratifier expression returns multiple
     * values.
     *
     * <p><b>Note:</b> Maps are handled separately and should NOT be expanded as Iterables.
     *
     * @return keys and values to add to the subject results table
     */
    private static List<StratumTableRow> addIterableValueRows(String qualifiedSubject, Iterable<?> iterableValue) {

        var tableRows = new ArrayList<StratumTableRow>();
        int index = 0;
        for (Object value : iterableValue) {
            // Use value-based key to create unique row per value.
            // This allows groupSubjectsByValueDefSet to group rows by value, not by subject.
            String valueKey = normalizeValueKey(value, index);
            StratifierRowKey rowKey = StratifierRowKey.withInput(qualifiedSubject, valueKey);
            StratumValueWrapper stratumValueWrapper = new StratumValueWrapper(value);
            tableRows.add(new StratumTableRow(rowKey, stratumValueWrapper));
            index++;
        }

        return tableRows;
    }

    /**
     * Adds a single row for a scalar value per subject (standard case).
     *
     * <p>Null values are allowed - they will be grouped into a special "null" stratum.
     */
    private static StratumTableRow addScalarValueRow(String qualifiedSubject, Object rawValue) {

        // Null values are allowed - they will be grouped into a special "null" stratum

        return new StratumTableRow(StratifierRowKey.subjectOnly(qualifiedSubject), new StratumValueWrapper(rawValue));
    }

    /**
     * Normalize a resource to its ID string for use as a row key component.
     */
    private static String normalizeResourceKey(Object obj) {
        if (obj instanceof IBaseResource resource
                && resource.getIdElement() != null
                && !resource.getIdElement().isEmpty()) {
            return resource.getIdElement().toVersionless().getValue();
        }
        return String.valueOf(obj);
    }
    /**
     * Groups stratifier results into strata by the full set of component values.
     *
     * <h3>Input (normalized stratifier rows)</h3>
     *
     * <p>Each row represents a single evaluated unit:
     * <ul>
     *   <li>Subject-basis stratifier → one row per subject</li>
     *   <li>NON_SUBJECT_VALUE stratifier → one row per (subject + input parameter)</li>
     * </ul>
     *
     * <h4>Subject-basis stratifier (scalar values)</h4>
     *
     * <pre>
     * | RowKey (subject) | Stratifier Value | Component |
     * | ---------------- | ---------------- | --------- |
     * | Patient/A        | M                | gender    |
     * | Patient/B        | F                | gender    |
     * | Patient/C        | M                | gender    |
     * | Patient/D        | F                | gender    |
     * | Patient/E        | F                | gender    |
     * | Patient/A        | white            | race      |
     * | Patient/B        | hispanic/latino  | race      |
     * | Patient/C        | hispanic/latino  | race      |
     * | Patient/D        | black            | race      |
     * | Patient/E        | black            | race      |
     * </pre>
     *
     * <h4>NON_SUBJECT_VALUE stratifier (function results)</h4>
     *
     * <p>Each function returns Map&lt;inputParam, producedValue&gt;.
     * The input parameter is used for alignment and intersection;
     * the produced value is what is displayed in the stratum.
     *
     * <pre>
     * | RowKey (subject | input)        | Stratifier Value | Component |
     * | ------------------------------ | ---------------- | --------- |
     * | Patient/A | Encounter/1001     | finished         | status    |
     * | Patient/A | Encounter/1002     | in-progress      | status    |
     * | Patient/B | Encounter/2001     | finished         | status    |
     * | Patient/A | Encounter/1001     | P0Y--P21Y        | age-band  |
     * | Patient/A | Encounter/1002     | P21Y--P41Y       | age-band  |
     * | Patient/B | Encounter/2001     | P0Y--P21Y        | age-band  |
     * </pre>
     *
     * <h3>Output (grouped into strata)</h3>
     *
     * <p>Rows are grouped by their full set of component values.
     *
     * <h4>Subject-basis output</h4>
     *
     * <pre>
     * | Stratum Values            | Subjects            |
     * | ------------------------- | ------------------- |
     * | { M, white }              | [Patient/A]         |
     * | { F, hispanic/latino }    | [Patient/B]         |
     * | { M, hispanic/latino }    | [Patient/C]         |
     * | { F, black }              | [Patient/D, Patient/E] |
     * </pre>
     *
     * <h4>NON_SUBJECT_VALUE output (row-level strata)</h4>
     *
     * <pre>
     * | Stratum Values                    | Rows                              |
     * | --------------------------------- | --------------------------------- |
     * | { finished, P0Y--P21Y }            | [Patient/A|Encounter/1001,
     * |                                   |  Patient/B|Encounter/2001]        |
     * | { in-progress, P21Y--P41Y }        | [Patient/A|Encounter/1002]        |
     * </pre>
     *
     * <p>Each output row corresponds to a {@code StratumDef}.
     * Subject lists are derived from the row keys.
     */
    private static Map<Set<StratumValueDef>, List<StratifierRowKey>> groupSubjectsByValueDefSet(
            Table<StratifierRowKey, StratumValueWrapper, StratifierComponentDef> table) {

        // Step 1: Build Map<RowKey, Set<ValueDef>>
        final Map<StratifierRowKey, Set<StratumValueDef>> rowKeyToValueDefs = new HashMap<>();

        for (Table.Cell<StratifierRowKey, StratumValueWrapper, StratifierComponentDef> cell : table.cellSet()) {
            rowKeyToValueDefs
                    .computeIfAbsent(cell.getRowKey(), k -> new HashSet<>())
                    .add(new StratumValueDef(cell.getColumnKey(), cell.getValue()));
        }

        // Step 2: Invert to Map<Set<ValueDef>, List<RowKey>>
        return rowKeyToValueDefs.entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue,
                        Collector.of(ArrayList::new, (list, e) -> list.add(e.getKey()), (l1, l2) -> {
                            l1.addAll(l2);
                            return l1;
                        })));
    }

    /**
     * Calculate the intersection between stratifier results and population results for criteria-based stratifiers.
     *
     * <p>Intersection rules:
     * <ul>
     *   <li>If the stratifier result is {@code Map<inputParam, producedValue>}, intersect using {@code map.keySet()} (the input params)</li>
     *   <li>Otherwise, intersect using {@link CriteriaResult#valueAsSet()}</li>
     * </ul>
     */
    private static Set<Object> calculateCriteriaStratifierIntersection(
            StratifierDef stratifierDef, PopulationDef populationDef) {

        final Map<String, CriteriaResult> stratifierResultsBySubject = stratifierDef.getResults();
        final List<Object> allPopulationStratumIntersectingResources = new ArrayList<>();

        // For each subject, we intersect between the population and stratifier results
        for (Entry<String, CriteriaResult> stratifierEntryBySubject : stratifierResultsBySubject.entrySet()) {
            final Set<Object> stratifierResultsPerSubject =
                    criteriaResultAsIntersectionSet(stratifierEntryBySubject.getValue());

            final Set<Object> populationResultsPerSubject =
                    populationDef.getResourcesForSubject(stratifierEntryBySubject.getKey());

            allPopulationStratumIntersectingResources.addAll(
                    Sets.intersection(populationResultsPerSubject, stratifierResultsPerSubject));
        }

        // We add up all the results of the intersections here:
        return new HashSetForFhirResourcesAndCqlTypes<>(allPopulationStratumIntersectingResources);
    }

    /**
     * Convert a CriteriaResult into the set that should be used for intersection.
     *
     * <p>For Map-based results (Map<inputParam, producedValue>), the input parameters (map keys)
     * are the intersectable items.
     */
    private static Set<Object> criteriaResultAsIntersectionSet(CriteriaResult result) {
        if (result == null) {
            return Set.of();
        }

        Object raw = result.rawValue();
        if (raw instanceof Map<?, ?> m) {
            return new HashSet<>(m.keySet());
        }

        return result.valueAsSet();
    }

    /**
     * Calculate resource IDs for value stratifiers (VALUE and NON_SUBJECT_VALUE types).
     *
     * <p>For NON_SUBJECT_VALUE stratifiers with function results, the row keys contain composite keys
     * with both subject and input parameter. We need to:
     * <ol>
     *   <li>Extract the resource IDs from the row keys (the inputParamId)</li>
     *   <li>Intersect those with the population's resources to get only resources that qualify</li>
     * </ol>
     *
     * <p>For VALUE stratifiers (subject-basis scalar values), we fall back to the original behavior
     * of getting all resources for the qualifying subjects.
     */
    private static List<String> getResourceIdsForValueStratifier(
            FhirContext fhirContext,
            StratifierDef stratifierDef,
            List<StratifierRowKey> rowKeys,
            GroupDef groupDef,
            PopulationDef populationDef) {
        // Check if we have composite row keys for NON_SUBJECT_VALUE stratifiers
        if (stratifierDef.getStratifierType() == MeasureStratifierType.NON_SUBJECT_VALUE) {
            // Extract resource IDs from row keys that have input parameters
            Set<String> stratumResourceIds = rowKeys.stream()
                    .filter(StratifierRowKey::hasInputParam)
                    .map(key -> key.inputParamId().orElseThrow())
                    .collect(Collectors.toSet());

            if (!stratumResourceIds.isEmpty()) {
                // Get all population resource IDs
                Set<String> populationResourceIds = getPopulationResourceIdSet(fhirContext, groupDef, populationDef);

                // Intersect stratum resource IDs with population resource IDs
                return stratumResourceIds.stream()
                        .filter(populationResourceIds::contains)
                        .toList();
            }
        }

        return List.of();
    }

    /**
     * Get all resource IDs from a population as a Set for efficient intersection.
     *
     * <p>For MEASUREOBSERVATION populations, the subjectResources contain Set&lt;Map&lt;inputResource, outputValue&gt;&gt;
     * so we extract the keys (input resources) from those maps.
     */
    private static Set<String> getPopulationResourceIdSet(
            FhirContext fhirContext, GroupDef groupDef, PopulationDef populationDef) {
        final String resourceType = FhirResourceUtils.determineFhirResourceTypeOrNull(fhirContext, groupDef);
        boolean isResourceType = resourceType != null;
        Set<String> resourceIds = new HashSet<>();

        if (populationDef.getSubjectResources() != null) {
            for (Set<Object> resources : populationDef.getSubjectResources().values()) {
                if (resources != null) {
                    // For MEASUREOBSERVATION, resources are Map<inputResource, outputValue>
                    // We need to extract the keys (input resources)
                    if (populationDef.type() == MeasurePopulationType.MEASUREOBSERVATION) {
                        resources.stream()
                                .filter(Map.class::isInstance)
                                .map(m -> (Map<?, ?>) m)
                                .flatMap(m -> m.keySet().stream())
                                .map(MeasureMultiSubjectEvaluator::normalizePopulationKey)
                                .filter(java.util.Objects::nonNull)
                                .forEach(resourceIds::add);
                    } else if (isResourceType) {
                        resources.stream()
                                .map(MeasureMultiSubjectEvaluator::normalizePopulationKey)
                                .filter(java.util.Objects::nonNull)
                                .forEach(resourceIds::add);
                    } else {
                        resources.stream().map(Object::toString).forEach(resourceIds::add);
                    }
                }
            }
        }
        return resourceIds;
    }

    /**
     * Normalize a population result item (FHIR type) into a stable string key.
     *
     * <p>This is used for intersection when the population basis is non-boolean.
     * For resources, we use the versionless reference (e.g., "Encounter/123").
     * For non-resource FHIR types and primitives, we fall back to {@code String.valueOf(obj)}.
     */
    private static String normalizePopulationKey(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof IBaseResource resource) {
            if (resource.getIdElement() != null && !resource.getIdElement().isEmpty()) {
                return resource.getIdElement().toVersionless().getValueAsString();
            }
            // If the resource is present but has no id, fall back to toString for best-effort logging/debug.
            return resource.toString();
        }
        return String.valueOf(obj);
    }

    /**
     * Normalize a value to a string key for use in composite row keys when expanding Iterables.
     * Uses the index as a fallback for null values to ensure unique keys.
     */
    private static String normalizeValueKey(Object value, int index) {
        if (value == null) {
            return "null_" + index;
        }
        if (value instanceof IBaseResource resource
                && resource.getIdElement() != null
                && !resource.getIdElement().isEmpty()) {
            return resource.getIdElement().toVersionless().getValue();
        }

        return "value_" + index + "_" + value;
    }
}
