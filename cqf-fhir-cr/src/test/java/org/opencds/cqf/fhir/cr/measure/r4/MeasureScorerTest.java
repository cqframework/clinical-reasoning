package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.test.FhirResourceLoader;

public class MeasureScorerTest {

    List<Measure> myMeasures = getMyMeasures();
    List<MeasureReport> myMeasureReports = getMyMeasureReports();

    @Test
    public void testScore_groupIdMultiRateMeasure() {
        var measureUrl = "http://content.alphora.com/fhir/uv/mips-qm-content-r4/Measure/multirate-groupid";
        var measureScoringDef = getMeasureScoringDef(measureUrl);
        var measureReport = getMyMeasureReport(measureUrl);

        R4MeasureReportScorer scorer = new R4MeasureReportScorer();
        scorer.score(measureScoringDef, measureReport);
        assertEquals(
                group(measureReport, "DataCompleteness")
                        .getMeasureScore()
                        .getValue()
                        .toString(),
                "1.0");
        assertEquals(
                group(measureReport, "PerformanceRate")
                        .getMeasureScore()
                        .getValue()
                        .toString(),
                "1.0");
    }

    @Test
    public void testScore_populationIdMultiRate() {
        var measureUrl = "http://ecqi.healthit.gov/ecqms/Measure/FHIR347";
        var measureScoringDef = getMeasureScoringDef(measureUrl);
        var measureReport = getMyMeasureReport(measureUrl);

        R4MeasureReportScorer scorer = new R4MeasureReportScorer();
        scorer.score(measureScoringDef, measureReport);
        assertTrue(measureReport.getGroup().get(0).getMeasureScore().getValue().doubleValue() > 0);
        assertTrue(measureReport.getGroup().get(1).getMeasureScore().getValue().doubleValue() > 0);
        assertTrue(measureReport.getGroup().get(2).getMeasureScore().getValue().doubleValue() > 0);
    }

    @Test
    public void testScore_error_noids() {
        var measureUrl = "http://content.alphora.com/fhir/uv/mips-qm-content-r4/Measure/multirate-groupid-error";
        var measureScoringDef = getMeasureScoringDef(measureUrl);
        var measureReport = getMyMeasureReport(measureUrl);

        R4MeasureReportScorer scorer = new R4MeasureReportScorer();
        assertThrows(
                IllegalArgumentException.class,
                () -> scorer.score(measureScoringDef, measureReport),
                "No MeasureScoring value set");
    }

    public MeasureReportGroupComponent group(MeasureReport measureReport, String id) {
        return measureReport.getGroup().stream()
                .filter(g -> g.getId().equals(id))
                .findFirst()
                .get();
    }

    public List<Measure> getMyMeasures() {
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

    public List<MeasureReport> getMyMeasureReports() {
        FhirResourceLoader measureReports = new FhirResourceLoader(
                FhirContext.forR4(), this.getClass(), List.of("MeasureScoring/MeasureReports/"), false);
        List<MeasureReport> measureReportList = new ArrayList<>();
        var reportResourceList = measureReports.getResources();
        for (IBaseResource resource : reportResourceList) {
            measureReportList.add((MeasureReport) resource);
        }
        return measureReportList;
    }

    public Map<GroupDef, MeasureScoring> getMeasureScoringDef(String measureUrl) {
        var measureRes = myMeasures.stream()
                .filter(measure -> measureUrl.equals(measure.getUrl()))
                .findAny()
                .orElse(null);
        R4MeasureDefBuilder measureDefBuilder = new R4MeasureDefBuilder();
        return measureDefBuilder.build(measureRes).scoring();
    }

    public MeasureReport getMyMeasureReport(String measureUrl) {
        return myMeasureReports.stream()
                .filter(measureReport -> measureUrl.equals(measureReport.getMeasure()))
                .findAny()
                .orElse(null);
    }
}
