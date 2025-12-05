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
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumPopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueWrapper;
import org.opencds.cqf.fhir.utility.repository.FhirResourceLoader;

class MeasureScorerTest {

    // Population type codes
    private static final String INITIAL_POPULATION = "initial-population";
    private static final String DENOMINATOR = "denominator";
    private static final String DENOMINATOR_EXCLUSION = "denominator-exclusion";
    private static final String DENOMINATOR_EXCEPTION = "denominator-exception";
    private static final String NUMERATOR = "numerator";

    /**
     * Record to hold population counts for a group or stratum.
     * Added by Claude Sonnet 4.5 on 2025-12-02.
     */
    record PopulationCounts(
            int initialPopulation,
            int denominator,
            int denominatorExclusion,
            int denominatorException,
            int numerator) {}

    /**
     * Record to hold stratum population counts with the stratum value.
     * Added by Claude Sonnet 4.5 on 2025-12-02.
     */
    record StratumCounts(String stratumValue, PopulationCounts populationCounts) {}

    List<Measure> measures = getMeasures();
    List<MeasureReport> measureReports = getMeasureReports();

    @Test
    void scoreOnlyPopulationIdMultiRateMeasure() {
        var measureUrl = "http://content.alphora.com/fhir/uv/mips-qm-content-r4/Measure/multirate-groupid";
        var measureScoringDef = getMeasureScoringDef(measureUrl);
        var measureReport = getMeasureReport(measureUrl);
        R4MeasureReportScorer scorer = new R4MeasureReportScorer();

        try {
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

        // Hard-coded counts from measurereport-fhir347.json
        // Group: group-1, Populations: initial-population=1, denominator=1, denominator-exclusion=0,
        // denominator-exception=0, numerator=1
        // Group: group-2, Populations: initial-population=2, denominator=3, denominator-exclusion=1,
        // denominator-exception=0, numerator=1
        // Group: group-3, Populations: initial-population=1, denominator=3, denominator-exclusion=0,
        // denominator-exception=1, numerator=1
        List<PopulationCounts> groupCounts = List.of(
                new PopulationCounts(1, 1, 0, 0, 1), // group-1
                new PopulationCounts(2, 3, 1, 0, 1), // group-2
                new PopulationCounts(1, 3, 0, 1, 1) // group-3
                );

        // Populate MeasureDef using the hard-coded integer counts
        populateMeasureDefWithCounts(measureScoringDef, groupCounts);

        // Verify counts match between MeasureDef and MeasureReport for each group
        verifyGroupPopulationCounts(measureScoringDef, measureReport);

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

        // Hard-coded counts from measurereport-primarycariespreventionasofferedbypcpincludingdentistfhir.json
        // Group: group-1, Populations: initial-population=10, denominator=10, denominator-exclusion=0,
        // numerator=5
        List<PopulationCounts> groupCounts = List.of(
                new PopulationCounts(10, 10, 0, 0, 5) // group-1
                );

        // Populate MeasureDef using the hard-coded integer counts
        populateMeasureDefWithCounts(measureScoringDef, groupCounts);

        // Hard-coded stratum counts for stratifiers
        // Stratifier 0 (Stratum 3): value "true", initial-population=10, denominator=10,
        // denominator-exclusion=0, numerator=5
        // Stratifier 1 (Stratum 4): value "false", initial-population=5, denominator=5,
        // denominator-exclusion=0, numerator=3
        // Stratifier 2 (Stratum 5): value "false", initial-population=3, denominator=3,
        // denominator-exclusion=0, numerator=1
        List<List<StratumCounts>> stratifierCounts = List.of(
                List.of(new StratumCounts("true", new PopulationCounts(10, 10, 0, 0, 5))), // stratifier 0
                List.of(new StratumCounts("false", new PopulationCounts(5, 5, 0, 0, 3))), // stratifier 1
                List.of(new StratumCounts("false", new PopulationCounts(3, 3, 0, 0, 1))) // stratifier 2
                );

        GroupDef groupDef = measureScoringDef.groups().get(0);
        for (int i = 0; i < stratifierCounts.size(); i++) {
            populateStratifierWithCounts(groupDef, i, stratifierCounts.get(i));
        }

        // Verify counts match between MeasureDef and MeasureReport for group-level populations
        verifyGroupPopulationCounts(measureScoringDef, measureReport);

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
        assertEquals(16, report.getGroupFirstRep().getPopulation().get(0).getCount());
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
        FhirResourceLoader measuresInner = new FhirResourceLoader(
                FhirContext.forR4(), this.getClass(), List.of("MeasureScoring/Measures/"), false);
        List<Measure> measureList = new ArrayList<>();
        var resourceList = measuresInner.getResources();
        for (IBaseResource resource : resourceList) {
            measureList.add((Measure) resource);
        }
        return measureList;
    }

    private List<MeasureReport> getMeasureReports() {
        FhirResourceLoader measureReportsInner = new FhirResourceLoader(
                FhirContext.forR4(), this.getClass(), List.of("MeasureScoring/MeasureReports/"), false);
        List<MeasureReport> measureReportList = new ArrayList<>();
        var reportResourceList = measureReportsInner.getResources();
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

    /**
     * Added by Claude Sonnet 4.5 on 2025-12-02
     * Verify that counts match between MeasureDef and MeasureReport for all groups.
     *
     * @param measureDef the MeasureDef with populated counts
     * @param measureReport the MeasureReport to compare against
     */
    private void verifyGroupPopulationCounts(MeasureDef measureDef, MeasureReport measureReport) {
        for (int i = 0; i < measureDef.groups().size(); i++) {
            GroupDef groupDef = measureDef.groups().get(i);
            MeasureReportGroupComponent reportGroup = measureReport.getGroup().get(i);

            for (var populationDef : groupDef.populations()) {
                int defCount = populationDef.getCount(groupDef);
                int reportCount = reportGroup.getPopulation().stream()
                        .filter(pop -> pop.getCode()
                                .getCodingFirstRep()
                                .getCode()
                                .equals(populationDef.type().toCode()))
                        .map(MeasureReportGroupPopulationComponent::getCount)
                        .findFirst()
                        .orElse(0);
                assertEquals(
                        reportCount,
                        defCount,
                        "Count mismatch for group "
                                + i
                                + " population "
                                + populationDef.type()
                                + ": MeasureReport has "
                                + reportCount
                                + ", MeasureDef has "
                                + defCount);
            }
        }
    }

    /**
     * Added by Claude Sonnet 4.5 on 2025-12-02
     * Populate a PopulationDef with the specified count of mock subjects/resources.
     * Independent of MeasureReport - only uses the integer count.
     *
     * @param populationDef the PopulationDef to populate
     * @param count the number of subjects/resources to add
     * @param groupIndex the group index (used for generating unique subject IDs)
     * @param isBooleanBasis whether the group uses boolean basis (true) or resource basis (false)
     */
    private void populatePopulationDefWithCount(
            PopulationDef populationDef, int count, int groupIndex, boolean isBooleanBasis) {
        if (isBooleanBasis) {
            // Add subjects (e.g., "Patient/1", "Patient/2", ...)
            for (int i = 0; i < count; i++) {
                populationDef.addResource("Patient/" + (groupIndex * 1000 + i), new Object());
            }
        } else {
            // Add resources
            for (int i = 0; i < count; i++) {
                populationDef.addResource("subject-" + i, new Object());
            }
        }
    }

    /**
     * Updated by Claude Sonnet 4.5 on 2025-12-02
     * Populate MeasureDef's PopulationDef objects using PopulationCounts records.
     * This simulates the state after evaluation, where PopulationDef has been
     * populated with evaluated subjects/resources.
     *
     * @param measureDef the MeasureDef to populate
     * @param groupCounts list of PopulationCounts, one per group
     */
    private void populateMeasureDefWithCounts(MeasureDef measureDef, List<PopulationCounts> groupCounts) {
        for (int groupIndex = 0;
                groupIndex < measureDef.groups().size() && groupIndex < groupCounts.size();
                groupIndex++) {
            GroupDef groupDef = measureDef.groups().get(groupIndex);
            PopulationCounts counts = groupCounts.get(groupIndex);

            // Populate each PopulationDef with mock subjects based on the counts
            for (var populationDef : groupDef.populations()) {
                int count =
                        getCountForPopulationType(counts, populationDef.type().toCode());
                if (count > 0) {
                    populatePopulationDefWithCount(populationDef, count, groupIndex, groupDef.isBooleanBasis());
                }
            }
        }
    }

    /**
     * Helper to extract count from PopulationCounts record by population type code.
     * Added by Claude Sonnet 4.5 on 2025-12-02.
     */
    private int getCountForPopulationType(PopulationCounts counts, String populationType) {
        return switch (populationType) {
            case INITIAL_POPULATION -> counts.initialPopulation();
            case DENOMINATOR -> counts.denominator();
            case DENOMINATOR_EXCLUSION -> counts.denominatorExclusion();
            case DENOMINATOR_EXCEPTION -> counts.denominatorException();
            case NUMERATOR -> counts.numerator();
            default -> 0;
        };
    }

    /**
     * Updated by Claude Sonnet 4.5 on 2025-12-02
     * Populate StratifierDef with StratumDef objects containing StratumPopulationDef data.
     * This creates the stratum populations needed for stratified scoring.
     *
     * @param groupDef the GroupDef containing the stratifiers
     * @param stratifierIndex the index of the stratifier to populate
     * @param stratumCountsList list of StratumCounts for this stratifier
     */
    private void populateStratifierWithCounts(
            GroupDef groupDef, int stratifierIndex, List<StratumCounts> stratumCountsList) {

        if (stratifierIndex >= groupDef.stratifiers().size()) {
            return;
        }

        var stratifierDef = groupDef.stratifiers().get(stratifierIndex);
        var stratumDefs = new ArrayList<StratumDef>();

        // Create a StratumDef for each stratum value
        for (StratumCounts stratumCounts : stratumCountsList) {
            String stratumValue = stratumCounts.stratumValue();
            PopulationCounts populationCounts = stratumCounts.populationCounts();

            // Create StratumPopulationDef for each population in this stratum
            var stratumPopulations = new ArrayList<StratumPopulationDef>();

            for (var populationDef : groupDef.populations()) {
                int count = getCountForPopulationType(
                        populationCounts, populationDef.type().toCode());
                if (count > 0) {
                    // For CRITERIA stratifiers, the count comes from evaluationResultIntersection
                    // Create mock evaluation results for the count
                    var evaluationResults = new HashSet<Object>();
                    for (int i = 0; i < count; i++) {
                        evaluationResults.add("Result-" + stratumValue + "-" + i);
                    }

                    // Create a simple CodeDef for population basis (assuming boolean)
                    var populationBasis =
                            new CodeDef("boolean", "http://terminology.hl7.org/CodeSystem/fhir-types", null, null);

                    // Create StratumPopulationDef
                    // For CRITERIA stratifiers, the count comes from populationDefEvaluationResultIntersection
                    var stratumPopDef = new StratumPopulationDef(
                            populationDef, // Use direct PopulationDef reference
                            new HashSet<>(), // subjects (not used for CRITERIA)
                            evaluationResults, // evaluationResultIntersection (used for CRITERIA count)
                            new ArrayList<>(), // resourceIdsForSubjectList
                            MeasureStratifierType.CRITERIA,
                            populationBasis);

                    stratumPopulations.add(stratumPopDef);
                }
            }

            // Create StratumValueDef
            var stratumValueWrapper = new StratumValueWrapper(stratumValue);
            var stratumValueDef = new StratumValueDef(stratumValueWrapper, null);
            var stratumValueDefs = new HashSet<StratumValueDef>();
            stratumValueDefs.add(stratumValueDef);

            // Create StratumDef
            var stratumDef = new StratumDef(
                    stratumPopulations, stratumValueDefs, new ArrayList<>() // subjectIds
                    );

            stratumDefs.add(stratumDef);
        }

        // Add all StratumDefs to the StratifierDef
        stratifierDef.addAllStratum(stratumDefs);
    }
}
