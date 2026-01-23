package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.context.FhirContext;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregates individual subject evaluation results into stratum definitions for measure reporting across multiple subjects.
 *
 * Supports:
 *  - subject-basis stratifiers (subject -> scalar)
 *  - non-subject / function-result stratifiers (subject -> Map<inputParam, producedValue>)
 *
 * For Map<inputParam, producedValue>:
 *  - inputParam (key) is used for alignment + population intersection (pop-basis)
 *  - producedValue (value) is what becomes StratumValueWrapper (component.text display)
 *
 * This refactor removes string row keys like "Patient/x|Encounter/y" and replaces them with a structured RowKey.
 */
public class MeasureMultiSubjectEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(MeasureMultiSubjectEvaluator.class);

    /**
     * Row identifier for component stratification results.
     *
     * - subjectQualified: "Patient/patient-1" (always present)
     * - criteriaKey: the "pop-basis" alignment key (e.g., Encounter/123) for non-subject stratifiers
     *   null for subject-basis stratifiers.
     */
    private static final class RowKey {
        private final String subjectQualified;
        private final String criteriaKey; // nullable

        private RowKey(String subjectQualified, String criteriaKey) {
            this.subjectQualified = subjectQualified;
            this.criteriaKey = criteriaKey;
        }

        public String subjectQualified() {
            return subjectQualified;
        }

        public String criteriaKey() {
            return criteriaKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RowKey other)) return false;
            return Objects.equals(subjectQualified, other.subjectQualified)
                && Objects.equals(criteriaKey, other.criteriaKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(subjectQualified, criteriaKey);
        }

        @Override
        public String toString() {
            return "RowKey{subject=" + subjectQualified + ", criteriaKey=" + criteriaKey + "}";
        }
    }

    /**
     * Take the accumulated subject-by-subject evaluation results and use it to build StratumDefs
     * and StratumPopulationDefs.
     */
    public static void postEvaluationMultiSubject(FhirContext fhirContext, MeasureDef measureDef) {

        for (GroupDef groupDef : measureDef.groups()) {
            for (StratifierDef stratifierDef : groupDef.stratifiers()) {
                final List<StratumDef> stratumDefs;

                if (stratifierDef.isComponentStratifier()) {
                    stratumDefs = componentStratumPlural(fhirContext, stratifierDef, groupDef.populations(), groupDef);
                } else {
                    stratumDefs =
                        nonComponentStratumPlural(fhirContext, stratifierDef, groupDef.populations(), groupDef);
                }

                stratifierDef.addAllStratum(stratumDefs);
            }
        }
    }

    private static StratumDef buildStratumDef(
        FhirContext fhirContext,
        StratifierDef stratifierDef,
        Set<StratumValueDef> values,
        List<String> subjectIds,
        List<PopulationDef> populationDefs,
        GroupDef groupDef) {

        List<StratumPopulationDef> stratumPopulations = populationDefs.stream()
            .map(popDef -> buildStratumPopulationDef(fhirContext, stratifierDef, popDef, subjectIds, groupDef))
            .toList();

        MeasureObservationStratumCache observationCache =
            buildMeasureObservationCacheIfApplicable(groupDef, stratumPopulations);

        return new StratumDef(stratumPopulations, values, subjectIds, observationCache);
    }

    private static StratumPopulationDef buildStratumPopulationDef(
        FhirContext fhirContext,
        StratifierDef stratifierDef,
        PopulationDef populationDef,
        List<String> subjectIds,
        GroupDef groupDef) {

        var popSubjectIds = populationDef.getSubjects().stream()
            .map(FhirResourceUtils::addPatientQualifier)
            .collect(Collectors.toUnmodifiableSet());

        var qualifiedSubjectIdsCommonToPopulation = Sets.intersection(new HashSet<>(subjectIds), popSubjectIds);

        Set<Object> populationDefEvaluationResultIntersection;
        List<String> resourceIdsForSubjectList;

        if (stratifierDef.isCriteriaStratifier()) {
            if (groupDef.isBooleanBasis()) {
                // boolean-basis criteria: subject-alignment intersection
                populationDefEvaluationResultIntersection =
                    calculateCriteriaStratifierIntersectionBySubject(stratifierDef, populationDef);
            } else {
                // non-boolean basis: if stratifier results are Map<inputParam, value>, intersect by inputParam keys
                if (hasInputParameterFunctionResults(stratifierDef)) {
                    populationDefEvaluationResultIntersection =
                        calculateCriteriaStratifierIntersectionByInputParameter(stratifierDef, populationDef);
                } else {
                    // legacy fallback
                    populationDefEvaluationResultIntersection =
                        calculateCriteriaStratifierIntersectionBySubject(stratifierDef, populationDef);
                }
            }
        } else {
            populationDefEvaluationResultIntersection = Set.of();
        }

        if (groupDef.isBooleanBasis()) {
            resourceIdsForSubjectList = List.of();
        } else {
            resourceIdsForSubjectList =
                getResourceIds(fhirContext, qualifiedSubjectIdsCommonToPopulation, groupDef, populationDef);
        }

        return new StratumPopulationDef(
            populationDef,
            qualifiedSubjectIdsCommonToPopulation,
            populationDefEvaluationResultIntersection,
            resourceIdsForSubjectList,
            stratifierDef.getStratifierType(),
            groupDef.getPopulationBasis());
    }

    private static MeasureObservationStratumCache buildMeasureObservationCacheIfApplicable(
        GroupDef groupDef, List<StratumPopulationDef> stratumPopulations) {

        if (!groupDef.hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
            return null;
        }

        List<PopulationDef> measureObservationPopulationDefs =
            groupDef.getPopulationDefs(MeasurePopulationType.MEASUREOBSERVATION);

        if (measureObservationPopulationDefs.isEmpty()) {
            return null;
        }

        PopulationDef numObsPopDef = groupDef.findRatioObservationPopulationDef(
            measureObservationPopulationDefs, MeasurePopulationType.NUMERATOR);
        PopulationDef denObsPopDef = groupDef.findRatioObservationPopulationDef(
            measureObservationPopulationDefs, MeasurePopulationType.DENOMINATOR);

        if (numObsPopDef == null || denObsPopDef == null) {
            return null;
        }

        StratumPopulationDef numObsStratumPop = stratumPopulations.stream()
            .filter(sp -> sp.populationDef() == numObsPopDef)
            .findFirst()
            .orElse(null);

        StratumPopulationDef denObsStratumPop = stratumPopulations.stream()
            .filter(sp -> sp.populationDef() == denObsPopDef)
            .findFirst()
            .orElse(null);

        if (numObsStratumPop == null || denObsStratumPop == null) {
            return null;
        }

        return new MeasureObservationStratumCache(numObsStratumPop, denObsStratumPop);
    }

    // ---------------------------------------------------------------------
    // COMPONENT STRATIFIERS (refactored table structure)
    // ---------------------------------------------------------------------

    private static List<StratumDef> componentStratumPlural(
        FhirContext fhirContext,
        StratifierDef stratifierDef,
        List<PopulationDef> populationDefs,
        GroupDef groupDef) {

        // Unified table:
        // rowKey = RowKey(subjectQualified, criteriaKey?)  (criteriaKey is null for subject-basis)
        // colKey = StratifierComponentDef
        // value  = StratumValueWrapper(displayValue)
        final Table<RowKey, StratifierComponentDef, StratumValueWrapper> table =
            buildUnifiedComponentResultsTable(stratifierDef.components());

        // Group rows by the Set<StratumValueDef> combination (across components)
        final Map<Set<StratumValueDef>, List<RowKey>> rowsByValueDefSet = groupRowsByValueDefSet(table);

        var stratumDefs = new ArrayList<StratumDef>();

        rowsByValueDefSet.forEach((valueSet, rowKeys) -> {
            // StratumDef still expects subjectIds; collapse RowKey -> distinct subjects
            List<String> subjects = rowKeys.stream()
                .map(RowKey::subjectQualified)
                .distinct()
                .toList();

            var stratumDef = buildStratumDef(fhirContext, stratifierDef, valueSet, subjects, populationDefs, groupDef);
            stratumDefs.add(stratumDef);
        });

        return stratumDefs;
    }

    /**
     * Builds a unified table for component stratifiers.
     *
     * SUBJECT-BASIS:
     *   subject -> scalar
     *   RowKey(subjectQualified, null) => displayValue
     *
     * NON-SUBJECT / FUNCTION RESULTS:
     *   subject -> Map<inputParam, producedValue>
     *   RowKey(subjectQualified, normalizeKey(inputParam)) => displayValue(producedValue)
     *
     * NOTE: displayValue is what eventually becomes MeasureReport...stratum.component.text (via wrappers/defs).
     *       criteriaKey is only used for alignment/intersection logic.
     */
    private static Table<RowKey, StratifierComponentDef, StratumValueWrapper> buildUnifiedComponentResultsTable(
        List<StratifierComponentDef> componentDefs) {

        final Table<RowKey, StratifierComponentDef, StratumValueWrapper> t = HashBasedTable.create();

        componentDefs.forEach(componentDef ->
            componentDef.getResults().forEach((subject, criteriaResult) -> {

                String subjectQualified = FhirResourceUtils.addPatientQualifier(subject);
                Object raw = criteriaResult == null ? null : criteriaResult.rawValue();

                // Non-subject / function results: Map<inputParam, producedValue>
                if (raw instanceof Map<?, ?> m) {
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        String criteriaKey = normalizeKey(e.getKey()); // pop-basis key used for alignment
                        RowKey rk = new RowKey(subjectQualified, criteriaKey);

                        // Produced value is what we display (NOT the input parameter)
                        StratumValueWrapper wrapper = new StratumValueWrapper(e.getValue());
                        t.put(rk, componentDef, wrapper);
                    }
                    return;
                }

                // Subject-basis result
                RowKey rk = new RowKey(subjectQualified, null);
                t.put(rk, componentDef, new StratumValueWrapper(raw));
            }));

        return t;
    }

    /**
     * Group RowKeys by the set of value defs present across components.
     */
    private static Map<Set<StratumValueDef>, List<RowKey>> groupRowsByValueDefSet(
        Table<RowKey, StratifierComponentDef, StratumValueWrapper> table) {

        final Map<RowKey, Set<StratumValueDef>> rowToValueDefs = new HashMap<>();

        for (Table.Cell<RowKey, StratifierComponentDef, StratumValueWrapper> cell : table.cellSet()) {
            RowKey rowKey = cell.getRowKey();
            StratifierComponentDef componentDef = cell.getColumnKey();
            StratumValueWrapper wrapper = cell.getValue();

            rowToValueDefs
                .computeIfAbsent(rowKey, k -> new HashSet<>())
                .add(new StratumValueDef(wrapper, componentDef));
        }

        return rowToValueDefs.entrySet().stream()
            .collect(Collectors.groupingBy(
                Map.Entry::getValue,
                Collector.of(
                    ArrayList::new,
                    (list, e) -> list.add(e.getKey()),
                    (l1, l2) -> {
                        l1.addAll(l2);
                        return l1;
                    })));
    }

    // ---------------------------------------------------------------------
    // NON-COMPONENT STRATIFIERS (unchanged, but left here)
    // ---------------------------------------------------------------------

    private static List<StratumDef> nonComponentStratumPlural(
        FhirContext fhirContext,
        StratifierDef stratifierDef,
        List<PopulationDef> populationDefs,
        GroupDef groupDef) {

        final Map<String, CriteriaResult> subjectValues = stratifierDef.getResults();

        if (stratifierDef.isCriteriaStratifier()) {
            var stratValues = Set.<StratumValueDef>of();
            var patients = List.<String>of();
            var stratum = buildStratumDef(fhirContext, stratifierDef, stratValues, patients, populationDefs, groupDef);
            return List.of(stratum);
        }

        Map<StratumValueWrapper, List<String>> subjectsByValue = subjectValues.keySet().stream()
            .collect(Collectors.groupingBy(x -> new StratumValueWrapper(subjectValues.get(x).rawValue())));

        var stratumMultiple = new ArrayList<StratumDef>();

        for (Map.Entry<StratumValueWrapper, List<String>> stratValue : subjectsByValue.entrySet()) {
            var patientsSubjects = stratValue.getValue().stream()
                .map(FhirResourceUtils::addPatientQualifier)
                .toList();

            Set<StratumValueDef> stratValues = Set.of(new StratumValueDef(stratValue.getKey(), null));

            var stratum = buildStratumDef(
                fhirContext, stratifierDef, stratValues, patientsSubjects, populationDefs, groupDef);

            stratumMultiple.add(stratum);
        }

        return stratumMultiple;
    }

    // ---------------------------------------------------------------------
    // CRITERIA STRATIFIER INTERSECTION RULES (separated + explicit)
    // ---------------------------------------------------------------------

    private static boolean hasInputParameterFunctionResults(StratifierDef stratifierDef) {
        if (stratifierDef == null) return false;
        Map<String, CriteriaResult> results = stratifierDef.getResults();
        if (results == null || results.isEmpty()) return false;
        return results.values().stream()
            .map(CriteriaResult::rawValue)
            .anyMatch(v -> v instanceof Map<?, ?>);
    }

    /**
     * Subject-key intersection (works for boolean basis + legacy cases).
     */
    private static Set<Object> calculateCriteriaStratifierIntersectionBySubject(
        StratifierDef stratifierDef, PopulationDef populationDef) {

        final Map<String, CriteriaResult> stratifierResultsBySubject = stratifierDef.getResults();
        final List<Object> all = new ArrayList<>();

        for (Entry<String, CriteriaResult> e : stratifierResultsBySubject.entrySet()) {
            final Set<Object> stratifierSet = e.getValue() != null ? e.getValue().valueAsSet() : Set.of();
            final Set<Object> popSet = populationDef.getResourcesForSubject(e.getKey());
            if (popSet == null || popSet.isEmpty()) continue;

            all.addAll(Sets.intersection(popSet, stratifierSet));
        }

        return new HashSetForFhirResourcesAndCqlTypes<>(all);
    }

    /**
     * Input-parameter-key intersection for non-subject criteria stratifiers:
     *
     * Stratifier results per subject are Map<inputParam, producedValue>.
     * We intersect population results against Map KEYS (inputParam) to count membership.
     *
     * IMPORTANT: The producedValue is display-only and must NOT drive population counting.
     */
    private static Set<Object> calculateCriteriaStratifierIntersectionByInputParameter(
        StratifierDef stratifierDef, PopulationDef populationDef) {

        final Map<String, CriteriaResult> stratifierResultsBySubject = stratifierDef.getResults();
        final List<Object> all = new ArrayList<>();

        for (Entry<String, CriteriaResult> entry : stratifierResultsBySubject.entrySet()) {
            final String subjectId = entry.getKey();
            final CriteriaResult crit = entry.getValue();
            final Object raw = crit == null ? null : crit.rawValue();

            if (!(raw instanceof Map<?, ?> functionResults)) {
                continue;
            }

            final Set<Object> popSet = populationDef.getResourcesForSubject(subjectId);
            if (popSet == null || popSet.isEmpty()) {
                continue;
            }

            // normalize population objects to comparable keys
            final Map<String, Object> popKeyToObj = new HashMap<>();
            for (Object popObj : popSet) {
                String k = normalizeKey(popObj);
                if (k != null) popKeyToObj.put(k, popObj);
            }

            // intersect using Map keys
            for (Object inputParam : functionResults.keySet()) {
                String k = normalizeKey(inputParam);
                if (k == null) continue;

                Object matched = popKeyToObj.get(k);
                if (matched != null) {
                    all.add(matched);
                }
            }
        }

        return new HashSetForFhirResourcesAndCqlTypes<>(all);
    }

    // ---------------------------------------------------------------------
    // UTILITIES
    // ---------------------------------------------------------------------

    /**
     * Normalize keys to allow comparing:
     * - Resources -> "ResourceType/id" (versionless)
     * - everything else -> toString()
     */
    private static String normalizeKey(Object o) {
        if (o == null) return null;

        if (o instanceof IBaseResource r) {
            if (r.getIdElement() == null || r.getIdElement().isEmpty()) return null;
            return r.getIdElement().toVersionless().getValueAsString();
        }
        return o.toString();
    }

    @Nonnull
    private static List<String> getResourceIds(
        FhirContext fhirContext, Collection<String> subjectIds, GroupDef groupDef, PopulationDef populationDef) {
        final String resourceType = FhirResourceUtils.determineFhirResourceTypeOrNull(fhirContext, groupDef);

        boolean isResourceType = resourceType != null;
        List<String> resourceIds = new ArrayList<>();
        if (populationDef.getSubjectResources() != null) {
            for (String subjectId : subjectIds) {
                Set<Object> resources;
                if (!populationDef.type().equals(MeasurePopulationType.MEASUREOBSERVATION)) {
                    resources = populationDef
                        .getSubjectResources()
                        .get(FhirResourceUtils.stripAnyResourceQualifier(subjectId));
                } else {
                    resources = extractResourceIds(populationDef, subjectId);
                }
                if (resources != null) {
                    if (isResourceType) {
                        resourceIds.addAll(resources.stream()
                            .map(MeasureMultiSubjectEvaluator::getPopulationResourceIds)
                            .toList());
                    } else {
                        resourceIds.addAll(resources.stream().map(Object::toString).toList());
                    }
                }
            }
        }
        return resourceIds;
    }

    private static Set<Object> extractResourceIds(PopulationDef populationDef, String subjectId) {
        if (populationDef == null || populationDef.getSubjectResources() == null) {
            return Set.of();
        }
        String[] parts = subjectId.split("/");
        String id = parts[1];

        var filtered = populationDef.getSubjectResources().entrySet().stream()
            .filter(entry -> entry.getKey().equals(id))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return filtered.values().stream()
            .flatMap(Set::stream)
            .filter(Map.class::isInstance)
            .map(m -> (Map<?, ?>) m)
            .flatMap(m -> m.keySet().stream())
            .collect(Collectors.toSet());
    }

    private static String getPopulationResourceIds(Object resourceObject) {
        if (resourceObject instanceof IBaseResource resource) {
            return resource.getIdElement().toVersionless().getValueAsString();
        }
        return null;
    }
}