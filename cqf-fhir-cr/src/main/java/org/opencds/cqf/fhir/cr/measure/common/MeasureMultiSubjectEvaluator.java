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
import org.opencds.cqf.fhir.cr.measure.common.def.report.GroupReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.MeasureReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.PopulationReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.StratifierComponentReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.StratifierReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.StratumPopulationReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.StratumReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.StratumValueReportDef;
import org.opencds.cqf.fhir.cr.measure.common.def.report.StratumValueWrapperReportDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregates individual subject evaluation results into stratum definitions for measure reporting across multiple subjects.
 */
public class MeasureMultiSubjectEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(MeasureMultiSubjectEvaluator.class);

    /**
     * Take the accumulated subject-by-subject evaluation results and use it to build StratumDefs
     * and StratumPopulationDefs
     *
     * @param fhirContext  FHIR context for FHIR version used
     * @param measureReportDef  to mutate post-evaluation with results of initial stratifier
     *                    subject-by-subject accumulations.
     *
     */
    public static void postEvaluationMultiSubject(FhirContext fhirContext, MeasureReportDef measureReportDef) {

        for (GroupReportDef groupDef : measureReportDef.groups()) {
            for (StratifierReportDef stratifierDef : groupDef.stratifiers()) {
                final List<StratumReportDef> stratumDefs;

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

    private static StratumReportDef buildStratumDef(
            FhirContext fhirContext,
            StratifierReportDef stratifierDef,
            Set<StratumValueReportDef> values,
            List<String> subjectIds,
            List<PopulationReportDef> populationDefs,
            GroupReportDef groupDef) {

        return new StratumReportDef(
                populationDefs.stream()
                        .map(popDef ->
                                buildStratumPopulationDef(fhirContext, stratifierDef, popDef, subjectIds, groupDef))
                        .toList(),
                values,
                subjectIds);
    }

    // Enhanced by Claude Sonnet 4.5 to calculate and populate all StratumPopulationDef fields
    private static StratumPopulationReportDef buildStratumPopulationDef(
            FhirContext fhirContext,
            StratifierReportDef stratifierDef,
            PopulationReportDef populationDef,
            List<String> subjectIds,
            GroupReportDef groupDef) {
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
            // For resource basis stratifiers, get resource IDs
            resourceIdsForSubjectList =
                    getResourceIds(fhirContext, qualifiedSubjectIdsCommonToPopulation, groupDef, populationDef);
        }

        return new StratumPopulationReportDef(
                populationDef,
                qualifiedSubjectIdsCommonToPopulation,
                populationDefEvaluationResultIntersection,
                resourceIdsForSubjectList,
                stratifierDef.getStratifierType(),
                groupDef.getPopulationBasis());
    }

    private static List<StratumReportDef> componentStratumPlural(
            FhirContext fhirContext,
            StratifierReportDef stratifierDef,
            List<PopulationReportDef> populationDefs,
            GroupReportDef groupDef) {

        final Table<String, StratumValueWrapperReportDef, StratifierComponentReportDef> subjectResultTable =
                buildSubjectResultsTable(stratifierDef.components());

        // Stratifiers should be of the same basis as population
        // Split subjects by result values
        // ex. all Male Patients and all Female Patients

        var componentSubjects = groupSubjectsByValueDefSet(subjectResultTable);

        var stratumDefs = new ArrayList<StratumReportDef>();

        componentSubjects.forEach((valueSet, subjects) -> {
            // converts table into component value combinations
            // | Stratum   | Set<ValueDef>           | List<Subjects(String)> |
            // | --------- | ----------------------- | ---------------------- |
            // | Stratum-1 | <'M','White>            | [subject-a]            |
            // | Stratum-2 | <'F','hispanic/latino'> | [subject-b]            |
            // | Stratum-3 | <'M','hispanic/latino'> | [subject-c]            |
            // | Stratum-4 | <'F','black'>           | [subject-d, subject-e] |

            var stratumDef = buildStratumDef(fhirContext, stratifierDef, valueSet, subjects, populationDefs, groupDef);

            stratumDefs.add(stratumDef);
        });

        return stratumDefs;
    }

    private static List<StratumReportDef> nonComponentStratumPlural(
            FhirContext fhirContext,
            StratifierReportDef stratifierDef,
            List<PopulationReportDef> populationDefs,
            GroupReportDef groupDef) {
        // standard Stratifier
        // one criteria expression defined, one set of criteria results

        // standard Stratifier
        // one criteria expression defined, one set of criteria results
        final Map<String, CriteriaResult> subjectValues = stratifierDef.getResults();

        // nonComponent stratifiers will have a single expression that can generate results, instead of grouping
        // combinations of results
        // example: 'gender' expression could produce values of 'M', 'F'
        // subject1: 'gender'--> 'M'
        // subject2: 'gender'--> 'F'
        // stratifier criteria results are: 'M', 'F'

        if (stratifierDef.isCriteriaStratifier()) {
            // Seems to be irrelevant for criteria based stratifiers
            var stratValues = Set.<StratumValueReportDef>of();
            // Seems to be irrelevant for criteria based stratifiers
            var patients = List.<String>of();

            var stratum = buildStratumDef(fhirContext, stratifierDef, stratValues, patients, populationDefs, groupDef);
            return List.of(stratum);
        }

        Map<StratumValueWrapperReportDef, List<String>> subjectsByValue = subjectValues.keySet().stream()
                .collect(Collectors.groupingBy(x ->
                        new StratumValueWrapperReportDef(subjectValues.get(x).rawValue())));

        var stratumMultiple = new ArrayList<StratumReportDef>();

        // Stratum 1
        // Value: 'M'--> subjects: subject1
        // Stratum 2
        // Value: 'F'--> subjects: subject2
        // loop through each value key
        for (Map.Entry<StratumValueWrapperReportDef, List<String>> stratValue : subjectsByValue.entrySet()) {
            // patch Patient values with prefix of ResourceType to match with incoming population subjects for stratum
            // TODO: should match context of CQL, not only Patient
            var patientsSubjects = stratValue.getValue().stream()
                    .map(FhirResourceUtils::addPatientQualifier)
                    .toList();
            // build the stratum for each unique value
            // non-component stratifiers will populate a 'null' for componentStratifierDef, since it doesn't have
            // multiple criteria
            // TODO: build out nonComponent stratum method
            Set<StratumValueReportDef> stratValues = Set.of(new StratumValueReportDef(stratValue.getKey(), null));
            var stratum = buildStratumDef(
                    fhirContext, stratifierDef, stratValues, patientsSubjects, populationDefs, groupDef);
            stratumMultiple.add(stratum);
        }

        return stratumMultiple;
    }

    private static Table<String, StratumValueWrapperReportDef, StratifierComponentReportDef> buildSubjectResultsTable(
            List<StratifierComponentReportDef> componentDefs) {

        final Table<String, StratumValueWrapperReportDef, StratifierComponentReportDef> subjectResultTable =
                HashBasedTable.create();

        // Component Stratifier
        // one or more criteria expression defined, one set of criteria results per component specified
        // results of component stratifier are an intersection of membership to both component result sets

        componentDefs.forEach(componentDef -> componentDef.getResults().forEach((subject, result) -> {
            StratumValueWrapperReportDef stratumValueWrapper = new StratumValueWrapperReportDef(result.rawValue());
            subjectResultTable.put(FhirResourceUtils.addPatientQualifier(subject), stratumValueWrapper, componentDef);
        }));

        return subjectResultTable;
    }

    private static Map<Set<StratumValueReportDef>, List<String>> groupSubjectsByValueDefSet(
            Table<String, StratumValueWrapperReportDef, StratifierComponentReportDef> table) {
        // input format
        // | Subject (String) | CriteriaResult (ValueWrapper) | StratifierComponentDef |
        // | ---------------- | ----------------------------- | ---------------------- |
        // | subject-a        | M                             | gender                 |
        // | subject-b        | F                             | gender                 |
        // | subject-c        | M                             | gender                 |
        // | subject-d        | F                             | gender                 |
        // | subject-e        | F                             | gender                 |
        // | subject-a        | white                         | race                   |
        // | subject-b        | hispanic/latino               | race                   |
        // | subject-c        | hispanic/latino               | race                   |
        // | subject-d        | black                         | race                   |
        // | subject-e        | black                         | race                   |

        // Step 1: Build Map<Subject, Set<ValueDef>>
        final Map<String, Set<StratumValueReportDef>> subjectToValueDefs = new HashMap<>();

        for (Table.Cell<String, StratumValueWrapperReportDef, StratifierComponentReportDef> cell : table.cellSet()) {
            subjectToValueDefs
                    .computeIfAbsent(cell.getRowKey(), k -> new HashSet<>())
                    .add(new StratumValueReportDef(cell.getColumnKey(), cell.getValue()));
        }
        // output format:
        // | Set<ValueDef>           | List<Subjects(String)> |
        // | ----------------------- | ---------------------- |
        // | <'M','White>            | [subject-a]            |
        // | <'F','hispanic/latino'> | [subject-b]            |
        // | <'M','hispanic/latino'> | [subject-c]            |
        // | <'F','black'>           | [subject-d, subject-e] |

        // Step 2: Invert to Map<Set<ValueDef>, List<Subject>>
        return subjectToValueDefs.entrySet().stream()
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
     * Example:
     * *population*:
     *
     * patient2
     *     Encounter/enc_in_progress_pat2_1
     *     Encounter/enc_triaged_pat2_1
     *     Encounter/enc_planned_pat2_1
     *     Encounter/enc_in_progress_pat2_2
     *     Encounter/enc_finished_pat2_1
     *
     *  patient1
     *     Encounter/enc_triaged_pat1_1
     *     Encounter/enc_planned_pat1_1
     *     Encounter/enc_in_progress_pat1_1
     *     Encounter/enc_finished_pat1_1
     *
     * *stratifier*:
     *   patient2
     *       Encounter/enc_in_progress_pat2_2
     *       Encounter/enc_in_progress_pat2_1
     *
     *   patient1
     *       Encounter/enc_in_progress_pat1_1
     *
     *   patient2:  intersection:   enc_in_progress_pat2_2, enc_in_progress_pat2_1
     *   patient1:  intersection:   enc_in_progress_pat1_1
     *
     *   result:
     *
     *   enc_in_progress_pat2_2, enc_in_progress_pat2_1, enc_in_progress_pat1_1
     *
     *   count: 3
     */
    // Moved from R4StratifierBuilder by Claude Sonnet 4.5
    private static Set<Object> calculateCriteriaStratifierIntersection(
            StratifierReportDef stratifierDef, PopulationReportDef populationDef) {

        final Map<String, CriteriaResult> stratifierResultsBySubject = stratifierDef.getResults();
        final List<Object> allPopulationStratumIntersectingResources = new ArrayList<>();

        // For each subject, we intersect between the population and stratifier results
        for (Entry<String, CriteriaResult> stratifierEntryBySubject : stratifierResultsBySubject.entrySet()) {
            final Set<Object> stratifierResultsPerSubject =
                    stratifierEntryBySubject.getValue().valueAsSet();
            final Set<Object> populationResultsPerSubject =
                    populationDef.getResourcesForSubject(stratifierEntryBySubject.getKey());

            allPopulationStratumIntersectingResources.addAll(
                    Sets.intersection(populationResultsPerSubject, stratifierResultsPerSubject));
        }

        // We add up all the results of the intersections here:
        return new HashSetForFhirResourcesAndCqlTypes<>(allPopulationStratumIntersectingResources);
    }

    /**
     * Extract resource IDs from the population and subject IDs.
     */
    // Moved from R4StratifierBuilder by Claude Sonnet 4.5
    @Nonnull
    private static List<String> getResourceIds(
            FhirContext fhirContext,
            Collection<String> subjectIds,
            GroupReportDef groupDef,
            PopulationReportDef populationDef) {
        final String resourceType = FhirResourceUtils.determineFhirResourceTypeOrNull(fhirContext, groupDef);

        // only ResourceType fhirType should return true here
        boolean isResourceType = resourceType != null;
        List<String> resourceIds = new ArrayList<>();
        if (populationDef.getSubjectResources() != null) {
            for (String subjectId : subjectIds) {
                Set<Object> resources;
                if (!populationDef.type().equals(MeasurePopulationType.MEASUREOBSERVATION)) {
                    // retrieve criteria results by subject Key
                    resources = populationDef
                            .getSubjectResources()
                            .get(FhirResourceUtils.stripAnyResourceQualifier(subjectId));
                } else {
                    // MeasureObservation will store subjectResources as <subjectId,Set<Map<Object,Object>>
                    // We need the Key from the Set<Map<Key,Value>>
                    // This can be any FHIRType: Resource, date, integer, string....etc
                    resources = extractResourceIds(populationDef, subjectId);
                }
                if (resources != null) {
                    if (isResourceType) {
                        resourceIds.addAll(resources.stream()
                                .map(MeasureMultiSubjectEvaluator::getPopulationResourceIds) // get resource id
                                .toList());
                    } else {
                        resourceIds.addAll(
                                resources.stream().map(Object::toString).toList());
                    }
                }
            }
        }
        return resourceIds;
    }

    /**
     * Extracts unique FHIR identifiers as Strings from a PopulationDef.
     * Works for Resource, Reference, IdType, PrimitiveType, String, Number, etc.
     */
    // Moved from R4StratifierBuilder by Claude Sonnet 4.5
    private static Set<Object> extractResourceIds(PopulationReportDef populationDef, String subjectId) {
        if (populationDef == null || populationDef.getSubjectResources() == null) {
            return Set.of();
        }
        String[] parts = subjectId.split("/");

        String resourceType = parts[0]; // "Patient"
        String id = parts[1]; // "81230987"

        var filtered = populationDef.getSubjectResources().entrySet().stream()
                .filter(entry -> entry.getKey().equals(id)) // <--- filter on key
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return filtered.values().stream()
                .flatMap(Set::stream)
                .filter(Map.class::isInstance) // Keep only Map<?,?>
                .map(m -> (Map<?, ?>) m) // Cast
                .flatMap(m -> m.keySet().stream()) // capture Key only, not Qty
                .collect(Collectors.toSet());
    }

    /**
     * Get resource ID from a resource object.
     */
    // Moved from R4StratifierBuilder by Claude Sonnet 4.5
    private static String getPopulationResourceIds(Object resourceObject) {
        if (resourceObject instanceof IBaseResource resource) {
            return resource.getIdElement().toVersionless().getValueAsString();
        }
        return null;
    }
}
