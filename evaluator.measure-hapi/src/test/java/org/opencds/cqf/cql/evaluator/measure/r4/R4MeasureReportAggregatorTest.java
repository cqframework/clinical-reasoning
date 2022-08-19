package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class R4MeasureReportAggregatorTest {

    protected R4MeasureReportAggregator aggregator;
    protected FhirContext fhirContext;

    @BeforeClass
    public void setup()
    {
        this.aggregator = new R4MeasureReportAggregator();
        this.fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    }

    @Test
    public void aggregate_null_returns_null() {
        MeasureReport actual = this.aggregator.aggregate(null);

        assertNull(actual);
    }

    @Test
    public void aggregate_empty_list_returns_null() {
        MeasureReport actual = this.aggregator.aggregate(Collections.emptyList());

        assertNull(actual);
    }

    @Test
    public void aggregate_singleton_list_returns_singleton() {
        MeasureReport expected = new MeasureReport();
        MeasureReport actual = aggregator.aggregate(Collections.singletonList(expected));

        assertEquals(expected, actual);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void aggregate_individual_report_throws_exception() {
        MeasureReport one = new MeasureReport();
        one.setType(MeasureReportType.INDIVIDUAL);
        aggregator.aggregate(Collections.singletonList(one));
    }


    @Test(expectedExceptions = IllegalArgumentException.class)
    public void aggregate_mismatched_measure_throws_exception() {
        MeasureReport one = new MeasureReport();
        one.setMeasure("1");

        MeasureReport two = new MeasureReport();
        one.setMeasure("2");

        aggregator.aggregate(Lists.newArrayList(one, two));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void aggregate_mismatched_period_throws_exception() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy");
        Period periodOne = new Period();
        periodOne.setStart(format.parse("1999"));
        periodOne.setEnd(format.parse("2000"));

        MeasureReport one = new MeasureReport();
        one.setPeriod(periodOne);

        Period periodTwo = new Period();
        periodTwo.setStart(format.parse("2000"));
        periodTwo.setEnd(format.parse("2001"));

        MeasureReport two = new MeasureReport();
        two.setPeriod(periodTwo);

        aggregator.aggregate(Lists.newArrayList(one, two));
    }
    

    @Test
    public void aggregateReports_combines_reports() {
        IParser parser = fhirContext.newJsonParser();
        MeasureReport left = (MeasureReport)parser.parseResource(R4MeasureReportAggregatorTest.class.getResourceAsStream("AggregateReport1.json"));
        assertNotNull(left);

        MeasureReport right = (MeasureReport)parser.parseResource(R4MeasureReportAggregatorTest.class.getResourceAsStream("AggregateReport2.json"));
        assertNotNull(right);

        MeasureReport expected = (MeasureReport)parser.parseResource(R4MeasureReportAggregatorTest.class.getResourceAsStream("AggregatedReport.json"));
        assertNotNull(expected);

        MeasureReport actual = this.aggregator.aggregate(Arrays.asList(left, right));

        MeasureReport.MeasureReportGroupComponent actualMrgc = actual.getGroup().get(0);
        MeasureReport.MeasureReportGroupComponent expectedMrgc = expected.getGroup().get(0);

        assertTrue(actual.getExtension().stream().anyMatch(item ->
                item.getUrl().equals("http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-supplementalData") &&
                        item.getValue() instanceof Reference &&
                        ((Reference) item.getValue()).getReference().equals("Encounter/DM1-patient-1-encounter-2") &&
                        ( item.getValue()).getExtension().size() == 3
        ));

        assertEquals(actual.getEvaluatedResource().size(), expected.getEvaluatedResource().size());

        assertTrue(actual.getEvaluatedResource().stream().anyMatch(item ->
                item.getReference().equals("Condition/DM1-patient-1-condition-1") &&
                        item.getExtension().size() == 2));

        MeasureValidationUtils.validateGroup(actualMrgc, "initial-population", 10);
        MeasureValidationUtils.validateGroup(expectedMrgc, "initial-population", 10);

        MeasureValidationUtils.validateGroup(actualMrgc, "numerator", 10);
        MeasureValidationUtils.validateGroup(expectedMrgc, "numerator", 10);

        MeasureValidationUtils.validateGroup(actualMrgc, "numerator-exclusion", 2);
        MeasureValidationUtils.validateGroup(expectedMrgc, "numerator-exclusion", 2);

        MeasureValidationUtils.validateGroup(actualMrgc, "denominator", 30);
        MeasureValidationUtils.validateGroup(expectedMrgc, "denominator", 30);

        MeasureValidationUtils.validateGroup(actualMrgc, "denominator-exclusion", 10);
        MeasureValidationUtils.validateGroup(expectedMrgc, "denominator-exclusion", 10);

        MeasureValidationUtils.validateGroup(actualMrgc, "denominator-exception", 1);
        MeasureValidationUtils.validateGroup(expectedMrgc, "denominator-exception", 1);

        MeasureValidationUtils.validateStratifier(actualMrgc.getStratifierFirstRep(), "male", "initial-population", 400);
        MeasureValidationUtils.validateStratifier(expectedMrgc.getStratifierFirstRep(), "male", "numerator", 150);

        MeasureValidationUtils.validateStratifier(actualMrgc.getStratifierFirstRep(), "male", "numerator-exclusion", 10);
        MeasureValidationUtils.validateStratifier(actualMrgc.getStratifierFirstRep(), "male", "denominator-exception", 25);
    }

    @Test
    public void aggregateReports_subject_listType_combines_reports() {
        IParser parser = fhirContext.newJsonParser();
        MeasureReport left = (MeasureReport)parser.parseResource(R4MeasureReportAggregatorTest.class.getResourceAsStream("AggregateReport-subject-list1.json"));
        assertNotNull(left);

        MeasureReport right = (MeasureReport)parser.parseResource(R4MeasureReportAggregatorTest.class.getResourceAsStream("AggregateReport-subject-list2.json"));
        assertNotNull(right);

        MeasureReport expected = (MeasureReport)parser.parseResource(R4MeasureReportAggregatorTest.class.getResourceAsStream("AggregateReport-subject-list.json"));
        assertNotNull(expected);

        MeasureReport actual = this.aggregator.aggregate(Arrays.asList(left, right));

        MeasureValidationUtils.validateMeasureReportContained(expected, actual);
        MeasureReport.MeasureReportGroupComponent actualMrgc = actual.getGroup().get(0);
        MeasureReport.MeasureReportGroupComponent expectedMrgc = actual.getGroup().get(0);

        MeasureValidationUtils.validateStratifier(actualMrgc.getStratifierFirstRep(), "false", "initial-population", 20);
        MeasureValidationUtils.validateStratifier(expectedMrgc.getStratifierFirstRep(), "false", "initial-population", 20);

        MeasureValidationUtils.validateStratifier(actualMrgc.getStratifierFirstRep(), "false", "denominator", 16);
        MeasureValidationUtils.validateStratifier(expectedMrgc.getStratifierFirstRep(), "false", "denominator", 16);

        MeasureValidationUtils.validateStratifier(actualMrgc.getStratifier().get(1), "true", "initial-population", 12);
        MeasureValidationUtils.validateStratifier(expectedMrgc.getStratifier().get(1), "true", "initial-population", 12);

        MeasureValidationUtils.validateStratifier(actualMrgc.getStratifier().get(1), "true", "denominator", 8);
        MeasureValidationUtils.validateStratifier(expectedMrgc.getStratifier().get(1), "true", "denominator", 8);

    }

}
