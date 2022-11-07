package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.ArrayList;
import java.util.List;

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
import org.opencds.cqf.cql.evaluator.measure.common.CodeDef;
import org.opencds.cqf.cql.evaluator.measure.common.ConceptDef;
import org.opencds.cqf.cql.evaluator.measure.common.GroupDef;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureDef;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureDefBuilder;
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureScoring;
import org.opencds.cqf.cql.evaluator.measure.common.PopulationDef;
import org.opencds.cqf.cql.evaluator.measure.common.SdeDef;
import org.opencds.cqf.cql.evaluator.measure.common.StratifierComponentDef;
import org.opencds.cqf.cql.evaluator.measure.common.StratifierDef;

import ca.uhn.fhir.util.StringUtil;

public class R4MeasureDefBuilder implements MeasureDefBuilder<Measure> {

    private final boolean enforceIds;

    public R4MeasureDefBuilder() {
        this(false);
    }

    public R4MeasureDefBuilder(boolean enforceIds) {
        this.enforceIds = enforceIds;
    }

    @Override
    public MeasureDef build(Measure measure) {
        checkId(measure);

        // SDES
        List<SdeDef> sdes = new ArrayList<>();
        for (MeasureSupplementalDataComponent s : measure.getSupplementalData()) {
            checkId(s);
            var sdeDef = new SdeDef(
                    s.getId(),
                    conceptToConceptDef(s.getCode()),
                    s.getCriteria().getExpression());
            sdes.add(sdeDef);
        }

        // Groups
        List<GroupDef> groups = new ArrayList<>();
        for (MeasureGroupComponent group : measure.getGroup()) {
            checkId(group);

            // Populations
            List<PopulationDef> populations = new ArrayList<>();
            for (MeasureGroupPopulationComponent pop : group.getPopulation()) {
                checkId(pop);
                MeasurePopulationType populationType = MeasurePopulationType
                        .fromCode(pop.getCode().getCodingFirstRep().getCode());

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

            groups.add(new GroupDef(
                    group.getId(),
                    conceptToConceptDef(group.getCode()),
                    stratifiers,
                    populations));

        }

        return new MeasureDef(
                measure.getId(),
                measure.getUrl(),
                measure.getVersion(),
                MeasureScoring.fromCode(measure.getScoring().getCodingFirstRep().getCode()),
                groups,
                sdes);
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
        if (enforceIds && (e.getId() == null || StringUtils.isBlank(e.getId()))) {
            throw new NullPointerException("id is required on all Elements of type: " + e.fhirType());
        }
    }

    private void checkId(Resource r) {
        if (enforceIds && (r.getId() == null || StringUtils.isBlank(r.getId()))) {
            throw new NullPointerException("id is required on all Resources of type: " + r.fhirType());
        }
    }
}
