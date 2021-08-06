package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.Optional;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.opencds.cqf.cql.evaluator.measure.common.BaseMeasureReportScorer;
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureScoring;

public class R4MeasureReportScorer extends BaseMeasureReportScorer<MeasureReport> {

    @Override
    public void score(MeasureScoring measureScoring, MeasureReport measureReport) {
        for (MeasureReportGroupComponent mrgc : measureReport.getGroup()) {
            scoreGroup(measureScoring, mrgc);
        }
    }

    protected void scoreGroup(MeasureScoring measureScoring, MeasureReportGroupComponent mrgc) {
        switch(measureScoring) {
            case PROPORTION:
            case RATIO:
                Double score = this.calcProportionScore(getPopulationCount(mrgc, MeasurePopulationType.NUMERATOR), getPopulationCount(mrgc, MeasurePopulationType.DENOMINATOR));
                if (score != null) {
                    mrgc.setMeasureScore(new Quantity(score));
                }
                break;
            default:
                break;
        }
    }

    protected Integer getPopulationCount(MeasureReportGroupComponent mrgc, MeasurePopulationType populationType) {
        Optional<MeasureReportGroupPopulationComponent> pop = mrgc.getPopulation().stream().filter(x -> x.getCode().getCodingFirstRep().getCode().equals(populationType.toCode())).findFirst();
        if (pop.isPresent()) {
            return pop.get().getCount();
        }

        return null;
    }
    
}
