package org.opencds.cqf.cql.evaluator.measure.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportType;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;

import static org.testng.Assert.*;

public class Dstu3MeasureReportAggregatorTest {

    protected Dstu3MeasureReportAggregator aggregator;
    protected FhirContext fhirContext;

    @BeforeClass
    public void setup()
    {
        this.aggregator = new Dstu3MeasureReportAggregator();
        this.fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);
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
        one.setMeasure(new Reference("1"));

        MeasureReport two = new MeasureReport();
        one.setMeasure(new Reference("2"));

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
        MeasureReport left = (MeasureReport)parser.parseResource(Dstu3MeasureReportAggregatorTest.class.getResourceAsStream("AggregateReport1.json"));
        assertNotNull(left);

        MeasureReport right = (MeasureReport)parser.parseResource(Dstu3MeasureReportAggregatorTest.class.getResourceAsStream("AggregateReport2.json"));
        assertNotNull(right);

        MeasureReport expected = (MeasureReport)parser.parseResource(Dstu3MeasureReportAggregatorTest.class.getResourceAsStream("AggregatedReport.json"));
        assertNotNull(expected);

        MeasureReport actual = this.aggregator.aggregate(Arrays.asList(left, right));

        MeasureReport.MeasureReportGroupComponent actualMrgc = actual.getGroup().get(0);
        MeasureReport.MeasureReportGroupComponent expectedMrgc = expected.getGroup().get(0);

        MeasureValidationUtils.validateGroup(actualMrgc, "initial-population", 2);
        MeasureValidationUtils.validateGroup(expectedMrgc, "initial-population", 2);

        MeasureValidationUtils.validateGroup(actualMrgc, "numerator", 1);
        MeasureValidationUtils.validateGroup(expectedMrgc, "numerator", 1);

        MeasureValidationUtils.validateGroup(actualMrgc, "denominator", 2);
        MeasureValidationUtils.validateGroup(expectedMrgc, "denominator", 2);

        MeasureValidationUtils.validateGroup(actualMrgc, "denominator-exclusion", 0);
        MeasureValidationUtils.validateGroup(expectedMrgc, "denominator-exclusion", 0);

    }

    @Test
    public void aggregateReports_subject_listType_combines_reports() {
        IParser parser = fhirContext.newJsonParser();
        MeasureReport left = (MeasureReport)parser.parseResource(Dstu3MeasureReportAggregatorTest.class.getResourceAsStream("AggregateReport-subject-list1.json"));
        assertNotNull(left);

        MeasureReport right = (MeasureReport)parser.parseResource(Dstu3MeasureReportAggregatorTest.class.getResourceAsStream("AggregateReport-subject-list2.json"));
        assertNotNull(right);

        MeasureReport expected = (MeasureReport)parser.parseResource(Dstu3MeasureReportAggregatorTest.class.getResourceAsStream("AggregateReport-subject-list.json"));
        assertNotNull(expected);

        MeasureReport actual = this.aggregator.aggregate(Arrays.asList(left, right));

        MeasureValidationUtils.validateMeasureReportContained(expected, actual);

        MeasureReport.MeasureReportGroupComponent actualMrgc = actual.getGroup().get(0);
        MeasureReport.MeasureReportGroupComponent expectedMrgc = actual.getGroup().get(0);

        MeasureValidationUtils.validateStratifier(actualMrgc.getStratifierFirstRep(), "true", "initial-population", 500);
        MeasureValidationUtils.validateStratifier(expectedMrgc.getStratifierFirstRep(), "true", "initial-population", 500);

        MeasureValidationUtils.validateStratifier(actualMrgc.getStratifierFirstRep(), "true", "numerator", 200);
        MeasureValidationUtils.validateStratifier(expectedMrgc.getStratifierFirstRep(), "true", "numerator", 200);

        MeasureValidationUtils.validateStratifier(actualMrgc.getStratifierFirstRep(), "false", "initial-population", 500);
        MeasureValidationUtils.validateStratifier(expectedMrgc.getStratifierFirstRep(), "false", "initial-population", 500);

        MeasureValidationUtils.validateStratifier(actualMrgc.getStratifier().get(1), "true", "initial-population", 500);
        MeasureValidationUtils.validateStratifier(expectedMrgc.getStratifier().get(1), "true", "initial-population", 500);

    }
}
