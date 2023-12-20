package org.opencds.cqf.fhir.cr.measure.dstu3;

import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.dstu3.model.MeasureReport.StratifierGroupPopulationComponent;
import org.opencds.cqf.fhir.cr.measure.common.BaseMeasureReportScorer;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;

public class Dstu3MeasureReportScorer extends BaseMeasureReportScorer<MeasureReport> {

    @Override
    public void score(Map<GroupDef, MeasureScoring> measureScoring, MeasureReport measureReport) {
        // Dstu3 only has MeasureScoring defined on Measure level,
        // use that for all groups
        var scoring = measureScoring.entrySet().stream().findFirst().get().getValue();
        for (MeasureReportGroupComponent mrgc : measureReport.getGroup()) {
            scoreGroup(scoring, mrgc);
        }
    }

    protected void scoreGroup(MeasureScoring measureScoring, MeasureReportGroupComponent mrgc) {
        switch (measureScoring) {
            case PROPORTION:
            case RATIO:
                Double score = this.calcProportionScore(
                        getPopulationCount(mrgc, MeasurePopulationType.NUMERATOR),
                        getPopulationCount(mrgc, MeasurePopulationType.DENOMINATOR));
                if (score != null) {
                    mrgc.setMeasureScore(score);
                }
                break;
            default:
                break;
        }

        for (MeasureReportGroupStratifierComponent stratifierComponent : mrgc.getStratifier()) {
            scoreStratifier(measureScoring, stratifierComponent);
        }
    }

    protected void scoreStratum(MeasureScoring measureScoring, StratifierGroupComponent stratum) {
        switch (measureScoring) {
            case PROPORTION:
            case RATIO:
                Double score = this.calcProportionScore(
                        getPopulationCount(stratum, MeasurePopulationType.NUMERATOR),
                        getPopulationCount(stratum, MeasurePopulationType.DENOMINATOR));
                if (score != null) {
                    stratum.setMeasureScore(score);
                }
                break;
            default:
                break;
        }
    }

    protected Integer getPopulationCount(MeasureReportGroupComponent mrgc, MeasurePopulationType populationType) {
        Optional<MeasureReportGroupPopulationComponent> pop = mrgc.getPopulation().stream()
                .filter(x -> x.getCode().getCodingFirstRep().getCode().equals(populationType.toCode()))
                .findFirst();
        if (pop.isPresent()) {
            return pop.get().getCount();
        }

        return null;
    }

    protected Integer getPopulationCount(StratifierGroupComponent sgc, MeasurePopulationType populationType) {
        Optional<StratifierGroupPopulationComponent> pop = sgc.getPopulation().stream()
                .filter(x -> x.getCode().getCodingFirstRep().getCode().equals(populationType.toCode()))
                .findFirst();
        if (pop.isPresent()) {
            return pop.get().getCount();
        }

        return null;
    }

    protected void scoreStratifier(
            MeasureScoring measureScoring, MeasureReportGroupStratifierComponent stratifierComponent) {
        for (StratifierGroupComponent sgc : stratifierComponent.getStratum()) {
            scoreStratum(measureScoring, sgc);
        }
    }
}
