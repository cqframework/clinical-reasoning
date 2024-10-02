package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_TOTAL_DENOMINATOR_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_TOTAL_NUMERATOR_URL;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.Quantity;
import org.opencds.cqf.fhir.cr.measure.common.BaseMeasureReportScorer;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;

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
    public void score(MeasureDef measureDef, MeasureReport measureReport) {
        // Measure Def Check
        if (measureDef == null) {
            throw new IllegalArgumentException("MeasureDef is required in order to score a Measure.");
        }
        // No groups to score, nothing to do.
        if (measureReport.getGroup().isEmpty()) {
            return;
        }

        for (MeasureReportGroupComponent mrgc : measureReport.getGroup()) {
            scoreGroup(
                    getGroupMeasureScoring(mrgc, measureDef),
                    mrgc,
                    getGroupDef(measureDef, mrgc).isPositiveImprovementNotation());
        }
    }

    protected GroupDef getGroupDef(MeasureDef measureDef, MeasureReportGroupComponent mrgc) {
        var groupDefs = measureDef.groups();
        if (groupDefs.size() == 1) {
            return groupDefs.get(0);
        }
        return groupDefs.stream()
                .filter(t -> t.id().equals(mrgc.getId()))
                .findFirst()
                .orElse(null);
    }

    protected MeasureScoring checkMissingScoringType(MeasureScoring measureScoring) {
        if (measureScoring == null) {
            throw new IllegalArgumentException(
                    "Measure does not have a scoring methodology defined. Add a \"scoring\" property to the measure definition or the group definition.");
        }
        return measureScoring;
    }

    protected void groupHasValidId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException(
                    "Measure resources with more than one group component require a unique group.id() defined to score appropriately.");
        }
    }

    protected MeasureScoring getGroupMeasureScoring(MeasureReportGroupComponent mrgc, MeasureDef measureDef) {
        MeasureScoring groupScoringType = null;
        // if not multi-rate, get first groupDef scoringType
        if (measureDef.groups().size() == 1) {
            groupScoringType = measureDef.groups().get(0).measureScoring();
        } else {
            // multi rate measure, match groupComponent to groupDef to extract scoringType
            for (GroupDef groupDef : measureDef.groups()) {
                var groupDefMeasureScoring = groupDef.measureScoring();
                // groups must have id populated
                groupHasValidId(mrgc.getId());
                groupHasValidId(groupDef.id());
                // Match by group id if available
                // Note: Match by group's population id, was removed per cqf-measures conformance change FHIR-45423
                // multi-rate measures must have a group.id defined to be conformant
                if (groupDef.id().equals(mrgc.getId())) {
                    groupScoringType = groupDefMeasureScoring;
                }
            }
        }
        return checkMissingScoringType(groupScoringType);
    }

    protected void scoreGroup(
            MeasureScoring measureScoring, MeasureReportGroupComponent mrgc, boolean isIncreaseImprovementNotation) {
        switch (measureScoring) {
            case PROPORTION:
            case RATIO:
                Double score = this.calcProportionScore(
                        getGroupExtensionCount(mrgc, EXT_TOTAL_NUMERATOR_URL),
                        getGroupExtensionCount(mrgc, EXT_TOTAL_DENOMINATOR_URL));
                if (score != null) {
                    if (isIncreaseImprovementNotation) {
                        mrgc.setMeasureScore(new Quantity(score));
                    } else {
                        mrgc.setMeasureScore(new Quantity(1 - score));
                    }
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
                        getStratumPopulationCount(stratum, EXT_TOTAL_NUMERATOR_URL),
                        getStratumPopulationCount(stratum, EXT_TOTAL_DENOMINATOR_URL));
                if (score != null) {
                    stratum.setMeasureScore(new Quantity(score));
                }
                break;
            default:
                break;
        }
    }

    protected Integer getGroupExtensionCount(MeasureReportGroupComponent mrgc, String extUrl) {
        var ext = mrgc.getExtension().stream()
                .filter(x -> x.getUrl().equals(extUrl))
                .findFirst();
        return ext.map(extension -> Integer.valueOf(extension.getValue().toString()))
                .orElse(null);
    }

    protected Integer getStratumPopulationCount(StratifierGroupComponent sgc, String extUrl) {
        var pop = sgc.getExtension();
        var ext =
                pop.stream().filter(x -> x.getUrl().equals(extUrl)).findFirst().orElse(null);
        if (ext != null) {
            return Integer.valueOf(ext.getValue().toString());
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
