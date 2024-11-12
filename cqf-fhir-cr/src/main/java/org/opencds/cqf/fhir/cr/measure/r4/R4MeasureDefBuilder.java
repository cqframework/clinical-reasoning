package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DATEOFCOMPLIANCE;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.TOTALDENOMINATOR;
import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.TOTALNUMERATOR;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_SCORING_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.SDE_USAGE_CODE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponentComponent;
import org.hl7.fhir.r4.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDefBuilder;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.SdeDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierComponentDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;

public class R4MeasureDefBuilder implements MeasureDefBuilder<Measure> {
    @Override
    public MeasureDef build(Measure measure) {
        checkId(measure);

        // SDES
        List<SdeDef> sdes = new ArrayList<>();
        for (MeasureSupplementalDataComponent s : measure.getSupplementalData()) {
            checkId(s);
            checkSDEUsage(s);
            var sdeDef = new SdeDef(
                    s.getId(), conceptToConceptDef(s.getCode()), s.getCriteria().getExpression());
            sdes.add(sdeDef);
        }

        // Groups
        var measureLevelMeasureScoring = getMeasureScoring(measure);
        List<GroupDef> groups = new ArrayList<>();
        for (MeasureGroupComponent group : measure.getGroup()) {
            // group Measure Scoring
            var groupMeasureScoringCode = getGroupMeasureScoring(measureLevelMeasureScoring, group);

            if (groupMeasureScoringCode == null) {
                throw new IllegalArgumentException("MeasureScoring must be specified on Group or Measure");
            }

            // Populations
            List<PopulationDef> populations = new ArrayList<>();
            for (MeasureGroupPopulationComponent pop : group.getPopulation()) {
                checkId(pop);
                MeasurePopulationType populationType = MeasurePopulationType.fromCode(
                        pop.getCode().getCodingFirstRep().getCode());

                populations.add(new PopulationDef(
                        pop.getId(),
                        conceptToConceptDef(pop.getCode()),
                        populationType,
                        pop.getCriteria().getExpression()));
            }
            // total Denominator/Numerator Def Builder
            // validate population is not in Def
            if (checkPopulationForCode(populations, TOTALDENOMINATOR) == null) {
                // add to definition
                populations.add(new PopulationDef(
                        "totalDenominator", totalConceptDefCreator(TOTALDENOMINATOR), TOTALDENOMINATOR, null));
            }
            if (checkPopulationForCode(populations, TOTALNUMERATOR) == null) {
                // add to definition
                populations.add(new PopulationDef(
                        "totalNumerator", totalConceptDefCreator(TOTALNUMERATOR), TOTALNUMERATOR, null));
            }
            if (group.getExtensionByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL) != null
                    && checkPopulationForCode(populations, DATEOFCOMPLIANCE) == null) {
                // add to definition
                var expressionType = (Expression) group.getExtensionByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL)
                        .getValue();
                if (!expressionType.hasExpression()) {
                    throw new IllegalArgumentException(String.format(
                            "no expression was listed for extension: %s", CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL));
                }
                var expression = expressionType.getExpression();
                populations.add(new PopulationDef(
                        "dateOfCompliance", totalConceptDefCreator(DATEOFCOMPLIANCE), DATEOFCOMPLIANCE, expression));
            }

            // Stratifiers
            List<StratifierDef> stratifiers = new ArrayList<>();
            for (MeasureGroupStratifierComponent mgsc : group.getStratifier()) {
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

                var stratifierDef = new StratifierDef(
                        mgsc.getId(),
                        conceptToConceptDef(mgsc.getCode()),
                        mgsc.getCriteria().getExpression(),
                        components);

                stratifiers.add(stratifierDef);
            }

            var groupDef = new GroupDef(
                    group.getId(),
                    conceptToConceptDef(group.getCode()),
                    stratifiers,
                    populations,
                    groupMeasureScoringCode,
                    isGroupIncreaseImprovementNotation(measure, group),
                    groupHasImprovementNotationExt(group));
            groups.add(groupDef);
        }
        // define basis of measure
        R4MeasureBasisDef measureBasisDef = new R4MeasureBasisDef();
        return new MeasureDef(
                measure.getId(),
                measure.getUrl(),
                measure.getVersion(),
                groups,
                sdes,
                measureBasisDef.isBooleanBasis(measure),
                useMeasureImprovementNotation(groups));
    }

    public boolean useMeasureImprovementNotation(List<GroupDef> groups) {
        // if no groups are present then useMeasure
        if (groups == null || groups.isEmpty()) {
            return true;
        } else {
            boolean useGroupImpNotation = groups.stream().allMatch(GroupDef::useGroupDefImprovementNotation)
                    && groups.get(0).useGroupDefImprovementNotation();
            return !useGroupImpNotation;
        }
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

    private void checkSDEUsage(MeasureSupplementalDataComponent measureSupplementalDataComponent) {
        var hasUsage = measureSupplementalDataComponent.getUsage().stream()
                .filter(t -> t.getCodingFirstRep().getCode().equals(SDE_USAGE_CODE))
                .collect(Collectors.toList());
        if (hasUsage == null || hasUsage.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("SupplementalDataComponent usage is missing code: %s", SDE_USAGE_CODE));
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

    private void checkId(Element e) {
        if (e.getId() == null || StringUtils.isBlank(e.getId())) {
            throw new NullPointerException("id is required on all Elements of type: " + e.fhirType());
        }
    }

    private void checkId(Resource r) {
        if (r.getId() == null || StringUtils.isBlank(r.getId())) {
            throw new NullPointerException("id is required on all Resources of type: " + r.fhirType());
        }
    }

    private MeasureScoring getMeasureScoring(Measure measure) {
        var scoringCode = measure.getScoring().getCodingFirstRep().getCode();
        if (scoringCode != null) {
            var code = MeasureScoring.fromCode(scoringCode);
            if (code == null) {
                throw new IllegalArgumentException(
                        String.format("Measure Scoring code: %s, is not a valid Measure Scoring Type.", scoringCode));
            } else {
                return code;
            }
        }
        return null;
    }

    private MeasureScoring getGroupMeasureScoring(MeasureScoring measureLevelScoring, MeasureGroupComponent group) {
        // see if group component has scoring Url
        var scoringExtension = group.getExtensionByUrl(CQFM_SCORING_EXT_URL);
        if (scoringExtension != null) {
            // extract scoringType
            CodeableConcept coding = (CodeableConcept) scoringExtension.getValue();
            return MeasureScoring.fromCode(coding.getCodingFirstRep().getCode());
        }
        // otherwise return measureLevelScoring
        return measureLevelScoring;
    }

    private boolean isIncreaseImprovementNotation(CodeableConcept improvementNotationValue) {
        validateImprovementNotationCode(improvementNotationValue);
        return improvementNotationValue.hasCoding(
                MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, IMPROVEMENT_NOTATION_SYSTEM_INCREASE);
    }

    private void validateImprovementNotationCode(CodeableConcept improvementNotationValue) {
        var code = improvementNotationValue.getCodingFirstRep().getCode();
        var system = improvementNotationValue.getCodingFirstRep().getSystem();
        boolean hasValidSystem = system.equals(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM);
        boolean hasValidCode =
                IMPROVEMENT_NOTATION_SYSTEM_INCREASE.equals(code) || IMPROVEMENT_NOTATION_SYSTEM_DECREASE.equals(code);
        if (!hasValidCode || !hasValidSystem) {
            throw new IllegalArgumentException(String.format(
                    "ImprovementNotation Coding has invalid System: %s, code: %s, combination for Measure.",
                    system, code));
        }
    }

    private CodeableConcept getGroupImprovementNotationExt(MeasureGroupComponent group) {
        var ext = group.getExtensionByUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION)
                .getValue();
        assert ext instanceof CodeableConcept;
        return (CodeableConcept) ext;
    }

    private boolean groupHasImprovementNotationExt(MeasureGroupComponent group) {
        return group.getExtensionByUrl(MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION) != null;
    }

    private boolean isGroupIncreaseImprovementNotation(Measure measure, MeasureGroupComponent group) {
        // default improvement Notation
        boolean isIncreaseImpNotation = true;
        boolean useGroupImpNotation = groupHasImprovementNotationExt(group);
        if (useGroupImpNotation) {
            isIncreaseImpNotation = isIncreaseImprovementNotation(getGroupImprovementNotationExt(group));
        } else if (measure.hasImprovementNotation()) {
            isIncreaseImpNotation = isIncreaseImprovementNotation(measure.getImprovementNotation());
        }

        return isIncreaseImpNotation;
    }
}
