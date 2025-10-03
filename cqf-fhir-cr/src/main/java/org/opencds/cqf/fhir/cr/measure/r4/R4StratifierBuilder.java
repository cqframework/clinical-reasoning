package org.opencds.cqf.fhir.cr.measure.r4;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponentComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.measure.common.CriteriaResult;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierComponentDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierUtils;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureReportBuilder.BuilderContext;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureReportBuilder.ValueWrapper;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4ResourceIdUtils;

/**
 * Convenience class with functionality split out from {@link R4MeasureReportBuilder } to
 * handle stratifiers
 */
@SuppressWarnings("squid:S1135")
class R4StratifierBuilder {

    static void buildStratifier(
            BuilderContext bc,
            MeasureGroupStratifierComponent measureStratifier,
            MeasureReportGroupStratifierComponent reportStratifier,
            StratifierDef stratifierDef,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef) {
        // the top level stratifier 'id' and 'code'
        reportStratifier.setCode(getCodeForReportStratifier(groupDef, stratifierDef, measureStratifier));
        reportStratifier.setId(measureStratifier.getId());
        // if description is defined, add to MeasureReport
        if (measureStratifier.hasDescription()) {
            reportStratifier.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL,
                    new StringType(measureStratifier.getDescription()));
        }

        if (!stratifierDef.components().isEmpty()) {

            Table<String, ValueWrapper, StratifierComponentDef> subjectResultTable = HashBasedTable.create();

            // Component Stratifier
            // one or more criteria expression defined, one set of criteria results per component specified
            // results of component stratifier are an intersection of membership to both component result sets

            stratifierDef
                    .components()
                    .forEach(component -> component.getResults().forEach((subject, result) -> {
                        ValueWrapper valueWrapper = new ValueWrapper(result.rawValue());
                        subjectResultTable.put(R4ResourceIdUtils.addPatientQualifier(subject), valueWrapper, component);
                    }));

            // Stratifiers should be of the same basis as population
            // Split subjects by result values
            // ex. all Male Patients and all Female Patients
            componentStratifier(bc, stratifierDef, reportStratifier, populations, groupDef, subjectResultTable);

        } else {
            // standard Stratifier
            // one criteria expression defined, one set of criteria results
            Map<String, CriteriaResult> subjectValues = stratifierDef.getResults();
            nonComponentStratifier(bc, stratifierDef, reportStratifier, populations, groupDef, subjectValues);
        }
    }

    private static void componentStratifier(
            BuilderContext bc,
            StratifierDef stratifierDef,
            MeasureReportGroupStratifierComponent reportStratifier,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef,
            Table<String, ValueWrapper, StratifierComponentDef> subjectCompValues) {

        var componentSubjects = groupSubjectsByValueDefSet(subjectCompValues);

        componentSubjects.forEach((valueSet, subjects) -> {
            // converts table into component value combinations
            // | Stratum   | Set<ValueDef>           | List<Subjects(String)> |
            // | --------- | ----------------------- | ---------------------- |
            // | Stratum-1 | <'M','White>            | [subject-a]            |
            // | Stratum-2 | <'F','hispanic/latino'> | [subject-b]            |
            // | Stratum-3 | <'M','hispanic/latino'> | [subject-c]            |
            // | Stratum-4 | <'F','black'>           | [subject-d, subject-e] |

            var reportStratum = reportStratifier.addStratum();
            buildStratum(bc, stratifierDef, reportStratum, valueSet, subjects, populations, groupDef);
        });
    }

    private static void nonComponentStratifier(
            BuilderContext bc,
            StratifierDef stratifierDef,
            MeasureReportGroupStratifierComponent reportStratifier,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef,
            Map<String, CriteriaResult> subjectValues) {
        // nonComponent stratifiers will have a single expression that can generate results, instead of grouping
        // combinations of results
        // example: 'gender' expression could produce values of 'M', 'F'
        // subject1: 'gender'--> 'M'
        // subject2: 'gender'--> 'F'
        // stratifier criteria results are: 'M', 'F'

        if (StratifierUtils.isCriteriaBasedStratifier(groupDef, stratifierDef)) {
            var reportStratum = reportStratifier.addStratum();
            // Seems to be irrelevant for criteria based stratifiers
            var stratValues = Set.<ValueDef>of();
            // Seems to be irrelevant for criteria based stratifiers
            var patients = List.<String>of();

            buildStratum(bc, stratifierDef, reportStratum, stratValues, patients, populations, groupDef);
            return;
        }

        Map<ValueWrapper, List<String>> subjectsByValue = subjectValues.keySet().stream()
                .collect(Collectors.groupingBy(
                        x -> new ValueWrapper(subjectValues.get(x).rawValue())));
        // Stratum 1
        // Value: 'M'--> subjects: subject1
        // Stratum 2
        // Value: 'F'--> subjects: subject2
        // loop through each value key
        for (Map.Entry<ValueWrapper, List<String>> stratValue : subjectsByValue.entrySet()) {
            var reportStratum = reportStratifier.addStratum();
            // patch Patient values with prefix of ResourceType to match with incoming population subjects for stratum
            // TODO: should match context of CQL, not only Patient
            var patients = stratValue.getValue().stream()
                    .map(R4ResourceIdUtils::addPatientQualifier)
                    .toList();
            // build the stratum for each unique value
            // non-component stratifiers will populate a 'null' for componentStratifierDef, since it doesn't have
            // multiple criteria
            // TODO: build out nonComponent stratum method
            Set<ValueDef> stratValues = Set.of(new ValueDef(stratValue.getKey(), null));
            buildStratum(bc, stratifierDef, reportStratum, stratValues, patients, populations, groupDef);
        }
    }

    private static Map<Set<ValueDef>, List<String>> groupSubjectsByValueDefSet(
            Table<String, ValueWrapper, StratifierComponentDef> table) {
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
        Map<String, Set<ValueDef>> subjectToValueDefs = new HashMap<>();

        for (Table.Cell<String, ValueWrapper, StratifierComponentDef> cell : table.cellSet()) {
            subjectToValueDefs
                    .computeIfAbsent(cell.getRowKey(), k -> new HashSet<>())
                    .add(new ValueDef(cell.getColumnKey(), cell.getValue()));
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

    private static void buildStratum(
            BuilderContext bc,
            StratifierDef stratifierDef,
            StratifierGroupComponent stratum,
            Set<ValueDef> values,
            List<String> subjectIds,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef) {
        boolean isComponent = values.size() > 1;
        for (ValueDef valuePair : values) {
            ValueWrapper value = valuePair.value;
            var componentDef = valuePair.def;
            // Set Stratum value to indicate which value is displaying results
            // ex. for Gender stratifier, code 'Male'
            if (value.getValueClass().equals(CodeableConcept.class)) {
                if (isComponent) {
                    StratifierGroupComponentComponent sgcc = new StratifierGroupComponentComponent();
                    // component stratifier example: code: "gender", value: 'M'
                    // value being stratified: 'M'
                    sgcc.setValue(expressionResultToCodableConcept(value));
                    // code specified from componentDef: "gender"
                    sgcc.setCode(
                            new CodeableConcept().setText(componentDef.code().text()));
                    // set component on MeasureReport
                    stratum.addComponent(sgcc);
                } else {
                    // non-component stratifiers only set stratified value, code is set on stratifier object
                    // value being stratified: 'M'
                    stratum.setValue((CodeableConcept) value.getValue());
                }
            } else if (isComponent) {
                // component stratifier example: code: "gender", value: 'M'
                StratifierGroupComponentComponent sgcc = new StratifierGroupComponentComponent();
                // value being stratified: 'M'
                sgcc.setValue(expressionResultToCodableConcept(value));
                // code specified from componentDef: "gender"
                sgcc.setCode(new CodeableConcept().setText(componentDef.code().text()));
                // set component on MeasureReport
                stratum.addComponent(sgcc);
            } else if (!StratifierUtils.isCriteriaBasedStratifier(groupDef, stratifierDef)) {
                // non-component stratifiers only set stratified value, code is set on stratifier object
                // value being stratified: 'M'
                stratum.setValue(expressionResultToCodableConcept(value));
            }
        }

        // add stratum populations for stratifier
        // Group.populations
        // initial-population: subject1, subject 2
        // ** stratifier value: 'M'
        // ** subjects with stratifier value: 'M': subject1
        // ** stratum.population
        // ** ** initial-population: subject1
        // ** stratifier value: 'F'
        // ** subjects with stratifier value: 'F': subject2
        // ** stratum.population
        // ** ** initial-population: subject2
        for (MeasureGroupPopulationComponent mgpc : populations) {
            var stratumPopulation = stratum.addPopulation();
            buildStratumPopulation(bc, stratifierDef, stratumPopulation, subjectIds, mgpc, groupDef);
        }
    }

    // This is weird pattern where we have multiple qualifying values within a single stratum,
    // which was previously unsupported.  So for now, comma-delim the first five values.
    private static CodeableConcept expressionResultToCodableConcept(ValueWrapper value) {
        return new CodeableConcept().setText(value.getValueAsString());
    }

    private record ValueDef(ValueWrapper value, StratifierComponentDef def) {}

    private static void buildStratumPopulation(
            BuilderContext bc,
            StratifierDef stratifierDef,
            StratifierGroupPopulationComponent sgpc,
            List<String> subjectIds,
            MeasureGroupPopulationComponent population,
            GroupDef groupDef) {
        sgpc.setCode(population.getCode());
        sgpc.setId(population.getId());

        if (population.hasDescription()) {
            sgpc.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL, new StringType(population.getDescription()));
        }

        var populationDef = groupDef.populations().stream()
                .filter(t -> t.code()
                        .codes()
                        .get(0)
                        .code()
                        .equals(population.getCode().getCodingFirstRep().getCode()))
                .findFirst()
                .orElse(null);
        assert populationDef != null;
        if (groupDef.isBooleanBasis()) {
            buildBooleanBasisStratumPopulation(bc, sgpc, subjectIds, populationDef);
        } else {
            buildResourceBasisStratumPopulation(bc, stratifierDef, sgpc, subjectIds, populationDef, groupDef);
        }
    }

    private static void buildBooleanBasisStratumPopulation(
            BuilderContext bc,
            StratifierGroupPopulationComponent sgpc,
            List<String> subjectIds,
            PopulationDef populationDef) {
        var popSubjectIds = populationDef.getSubjects().stream()
                .map(R4ResourceIdUtils::addPatientQualifier)
                .toList();
        if (popSubjectIds.isEmpty()) {
            sgpc.setCount(0);
            return;
        }
        // intersect population subjects to stratifier.value subjects
        Set<String> intersection = new HashSet<>(subjectIds);
        intersection.retainAll(popSubjectIds);
        sgpc.setCount(intersection.size());

        // subject-list ListResource to match intersection of results
        if (!intersection.isEmpty()
                && bc.report().getType() == org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUBJECTLIST) {
            ListResource popSubjectList =
                    R4StratifierBuilder.createIdList(UUID.randomUUID().toString(), intersection);
            bc.addContained(popSubjectList);
            sgpc.setSubjectResults(new Reference("#" + popSubjectList.getId()));
        }
    }

    private static void buildResourceBasisStratumPopulation(
            BuilderContext bc,
            StratifierDef stratifierDef,
            StratifierGroupPopulationComponent sgpc,
            List<String> subjectIds,
            PopulationDef populationDef,
            GroupDef groupDef) {

        final List<String> resourceIds = getResourceIds(subjectIds, groupDef, populationDef);

        final int stratumCount = getStratumCountUpper(stratifierDef, groupDef, populationDef, resourceIds);

        sgpc.setCount(stratumCount);

        if (resourceIds.isEmpty()) {
            return;
        }

        // subject-list ListResource to match intersection of results
        if (bc.report().getType() == org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUBJECTLIST) {
            ListResource popSubjectList =
                    R4StratifierBuilder.createIdList(UUID.randomUUID().toString(), resourceIds);
            bc.addContained(popSubjectList);
            sgpc.setSubjectResults(new Reference("#" + popSubjectList.getId()));
        }
    }

    private static int getStratumCountUpper(
            StratifierDef stratifierDef, GroupDef groupDef, PopulationDef populationDef, List<String> resourceIds) {

        if (StratifierUtils.isCriteriaBasedStratifier(groupDef, stratifierDef)) {
            final Set<Object> resources = populationDef.getResources();
            final Set<Object> results = stratifierDef.getAllCriteriaResultValues();

            if (resources.isEmpty() || results.isEmpty()) {
                // There's no intersection, so no point in going further.
                return 0;
            }

            final Class<?> resourcesClassFirst = resources.iterator().next().getClass();
            final Class<?> resultClassFirst = results.iterator().next().getClass();

            // Sanity check: isCriteriaBasedStratifier() should have filtered this out
            if (resourcesClassFirst != resultClassFirst) {
                // Different classes, so no point in going further.
                return 0;
            }

            final SetView<Object> intersection = Sets.intersection(resources, results);
            return intersection.size();
        }

        if (resourceIds.isEmpty()) {
            return 0;
        }

        return resourceIds.size();
    }

    @Nonnull
    private static List<String> getResourceIds(
            List<String> subjectIds, GroupDef groupDef, PopulationDef populationDef) {
        String resourceType;
        try {
            // when this method is checked with a primitive value and not ResourceType it returns an error
            // this try/catch is to prevent the exception thrown from setting the correct value
            resourceType =
                    ResourceType.fromCode(groupDef.getPopulationBasis().code()).toString();
        } catch (FHIRException e) {
            resourceType = null;
        }

        // only ResourceType fhirType should return true here
        boolean isResourceType = resourceType != null;
        List<String> resourceIds = new ArrayList<>();
        assert populationDef != null;
        if (populationDef.getSubjectResources() != null) {
            for (String subjectId : subjectIds) {
                // retrieve criteria results by subject Key
                var resources =
                        populationDef.getSubjectResources().get(R4ResourceIdUtils.stripPatientQualifier(subjectId));
                if (resources != null) {
                    if (isResourceType) {
                        resourceIds.addAll(resources.stream()
                                .map(R4StratifierBuilder::getPopulationResourceIds) // get resource id
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

    protected static ListResource createIdList(String id, Collection<String> ids) {
        return createReferenceList(id, ids.stream().map(Reference::new).toList());
    }

    protected static ListResource createReferenceList(String id, Collection<Reference> references) {
        ListResource referenceList = R4MeasureReportBuilder.createList(id);
        for (Reference reference : references) {
            referenceList.addEntry().setItem(reference);
        }

        return referenceList;
    }

    protected static String getPopulationResourceIds(Object resourceObject) {
        if (resourceObject instanceof IBaseResource resource) {
            return resource.getIdElement().toVersionless().getValueAsString();
        }
        return null;
    }

    @Nonnull
    private static List<CodeableConcept> getCodeForReportStratifier(
            GroupDef groupDef, StratifierDef stratifierDef, MeasureGroupStratifierComponent measureStratifier) {

        final Expression criteria = measureStratifier.getCriteria();

        if (StratifierUtils.isCriteriaBasedStratifier(groupDef, stratifierDef)
                && criteria != null
                && criteria.hasLanguage()
                && "text/cql.identifier".equals(criteria.getLanguage())) {
            final CodeableConcept codableConcept = new CodeableConcept();
            codableConcept.setText(criteria.getExpression());
            return Collections.singletonList(codableConcept);
        }

        return Collections.singletonList(measureStratifier.getCode());
    }
}
