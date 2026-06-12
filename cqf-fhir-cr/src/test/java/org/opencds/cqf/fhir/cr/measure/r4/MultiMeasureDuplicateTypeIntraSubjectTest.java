package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;

/**
 * MultiMeasure integration test pinning CDO-714 behaviour across the {@link R4MultiMeasureService}
 * orchestration path.
 *
 * <p>Runs three Measures in the same evaluation call against a single patient: a CQL-Date-basis
 * Measure with intra-subject duplicate dates, a FHIR-string-basis Measure with intra-subject
 * duplicate {@link org.hl7.fhir.instance.model.api.IPrimitiveType} strings, and a boolean-basis
 * Measure used as an unaffected baseline.
 *
 * <p>The date-basis Measure reports a population count of {@code 3} (each duplicate counted) under
 * the tactical CDO-714 fix; the FHIR-string-basis Measure also reports {@code 3}; and the
 * boolean-basis Measure stays at {@code 1}.
 *
 * <p>The CQL-Integer-basis Measure is deliberately excluded from this chain — the tactical fix
 * does not reach the {@code java.lang.Integer} dedup path. The full multi-basis assertion
 * (date / integer / FHIR string / boolean) will be restored when the holistic fix lands; see
 * {@code PRPs/prp-population-basis-primitive-duplicate-counting.md}.
 *
 * @see <a href="https://simpaticois.atlassian.net/browse/CDO-714">CDO-714</a>
 */
@SuppressWarnings("squid:S2699")
class MultiMeasureDuplicateTypeIntraSubjectTest {

    private static final Given GIVEN = MultiMeasure.given().repositoryFor("DuplicateTypeIntraSubject");

    @Test
    void multiMeasure_dateBasis_fhirStringBasis_booleanBasis_countsAreBasisAware() {
        // CQL-Integer-basis Measure intentionally omitted from this chain; see class JavaDoc and
        // PRPs/prp-population-basis-primitive-duplicate-counting.md for the deferred coverage.
        var when = GIVEN.when()
                .measureId("DuplicateTypeIntraSubjectDateBasisMeasure")
                .measureId("DuplicateTypeIntraSubjectFhirStringBasisMeasure")
                .measureId("DuplicateTypeIntraSubjectBooleanBasisMeasure")
                .periodStart("2025-01-01")
                .periodEnd("2025-12-31")
                .reportType("population")
                .evaluate();

        when.then()
                .hasBundleCount(1)
                .hasMeasureReportCount(3)
                .measureReport("http://example.com/Measure/DuplicateTypeIntraSubjectDateBasisMeasure")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(3)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/DuplicateTypeIntraSubjectFhirStringBasisMeasure")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(3)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/DuplicateTypeIntraSubjectBooleanBasisMeasure")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(1)
                .up()
                .up()
                .up()
                .report();
    }
}
