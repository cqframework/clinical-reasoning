package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.SdeDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

class R4MeasureReportBuilderTest {

    public static final String MEASURE_ID_1 = "measure1";
    public static final String MEASURE_ID_2 = "measure1";
    public static final String MEASURE_URL_1 = "http://something.com/measure1";
    public static final String MEASURE_URL_2 = "http://something.com/measure2|something";

    @Test
    void happyPathEmptySdes() {
        var r4MeasureReportBuilder = new R4MeasureReportBuilder();

        var measureReport = r4MeasureReportBuilder.build(
                buildMeasure(MEASURE_ID_1, MEASURE_URL_1, 2, 0),
                buildMeasureDef(MEASURE_ID_1, MEASURE_URL_1, 2, 0, true, Set.of(buildInterval())),
                MeasureReportType.INDIVIDUAL,
                null,
                List.of());

        assertNotNull(measureReport);

        final List<Resource> contained = measureReport.getContained();

        assertTrue(contained.isEmpty());
    }

    @Test
    void happyPathEmptySdesAllResourcesAsNull() {
        var r4MeasureReportBuilder = new R4MeasureReportBuilder();

        var measureReport = r4MeasureReportBuilder.build(
                buildMeasure(MEASURE_ID_2, MEASURE_URL_2, 2, 0),
                buildMeasureDef(MEASURE_ID_2, MEASURE_URL_2, 2, 0, true, null),
                MeasureReportType.INDIVIDUAL,
                null,
                List.of());

        assertNotNull(measureReport);

        final List<Resource> contained = measureReport.getContained();

        assertTrue(contained.isEmpty());
    }

    @Test
    void happyPathEmptySdesAllNullResources() {
        var r4MeasureReportBuilder = new R4MeasureReportBuilder();

        var nulls = new ArrayList<>();
        nulls.add(null);

        var measureReport = r4MeasureReportBuilder.build(
                buildMeasure(MEASURE_ID_1, MEASURE_URL_1, 2, 0),
                buildMeasureDef(MEASURE_ID_1, MEASURE_URL_1, 2, 0, true, nulls),
                MeasureReportType.INDIVIDUAL,
                null,
                List.of());

        assertNotNull(measureReport);

        final List<Resource> contained = measureReport.getContained();

        assertTrue(contained.isEmpty());
    }

    @Test
    void happyPathNonEmptySdes() {
        var r4MeasureReportBuilder = new R4MeasureReportBuilder();

        var measureReport = r4MeasureReportBuilder.build(
                buildMeasure(MEASURE_ID_1, MEASURE_URL_1, 2, 3),
                buildMeasureDef(MEASURE_ID_1, MEASURE_URL_1, 2, 3, true, Set.of()),
                MeasureReportType.INDIVIDUAL,
                null,
                List.of());

        assertNotNull(measureReport);

        final List<Resource> contained = measureReport.getContained();

        assertEquals(1, contained.size());

        final List<Patient> patients = contained.stream()
                .filter(Patient.class::isInstance)
                .map(Patient.class::cast)
                .toList();

        assertEquals(1, patients.size());
    }

    @Test
    void happyPathNonEmptySdesCreateObservations() {
        var r4MeasureReportBuilder = new R4MeasureReportBuilder();

        var measureReport = r4MeasureReportBuilder.build(
                buildMeasure(MEASURE_ID_1, null, 2, 3),
                buildMeasureDef(MEASURE_ID_1, MEASURE_URL_1, 2, 3, false, Set.of()),
                MeasureReportType.INDIVIDUAL,
                null,
                List.of());

        assertNotNull(measureReport);

        final List<Resource> contained = measureReport.getContained();

        assertEquals(3, contained.size());

        final List<Observation> observations = contained.stream()
                .filter(Observation.class::isInstance)
                .map(Observation.class::cast)
                .toList();

        assertEquals(3, observations.size());
    }

    @Test
    void errorMismatchedGroupsSizes_tooMany() {
        var r4MeasureReportBuilder = new R4MeasureReportBuilder();
        final Measure measure = buildMeasure(MEASURE_ID_1, MEASURE_URL_1, 1, 2);
        final MeasureDef measureDef = buildMeasureDef(MEASURE_ID_1, MEASURE_URL_1, 2, 2, true, Set.of());
        final List<String> subjectIds = List.of();

        try {
            r4MeasureReportBuilder.build(measure, measureDef, MeasureReportType.INDIVIDUAL, null, subjectIds);
            fail("expected failure");
        } catch (InternalErrorException exception) {
            assertEquals(
                    "The Measure has a different number of groups defined than the MeasureDef for Measure: http://something.com/measure1",
                    exception.getMessage());
        }
    }

    @Test
    void errorMismatchedGroupsSizes_tooFew() {
        var r4MeasureReportBuilder = new R4MeasureReportBuilder();
        final Measure measure = buildMeasure(MEASURE_ID_1, MEASURE_URL_1, 2, 2);
        final MeasureDef measureDef = buildMeasureDef(MEASURE_ID_1, MEASURE_URL_1, 1, 2, true, Set.of());
        final List<String> subjectIds = List.of();

        try {
            r4MeasureReportBuilder.build(measure, measureDef, MeasureReportType.INDIVIDUAL, null, subjectIds);
            fail("expected failure");
        } catch (InternalErrorException exception) {
            assertEquals(
                    "The Measure has a different number of groups defined than the MeasureDef for Measure: http://something.com/measure1",
                    exception.getMessage());
        }
    }

    @Test
    void invalidPopulationResource() {
        var r4MeasureReportBuilder = new R4MeasureReportBuilder();

        try {
            r4MeasureReportBuilder.build(
                    buildMeasure(null, MEASURE_URL_1, 2, 2),
                    buildMeasureDef(MEASURE_ID_1, MEASURE_URL_1, 2, 2, true, Set.of(new Patient())),
                    MeasureReportType.INDIVIDUAL,
                    null,
                    List.of());
        } catch (AssertionError exception) {
            assertNull(exception.getMessage());
        }
    }

    @Nonnull
    private static MeasureDef buildMeasureDef(
            String id,
            String url,
            int numGroups,
            int numSdes,
            boolean isKeyResource,
            Collection<Object> evaluatedResources) {
        return new MeasureDef(
                new IdType(ResourceType.Measure.name(), id),
                url,
                null,
                IntStream.range(0, numGroups)
                        .mapToObj(num -> buildGroupDef("group_" + num, evaluatedResources))
                        .toList(),
                IntStream.range(0, numSdes)
                        .mapToObj(num -> buildSdes("sde_" + num, isKeyResource, evaluatedResources))
                        .toList());
    }

    private static SdeDef buildSdes(String id, boolean isKeyResource, @Nullable Collection<Object> evaluatedResources) {
        final SdeDef sdeDef = new SdeDef(
                id,
                new ConceptDef(List.of(new CodeDef("system", MeasurePopulationType.DATEOFCOMPLIANCE.toCode())), null),
                null);

        if (evaluatedResources != null) {
            sdeDef.putResult(
                    "subject",
                    isKeyResource ? new Patient().setId(new IdType("Patient", "patient1")) : "nonResource",
                    evaluatedResources.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet()));
        }

        return sdeDef;
    }

    @Nonnull
    private static GroupDef buildGroupDef(String id, Collection<Object> resources) {
        return new GroupDef(
                id,
                null,
                List.of(buildStratifierDef()),
                List.of(buildPopulationRef(resources)),
                MeasureScoring.PROPORTION,
                false,
                null,
                new CodeDef(MeasureConstants.POPULATION_BASIS_URL, "boolean"));
    }

    private static PopulationDef buildPopulationRef(Collection<Object> resources) {
        final PopulationDef populationDef = new PopulationDef(
                null,
                new ConceptDef(List.of(new CodeDef("system", MeasurePopulationType.DATEOFCOMPLIANCE.toCode())), null),
                MeasurePopulationType.DATEOFCOMPLIANCE,
                null,
                null,
                null);

        if (resources != null) {
            resources.forEach(res -> populationDef.addResource("subj", res));
        }

        return populationDef;
    }

    private Interval buildInterval() {
        return new Interval(
                toJavaUtilDate(LocalDate.of(1999, Month.DECEMBER, 31)),
                true,
                toJavaUtilDate(LocalDate.of(2000, Month.JANUARY, 1)),
                false);
    }

    private static Date toJavaUtilDate(LocalDate localDate) {
        return new Date(localDate);
    }

    @Nonnull
    private static StratifierDef buildStratifierDef() {
        return new StratifierDef(null, null, null, MeasureStratifierType.VALUE);
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
