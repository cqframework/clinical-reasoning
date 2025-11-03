package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ResourceType;
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

                if (!stratifierDef.components().isEmpty()) {
                    stratumDefs = componentStratumPlural(
                            stratifierDef, groupDef.getPopulationBasis(), groupDef.populations());
                } else {
                    stratumDefs = nonComponentStratumPlural(
                            stratifierDef, groupDef.getPopulationBasis(), groupDef.populations());
                }

                stratifierDef.addAllStratum(stratumDefs);
            }
        }
    }

    private static StratumDef buildStratumDef(
            StratifierDef stratifierDef,
            Set<StratumValueDef> values,
            List<String> subjectIds,
            CodeDef populationBasis,
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
                        .map(popDef -> buildStratumPopulationDef(
                                stratifierDef.getStratifierType(),
                                stratifierDef.getResultsForComponentOrNonComponent(),
                                populationBasis,
                                popDef,
                                subjectIds))
                        .toList(),
                values,
                subjectIds);
    }

    private static StratumPopulationDef buildStratumPopulationDef(
            MeasureStratifierType measureStratifierType,
            Set<Object> evaluationResultsForStratifier,
            CodeDef groupPopulationBasis,
            PopulationDef populationDef,
            List<String> subjectIds) {

        var popSubjectIds = populationDef.getSubjects().stream()
                .map(R4ResourceIdUtils::addPatientQualifier)
                .collect(Collectors.toUnmodifiableSet());

        var qualifiedSubjectIdsCommonToPopulation = Sets.intersection(new HashSet<>(subjectIds), popSubjectIds);

        final Set<Object> populationDefEvaluationResultIntersection =
                getPopulationDefEvaluationResultIntersection(evaluationResultsForStratifier, populationDef);

        final String resourceType = getResourceType(groupPopulationBasis);

        final List<String> resourceIds = getResourceIds(resourceType, subjectIds, populationDef);

        logger.info("1234: resourceIds: {}", resourceIds);

        return new StratumPopulationDef(
                populationDef.id(),
                qualifiedSubjectIdsCommonToPopulation,
                populationDefEvaluationResultIntersection,
                resourceIds,
                measureStratifierType,
                resourceType);
    }

    private static List<StratumDef> componentStratumPlural(
            StratifierDef stratifierDef, CodeDef populationBasis, List<PopulationDef> populationDefs) {

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

            var stratumDef = buildStratumDef(stratifierDef, valueSet, subjects, populationBasis, populationDefs);

            stratumDefs.add(stratumDef);
        });

        return stratumDefs;
    }

    private static List<StratumDef> nonComponentStratumPlural(
            StratifierDef stratifierDef, CodeDef populationBasis, List<PopulationDef> populationDefs) {
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

        if (MeasureStratifierType.CRITERIA == stratifierDef.getStratifierType()) {
            // Seems to be irrelevant for criteria based stratifiers
            var stratValues = Set.<StratumValueDef>of();
            // Seems to be irrelevant for criteria based stratifiers
            var patients = List.<String>of();

            var stratum = buildStratumDef(stratifierDef, stratValues, patients, populationBasis, populationDefs);
            return List.of(stratum);
        }

        final Map<StratumValueWrapper, List<String>> subjectsByValue = subjectValues.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> entry.getValue().rawValue() != null)
                .collect(Collectors.groupingBy(
                        entry -> new StratumValueWrapper(entry.getValue().rawValue()),
                        Collectors.mapping(Entry::getKey, Collectors.toList())));

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
            var stratum =
                    buildStratumDef(stratifierDef, stratValues, patientsSubjects, populationBasis, populationDefs);
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

    private static Set<Object> getPopulationDefEvaluationResultIntersection(
            Set<Object> evaluationResults, PopulationDef populationDef) {

        final Set<Object> resources = populationDef.getResources();
        // LUKETODO:  for the component criteria scenario, we don't add the results directly to the stratifierDef,
        // but to each of the component defs, which is why this is empty
        if (resources.isEmpty() || evaluationResults.isEmpty()) {
            // There's no intersection, so no point in going further.
            return Set.of();
        }

        final Class<?> resourcesClassFirst = resources.iterator().next().getClass();
        final Class<?> resultClassFirst = evaluationResults.iterator().next().getClass();

        // Sanity check: isCriteriaBasedStratifier() should have filtered this out
        if (resourcesClassFirst != resultClassFirst) {
            // Different classes, so no point in going further.
            return Set.of();
        }

        return Sets.intersection(resources, evaluationResults);
    }

    @Nullable
    private static String getResourceType(CodeDef populationBasis) {
        try {
            // when this method is checked with a primitive value and not ResourceType it returns an error
            // this try/catch is to prevent the exception thrown from setting the correct value
            return ResourceType.fromCode(populationBasis.code()).toString();
        } catch (FHIRException e) {
            return null;
        }
    }

    // LUKETODO:  now that we have Quantities instead of Observations, this is sort of broken
    // since Quantities don't have version-agnostic ID representations
    @Nonnull
    private static List<String> getResourceIds(
            String resourceType, List<String> subjectIds, PopulationDef populationDef) {

        // only ResourceType fhirType should return true here
        boolean isResourceType = resourceType != null;
        List<String> resourceIds = new ArrayList<>();

        if (populationDef == null) {
            // LUKETODO:  enhance this message?
            throw new InternalErrorException("Population definition has not been set");
        }

        if (populationDef.getSubjectResources() != null) {
            for (String subjectId : subjectIds) {
                // retrieve criteria results by subject Key
                var resources =
                        populationDef.getSubjectResources().get(R4ResourceIdUtils.stripPatientQualifier(subjectId));
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

    // LUKETODO:  so we need to count the number of resources here, which would be ID-less quantities
    // LUKETODO:  we get a null ID here because we get a Map.Entry here, which fails the checks here:
    // Map.Entry<Encounter, Quantity>
    private static String getPopulationResourceIds(Object resourceObject) {
        if (resourceObject instanceof IBaseResource resource) {
            return resource.getIdElement().toVersionless().getValueAsString();
        }
        // If this is not a resource, then included the type
        if (resourceObject instanceof IBase baseType) {
            return baseType.fhirType();
        }
        return null;
    }
}
