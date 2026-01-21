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
 *  - inputParam is used to align component results across components
 *  - producedValue is what becomes StratumValueWrapper (and eventually component.text)
 */
public class MeasureMultiSubjectEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(MeasureMultiSubjectEvaluator.class);

    /**
     * Take the accumulated subject-by-subject evaluation results and use it to build StratumDefs
     * and StratumPopulationDefs
     *
     * @param fhirContext  FHIR context for FHIR version used
     * @param measureDef  to mutate post-evaluation with results of initial stratifier
     *                    subject-by-subject accumulations.
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

        // Build all stratum populations
        List<StratumPopulationDef> stratumPopulations = populationDefs.stream()
            .map(popDef -> buildStratumPopulationDef(fhirContext, stratifierDef, popDef, subjectIds, groupDef))
            .toList();

        // Pre-compute measure observation cache if applicable
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

        // population subjectIds
        var popSubjectIds = populationDef.getSubjects().stream()
            .map(FhirResourceUtils::addPatientQualifier)
            .collect(Collectors.toUnmodifiableSet());

        // intersect stratum subjectIds and population subjectIds
        var qualifiedSubjectIdsCommonToPopulation = Sets.intersection(new HashSet<>(subjectIds), popSubjectIds);

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
            // For resource basis stratifiers, get resource IDs
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

    /**
     * Build measure observation cache if this measure has MEASUREOBSERVATION populations
     * linked to NUMERATOR and DENOMINATOR populations.
     */
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

    private static List<StratumDef> componentStratumPlural(
        FhirContext fhirContext,
        StratifierDef stratifierDef,
        List<PopulationDef> populationDefs,
        GroupDef groupDef) {

        final Table<String, StratumValueWrapper, StratifierComponentDef> subjectResultTable =
            buildSubjectResultsTable(stratifierDef.components());

        // Split subjects/rows by the Set<StratumValueDef> combination
        var componentSubjects = groupSubjectsByValueDefSet(subjectResultTable);

        var stratumDefs = new ArrayList<StratumDef>();

        componentSubjects.forEach((valueSet, subjectOrRows) -> {
            // If we used synthetic row keys "Patient/x|Encounter/y", collapse back to "Patient/x"
            List<String> subjects = subjectOrRows.stream()
                .map(MeasureMultiSubjectEvaluator::stripInputParamFromRowKey)
                .distinct()
                .toList();

            var stratumDef = buildStratumDef(fhirContext, stratifierDef, valueSet, subjects, populationDefs, groupDef);
            stratumDefs.add(stratumDef);
        });

        return stratumDefs;
    }

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
            .collect(Collectors.groupingBy(
                x -> new StratumValueWrapper(subjectValues.get(x).rawValue())));

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

    /**
     * Builds a table:
     *
     * SUBJECT-BASIS:
     *   rowKey = "Patient/123"
     *   columnKey = StratumValueWrapper(subject scalar result)
     *
     * FUNCTION / NON-SUBJECT RESULT (Map<inputParam, producedValue>):
     *   rowKey = "Patient/123|Encounter/1001"
     *   columnKey = StratumValueWrapper(producedValue)  <-- THIS becomes component.text
     */
    private static Table<String, StratumValueWrapper, StratifierComponentDef> buildSubjectResultsTable(
        List<StratifierComponentDef> componentDefs) {

        final Table<String, StratumValueWrapper, StratifierComponentDef> subjectResultTable = HashBasedTable.create();

        componentDefs.forEach(componentDef ->
            componentDef.getResults().forEach((subject, result) -> {

                String qualifiedSubject = FhirResourceUtils.addPatientQualifier(subject);

                Object raw = result == null ? null : result.rawValue();

                // NEW: Map<inputParam, producedValue>
                if (raw instanceof Map<?, ?> m) {
                    for (Map.Entry<?, ?> entry : m.entrySet()) {
                        String inputParamKey = inputParamRowKey(entry.getKey());
                        String rowKey = qualifiedSubject + "|" + inputParamKey;

                        // IMPORTANT: produced value becomes the stratum value (component.text)
                        StratumValueWrapper wrapper = new StratumValueWrapper(entry.getValue());
                        subjectResultTable.put(rowKey, wrapper, componentDef);
                    }
                    return;
                }

                // Existing: subject basis value stratifier
                StratumValueWrapper wrapper = new StratumValueWrapper(raw);
                subjectResultTable.put(qualifiedSubject, wrapper, componentDef);
            })
        );

        return subjectResultTable;
    }

    private static String inputParamRowKey(Object inputParam) {
        // Normalize to "ResourceType/id" when possible
        if (inputParam instanceof IBaseResource r && r.getIdElement() != null && !r.getIdElement().isEmpty()) {
            return r.getIdElement().toVersionless().getValue();
        }
        return String.valueOf(inputParam);
    }

    private static String stripInputParamFromRowKey(String rowKey) {
        int idx = rowKey.indexOf('|');
        return idx >= 0 ? rowKey.substring(0, idx) : rowKey;
    }

    private static Map<Set<StratumValueDef>, List<String>> groupSubjectsByValueDefSet(
        Table<String, StratumValueWrapper, StratifierComponentDef> table) {

        // Step 1: Build Map<RowKey, Set<ValueDef>>
        final Map<String, Set<StratumValueDef>> subjectToValueDefs = new HashMap<>();

        for (Table.Cell<String, StratumValueWrapper, StratifierComponentDef> cell : table.cellSet()) {
            subjectToValueDefs
                .computeIfAbsent(cell.getRowKey(), k -> new HashSet<>())
                .add(new StratumValueDef(cell.getColumnKey(), cell.getValue()));
        }

        // Step 2: Invert to Map<Set<ValueDef>, List<RowKey>>
        return subjectToValueDefs.entrySet().stream()
            .collect(Collectors.groupingBy(
                Map.Entry::getValue,
                Collector.of(ArrayList::new, (list, e) -> list.add(e.getKey()), (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                })));
    }

    /**
     * Criteria stratifier intersection:
     * - If stratifier result is Map<inputParam, producedValue>, intersect using map.keySet() (input params)
     * - Else, intersect using CriteriaResult.valueAsSet()
     */
    private static Set<Object> calculateCriteriaStratifierIntersection(
        StratifierDef stratifierDef, PopulationDef populationDef) {

        final Map<String, CriteriaResult> stratifierResultsBySubject = stratifierDef.getResults();
        final List<Object> allPopulationStratumIntersectingResources = new ArrayList<>();

        for (Entry<String, CriteriaResult> stratifierEntryBySubject : stratifierResultsBySubject.entrySet()) {

            final Set<Object> stratifierResultsPerSubject =
                criteriaResultAsIntersectionSet(stratifierEntryBySubject.getValue());

            final Set<Object> populationResultsPerSubject =
                populationDef.getResourcesForSubject(stratifierEntryBySubject.getKey());

            allPopulationStratumIntersectingResources.addAll(
                Sets.intersection(populationResultsPerSubject, stratifierResultsPerSubject));
        }

        return new HashSetForFhirResourcesAndCqlTypes<>(allPopulationStratumIntersectingResources);
    }

    private static Set<Object> criteriaResultAsIntersectionSet(CriteriaResult result) {
        if (result == null) {
            return Set.of();
        }
        Object raw = result.rawValue();
        if (raw instanceof Map<?, ?> m) {
            // IMPORTANT: keys are the resources / input params to intersect against population membership
            return m.keySet().stream().collect(Collectors.toSet());
        }
        return result.valueAsSet();
    }

    /**
     * Extract resource IDs from the population and subject IDs.
     */
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

    /**
     * MeasureObservation will store subjectResources as <subjectId,Set<Map<Object,Object>>>
     * We need the Key from the Set<Map<Key,Value>>.
     */
    private static Set<Object> extractResourceIds(PopulationDef populationDef, String subjectId) {
        if (populationDef == null || populationDef.getSubjectResources() == null) {
            return Set.of();
        }
        String[] parts = subjectId.split("/");

        String resourceType = parts[0]; // "Patient"
        String id = parts[1]; // "81230987"

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

    /**
     * Get resource ID from a resource object.
     */
    private static String getPopulationResourceIds(Object resourceObject) {
        if (resourceObject instanceof IBaseResource resource) {
            return resource.getIdElement().toVersionless().getValueAsString();
        }
        return null;
    }
}