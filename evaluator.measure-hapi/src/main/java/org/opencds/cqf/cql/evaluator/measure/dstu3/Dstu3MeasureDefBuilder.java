package org.opencds.cqf.cql.evaluator.measure.dstu3;

import java.util.List;

import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureSupplementalDataComponent;
import org.opencds.cqf.cql.evaluator.measure.common.GroupDef;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureDef;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureDefBuilder;
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureScoring;
import org.opencds.cqf.cql.evaluator.measure.common.SdeDef;
import org.opencds.cqf.cql.evaluator.measure.common.StratifierDef;

public class Dstu3MeasureDefBuilder implements MeasureDefBuilder<Measure> {

    @Override
    public MeasureDef build(Measure measure) {
        MeasureDef measureDef = new MeasureDef();

        measureDef.setUrl(measure.getUrl());
        measureDef.setMeasureScoring(MeasureScoring.fromCode(measure.getScoring().getCodingFirstRep().getCode()));

        // SDES
        List<MeasureSupplementalDataComponent> sdes = measure.getSupplementalData();
        for (MeasureSupplementalDataComponent c : sdes) {
            SdeDef def = new SdeDef();

            String expression = c.getCriteria();
            // DSTU3 / R4 mismatch. Perhaps better to call this key?
            String code = c.hasId() ? c.getId()
                            : expression.toLowerCase().trim().replace(" ", "-");
            def.setCode(code);
            def.setExpression(c.getCriteria());

            measureDef.getSdes().add(def);
        }

        // Groups
        for (MeasureGroupComponent group : measure.getGroup()) {
            GroupDef groupDef = new GroupDef();
            for (MeasureGroupPopulationComponent pop : group.getPopulation()) {
                MeasurePopulationType populationType = MeasurePopulationType
                        .fromCode(pop.getCode().getCodingFirstRep().getCode());
                groupDef.createPopulation(populationType, pop.getCriteria());
            }

            // Stratifiers
            for (MeasureGroupStratifierComponent mgsc : group.getStratifier()) {
                String expression = mgsc.hasCriteria() ? mgsc.getCriteria() : null;

                StratifierDef stratifierDef = new StratifierDef();
                stratifierDef.setExpression(expression);

                groupDef.getStratifiers().add(stratifierDef);
            }

            measureDef.getGroups().add(groupDef);
        }

        return measureDef;
    }
}
