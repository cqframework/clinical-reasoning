package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.List;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponentComponent;
import org.hl7.fhir.r4.model.Measure.MeasureSupplementalDataComponent;
import org.opencds.cqf.cql.evaluator.measure.common.GroupDef;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureDef;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureDefBuilder;
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureScoring;
import org.opencds.cqf.cql.evaluator.measure.common.SdeDef;
import org.opencds.cqf.cql.evaluator.measure.common.StratifierComponentDef;
import org.opencds.cqf.cql.evaluator.measure.common.StratifierDef;

public class R4MeasureDefBuilder implements MeasureDefBuilder<Measure> {

    @Override
    public MeasureDef build(Measure measure) {
        MeasureDef measureDef = new MeasureDef();

        measureDef.setUrl(measure.getUrl());
        measureDef.setMeasureScoring(MeasureScoring.fromCode(measure.getScoring().getCodingFirstRep().getCode()));

        // SDES
        List<MeasureSupplementalDataComponent> sdes = measure.getSupplementalData();
        for (MeasureSupplementalDataComponent c : sdes) {
            SdeDef def = new SdeDef();

            String expression = c.getCriteria().getExpression();
            String code = c.hasCode() && c.getCode().getCodingFirstRep().hasCode()
                    ? c.getCode().getCodingFirstRep().getCode()
                    : c.hasCode() && c.getCode().hasText() ? c.getCode().getText()
                            : expression.replace(" ", "-").toLowerCase();
            def.setCode(code);
            def.setExpression(expression);

            measureDef.getSdes().add(def);
        }

        // Groups
        for (MeasureGroupComponent group : measure.getGroup()) {
            GroupDef groupDef = new GroupDef();
            for (MeasureGroupPopulationComponent pop : group.getPopulation()) {
                MeasurePopulationType populationType = MeasurePopulationType
                        .fromCode(pop.getCode().getCodingFirstRep().getCode());
                groupDef.createPopulation(populationType, pop.getCriteria().getExpression());
            }

            // Stratifiers
            for (MeasureGroupStratifierComponent mgsc : group.getStratifier()) {
                String expression = mgsc.hasCriteria() ? mgsc.getCriteria().getExpression() : null;
                String code = mgsc.hasCode() ? mgsc.getCode().getCodingFirstRep().getCode()
                        : (expression != null ? expression.replace(" ", "-").toLowerCase() : null);

                StratifierDef stratifierDef = new StratifierDef();
                stratifierDef.setCode(code);
                stratifierDef.setExpression(expression);

                for (MeasureGroupStratifierComponentComponent scc : mgsc.getComponent()) {

                    expression = scc.hasCriteria() ? scc.getCriteria().getExpression() : null;
                    code = scc.hasCode() ? scc.getCode().getCodingFirstRep().getCode()
                            : (expression != null ? expression.replace(" ", "-").toLowerCase() : null);

                    StratifierComponentDef stratifierComponentDef = new StratifierComponentDef();
                    stratifierComponentDef.setCode(code);
                    stratifierComponentDef.setExpression(expression);

                    stratifierDef.getComponents().add(stratifierComponentDef);
                }

                groupDef.getStratifiers().add(stratifierDef);
            }

            measureDef.getGroups().add(groupDef);
        }

        return measureDef;
    }
}
