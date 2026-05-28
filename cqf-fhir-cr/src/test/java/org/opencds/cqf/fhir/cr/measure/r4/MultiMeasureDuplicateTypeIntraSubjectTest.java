package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;

/**
 * MultiMeasure integration test pinning CDO-714 behaviour across the {@link R4MultiMeasureService}
 * orchestration path.
 *
 * <p>Runs four Measures in the same evaluation call against a single patient: a CQL-Date-basis
 * Measure with intra-subject duplicate dates, a CQL-Integer-basis Measure with intra-subject
 * duplicate integers, a FHIR-string-basis Measure with intra-subject duplicate
 * {@link org.hl7.fhir.instance.model.api.IPrimitiveType} strings, and a boolean-basis Measure
 * used as an unaffected baseline.
 *
 * <p>The date- and integer-basis Measures must each report a population count of {@code 3} (each
 * duplicate counted), the FHIR-string-basis Measure must also report {@code 3}, and the
 * boolean-basis Measure must stay at {@code 1}. On {@code main} this test FAILS on the
 * date-basis assertion (checked first in the chain); the integer-basis assertion would also fail
 * if reached, while the FHIR-string-basis assertion would pass independently and is included as a
 * regression guard for the future fix.
 *
 * @see <a href="https://simpaticois.atlassian.net/browse/CDO-714">CDO-714</a>
 */
@SuppressWarnings("squid:S2699")
class MultiMeasureDuplicateTypeIntraSubjectTest {

    private static final Given GIVEN = MultiMeasure.given().repositoryFor("DuplicateTypeIntraSubject");

    @Test
    void multiMeasure_dateBasis_integerBasis_fhirStringBasis_booleanBasis_countsAreBasisAware() {
        var when = GIVEN.when()
                .measureId("DuplicateTypeIntraSubjectDateBasisMeasure")
                .measureId("DuplicateTypeIntraSubjectIntegerBasisMeasure")
                .measureId("DuplicateTypeIntraSubjectFhirStringBasisMeasure")
                .measureId("DuplicateTypeIntraSubjectBooleanBasisMeasure")
                .periodStart("2025-01-01")
                .periodEnd("2025-12-31")
                .reportType("population")
                .evaluate();

        when.then()
                .hasBundleCount(1)
                .hasMeasureReportCount(4)
                .measureReport("http://example.com/Measure/DuplicateTypeIntraSubjectDateBasisMeasure")
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(3)
                .up()
                .up()
                .up()
                .measureReport("http://example.com/Measure/DuplicateTypeIntraSubjectIntegerBasisMeasure")
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
