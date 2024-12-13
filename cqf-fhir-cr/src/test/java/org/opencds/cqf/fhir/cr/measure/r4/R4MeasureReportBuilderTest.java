package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.SdeDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

class R4MeasureReportBuilderTest {

    public static final String MEASURE_ID_1 = "measure1";
    public static final String MEASURE_URL_1 = "http://something.com/measure1";

    @Test
    void happyPathEmptySdes() {
        var r4MeasureReportBuilder = new R4MeasureReportBuilder();

        var measureReport = r4MeasureReportBuilder.build(
                buildMeasure(MEASURE_ID_1, MEASURE_URL_1, 2, 0),
                buildMeasureDef(MEASURE_ID_1, MEASURE_URL_1, 2, 0, Set.of()),
                MeasureReportType.INDIVIDUAL,
                null,
                List.of());

        assertNotNull(measureReport);
    }

    @Test
    void happyPathNonEmptySdes() {
        var r4MeasureReportBuilder = new R4MeasureReportBuilder();

        var measureReport = r4MeasureReportBuilder.build(
                buildMeasure(MEASURE_ID_1, MEASURE_URL_1, 2, 3),
                buildMeasureDef(MEASURE_ID_1, MEASURE_URL_1, 2, 3, Set.of()),
                MeasureReportType.INDIVIDUAL,
                null,
                List.of());

        assertNotNull(measureReport);
    }

    @Test
    void errorMismatchedGroupsSizes_tooMany() {
        var r4MeasureReportBuilder = new R4MeasureReportBuilder();

        try {
            r4MeasureReportBuilder.build(
                    buildMeasure(MEASURE_ID_1, MEASURE_URL_1, 1, 2),
                    buildMeasureDef(MEASURE_ID_1, MEASURE_URL_1, 2, 2, Set.of()),
                    MeasureReportType.INDIVIDUAL,
                    null,
                    List.of());
            fail("expected failure");
        } catch (InvalidRequestException exception) {
            assertEquals(
                    "The Measure has a different number of groups defined than the MeasureDef for Measure: http://something.com/measure1",
                    exception.getMessage());
        }
    }

    @Test
    void errorMismatchedGroupsSizes_tooFew() {
        var r4MeasureReportBuilder = new R4MeasureReportBuilder();

        try {
            r4MeasureReportBuilder.build(
                    buildMeasure(MEASURE_ID_1, MEASURE_URL_1, 2, 2),
                    buildMeasureDef(MEASURE_ID_1, MEASURE_URL_1, 1, 2, Set.of()),
                    MeasureReportType.INDIVIDUAL,
                    null,
                    List.of());
            fail("expected failure");
        } catch (InvalidRequestException exception) {
            assertEquals(
                    "The Measure has a different number of groups defined than the MeasureDef for Measure: http://something.com/measure1",
                    exception.getMessage());
        }
    }

    @Test
    void errorInvalidReference() {
        var r4MeasureReportBuilder = new R4MeasureReportBuilder();

        r4MeasureReportBuilder.build(
                buildMeasure(MEASURE_ID_1, MEASURE_URL_1, 2, 2),
                buildMeasureDef(MEASURE_ID_1, MEASURE_URL_1, 2, 2, Set.of(new Patient())),
                MeasureReportType.INDIVIDUAL,
                null,
                List.of());
    }

    @Nonnull
    private static MeasureDef buildMeasureDef(
            String id, String url, int numGroups, int numSdes, Set<Resource> evaluatedResources) {
        return new MeasureDef(
                id,
                url,
                null,
                IntStream.range(0, numGroups)
                        .mapToObj(num -> buildGroupDef("group_" + num))
                        .toList(),
                IntStream.range(0, numSdes)
                        .mapToObj(num -> buildSdes("sde_" + num, evaluatedResources))
                        .toList());
    }

    private static SdeDef buildSdes(String id, Set<Resource> evaluatedResources) {
        final SdeDef sdeDef = new SdeDef(id, new ConceptDef(List.of(), null), null);

        sdeDef.putResult(
                "subject",
                new Patient().setId(new IdType("Patient", "patient1")),
                evaluatedResources.stream().collect(Collectors.toUnmodifiableSet()));

        return sdeDef;
    }

    @Nonnull
    private static GroupDef buildGroupDef(String id) {
        return new GroupDef(
                id,
                null,
                List.of(buildStratifierDef()),
                List.of(),
                MeasureScoring.PROPORTION,
                false,
                null,
                new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "boolean"));
    }

    @Nonnull
    private static StratifierDef buildStratifierDef() {
        return new StratifierDef(null, null, null);
    }

    private static Measure buildMeasure(String id, String url, int numGroups, int numSdes) {
        var measure = (Measure) new Measure().setUrl(url).setId(new IdType("Measure", id));

        IntStream.range(0, numGroups).forEach(num -> measure.addGroup(buildGroup("group_" + num)));
        IntStream.range(0, numSdes)
                .forEach(num -> measure.addSupplementalData(buildSupplementalDataComponent("sde_" + num)));

        return measure;
    }

    @Nonnull
    private static MeasureSupplementalDataComponent buildSupplementalDataComponent(String id) {
        return (MeasureSupplementalDataComponent) new MeasureSupplementalDataComponent().setId(id);
    }

    private static MeasureGroupComponent buildGroup(String id) {
        return (MeasureGroupComponent) new MeasureGroupComponent()
                .addStratifier(new MeasureGroupStratifierComponent())
                .setId(id);
    }
}
