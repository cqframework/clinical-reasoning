package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Disabled;
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
 *       expression. The tactical CDO-714 fix disables the {@code CqlType} dedup branches in
 *       {@code HashSetForFhirResourcesAndCqlTypes} so distinct {@code runtime.Date} instances
 *       survive default {@link java.util.HashSet} identity semantics and the count reflects every
 *       yielded date.
 *   <li><b>CQL Integer</b> ({@code System.Integer} → {@code java.lang.Integer}) yielded by a
 *       literal list expression. <b>Deferred to the holistic fix</b>: {@code Integer} is neither
 *       {@code IBaseResource} nor {@code CqlType}, and {@code Integer}'s value-based
 *       {@code equals}/{@code hashCode} still dedups in the default {@link java.util.HashSet}
 *       path. See {@code PRPs/prp-population-basis-primitive-duplicate-counting.md}.
 *   <li><b>FHIR primitive</b> ({@link org.hl7.fhir.instance.model.api.IPrimitiveType}) yielded by
 *       walking {@code Patient.address[0].line}. Regression guard: HAPI FHIR primitive instances
 *       are neither {@code IBaseResource} nor {@code CqlType}, and {@code Base.equals} is not
 *       overridden, so default {@link java.util.HashSet} identity semantics keep distinct
 *       instances. The tactical fix must (and does) preserve this.
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

    // Deferred: the CQL engine returns java.lang.Integer for CQL Integer literals, which has
    // value-based equals/hashCode and dedups via default HashSet semantics. The tactical
    // CDO-714 fix (disabling CqlType branches in HashSetForFhirResourcesAndCqlTypes) does not
    // reach the java.lang.Integer path. The holistic fix is captured in
    // PRPs/prp-population-basis-primitive-duplicate-counting.md and is sequenced after the
    // CQL 5.0 (cql1) ExpressionResult type changes land.
    @Disabled("CDO-714 — deferred to PRPs/prp-population-basis-primitive-duplicate-counting.md")
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
