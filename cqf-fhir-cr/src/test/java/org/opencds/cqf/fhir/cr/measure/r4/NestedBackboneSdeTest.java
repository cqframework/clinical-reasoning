package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_SDE_REFERENCE_URL;

import java.math.BigDecimal;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

/**
 * Integration coverage for backbone elements nested inside other backbone elements, e.g.
 * {@code ExplanationOfBenefit.item.adjudication}.
 * <p>
 * A supplemental data element that returns whole resources hands the CQL engine's native
 * {@code ClassInstance} values to {@code StratumValueWrapper}, which converts them back to HAPI FHIR
 * through {@code CqlFhirParametersConverter.toFhirValue}. That converter disambiguates HAPI inner
 * classes using the immediate parent element's type name, but HAPI declares every backbone element as
 * a direct inner class of its owning resource ({@code ExplanationOfBenefit$AdjudicationComponent}),
 * never nested by path ({@code ItemComponent$AdjudicationComponent}). Depth-1 backbones are only
 * accidentally correct; depth-2 backbones fail with
 * {@code IllegalArgumentException: Could not resolve inner FHIR type: AdjudicationComponent}.
 *
 * @see org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter
 */
@SuppressWarnings("squid:S2699")
class NestedBackboneSdeTest {

    private static final Given GIVEN = Measure.given().repositoryFor("NestedBackboneSde");

    private static final String SDE_ID = "sde-explanation-of-benefit";

    /**
     * Depth-1 guard: {@code ExplanationOfBenefit.item} carries no nested backbone element, so the
     * converter's parent-name heuristic happens to be correct and conversion succeeds. This is the
     * behaviour that must not regress when the heuristic is fixed.
     */
    @Test
    void depthOneBackboneElementConvertsForSupplementalData() {
        var then = GIVEN.when()
                .measureId("NestedBackboneSde")
                .subject("Patient/patient-flat")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .evaluate()
                .then();

        then.hasStatus(MeasureReportStatus.COMPLETE)
                .hasExtension(EXT_SDE_REFERENCE_URL, 1)
                .extensionByValueReference("ExplanationOfBenefit/eob-flat")
                .extensionHasSDEId(SDE_ID)
                .up()
                .report();

        var eob = supplementalDataResource(then.def().measureDef());
        assertEquals("eob-flat", eob.getIdElement().getIdPart());
        assertEquals(1, eob.getItem().size());
        assertEquals(1, eob.getItemFirstRep().getSequence());
        assertFalse(eob.getItemFirstRep().hasAdjudication());
    }

    /**
     * Depth-2 regression: {@code ExplanationOfBenefit.item.adjudication} is a backbone element inside
     * another backbone element. The converter is handed {@code parentName = "ItemComponent"} and tries
     * to load {@code ...ExplanationOfBenefit$ItemComponent$AdjudicationComponent}, which has never
     * existed, then throws instead of falling back.
     * <p>
     * On {@code main} this fails during {@code SdeDef.accumulate()} with
     * {@code IllegalArgumentException: Could not resolve inner FHIR type: AdjudicationComponent},
     * surfacing out of {@code then()} before a MeasureReport is ever built.
     */
    @Test
    void nestedBackboneElementConvertsForSupplementalData() {
        var then = GIVEN.when()
                .measureId("NestedBackboneSde")
                .subject("Patient/patient-nested")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .evaluate()
                .then();

        then.hasStatus(MeasureReportStatus.COMPLETE)
                .hasExtension(EXT_SDE_REFERENCE_URL, 1)
                .extensionByValueReference("ExplanationOfBenefit/eob-nested")
                .extensionHasSDEId(SDE_ID)
                .up()
                .report();

        // The nested backbone element must survive the round trip intact, not merely avoid throwing.
        var eob = supplementalDataResource(then.def().measureDef());
        assertEquals("eob-nested", eob.getIdElement().getIdPart());
        var adjudication = eob.getItemFirstRep().getAdjudicationFirstRep();
        assertEquals("benefit", adjudication.getCategory().getCodingFirstRep().getCode());
        assertEquals(
                0, new BigDecimal("100.00").compareTo(adjudication.getAmount().getValue()));
        assertEquals("USD", adjudication.getAmount().getCurrency());
    }

    /**
     * Population-level report over both subjects. The SDE accumulation that converts the values runs
     * for every report type, so the population report fails identically on {@code main}.
     */
    @Test
    void nestedBackboneElementConvertsForSupplementalDataInPopulationReport() {
        GIVEN.when()
                .measureId("NestedBackboneSde")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasExtension(EXT_SDE_REFERENCE_URL, 2)
                .extensionByValueReference("ExplanationOfBenefit/eob-nested")
                .extensionHasSDEId(SDE_ID)
                .up()
                .extensionByValueReference("ExplanationOfBenefit/eob-flat")
                .extensionHasSDEId(SDE_ID)
                .up()
                .report();
    }

    private static ExplanationOfBenefit supplementalDataResource(MeasureDef measureDef) {
        return SdeValues.onlySupplementalDataResource(measureDef, ExplanationOfBenefit.class);
    }
}
