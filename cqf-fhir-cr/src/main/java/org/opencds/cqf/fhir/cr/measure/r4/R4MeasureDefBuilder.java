package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DATEOFCOMPLIANCE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_SCORING_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CQFM_AGGREGATE_METHOD_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.FHIR_ALL_TYPES_SYSTEM_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.SDE_USAGE_CODE;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponentComponent;
import org.hl7.fhir.r4.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDefBuilder;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.SdeDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierComponentDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

public class R4MeasureDefBuilder implements MeasureDefBuilder<Measure> {
    @Override
    public MeasureDef build(Measure measure) {
        checkId(measure);

        // scoring
        var measureScoring = getMeasureScoring(measure);
        // populationBasis
        var measureBasis = getMeasureBasis(measure);
        // improvement Notation
        var measureImpNotation = getMeasureImprovementNotation(measure);

        // Groups
        List<GroupDef> groups = new ArrayList<>();
        for (MeasureGroupComponent group : measure.getGroup()) {
            var groupDef = buildGroupDef(measure, group, measureScoring, measureImpNotation, measureBasis);

            groups.add(groupDef);
        }

        return new MeasureDef(
                // We don't need either the version of the "Measure" qualifier here
                measure.getIdElement(), measure.getUrl(), measure.getVersion(), groups, getSdeDefs(measure));
    }

    private GroupDef buildGroupDef(
            Measure measure,
            MeasureGroupComponent group,
            MeasureScoring measureScoring,
            CodeDef measureImpNotation,
            CodeDef measureBasis) {

        // group Measure Scoring
        var groupScoring = getGroupMeasureScoring(measure, group);
        // populationBasis
        var groupBasis = getGroupPopulationBasis(group);
        // improvement Notation
        var groupImpNotation = getGroupImpNotation(measure, group);
        var hasGroupImpNotation = groupImpNotation != null;

        final Optional<MeasureGroupPopulationComponent> optMeasureObservationPopulation = group.getPopulation().stream()
                .filter(this::isMeasureObservation)
                .findFirst();

        // aggregateMethod is used to capture continuous-variable method of aggregating MeasureObservation
        final ContinuousVariableObservationAggregateMethod aggregateMethod =
                getAggregateMethod(measure.getUrl(), optMeasureObservationPopulation.orElse(null));

        final String criteriaReference =
                getCriteriaReference(measure.getUrl(), group, optMeasureObservationPopulation.orElse(null));

        // Populations
        checkIds(group);

        var populationsWithCriteriaReference = group.getPopulation().stream()
                .map(population -> buildPopulationDef(population, criteriaReference))
                .toList();

        final Optional<PopulationDef> optPopulationDefDateOfCompliance =
                buildPopulationDefForDateOfCompliance(measure.getUrl(), group, populationsWithCriteriaReference);

        // Stratifiers
        var stratifiers = group.getStratifier().stream()
                .map(mgsc -> buildStratifierDef(measure.getUrl(), mgsc))
                .toList();

        return new GroupDef(
                group.getId(),
                conceptToConceptDef(group.getCode()),
                stratifiers,
                mergePopulations(populationsWithCriteriaReference, optPopulationDefDateOfCompliance.orElse(null)),
                getScoringDef(measure, measureScoring, groupScoring),
                hasGroupImpNotation,
                getImprovementNotation(measureImpNotation, groupImpNotation),
                getPopulationBasisDef(measureBasis, groupBasis),
                aggregateMethod);
    }

    private void checkIds(MeasureGroupComponent group) {
        group.getPopulation().forEach(R4MeasureDefBuilder::checkId);
    }

    private List<PopulationDef> mergePopulations(
            List<PopulationDef> populationsWithCriteriaReference, @Nullable PopulationDef populationDef) {

        final Builder<PopulationDef> immutableListBuilder = ImmutableList.builder();

        immutableListBuilder.addAll(populationsWithCriteriaReference);

        Optional.ofNullable(populationDef).ifPresent(immutableListBuilder::add);

        return immutableListBuilder.build();
    }

    @Nonnull
    private PopulationDef buildPopulationDef(MeasureGroupPopulationComponent population, String criteriaReference) {
        return new PopulationDef(
                population.getId(),
                conceptToConceptDef(population.getCode()),
                MeasurePopulationType.fromCode(
                        population.getCode().getCodingFirstRep().getCode()),
                population.getCriteria().getExpression(),
                criteriaReference);
    }

    private Optional<PopulationDef> buildPopulationDefForDateOfCompliance(
            String measureUrl, MeasureGroupComponent group, List<PopulationDef> populationDefs) {

        if (group.getExtensionByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL) == null
                || checkPopulationForCode(populationDefs, DATEOFCOMPLIANCE) == null) {
            return Optional.empty();
        }

        // add to definition
        var expressionType = (Expression) group.getExtensionByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL)
                .getValue();
        if (!expressionType.hasExpression()) {
            throw new InvalidRequestException("no expression was listed for extension: %s for Measure: %s"
                    .formatted(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL, measureUrl));
        }
        var expression = expressionType.getExpression();
        var populateDefDateOfCompliance = new PopulationDef(
                "dateOfCompliance", totalConceptDefCreator(DATEOFCOMPLIANCE), DATEOFCOMPLIANCE, expression);

        return Optional.of(populateDefDateOfCompliance);
    }

    private ContinuousVariableObservationAggregateMethod getAggregateMethod(
            String measureUrl, @Nullable MeasureGroupPopulationComponent measureObservationPopulation) {

        if (measureObservationPopulation == null) {
            return ContinuousVariableObservationAggregateMethod.N_A;
        }

        var aggMethodExt = measureObservationPopulation.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        if (aggMethodExt != null) {
            // this method is only required if scoringType = continuous-variable
            var aggregateMethodString = aggMethodExt.getValue().toString();

            var aggregateMethod = ContinuousVariableObservationAggregateMethod.fromString(aggregateMethodString);

            // check that method is accepted
            if (aggregateMethod == null) {
                throw new InvalidRequestException("Measure Observation method: %s is not a valid value for Measure: %s"
                        .formatted(aggregateMethodString, measureUrl));
            }

            return aggregateMethod;
        }

        return ContinuousVariableObservationAggregateMethod.N_A;
    }

    @Nullable
    private String getCriteriaReference(
            String measureUrl,
            MeasureGroupComponent group,
            @Nullable MeasureGroupPopulationComponent measureObservationPopulation) {

        if (measureObservationPopulation == null) {
            return null;
        }

        var populationCriteriaExt = measureObservationPopulation.getExtensionByUrl(EXT_CQFM_CRITERIA_REFERENCE);
        if (populationCriteriaExt != null) {
            // required for measure-observation populations
            // the underlying expression is a cql function
            // the criteria reference is what is used to populate parameters of the function
            String critReference = populationCriteriaExt.getValue().toString();
            // check that the reference exists in the GroupDef.populationId
            if (group.getPopulation().stream().map(Element::getId).noneMatch(id -> id.equals(critReference))) {
                throw new InvalidRequestException(
                        "no matching criteria reference was found for extension: %s for Measure: %s"
                                .formatted(EXT_CQFM_CRITERIA_REFERENCE, measureUrl));
            }
            // assign validated reference
            return critReference;
        }

        return null;
    }

    private boolean isMeasureObservation(MeasureGroupPopulationComponent pop) {

        checkId(pop);

        MeasurePopulationType populationType =
                MeasurePopulationType.fromCode(pop.getCode().getCodingFirstRep().getCode());

        return populationType != null && populationType.equals(MeasurePopulationType.MEASUREOBSERVATION);
    }

    @Nonnull
    private StratifierDef buildStratifierDef(String measureUrl, MeasureGroupStratifierComponent mgsc) {
        checkId(mgsc);

        // Components
        var components = new ArrayList<StratifierComponentDef>();
        for (MeasureGroupStratifierComponentComponent scc : mgsc.getComponent()) {
            checkId(scc);
            var scd = new StratifierComponentDef(
                    scc.getId(),
                    conceptToConceptDef(scc.getCode()),
                    scc.hasCriteria() ? scc.getCriteria().getExpression() : null);

            components.add(scd);
        }

        if (!components.isEmpty() && mgsc.getCriteria().getExpression() != null) {
            throw new InvalidRequestException(
                    "Measure stratifier: %s, has both component and stratifier criteria expression defined. Only one should be specified"
                            .formatted(mgsc.getId()));
        }

        return new StratifierDef(
                mgsc.getId(),
                conceptToConceptDef(mgsc.getCode()),
                mgsc.getCriteria().getExpression(),
                getStratifierType(measureUrl, mgsc),
                components);
    }

    public static void triggerFirstPassValidation(List<Measure> measures) {
        measures.forEach(R4MeasureDefBuilder::triggerFirstPassValidation);
    }

    @Nonnull
    private List<SdeDef> getSdeDefs(Measure measure) {
        final List<SdeDef> sdes = new ArrayList<>();
        for (MeasureSupplementalDataComponent s : measure.getSupplementalData()) {
            checkId(s);
            checkSDEUsage(measure, s);
            var sdeDef = new SdeDef(
                    s.getId(), conceptToConceptDef(s.getCode()), s.getCriteria().getExpression());
            sdes.add(sdeDef);
        }
        return sdes;
    }

    /*
        Must be defined on the field "criteria", and cannot be defined under stratifier.component.
    Criteria Stratifier must be of the same basis as group.populations to allow proper intersection of results
    No Component criteria stratifier capability, this can be done with one expression, given there is no combination of values visible on final reports
    "stratifier": [
            {
              "id": "stratifier-criteria-based",
              "criteria": {
                "language": "text/cql.identifier",
                "expression": "Age"
              }
            }
          ]
        }]
         */

    /*
        value Stratifier Definition
    Must be defined on Stratifier.component
    Value based stratifiers must define at minimum 1 criteria, but can be up to N different criteria values
    "stratifier": [
            {
              "id": "stratifier-1",
              "code" : {
                "text": "Gender and Age"
              },
              "component": [
                {
                  "id": "stratifier-comp-1",
                  "code" : {
                    "text": "Gender"
                  },
                  "criteria": {
                    "language": "text/cql.identifier",
                    "expression": "Gender Stratification"
                  }
                },
                {
                  "id": "stratifier-comp-2",
                  "code" : {
                    "text": "Age"
                  },
                  "criteria": {
                    "language": "text/cql.identifier",
                    "expression": "Age"
                  }
                }
              ]
            }
          ]
        }]
         */

    /*
    Acceptance Criteria
        * value based stratifiers defined in Stratifier.criteria will throw error for invalid definition >>>>>>>   what does that mean????
        * criteria based stratifiers defined in Stratifier.component will throw error for invalid definition   >>>>> what does that mean????
        * previous ext used to define 'criteria' stratifier will be deprecated >>>> what do we mean by deprecated?  can't we just delete this altogether?  if not, how to enforce the deprecation?
        * Any current tests or resources that define 'value' based stratifiers outside stratifier.component need to be updated
     */

    // LUKETODO: CRITERIA requirements
    // 1. Check if stratifier criteria exists
    // 2. Check if the expression conforms to the group population basis:  (how can we do this upfront?)
    // 3. There are NO COMPONENTS

    // LUKETODO: VALUE requirements
    // 1. Check if there's at least one component
    // 2. Check that there's at least one criteria, but there can be multiple
    // LUKETODO:  try to make this as FHIR version-agnostic as possible

    private static MeasureStratifierType getStratifierType(
            String measureUrl, MeasureGroupStratifierComponent measureGroupStratifierComponent) {
        if (measureGroupStratifierComponent == null) {
            return MeasureStratifierType.VALUE;
        }

        final boolean hasCriteria = measureGroupStratifierComponent.hasCriteria();

        final boolean hasAnyComponentCriteria = measureGroupStratifierComponent.getComponent().stream()
                .anyMatch(MeasureGroupStratifierComponentComponent::hasCriteria);

        if (hasCriteria && hasAnyComponentCriteria) {
            throw new InvalidRequestException(
                    "Stratifier Cannot have both criteria: %s and any component criteria: %s for measure: %s"
                            .formatted(hasCriteria, hasAnyComponentCriteria, measureUrl));
        }

        if (!hasCriteria && !hasAnyComponentCriteria) {
            throw new InvalidRequestException(
                    "Stratifier cannot have neither criteria nor component for measure: %s".formatted(measureUrl));
        }

        if (hasCriteria) {
            return MeasureStratifierType.CRITERIA;
        }

        return MeasureStratifierType.VALUE;
    }

    private static void triggerFirstPassValidation(Measure measure) {

        checkId(measure);

        // SDES
        for (MeasureSupplementalDataComponent s : measure.getSupplementalData()) {
            checkId(s);
            checkSDEUsage(measure, s);
        }
        // scoring
        getMeasureScoring(measure);

        validateMeasureImprovementNotation(measure);
    }

    private PopulationDef checkPopulationForCode(
            List<PopulationDef> populations, MeasurePopulationType measurePopType) {
        return populations.stream()
                .filter(e -> e.code().first().code().equals(measurePopType.toCode()))
                .findAny()
                .orElse(null);
    }

    private ConceptDef totalConceptDefCreator(MeasurePopulationType measurePopulationType) {
        return new ConceptDef(
                Collections.singletonList(
                        new CodeDef(measurePopulationType.getSystem(), measurePopulationType.toCode())),
                null);
    }

    private static void checkSDEUsage(
            Measure measure, MeasureSupplementalDataComponent measureSupplementalDataComponent) {
        var hasUsage = measureSupplementalDataComponent.getUsage().stream()
                .filter(t -> t.getCodingFirstRep().getCode().equals(SDE_USAGE_CODE))
                .toList();
        if (CollectionUtils.isEmpty(hasUsage)) {
            throw new InvalidRequestException("SupplementalDataComponent usage is missing code: %s for Measure: %s"
                    .formatted(SDE_USAGE_CODE, measure.getUrl()));
        }
    }

    private ConceptDef conceptToConceptDef(CodeableConcept codeable) {
        if (codeable == null) {
            return null;
        }

        List<CodeDef> codes = new ArrayList<>();
        for (var c : codeable.getCoding()) {
            codes.add(codeToCodeDef(c));
        }

        return new ConceptDef(codes, codeable.getText());
    }

    private CodeDef codeToCodeDef(Coding coding) {
        return new CodeDef(coding.getSystem(), coding.getVersion(), coding.getCode(), coding.getDisplay());
    }

    private static void checkId(Element e) {
        if (e.getId() == null || StringUtils.isBlank(e.getId())) {
            throw new InvalidRequestException("id is required on all Elements of type: " + e.fhirType());
        }
    }

    private static void checkId(Resource r) {
        if (r.getId() == null || StringUtils.isBlank(r.getId())) {
            throw new InvalidRequestException("id is required on all Resources of type: " + r.fhirType());
        }
    }

    private static MeasureScoring getMeasureScoring(Measure measure, @Nullable String scoringCode) {
        if (scoringCode != null) {
            var code = MeasureScoring.fromCode(scoringCode);
            if (code == null) {
                throw new InvalidRequestException(
                        "Measure Scoring code: %s, is not a valid Measure Scoring Type for measure: %s."
                                .formatted(scoringCode, measure.getUrl()));
            } else {
                return code;
            }
        }
        return null;
    }

    private static MeasureScoring getMeasureScoring(Measure measure) {
        var scoringCode = measure.getScoring().getCodingFirstRep().getCode();
        return getMeasureScoring(measure, scoringCode);
    }

    private static void validateImprovementNotationCode(Measure measure, CodeDef improvementNotation) {
        var code = improvementNotation.code();
        var system = improvementNotation.system();
        boolean hasValidSystem = system.equals(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM);
        boolean hasValidCode =
                IMPROVEMENT_NOTATION_SYSTEM_INCREASE.equals(code) || IMPROVEMENT_NOTATION_SYSTEM_DECREASE.equals(code);
        if (!hasValidCode || !hasValidSystem) {
            throw new InvalidRequestException(
                    "ImprovementNotation Coding has invalid System: %s, code: %s, combination for Measure: %s"
                            .formatted(system, code, measure.getUrl()));
        }
    }

    public CodeDef getMeasureBasis(Measure measure) {

        var ext = measure.getExtensionByUrl(MeasureConstants.POPULATION_BASIS_URL);
        // check for population-basis Extension, assume boolean if no Extension is found
        if (ext != null) {
            return makeCodeDefFromExtension(ext);
        }
        return null;
    }

    private CodeDef makeCodeDefFromExtension(Extension extension) {
        var code = extension.getValue().toString();
        // validate code membership
        assert Enumerations.FHIRAllTypes.fromCode(code) != null;
        return new CodeDef(MeasureConstants.POPULATION_BASIS_URL, code);
    }

    public CodeDef getMeasureImprovementNotation(Measure measure) {
        if (measure.hasImprovementNotation()) {
            var improvementNotationValue = measure.getImprovementNotation();
            var codeDef = new CodeDef(
                    improvementNotationValue.getCodingFirstRep().getSystem(),
                    improvementNotationValue.getCodingFirstRep().getCode());
            validateImprovementNotationCode(measure, codeDef);
            return codeDef;
        }
        return null;
    }

    private static void validateMeasureImprovementNotation(Measure measure) {
        if (measure.hasImprovementNotation()) {
            var improvementNotationValue = measure.getImprovementNotation();
            var codeDef = new CodeDef(
                    improvementNotationValue.getCodingFirstRep().getSystem(),
                    improvementNotationValue.getCodingFirstRep().getCode());
            validateImprovementNotationCode(measure, codeDef);
        }
    }

    public CodeDef getGroupImpNotation(Measure measure, MeasureGroupComponent group) {
        var ext = group.getExtensionByUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION);
        if (ext != null) {
            var value = ext.getValue();
            if (value instanceof CodeableConcept coding) {
                var codeDef = new CodeDef(
                        coding.getCodingFirstRep().getSystem(),
                        coding.getCodingFirstRep().getCode());
                validateImprovementNotationCode(measure, codeDef);
                return codeDef;
            }
        }
        return null;
    }

    public MeasureScoring getGroupMeasureScoring(Measure measure, MeasureGroupComponent group) {
        var ext = group.getExtensionByUrl(CQFM_SCORING_EXT_URL);
        if (ext != null) {
            var extVal = ext.getValue();
            assert extVal instanceof CodeableConcept;
            CodeableConcept coding = (CodeableConcept) extVal;
            return getMeasureScoring(measure, coding.getCodingFirstRep().getCode());
        }
        return null;
    }

    public CodeDef getGroupPopulationBasis(MeasureGroupComponent group) {
        var ext = group.getExtensionByUrl(MeasureConstants.POPULATION_BASIS_URL);
        // check for population-basis Extension, assume boolean if no Extension is found
        if (ext != null) {
            return makeCodeDefFromExtension(ext);
        }
        return null;
    }

    private MeasureScoring getScoringDef(Measure measure, MeasureScoring measureScoring, MeasureScoring groupScoring) {
        if (groupScoring == null && measureScoring == null) {
            throw new InvalidRequestException(
                    "MeasureScoring must be specified on Group or Measure for Measure: " + measure.getUrl());
        }
        if (groupScoring != null) {
            return groupScoring;
        }
        return measureScoring;
    }

    private CodeDef getPopulationBasisDef(@Nullable CodeDef measureBasis, @Nullable CodeDef groupBasis) {
        if (measureBasis == null && groupBasis == null) {
            // default basis, if not defined
            return new CodeDef(FHIR_ALL_TYPES_SYSTEM_URL, "boolean");
        }
        return defaultCodeDef(groupBasis, measureBasis);
    }

    private CodeDef getImprovementNotation(@Nullable CodeDef measureImpNotation, @Nullable CodeDef groupImpNotation) {
        if (measureImpNotation == null && groupImpNotation == null) {
            // default Improvement Notation, if not defined
            return new CodeDef(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, IMPROVEMENT_NOTATION_SYSTEM_INCREASE);
        }
        return defaultCodeDef(groupImpNotation, measureImpNotation);
    }

    private CodeDef defaultCodeDef(@Nullable CodeDef code, @Nullable CodeDef codeDefault) {
        if (code != null) {
            return code;
        }
        return codeDefault;
    }
}
