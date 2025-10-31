package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumPopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueWrapper;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureReportBuilder.BuilderContext;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4ResourceIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience class with functionality split out from {@link R4MeasureReportBuilder} to
 * handle stratifiers
 */
@SuppressWarnings({"squid:S1135", "squid:S107"})
class R4StratifierBuilder {
    private static final Logger logger = LoggerFactory.getLogger(R4StratifierBuilder.class);

    private R4StratifierBuilder() {
        // static class
    }

    static void buildStratifier(
            BuilderContext bc,
            MeasureGroupStratifierComponent measureStratifier,
            MeasureReportGroupStratifierComponent reportStratifier,
            StratifierDef stratifierDef,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef) {
        // the top level stratifier 'id' and 'code'
        reportStratifier.setCode(getCodeForReportStratifier(stratifierDef, measureStratifier));
        reportStratifier.setId(measureStratifier.getId());
        // if description is defined, add to MeasureReport
        if (measureStratifier.hasDescription()) {
            reportStratifier.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL,
                    new StringType(measureStratifier.getDescription()));
        }

        buildMultipleStratum(bc, reportStratifier, stratifierDef, populations, groupDef);
    }

    private static void buildMultipleStratum(
            BuilderContext bc,
            MeasureReportGroupStratifierComponent reportStratifier,
            StratifierDef stratifierDef,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef) {

        if (!stratifierDef.components().isEmpty()) {
            componentStratifier(bc, stratifierDef, reportStratifier, populations, groupDef);
        } else {
            nonComponentStratifier(bc, stratifierDef, reportStratifier, populations, groupDef);
        }
    }

    private static void componentStratifier(
            BuilderContext bc,
            StratifierDef stratifierDef,
            MeasureReportGroupStratifierComponent reportStratifier,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef) {

        stratifierDef.getStratum().forEach(stratumDef -> {
            var reportStratum = reportStratifier.addStratum();

            buildStratum(
                    bc,
                    stratifierDef,
                    stratumDef,
                    reportStratum,
                    stratumDef.getValueDefs(),
                    stratumDef.getSubjectIds(),
                    populations,
                    groupDef);
        });
    }

    private static void nonComponentStratifier(
            BuilderContext bc,
            StratifierDef stratifierDef,
            MeasureReportGroupStratifierComponent reportStratifier,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef) {

        // nonComponent stratifiers will have a single expression that can generate results, instead of grouping
        // combinations of results
        // example: 'gender' expression could produce values of 'M', 'F'
        // subject1: 'gender'--> 'M'
        // subject2: 'gender'--> 'F'
        // stratifier criteria results are: 'M', 'F'
        if (MeasureStratifierType.CRITERIA == stratifierDef.getStratifierType()) {
            var reportStratum = reportStratifier.addStratum();
            // Ideally, the stratum def should have these values empty in MeasureEvaluator
            // Seems to be irrelevant for criteria based stratifiers
            var stratValues = Set.<StratumValueDef>of();
            // Seems to be irrelevant for criteria based stratifiers
            var patients = List.<String>of();

            buildStratum(
                    bc,
                    stratifierDef,
                    getOnlyStratumDef(stratifierDef),
                    reportStratum,
                    stratValues,
                    patients,
                    populations,
                    groupDef);
            return; // short-circuit so we don't process non-criteria logic
        }

        // Stratum 1
        // Value: 'M'--> subjects: subject1
        // Stratum 2
        // Value: 'F'--> subjects: subject2
        // loop through each value key
        for (StratumDef stratumDef : stratifierDef.getStratum()) {
            buildStratumOuter(bc, stratifierDef, stratumDef, reportStratifier, populations, groupDef);
        }
    }

    private static void buildStratumOuter(
            BuilderContext bc,
            StratifierDef stratifierDef,
            StratumDef stratumDef,
            MeasureReportGroupStratifierComponent reportStratifier,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef) {

        var reportStratum = reportStratifier.addStratum();

        buildStratum(
                bc,
                stratifierDef,
                stratumDef,
                reportStratum,
                stratumDef.getValueDefs(),
                stratumDef.getSubjectIds(),
                populations,
                groupDef);
    }

    private static void buildStratum(
            BuilderContext bc,
            StratifierDef stratifierDef,
            StratumDef stratumDef,
            StratifierGroupComponent stratum,
            Set<StratumValueDef> values,
            List<String> subjectIds,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef) {
        boolean isComponent = values.size() > 1;
        for (StratumValueDef valuePair : values) {
            StratumValueWrapper value = valuePair.value();
            var componentDef = valuePair.def();
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
            } else if (MeasureStratifierType.VALUE == stratifierDef.getStratifierType()) {
                // non-component stratifiers only set stratified value, code is set on stratifier object
                // value being stratified: 'M'
                stratum.setValue(expressionResultToCodableConcept(value));
                //                stratumDefText = stratum.getValue().getText();
            } else if (MeasureStratifierType.CRITERIA == stratifierDef.getStratifierType()) {
                //                stratumDefText = value.getValueAsString();
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
        for (StratumPopulationDef stratumPopulationDef : stratumDef.getStratumPopulations()) {
            // This is nasty, and ideally, we ought to be driving this logic entirely off StratumPopulationDef
            final Optional<MeasureGroupPopulationComponent> optMgpc = populations.stream()
                    .filter(population -> population.getId().equals(stratumPopulationDef.getId()))
                    .findFirst();
            if (optMgpc.isEmpty()) {
                throw new InternalErrorException("could not find MeasureGroupPopulationComponent");
            }
            var stratumPopulation = stratum.addPopulation();
            buildStratumPopulation(
                    bc, stratifierDef, stratumPopulationDef, stratumPopulation, subjectIds, optMgpc.get(), groupDef);
        }
    }

    private static StratumDef getOnlyStratumDef(StratifierDef stratifierDef) {
        final List<StratumDef> stratumDefs = stratifierDef.getStratum();

        if (stratumDefs.size() != 1) {
            throw new InternalErrorException(
                    "There must be one and only one stratum for this stratifier but there was: %s"
                            .formatted(stratumDefs.size()));
        }

        return stratumDefs.get(0);
    }

    // This is weird pattern where we have multiple qualifying values within a single stratum,
    // which was previously unsupported.  So for now, comma-delim the first five values.
    private static CodeableConcept expressionResultToCodableConcept(StratumValueWrapper value) {
        return new CodeableConcept().setText(value.getValueAsString());
    }

    // TODO: LD: take the StratumDef and use it to figure out the subject ID intersection instead of
    // the provided list of subjectIds
    private static void buildStratumPopulation(
            BuilderContext bc,
            StratifierDef stratifierDef,
            StratumPopulationDef stratumPopulationDef,
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

        if (populationDef == null) {
            throw new InvalidRequestException("Invalid population definition for measure: %s since it's missing %s"
                    .formatted(
                            bc.getMeasureUrl(),
                            population.getCode().getCodingFirstRep().getCode()));
        }

        final Set<String> subjectsQualifiedOrUnqualified = stratumPopulationDef.getSubjectsQualifiedOrUnqualified();

        if (groupDef.isBooleanBasis()) {
            buildBooleanBasisStratumPopulation(
                bc,
                sgpc,
                stratumPopulationDef,
                populationDef,
                subjectsQualifiedOrUnqualified);
        } else {
            buildResourceBasisStratumPopulation(
                bc,
                stratifierDef,
                stratumPopulationDef,
                sgpc,
                subjectIds,
                populationDef,
                groupDef);
        }
    }

    private static void buildBooleanBasisStratumPopulation(
            BuilderContext bc,
            StratifierGroupPopulationComponent sgpc,
            StratumPopulationDef stratumPopulationDef,
            PopulationDef populationDef,
            Set<String> subjectIdsCommonToPopulation) {

        var popSubjectIds = populationDef.getSubjects().stream()
                .map(R4ResourceIdUtils::addPatientQualifier)
                .toList();
        if (popSubjectIds.isEmpty()) {
            sgpc.setCount(0);
            return;
        }

        sgpc.setCount(subjectIdsCommonToPopulation.size());

        // subject-list ListResource to match intersection of results
        if (!subjectIdsCommonToPopulation.isEmpty()
                && bc.report().getType() == org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUBJECTLIST) {
            ListResource popSubjectList =
                    R4StratifierBuilder.createIdList(UUID.randomUUID().toString(), subjectIdsCommonToPopulation);
            bc.addContained(popSubjectList);
            sgpc.setSubjectResults(new Reference("#" + popSubjectList.getId()));
        }
    }

    private static void buildResourceBasisStratumPopulation(
            BuilderContext bc,
            StratifierDef stratifierDef,
            StratumPopulationDef stratumPopulationDef,
            StratifierGroupPopulationComponent sgpc,
            List<String> subjectIds,
            PopulationDef populationDef,
            GroupDef groupDef) {

        final List<String> resourceIds = getResourceIds(subjectIds, groupDef, populationDef);

        // LUKETODO:  this is wrong for our purposes:
        // 1) we are getting non-distinct Date values, one duplicate for each of the 2 dates resolved by the population
        // 2) we are doing the computation in the Builder, when we ought to do it in the MeasureEvaluator
        // 3) We're conflating the intersection code with the counting, but this needs to be done separately
        // 4) So we need to capture the intersection of resources in the MeasureEvaluator, then count them separately
        // 5) As a first step, move this code to the MeasureEvaluator and ensure all existing tests pass
        final int stratumCount = getStratumCountUpper(stratifierDef, populationDef, resourceIds);

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
            StratifierDef stratifierDef, PopulationDef populationDef, List<String> resourceIds) {

        if (MeasureStratifierType.CRITERIA == stratifierDef.getStratifierType()) {
            final Set<Object> resources = populationDef.getResources();
            // LUKETODO:  for the component criteria scenario, we don't add the results directly to the stratifierDef,
            // but to each of the component defs, which is why this is empty
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

    // TODO: LD:  move this to MeasureEvaluator
    @Nonnull
    private static List<CodeableConcept> getCodeForReportStratifier(
            StratifierDef stratifierDef, MeasureGroupStratifierComponent measureStratifier) {

        final Expression criteria = measureStratifier.getCriteria();

        if (MeasureStratifierType.CRITERIA == stratifierDef.getStratifierType()
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
