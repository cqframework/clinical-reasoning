package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponentComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationException;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumPopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueWrapper;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

/**
 * Convenience class with functionality split out from {@link R4MeasureReportBuilder} to
 * handle stratifiers
 */
@SuppressWarnings({"squid:S1135", "squid:S107"})
class R4StratifierBuilder {

    private R4StratifierBuilder() {
        // static class
    }

    static void buildStratifier(
            R4MeasureReportBuilderContext bc,
            MeasureReportGroupStratifierComponent reportStratifier,
            StratifierDef stratifierDef,
            GroupDef groupDef) {
        // the top level stratifier 'id' and 'code'
        reportStratifier.setCode(getCodeForReportStratifier(stratifierDef));
        reportStratifier.setId(stratifierDef.id());
        // if description is defined, add to MeasureReport
        if (stratifierDef.description() != null && !stratifierDef.description().isEmpty()) {
            reportStratifier.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL, new StringType(stratifierDef.description()));
        }

        buildMultipleStratum(bc, reportStratifier, stratifierDef, groupDef);
    }

    private static void buildMultipleStratum(
            R4MeasureReportBuilderContext bc,
            MeasureReportGroupStratifierComponent reportStratifier,
            StratifierDef stratifierDef,
            GroupDef groupDef) {

        if (stratifierDef.isCriteriaStratifier()) {
            criteriaStratifier(bc, stratifierDef, reportStratifier, groupDef);
        } else {
            valueOrNonSubjectValueStratifier(bc, stratifierDef, reportStratifier, groupDef);
        }
    }

    private static void valueOrNonSubjectValueStratifier(
            R4MeasureReportBuilderContext bc,
            StratifierDef stratifierDef,
            MeasureReportGroupStratifierComponent reportStratifier,
            GroupDef groupDef) {

        bc.state().stratifier(stratifierDef).getStrata().forEach(stratumDef -> {
            var reportStratum = reportStratifier.addStratum();

            buildStratum(bc, stratifierDef, stratumDef, reportStratum, stratumDef.valueDefs(), groupDef);
        });
    }

    private static void criteriaStratifier(
            R4MeasureReportBuilderContext bc,
            StratifierDef stratifierDef,
            MeasureReportGroupStratifierComponent reportStratifier,
            GroupDef groupDef) {

        // nonComponent stratifiers will have a single expression that can generate results, instead of grouping
        // combinations of results
        // example: 'gender' expression could produce values of 'M', 'F'
        // subject1: 'gender'--> 'M'
        // subject2: 'gender'--> 'F'
        // stratifier criteria results are: 'M', 'F'
        if (stratifierDef.isCriteriaStratifier()) {
            var reportStratum = reportStratifier.addStratum();
            // Ideally, the stratum def should have these values empty in MeasureEvaluator
            // Seems to be irrelevant for criteria based stratifiers
            var stratValues = Set.<StratumValueDef>of();

            buildStratum(bc, stratifierDef, getOnlyStratumDef(bc, stratifierDef), reportStratum, stratValues, groupDef);
            return; // short-circuit so we don't process non-criteria logic
        }

        // Stratum 1
        // Value: 'M'--> subjects: subject1
        // Stratum 2
        // Value: 'F'--> subjects: subject2
        // loop through each value key
        for (StratumDef stratumDef : bc.state().stratifier(stratifierDef).getStrata()) {
            buildStratumOuter(bc, stratifierDef, stratumDef, reportStratifier, groupDef);
        }
    }

    private static void buildStratumOuter(
            R4MeasureReportBuilderContext bc,
            StratifierDef stratifierDef,
            StratumDef stratumDef,
            MeasureReportGroupStratifierComponent reportStratifier,
            GroupDef groupDef) {

        var reportStratum = reportStratifier.addStratum();

        buildStratum(bc, stratifierDef, stratumDef, reportStratum, stratumDef.valueDefs(), groupDef);
    }

    private static void buildStratum(
            R4MeasureReportBuilderContext bc,
            StratifierDef stratifierDef,
            StratumDef stratumDef,
            StratifierGroupComponent stratum,
            Set<StratumValueDef> values,
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
            } else if (MeasureStratifierType.VALUE == stratifierDef.getStratifierType()
                    || MeasureStratifierType.NON_SUBJECT_VALUE == stratifierDef.getStratifierType()) {
                // non-component stratifiers (single-component or non-component) only set stratified value
                // value being stratified: 'M', '35', etc.
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
        for (StratumPopulationDef stratumPopulationDef : stratumDef.stratumPopulations()) {
            var populationDef = groupDef.findPopulationById(stratumPopulationDef.id());
            if (populationDef == null) {
                throw new MeasureEvaluationException(
                        "could not find PopulationDef with id: " + stratumPopulationDef.id());
            }
            var stratumPopulation = stratum.addPopulation();
            buildStratumPopulation(bc, stratumPopulationDef, stratumPopulation, populationDef, groupDef);
        }
    }

    private static StratumDef getOnlyStratumDef(R4MeasureReportBuilderContext bc, StratifierDef stratifierDef) {
        final List<StratumDef> stratumDefs =
                bc.state().stratifier(stratifierDef).getStrata();

        if (stratumDefs.size() != 1) {
            throw new MeasureEvaluationException(
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
    // Simplified by Claude Sonnet 4.5 to use calculated values from StratumPopulationDef
    private static void buildStratumPopulation(
            R4MeasureReportBuilderContext bc,
            StratumPopulationDef stratumPopulationDef,
            StratifierGroupPopulationComponent sgpc,
            PopulationDef populationDef,
            GroupDef groupDef) {

        sgpc.setCode(conceptDefToConcept(populationDef.code()));
        sgpc.setId(populationDef.id());

        if (populationDef.description() != null && !populationDef.description().isEmpty()) {
            sgpc.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL, new StringType(populationDef.description()));
        }

        // Use the calculated count from StratumPopulationDef
        sgpc.setCount(stratumPopulationDef.getCount());

        final Set<String> subjectsQualifiedOrUnqualified = stratumPopulationDef.subjectsQualifiedOrUnqualified();

        if (groupDef.isBooleanBasis()) {
            buildBooleanBasisStratumPopulation(bc, sgpc, subjectsQualifiedOrUnqualified);
        } else {
            buildResourceBasisStratumPopulation(bc, sgpc, stratumPopulationDef.resourceIdsForSubjectList());
        }
    }

    /**
     * Convert a {@link ConceptDef} to a {@link CodeableConcept}, returning null for null input.
     */
    private static CodeableConcept conceptDefToConcept(ConceptDef c) {
        if (c == null) return null;
        var cc = new CodeableConcept().setText(c.text());
        for (var cd : c.codes()) {
            cc.addCoding(new Coding()
                    .setSystem(cd.system())
                    .setCode(cd.code())
                    .setVersion(cd.version())
                    .setDisplay(cd.display()));
        }
        return cc;
    }

    // Simplified by Claude Sonnet 4.5 to use pre-calculated counts from StratumPopulationDef
    private static void buildBooleanBasisStratumPopulation(
            R4MeasureReportBuilderContext bc,
            StratifierGroupPopulationComponent sgpc,
            Set<String> subjectIdsCommonToPopulation) {

        // subject-list ListResource to match intersection of results
        if (!subjectIdsCommonToPopulation.isEmpty()
                && bc.report().getType() == org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUBJECTLIST) {
            ListResource popSubjectList =
                    R4StratifierBuilder.createIdList(UUID.randomUUID().toString(), subjectIdsCommonToPopulation);
            bc.addContained(popSubjectList);
            sgpc.setSubjectResults(new Reference("#" + popSubjectList.getId()));
        }
    }

    // Simplified by Claude Sonnet 4.5 to use pre-calculated resource IDs from StratumPopulationDef
    private static void buildResourceBasisStratumPopulation(
            R4MeasureReportBuilderContext bc, StratifierGroupPopulationComponent sgpc, List<String> resourceIds) {

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

    // Use StratifierDef only to derive the code for the report stratifier
    private static List<CodeableConcept> getCodeForReportStratifier(StratifierDef stratifierDef) {
        if (stratifierDef.isCriteriaStratifier() && stratifierDef.expression() != null) {
            return Collections.singletonList(new CodeableConcept().setText(stratifierDef.expression()));
        }

        ConceptDef code = stratifierDef.code();
        if (code == null) return Collections.emptyList();
        var cc = new CodeableConcept().setText(code.text());
        for (var cd : code.codes()) {
            cc.addCoding(new Coding()
                    .setSystem(cd.system())
                    .setCode(cd.code())
                    .setVersion(cd.version())
                    .setDisplay(cd.display()));
        }
        return Collections.singletonList(cc);
    }
}
