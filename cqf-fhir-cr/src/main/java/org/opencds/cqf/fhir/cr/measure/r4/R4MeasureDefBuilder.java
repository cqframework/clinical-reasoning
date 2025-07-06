package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DATEOFCOMPLIANCE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_SCORING_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.FHIR_ALL_TYPES_SYSTEM_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.SDE_USAGE_CODE;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

public class R4MeasureDefBuilder implements MeasureDefBuilder<Measure> {
    @Override
    public MeasureDef build(Measure measure) {
        checkId(measure);

        // SDES
        List<SdeDef> sdes = new ArrayList<>();
        for (MeasureSupplementalDataComponent s : measure.getSupplementalData()) {
            checkId(s);
            checkSDEUsage(measure, s);
            var sdeDef = new SdeDef(
                    s.getId(), conceptToConceptDef(s.getCode()), s.getCriteria().getExpression());
            sdes.add(sdeDef);
        }
        // scoring
        var measureScoring = getMeasureScoring(measure);
        // populationBasis
        var measureBasis = getMeasureBasis(measure);
        // improvement Notation
        var measureImpNotation = getMeasureImprovementNotation(measure);

        // Groups
        List<GroupDef> groups = new ArrayList<>();
        for (MeasureGroupComponent group : measure.getGroup()) {
            // group Measure Scoring
            var groupScoring = getGroupMeasureScoring(measure, group);
            // populationBasis
            var groupBasis = getGroupPopulationBasis(group);
            // improvement Notation
            var groupImpNotation = getGroupImpNotation(measure, group);
            var hasGroupImpNotation = groupImpNotation != null;

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

            if (group.getExtensionByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL) != null
                    && checkPopulationForCode(populations, DATEOFCOMPLIANCE) == null) {
                // add to definition
                var expressionType = (Expression) group.getExtensionByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL)
                        .getValue();
                if (!expressionType.hasExpression()) {
                    throw new InvalidRequestException("no expression was listed for extension: %s for Measure: %s"
                            .formatted(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL, measure.getUrl()));
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

                if (!components.isEmpty() && mgsc.getCriteria().getExpression() != null) {
                    throw new InvalidRequestException(
                            "Measure stratifier: %s, has both component and stratifier criteria expression defined. Only one should be specified"
                                    .formatted(mgsc.getId()));
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
                    getScoringDef(measure, measureScoring, groupScoring),
                    hasGroupImpNotation,
                    getImprovementNotation(measureImpNotation, groupImpNotation),
                    getPopulationBasisDef(measureBasis, groupBasis));
            groups.add(groupDef);
        }
        return new MeasureDef(measure.getId(), measure.getUrl(), measure.getVersion(), groups, sdes);
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

    private void checkSDEUsage(Measure measure, MeasureSupplementalDataComponent measureSupplementalDataComponent) {
        var hasUsage = measureSupplementalDataComponent.getUsage().stream()
                .filter(t -> t.getCodingFirstRep().getCode().equals(SDE_USAGE_CODE))
                .collect(Collectors.toList());
        if (hasUsage == null || hasUsage.isEmpty()) {
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

    private MeasureScoring getMeasureScoring(Measure measure, @Nullable String scoringCode) {
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

    private MeasureScoring getMeasureScoring(Measure measure) {
        var scoringCode = measure.getScoring().getCodingFirstRep().getCode();
        return getMeasureScoring(measure, scoringCode);
    }

    private void validateImprovementNotationCode(Measure measure, CodeDef improvementNotation) {
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
