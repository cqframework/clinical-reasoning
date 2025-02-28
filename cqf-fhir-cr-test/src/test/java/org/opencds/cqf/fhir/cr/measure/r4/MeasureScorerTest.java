package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.utility.repository.FhirResourceLoader;

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
            scorer.score(measureUrl, measureScoringDef, measureReport);
            fail("this should throw error");
        } catch (InvalidRequestException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "Measure resources with more than one group component require a unique group.id() defined to score appropriately for MeasureDef: http://content.alphora.com/fhir/uv/mips-qm-content-r4/Measure/multirate-groupid"));
        }
    }

    @Test
    void scorerThrowsIfNoScoringSupplied() {
        var measureUrl = "http://some.measure.with.no.scoring";
        var mr = new MeasureReport();
        mr.addGroup();
        R4MeasureReportScorer scorer = new R4MeasureReportScorer();

        try {
            scorer.score(measureUrl, null, mr);
        } catch (InvalidRequestException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "MeasureDef is required in order to score a Measure for Measure: http://some.measure.with.no.scoring"));
        }
    }

    @Test
    void scorePopulationIdMultiRate() {
        var measureUrl = "http://ecqi.healthit.gov/ecqms/Measure/FHIR347";
        var measureScoringDef = getMeasureScoringDef(measureUrl);
        var measureReport = getMeasureReport(measureUrl);

        R4MeasureReportScorer scorer = new R4MeasureReportScorer();
        scorer.score(measureUrl, measureScoringDef, measureReport);

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
            scorer.score(measureUrl, measureScoringDef, measureReport);
        } catch (InvalidRequestException e) {
            assertTrue(
                    e.getMessage()
                            .contains(
                                    "Measure resources with more than one group component require a unique group.id() defined to score appropriately for MeasureDef: http://content.alphora.com/fhir/uv/mips-qm-content-r4/Measure/multirate-groupid-error"));
        }
    }

    @Test
    void scoreZeroDenominator() {
        var measureUrl = "http://content.alphora.com/fhir/uv/mips-qm-content-r4/Measure/multirate-zeroden";
        var measureScoringDef = getMeasureScoringDef(measureUrl);
        var measureReport = getMeasureReport(measureUrl);

        R4MeasureReportScorer scorer = new R4MeasureReportScorer();
        scorer.score(measureUrl, measureScoringDef, measureReport);
        // when denominator =0, no score should be added to report
        assertNull(group(measureReport, "DataCompleteness").getMeasureScore().getValue());
    }

    @Test
    void scoreNoExtension() {
        var measureUrl = "http://content.alphora.com/fhir/uv/mips-qm-content-r4/Measure/multirate-noext";
        var measureScoringDef = getMeasureScoringDef(measureUrl);
        var measureReport = getMeasureReport(measureUrl);

        R4MeasureReportScorer scorer = new R4MeasureReportScorer();
        scorer.score(measureUrl, measureScoringDef, measureReport);
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
        scorer.score(measureUrl, measureScoringDef, measureReport);
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

    @Test
    void missingScoringType() {
        var scorer = new R4MeasureReportScorer();

        var measureDef = mockMeasureDef(0);
        var measureReport = mockMeasureReport(null);

        try {
            scorer.score("url", measureDef, measureReport);
            fail("expected Exception");
        } catch (InvalidRequestException exception) {
            assertEquals(
                    "Measure does not have a scoring methodology defined. Add a \"scoring\" property to the measure definition or the group definition for MeasureDef: null",
                    exception.getMessage());
        }
    }

    @Test
    void emptyGroupId() {
        var scorer = new R4MeasureReportScorer();

        var measureDef = mockMeasureDef(2);
        var measureReport = mockMeasureReport("");

        try {
            scorer.score("url", measureDef, measureReport);
            fail("expected Exception");
        } catch (InvalidRequestException exception) {
            assertEquals(
                    "Measure resources with more than one group component require a unique group.id() defined to score appropriately for MeasureDef: null",
                    exception.getMessage());
        }
    }

    /**
     * test to validate that measure with MeasureScorer specified at the group level
     * and nothing on measure-level MeasureScorer
     */
    @Test
    void measure_eval_group_measurescorer() {
        var when = org.opencds.cqf.fhir.cr.measure.r4.Measure.given()
                .repositoryFor("MeasureScoring")
                .when()
                .measureId("GroupLevelMeasureScorerNoMeasureLevel")
                .subject(null)
                .periodStart("2018-01-01")
                .periodEnd("2030-12-31")
                .reportType("population")
                .evaluate();
        MeasureReport report = when.then().report();
        assertNotNull(report);
        assertEquals(1, report.getGroup().size());
        assertEquals(4, report.getGroupFirstRep().getPopulation().get(0).getCount());
    }

    @Test
    void measure_eval_group_measurescorer_invalidMeasureScore() {
        // Removed MeasureScorer from Measure, should trigger exception
        var when = org.opencds.cqf.fhir.cr.measure.r4.Measure.given()
                .repositoryFor("MeasureScoring")
                .when()
                .measureId("InvalidMeasureScorerMissing")
                .subject(null)
                .periodStart("2018-01-01")
                .periodEnd("2030-12-31")
                .reportType("population")
                .evaluate();

        String errorMsg =
                "MeasureScoring must be specified on Group or Measure for Measure: https://madie.cms.gov/Measure/InvalidMeasureScorerMissing";
        var e = assertThrows(InvalidRequestException.class, when::then);
        assertEquals(errorMsg, e.getMessage());
    }

    private MeasureDef mockMeasureDef(int numGroups) {
        final MeasureDef measureDef = mock(MeasureDef.class);
        doReturn(mockGroupDefs(numGroups)).when(measureDef).groups();
        return measureDef;
    }

    private List<GroupDef> mockGroupDefs(int numGroups) {
        return IntStream.range(0, numGroups).mapToObj(num -> mockGroupDef()).toList();
    }

    private GroupDef mockGroupDef() {
        final GroupDef groupDef = mock(GroupDef.class);

        doReturn(mockMeasureScoring()).when(groupDef).measureScoring();

        return groupDef;
    }

    private MeasureScoring mockMeasureScoring() {
        return mock(MeasureScoring.class);
    }

    private MeasureReport mockMeasureReport(@Nullable String groupId) {
        var measureReport = mock(MeasureReport.class);

        doReturn(List.of(mockMeasureGroup(groupId))).when(measureReport).getGroup();

        return measureReport;
    }

    private MeasureReportGroupComponent mockMeasureGroup(@Nullable String id) {
        var group = mock(MeasureReportGroupComponent.class);
        doReturn(id).when(group).getId();
        return group;
    }

    private MeasureReportGroupComponent group(MeasureReport measureReport, String id) {
        return measureReport.getGroup().stream()
                .filter(g -> g.getId().equals(id))
                .findFirst()
                .get();
    }

    private MeasureReport.MeasureReportGroupStratifierComponent getStratumById(
            MeasureReport measureReport, String groupId, String stratifierId) {
        var group = group(measureReport, groupId);
        return group.getStratifier().stream()
                .filter(g -> g.getId().equals(stratifierId))
                .findFirst()
                .orElseThrow();
    }

    private List<Measure> getMeasures() {
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

    private List<MeasureReport> getMeasureReports() {
        FhirResourceLoader measureReports = new FhirResourceLoader(
                FhirContext.forR4(), this.getClass(), List.of("MeasureScoring/MeasureReports/"), false);
        List<MeasureReport> measureReportList = new ArrayList<>();
        var reportResourceList = measureReports.getResources();
        for (IBaseResource resource : reportResourceList) {
            measureReportList.add((MeasureReport) resource);
        }
        return measureReportList;
    }

    private MeasureDef getMeasureScoringDef(String measureUrl) {
        var measureRes = measures.stream()
                .filter(measure -> measureUrl.equals(measure.getUrl()))
                .findAny()
                .orElse(null);
        R4MeasureDefBuilder measureDefBuilder = new R4MeasureDefBuilder();
        return measureDefBuilder.build(measureRes);
    }

    private MeasureReport getMeasureReport(String measureUrl) {
        return measureReports.stream()
                .filter(measureReport -> measureUrl.equals(measureReport.getMeasure()))
                .findAny()
                .orElse(null);
    }
}
