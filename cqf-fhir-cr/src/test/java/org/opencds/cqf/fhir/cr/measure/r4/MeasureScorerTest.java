package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.context.FhirContext;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.test.FhirResourceLoader;

class MeasureScorerTest {

    List<Measure> measures = getMeasures();
    List<MeasureReport> measureReports = getMeasureReports();

    @Test
    void scoreOnlyPopulationIdMultiRateMeasure() {
        var measureUrl = "http://content.alphora.com/fhir/uv/mips-qm-content-r4/Measure/multirate-groupid";
        var measureScoringDef = getMeasureScoringDef(measureUrl);
        var measureReport = getMeasureReport(measureUrl);

        try {
            R4MeasureReportScorer scorer = new R4MeasureReportScorer();
            scorer.score(measureScoringDef, measureReport);
            fail("this should throw error");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "Measure resources with more than one group component require a unique group.id() defined to score appropriately"));
        }
    }

    @Test
    void scorerThrowsIfNoScoringSupplied() {
        var mr = new MeasureReport();
        mr.addGroup();
        R4MeasureReportScorer scorer = new R4MeasureReportScorer();

        try {
            scorer.score(null, mr);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MeasureDef is required in order to score a Measure."));
        }
    }

    @Test
    void scorePopulationIdMultiRate() {
        var measureUrl = "http://ecqi.healthit.gov/ecqms/Measure/FHIR347";
        var measureScoringDef = getMeasureScoringDef(measureUrl);
        var measureReport = getMeasureReport(measureUrl);

        R4MeasureReportScorer scorer = new R4MeasureReportScorer();
        scorer.score(measureScoringDef, measureReport);

        assertEquals(
                "1.0",
                group(measureReport, "group-1").getMeasureScore().getValue().toString());
        assertEquals(
                "0.5",
                group(measureReport, "group-2").getMeasureScore().getValue().toString());
        assertEquals(
                "0.5",
                group(measureReport, "group-3").getMeasureScore().getValue().toString());
    }

    @Test
    void scoreErrorNoIds() {
        var measureUrl = "http://content.alphora.com/fhir/uv/mips-qm-content-r4/Measure/multirate-groupid-error";
        var measureScoringDef = getMeasureScoringDef(measureUrl);
        var measureReport = getMeasureReport(measureUrl);
        try {
            R4MeasureReportScorer scorer = new R4MeasureReportScorer();
            scorer.score(measureScoringDef, measureReport);
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "Measure resources with more than one group component require a unique group.id() defined to score appropriately"));
        }
    }

    @Test
    void scoreZeroDenominator() {
        var measureUrl = "http://content.alphora.com/fhir/uv/mips-qm-content-r4/Measure/multirate-zeroden";
        var measureScoringDef = getMeasureScoringDef(measureUrl);
        var measureReport = getMeasureReport(measureUrl);

        R4MeasureReportScorer scorer = new R4MeasureReportScorer();
        scorer.score(measureScoringDef, measureReport);
        // when denominator =0, no score should be added to report
        assertNull(group(measureReport, "DataCompleteness").getMeasureScore().getValue());
    }

    @Test
    void scoreNoExtension() {
        var measureUrl = "http://content.alphora.com/fhir/uv/mips-qm-content-r4/Measure/multirate-noext";
        var measureScoringDef = getMeasureScoringDef(measureUrl);
        var measureReport = getMeasureReport(measureUrl);

        R4MeasureReportScorer scorer = new R4MeasureReportScorer();
        scorer.score(measureScoringDef, measureReport);
        // if no extension elements are generated for totalDen or totalNum, no measureScore should be added
        assertNull(group(measureReport, "PerformanceRate").getMeasureScore().getValue());
    }

    @Test
    void scoreGroupIdMultiStratum() {
        var measureUrl =
                "http://ecqi.healthit.gov/ecqms/Measure/PrimaryCariesPreventionasOfferedbyPCPsincludingDentistsFHIR";
        var measureScoringDef = getMeasureScoringDef(measureUrl);
        var measureReport = getMeasureReport(measureUrl);

        R4MeasureReportScorer scorer = new R4MeasureReportScorer();
        scorer.score(measureScoringDef, measureReport);
        assertEquals(
                "0.5",
                group(measureReport, "group-1").getMeasureScore().getValue().toString());
        // stratum 3
        assertEquals(
                "0.5",
                getStratumById(measureReport, "group-1", "8C1CBAAF-B59F-496F-9282-428E5D2C31FB")
                        .getStratum()
                        .get(0)
                        .getMeasureScore()
                        .getValue()
                        .toString());
        // stratum 4
        assertEquals(
                "0.6",
                getStratumById(measureReport, "group-1", "84277D0F-546A-48D8-B4D6-4B2E849935E3")
                        .getStratum()
                        .get(0)
                        .getMeasureScore()
                        .getValue()
                        .toString());
        // stratum 5
        assertEquals(
                "0.3333333333333333",
                getStratumById(measureReport, "group-1", "75533601-6C60-4150-86E9-DEBA743ED515")
                        .getStratum()
                        .get(0)
                        .getMeasureScore()
                        .getValue()
                        .toString());
    }

    MeasureReportGroupComponent group(MeasureReport measureReport, String id) {
        return measureReport.getGroup().stream()
                .filter(g -> g.getId().equals(id))
                .findFirst()
                .get();
    }

    public MeasureReport.MeasureReportGroupStratifierComponent getStratumById(
            MeasureReport measureReport, String groupId, String stratifierId) {
        var group = group(measureReport, groupId);
        return group.getStratifier().stream()
                .filter(g -> g.getId().equals(stratifierId))
                .findFirst()
                .get();
    }

    public List<Measure> getMeasures() {
        // Measures
        FhirResourceLoader measures = new FhirResourceLoader(
                FhirContext.forR4(), this.getClass(), List.of("MeasureScoring/Measures/"), false);
        List<Measure> measureList = new ArrayList<>();
        var resourceList = measures.getResources();
        for (IBaseResource resource : resourceList) {
            measureList.add((Measure) resource);
        }
        return measureList;
    }

    public List<MeasureReport> getMeasureReports() {
        FhirResourceLoader measureReports = new FhirResourceLoader(
                FhirContext.forR4(), this.getClass(), List.of("MeasureScoring/MeasureReports/"), false);
        List<MeasureReport> measureReportList = new ArrayList<>();
        var reportResourceList = measureReports.getResources();
        for (IBaseResource resource : reportResourceList) {
            measureReportList.add((MeasureReport) resource);
        }
        return measureReportList;
    }

    public MeasureDef getMeasureScoringDef(String measureUrl) {
        var measureRes = measures.stream()
                .filter(measure -> measureUrl.equals(measure.getUrl()))
                .findAny()
                .orElse(null);
        R4MeasureDefBuilder measureDefBuilder = new R4MeasureDefBuilder();
        return measureDefBuilder.build(measureRes);
    }

    public MeasureReport getMeasureReport(String measureUrl) {
        return measureReports.stream()
                .filter(measureReport -> measureUrl.equals(measureReport.getMeasure()))
                .findAny()
                .orElse(null);
    }
}
