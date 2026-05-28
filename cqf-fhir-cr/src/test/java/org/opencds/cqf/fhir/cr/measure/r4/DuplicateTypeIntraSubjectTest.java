package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

/**
 * Integration test pinning the expected behaviour for CDO-714.
 *
 * <p>When a Measure has a non-boolean population basis and a CQL expression for a population
 * yields duplicate values within a single subject, each occurrence is a discrete data point and
 * the population count must include the duplicates.
 *
 * <p>This test covers three flavours of "primitive" basis that the evaluator must handle:
 * <ul>
 *   <li><b>CQL Date</b> ({@code System.Date} → {@code runtime.Date}) yielded by a literal list
 *       expression. <b>Fails on {@code main}</b>: the {@code HashSetForFhirResourcesAndCqlTypes}
 *       backing each subject's evaluated values dedups {@code CqlType} values via
 *       {@code EqualEvaluator.equal}, so the two {@code @2025-07-03} entries collapse and the
 *       count returns {@code 2} instead of {@code 3}.
 *   <li><b>CQL Integer</b> ({@code System.Integer} → {@code java.lang.Integer}) yielded by a
 *       literal list expression. <b>Fails on {@code main}</b>: {@code Integer} is neither
 *       {@code IBaseResource} nor {@code CqlType}, so it falls through to default
 *       {@link java.util.HashSet} semantics — and {@code Integer}'s value-based
 *       {@code equals}/{@code hashCode} dedups {@code 42} and {@code 42}, yielding {@code 2}.
 *   <li><b>FHIR primitive</b> ({@link org.hl7.fhir.instance.model.api.IPrimitiveType}) yielded by
 *       walking {@code Patient.address[0].line}. <b>Passes on {@code main}</b> as a regression
 *       guard: HAPI FHIR primitive instances are neither {@code IBaseResource} nor
 *       {@code CqlType}, and {@code Base.equals} is not overridden, so default
 *       {@link java.util.HashSet} identity semantics keep distinct instances. The future fix must
 *       preserve this behaviour.
 * </ul>
 *
 * @see <a href="https://simpaticois.atlassian.net/browse/CDO-714">CDO-714</a>
 */
@SuppressWarnings("squid:S2699")
class DuplicateTypeIntraSubjectTest {

    private static final Given GIVEN = Measure.given().repositoryFor("DuplicateTypeIntraSubject");

    @Test
    void cqlDateBasis_intraSubjectDuplicates_populationReport_includesDuplicates() {
        GIVEN.when()
                .measureId("DuplicateTypeIntraSubjectDateBasisMeasure")
                .periodStart("2025-01-01")
                .periodEnd("2025-12-31")
                .reportType("population")
                .evaluate()
                .then()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(3)
                .up()
                .up()
                .report();
    }

    @Test
    void cqlDateBasis_intraSubjectDuplicates_subjectReport_includesDuplicates() {
        GIVEN.when()
                .measureId("DuplicateTypeIntraSubjectDateBasisMeasure")
                .subject("Patient/patient-a")
                .periodStart("2025-01-01")
                .periodEnd("2025-12-31")
                .evaluate()
                .then()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(3)
                .up()
                .up()
                .report();
    }

    @Test
    void cqlIntegerBasis_intraSubjectDuplicates_populationReport_includesDuplicates() {
        GIVEN.when()
                .measureId("DuplicateTypeIntraSubjectIntegerBasisMeasure")
                .periodStart("2025-01-01")
                .periodEnd("2025-12-31")
                .reportType("population")
                .evaluate()
                .then()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(3)
                .up()
                .up()
                .report();
    }

    @Test
    void fhirStringBasis_intraSubjectDuplicates_populationReport_includesDuplicates() {
        GIVEN.when()
                .measureId("DuplicateTypeIntraSubjectFhirStringBasisMeasure")
                .periodStart("2025-01-01")
                .periodEnd("2025-12-31")
                .reportType("population")
                .evaluate()
                .then()
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(3)
                .up()
                .up()
                .report();
    }
}
