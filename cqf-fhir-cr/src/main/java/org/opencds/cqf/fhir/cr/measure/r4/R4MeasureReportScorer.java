package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Quantity;
import org.opencds.cqf.fhir.cr.measure.common.BaseMeasureReportScorer;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;

/**
 * <p>The R4 MeasureScorer takes population components from MeasureReport resources and scores each group population
 * according to the values populated.</p>
 *<br></br>
 * <p>The population scores within a group are each independently calculated as 'sets' and not counts.</p>
 *<br></br>
 *  <p>A person may be a member of 0, 1, or more sets.</p>
 *  <br></br>
 *  <p>The CQL returns "true" or "false" or "1" or "0" if a person is a member of a given set. It's not giving you a number to count, it's telling you whether a subject is a member of some population or not. The set math happens external to the CQL.</p>
 *<br></br>
 * <B>For example, given Patients A, B, C, D: </B>
 * <ul>
 * <li>"Denominator" [A, B, C, D] - "Denominator Exclusion" [ A, B, C, D] = "Total Denominator" []</li>
 * <li>"Denominator" [A, B, C, D] - "Denominator Exclusion" [] = "Total Denominator" [A, B, C, D]</li>
 * <li>"Denominator" [A, B, C] - "Denominator Exclusion" [ B, C ] = "Total Denominator" [A]</li>
 * <li>"Denominator" [] - "Denominator Exclusion" [ A, B, C ] = "Total Denominator" []</li>
 * <li>"Denominator" [A, B] - "Denominator Exclusion" [C, D] = "Total Denominator" [A, B]</li>
 * <li>"Denominator" [B, C, D] - "Denominator Exclusion" [A, B, C] = "Total Denominator" [D]</li>
 * </ul>
 * "Total Denominator" and "Total Numerator" are not explicit in the Measure, MeasureReport, or the CQL. Those values are calculated internally in the engine and are implicitly used in the score.
 */
public class R4MeasureReportScorer extends BaseMeasureReportScorer<MeasureReport> {

    @Override
    public void score(Map<GroupDef, MeasureScoring> measureScoring, MeasureReport measureReport) {
        for (MeasureReportGroupComponent mrgc : measureReport.getGroup()) {
            scoreGroup(getGroupMeasureScoring(mrgc, measureScoring), mrgc);
        }
    }

    protected MeasureScoring getGroupMeasureScoring(
            MeasureReportGroupComponent mrgc, Map<GroupDef, MeasureScoring> measureScoring) {
        MeasureScoring measureScoringFromGroup = null;
        // cycle through available Group Definitions to retrieve appropriate MeasureScoring
        for (Map.Entry<GroupDef, MeasureScoring> entry : measureScoring.entrySet()) {
            // Take only MeasureScoring available
            if (measureScoring.size() == 1) {
                measureScoringFromGroup = entry.getValue();
                break;
            }
            // Match by group id if available
            if (mrgc.getId() != null && entry.getKey().id() != null) {
                if (entry.getKey().id().equals(mrgc.getId())) {
                    measureScoringFromGroup = entry.getValue();
                    break;
                }
            }
            // Match by group's population id, if group-ids are not present
            if ((mrgc.getPopulation().size() == entry.getKey().populations().size())
                    && mrgc.getId() == null
                    && entry.getKey().id() == null) {
                int i = 0;
                for (MeasureReportGroupPopulationComponent popId : mrgc.getPopulation()) {
                    for (PopulationDef popDefEntry : entry.getKey().populations()) {
                        if (popId.getId() != null && popDefEntry.id() != null) {
                            if (popId.getId().equals(popDefEntry.id())) {
                                i++;
                                break;
                            }
                        }
                    }
                }
                // Check all populations found a match
                if (i == mrgc.getPopulation().size()) {
                    measureScoringFromGroup = entry.getValue();
                }
            }
        }
        if (measureScoringFromGroup == null) {
            throw new IllegalStateException("No MeasureScoring value set");
        }
        return measureScoringFromGroup;
    }

    protected void scoreGroup(MeasureScoring measureScoring, MeasureReportGroupComponent mrgc) {
        switch (measureScoring) {
            case PROPORTION:
            case RATIO:
                Double score = this.calcProportionScore(getGroupTotalNumerator(mrgc), getGroupTotalDenominator(mrgc));
                if (score != null) {
                    mrgc.setMeasureScore(new Quantity(score));
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
                        getStratumGroupTotalNumerator(stratum), getStratumGroupTotalDenominator(stratum));
                if (score != null) {
                    stratum.setMeasureScore(new Quantity(score));
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

    protected Integer getGroupTotalDenominator(MeasureReportGroupComponent mrgc) {
        var den = getPopulationCount(mrgc, MeasurePopulationType.DENOMINATOR);
        if (getPopulationCount(mrgc, MeasurePopulationType.DENOMINATOREXCEPTION) != null) {
            den = den - getPopulationCount(mrgc, MeasurePopulationType.DENOMINATOREXCEPTION);
        }
        if (getPopulationCount(mrgc, MeasurePopulationType.DENOMINATOREXCLUSION) != null) {
            den = den - getPopulationCount(mrgc, MeasurePopulationType.DENOMINATOREXCLUSION);
        }
        return den;
    }

    protected Integer getStratumGroupTotalDenominator(StratifierGroupComponent sgc) {
        var den = getPopulationCount(sgc, MeasurePopulationType.DENOMINATOR);
        if (getPopulationCount(sgc, MeasurePopulationType.DENOMINATOREXCEPTION) != null) {
            den = den - getPopulationCount(sgc, MeasurePopulationType.DENOMINATOREXCEPTION);
        }
        if (getPopulationCount(sgc, MeasurePopulationType.DENOMINATOREXCLUSION) != null) {
            den = den - getPopulationCount(sgc, MeasurePopulationType.DENOMINATOREXCLUSION);
        }
        return den;
    }

    protected Integer getStratumGroupTotalNumerator(StratifierGroupComponent sgc) {
        var num = getPopulationCount(sgc, MeasurePopulationType.NUMERATOR);
        if (getPopulationCount(sgc, MeasurePopulationType.NUMERATOREXCLUSION) != null) {
            num = num - getPopulationCount(sgc, MeasurePopulationType.NUMERATOREXCLUSION);
        }
        return num;
    }

    protected Integer getGroupTotalNumerator(MeasureReportGroupComponent mrgc) {
        var num = getPopulationCount(mrgc, MeasurePopulationType.NUMERATOR);
        if (getPopulationCount(mrgc, MeasurePopulationType.NUMERATOREXCLUSION) != null) {
            num = num - getPopulationCount(mrgc, MeasurePopulationType.NUMERATOREXCLUSION);
        }
        return num;
    }

    protected void scoreStratifier(
            MeasureScoring measureScoring, MeasureReportGroupStratifierComponent stratifierComponent) {
        for (StratifierGroupComponent sgc : stratifierComponent.getStratum()) {
            scoreStratum(measureScoring, sgc);
        }
    }
}
