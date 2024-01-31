package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_SCORING_EXT_URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Element;
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
            var sdeDef = new SdeDef(
                    s.getId(), conceptToConceptDef(s.getCode()), s.getCriteria().getExpression());
            sdes.add(sdeDef);
        }

        // Groups
        var measureLevelMeasureScoring = getMeasureScoring(measure);
        List<GroupDef> groups = new ArrayList<>();
        Map<GroupDef, MeasureScoring> groupMeasureScoring = new HashMap<>();
        for (MeasureGroupComponent group : measure.getGroup()) {
            // Ids are not required on groups in r4
            // checkId(group);

            // Use the measure level scoring as the default
            var groupMeasureScoringCode = measureLevelMeasureScoring;

            // But override measure level scoring if group scoring is present
            var scoringExtension = group.getExtensionByUrl(CQFM_SCORING_EXT_URL);
            if (scoringExtension != null) {
                CodeableConcept coding = (CodeableConcept)
                        group.getExtensionByUrl(CQFM_SCORING_EXT_URL).getValue();
                groupMeasureScoringCode =
                        MeasureScoring.fromCode(coding.getCodingFirstRep().getCode());
            }

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
            var groupDef = new GroupDef(group.getId(), conceptToConceptDef(group.getCode()), stratifiers, populations);
            groups.add(groupDef);
            groupMeasureScoring.put(groupDef, groupMeasureScoringCode);
        }

        return new MeasureDef(
                measure.getId(), measure.getUrl(), measure.getVersion(), groupMeasureScoring, groups, sdes);
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
        return MeasureScoring.fromCode(measure.getScoring().getCodingFirstRep().getCode());
    }
}
