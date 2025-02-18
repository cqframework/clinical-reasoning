package ca.uhn.fhir.cr.dstu3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.cr.dstu3.measure.MeasureOperationsProvider;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.math.BigDecimal;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {TestCrDstu3Config.class})
class MeasureOperationProviderTest extends BaseCrDstu3TestServer {

    @Autowired
    MeasureOperationsProvider myMeasureOperationsProvider;

    protected void compareMeasureReport(MeasureReport expected, MeasureReport actual) {
        assertNotNull(expected, "expected MeasureReport can not be null");
        assertNotNull(actual, "actual MeasureReport can not be null");

        String errorLocator = String.format(
                "Measure: %s, Subject: %s",
                expected.getMeasure().getReference(), expected.getPatient().getReference());

        assertThat(actual.hasGroup()).as(errorLocator).isEqualTo(expected.hasGroup());
        assertThat(actual.getGroup()).as(errorLocator).hasSameSizeAs(expected.getGroup());

        for (MeasureReportGroupComponent mrgcExpected : expected.getGroup()) {
            Optional<MeasureReportGroupComponent> mrgcActualOptional = actual.getGroup().stream()
                    .filter(x -> x.getId() != null
                            && x.getId().equals(mrgcExpected.getIdentifier().getValue()))
                    .findFirst();

            errorLocator = String.format(
                    "Measure: %s, Subject: %s, Group: %s",
                    expected.getMeasure().getReference(),
                    expected.getPatient().getReference(),
                    mrgcExpected.getIdentifier().getValue());
            assertTrue(mrgcActualOptional.isPresent(), errorLocator);

            MeasureReportGroupComponent mrgcActual = mrgcActualOptional.get();

            if (mrgcExpected.getMeasureScore() == null) {
                assertThat(mrgcActual.getMeasureScore()).as(errorLocator).isNull();
            } else {
                assertNotNull(mrgcActual.getMeasureScore(), errorLocator);
                BigDecimal decimalExpected = mrgcExpected.getMeasureScore();
                BigDecimal decimalActual = mrgcActual.getMeasureScore();

                assertThat(decimalActual).as(errorLocator).isEqualByComparingTo(decimalExpected);
            }
        }
    }

    // pull posted measureReport from measure bundle
    public MeasureReport getExpected(String measureReportId) {
        return ourClient
                .read()
                .resource(MeasureReport.class)
                .withId("MeasureReport/" + measureReportId)
                .execute();
    }

    public MeasureReport getActual(
            String periodStart,
            String periodEnd,
            String patient,
            String measureId,
            String reportType,
            Bundle additionalData) {

        var parametersEval1 = new Parameters();
        parametersEval1.addParameter().setName("periodStart").setValue(new DateType(periodStart));
        parametersEval1.addParameter().setName("periodEnd").setValue(new DateType(periodEnd));
        parametersEval1.addParameter().setName("patient").setValue(new StringType(patient));
        parametersEval1.addParameter().setName("reportType").setValue(new StringType(reportType));
        if (additionalData != null) {
            parametersEval1.addParameter().setName("additionalData").setResource(additionalData);
        }

        return ourClient
                .operation()
                .onInstance(measureId)
                .named(ProviderConstants.CR_OPERATION_EVALUATE_MEASURE)
                .withParameters(parametersEval1)
                .returnResourceType(MeasureReport.class)
                .execute();
    }

    // validate dstu3 evaluate calculates as expected
    @Test
    void test_EXM124_FHIR3_72000() {
        loadBundle("ca/uhn/fhir/cr/dstu3/connectathon/EXM124-FHIR3-7.2.000-bundle.json");
        var actual = getActual(
                "2019-01-01",
                "2019-12-31",
                "Patient/numer-EXM124-FHIR3",
                "Measure/measure-EXM124-FHIR3-7.2.000",
                "subject",
                null);
        var expected = getExpected("measurereport-numer-EXM124-FHIR3");

        compareMeasureReport(expected, actual);
    }

    // validate dstu3 evaluate executes for measure EXM104
    @Test
    void test_EXM104_FHIR3_81000() {
        loadBundle("ca/uhn/fhir/cr/dstu3/connectathon/EXM104-FHIR3-8.1.000-bundle.json");
        var actual = getActual(
                "2019-01-01",
                "2019-12-31",
                "Patient/numer-EXM104-FHIR3",
                "Measure/measure-EXM104-FHIR3-8.1.000",
                "subject",
                null);
        assertNotNull(actual);
    }

    // validate dstu3 evaluate executes for measure EXM105
    @Test
    void test_EXM105_FHIR3() {
        loadBundle("Exm105Fhir3Measure.json");
        var actual = getActual(
                "2019-01-01",
                "2020-01-01",
                "Patient/denom-EXM105-FHIR3",
                "Measure/measure-EXM105-FHIR3-8.0.000",
                "subject",
                null);
        assertNotNull(actual);
    }

    // validate dstu3 evaluate executes with additional data bundle
    // TODO: This test is failing because the Dstu3MeasureProcessor in the evaluator is not checking the additionalData
    // bundle for the patient
    @Test
    void testMeasureEvaluateWithAdditionalData() {
        loadBundle("Exm105FhirR3MeasurePartBundle.json");

        var additionalData = readResource(Bundle.class, "Exm105FhirR3MeasureAdditionalData.json");
        var actual = getActual(
                "2019-01-01",
                "2019-12-01",
                "Patient/denom-EXM105-FHIR3",
                "Measure/measure-EXM105-FHIR3-8.0.000",
                "subject",
                additionalData);

        assertNotNull(actual);
    }
}
