package org.opencds.cqf.fhir.cr.measure.common;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4ResourceIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// LUKETODO:  javadoc
public class MeasureMultiSubjectEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(MeasureMultiSubjectEvaluator.class);

    /**
     * Take the accumulated subject-by-subject evaluation results and use it to build StratumDefs
     * and StratumPopulationDefs
     *
     * @param measureDef to mutate post-evaluation with results of initial stratifier
     *                   subject-by-subject accumulations.
     *
     */
    public static void postEvaluationMultiSubject(MeasureDef measureDef) {

        for (GroupDef groupDef : measureDef.groups()) {
            for (StratifierDef stratifierDef : groupDef.stratifiers()) {
                final List<StratumDef> stratumDefs;

                if (stratifierDef.isComponentStratifier()) {
                    stratumDefs = componentStratumPlural(stratifierDef, groupDef.populations());
                } else {
                    stratumDefs = nonComponentStratumPlural(stratifierDef, groupDef.populations());
                }

                stratifierDef.addAllStratum(stratumDefs);
            }
        }
    }

    private static StratumDef buildStratumDef(
            StratifierDef stratifierDef,
            Set<StratumValueDef> values,
            List<String> subjectIds,
            List<PopulationDef> populationDefs) {

        boolean isComponent = values.size() > 1;
        String stratumText = null;

        for (StratumValueDef valuePair : values) {
            StratumValueWrapper value = valuePair.value();
            var componentDef = valuePair.def();
            // Set Stratum value to indicate which value is displaying results
            // ex. for Gender stratifier, code 'Male'
            if (value.getValueClass().equals(CodeableConcept.class)) {
                if (isComponent) {
                    // component stratifier example: code: "gender", value: 'M'
                    // value being stratified: 'M'
                    stratumText = componentDef.code().text();
                } else {
                    // non-component stratifiers only set stratified value, code is set on stratifier object
                    // value being stratified: 'M'
                    if (value.getValue() instanceof CodeableConcept codeableConcept) {
                        stratumText = codeableConcept.getText();
                    }
                }
            } else if (isComponent) {
                stratumText = expressionResultToCodableConcept(value).getText();
            } else if (MeasureStratifierType.VALUE == stratifierDef.getStratifierType()) {
                // non-component stratifiers only set stratified value, code is set on stratifier object
                // value being stratified: 'M'
                stratumText = expressionResultToCodableConcept(value).getText();
            }
        }

        return new StratumDef(
                stratumText,
                populationDefs.stream()
                        .map(popDef -> buildStratumPopulationDef(popDef, subjectIds))
                        .toList(),
                values,
                subjectIds);
    }

    private static StratumPopulationDef buildStratumPopulationDef(
            PopulationDef populationDef, List<String> subjectIds) {
        // population subjectIds
        var popSubjectIds = populationDef.getSubjects().stream()
                .map(R4ResourceIdUtils::addPatientQualifier)
                .collect(Collectors.toUnmodifiableSet());
        // intersect stratum subjectIds and population subjectIds
        var qualifiedSubjectIdsCommonToPopulation = Sets.intersection(new HashSet<>(subjectIds), popSubjectIds);

        return new StratumPopulationDef(populationDef.id(), qualifiedSubjectIdsCommonToPopulation);
    }

    private static List<StratumDef> componentStratumPlural(
            StratifierDef stratifierDef, List<PopulationDef> populationDefs) {

        final Table<String, StratumValueWrapper, StratifierComponentDef> subjectResultTable =
                buildSubjectResultsTable(stratifierDef.components());

        // Stratifiers should be of the same basis as population
        // Split subjects by result values
        // ex. all Male Patients and all Female Patients

        var componentSubjects = groupSubjectsByValueDefSet(subjectResultTable);

        var stratumDefs = new ArrayList<StratumDef>();

        componentSubjects.forEach((valueSet, subjects) -> {
            // converts table into component value combinations
            // | Stratum   | Set<ValueDef>           | List<Subjects(String)> |
            // | --------- | ----------------------- | ---------------------- |
            // | Stratum-1 | <'M','White>            | [subject-a]            |
            // | Stratum-2 | <'F','hispanic/latino'> | [subject-b]            |
            // | Stratum-3 | <'M','hispanic/latino'> | [subject-c]            |
            // | Stratum-4 | <'F','black'>           | [subject-d, subject-e] |

            var stratumDef = buildStratumDef(stratifierDef, valueSet, subjects, populationDefs);

            stratumDefs.add(stratumDef);
        });

        return stratumDefs;
    }

    private static List<StratumDef> nonComponentStratumPlural(
            StratifierDef stratifierDef, List<PopulationDef> populationDefs) {
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
            var stratValues = Set.<StratumValueDef>of();
            // Seems to be irrelevant for criteria based stratifiers
            var patients = List.<String>of();

            var stratum = buildStratumDef(stratifierDef, stratValues, patients, populationDefs);
            return List.of(stratum);
        }

        Map<StratumValueWrapper, List<String>> subjectsByValue = subjectValues.keySet().stream()
                .collect(Collectors.groupingBy(
                        x -> new StratumValueWrapper(subjectValues.get(x).rawValue())));

        var stratumMultiple = new ArrayList<StratumDef>();

        // Stratum 1
        // Value: 'M'--> subjects: subject1
        // Stratum 2
        // Value: 'F'--> subjects: subject2
        // loop through each value key
        for (Map.Entry<StratumValueWrapper, List<String>> stratValue : subjectsByValue.entrySet()) {
            // patch Patient values with prefix of ResourceType to match with incoming population subjects for stratum
            // TODO: should match context of CQL, not only Patient
            var patientsSubjects = stratValue.getValue().stream()
                    .map(R4ResourceIdUtils::addPatientQualifier)
                    .toList();
            // build the stratum for each unique value
            // non-component stratifiers will populate a 'null' for componentStratifierDef, since it doesn't have
            // multiple criteria
            // TODO: build out nonComponent stratum method
            Set<StratumValueDef> stratValues = Set.of(new StratumValueDef(stratValue.getKey(), null));
            var stratum = buildStratumDef(stratifierDef, stratValues, patientsSubjects, populationDefs);
            stratumMultiple.add(stratum);
        }

        return stratumMultiple;
    }

    private static Table<String, StratumValueWrapper, StratifierComponentDef> buildSubjectResultsTable(
            List<StratifierComponentDef> componentDefs) {

        final Table<String, StratumValueWrapper, StratifierComponentDef> subjectResultTable = HashBasedTable.create();

        // Component Stratifier
        // one or more criteria expression defined, one set of criteria results per component specified
        // results of component stratifier are an intersection of membership to both component result sets

        componentDefs.forEach(componentDef -> componentDef.getResults().forEach((subject, result) -> {
            StratumValueWrapper stratumValueWrapper = new StratumValueWrapper(result.rawValue());
            subjectResultTable.put(R4ResourceIdUtils.addPatientQualifier(subject), stratumValueWrapper, componentDef);
        }));

        return subjectResultTable;
    }

    private static Map<Set<StratumValueDef>, List<String>> groupSubjectsByValueDefSet(
            Table<String, StratumValueWrapper, StratifierComponentDef> table) {
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
        final Map<String, Set<StratumValueDef>> subjectToValueDefs = new HashMap<>();

        for (Table.Cell<String, StratumValueWrapper, StratifierComponentDef> cell : table.cellSet()) {
            subjectToValueDefs
                    .computeIfAbsent(cell.getRowKey(), k -> new HashSet<>())
                    .add(new StratumValueDef(cell.getColumnKey(), cell.getValue()));
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

    // This is weird pattern where we have multiple qualifying values within a single stratum,
    // which was previously unsupported.  So for now, comma-delim the first five values.
    private static CodeableConcept expressionResultToCodableConcept(StratumValueWrapper value) {
        return new CodeableConcept().setText(value.getValueAsString());
    }
}
