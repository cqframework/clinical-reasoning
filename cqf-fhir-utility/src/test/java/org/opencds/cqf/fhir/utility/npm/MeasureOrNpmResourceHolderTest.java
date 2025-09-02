package org.opencds.cqf.fhir.utility.npm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.Test;

class MeasureOrNpmResourceHolderTest {

    @Test
    void shouldCreateMeasureOnlyInstance() {
        var measure = new Measure();
        var holder = MeasureOrNpmResourceHolder.measureOnly(measure);

        assertEquals(measure, holder.getMeasure());
        assertEquals(NpmResourceHolder.EMPTY, holder.npmResourceHolder());
    }

    @Test
    void hasLibraryShouldReturnTrueWhenMeasureHasLibrary() {
        var measure = new Measure();
        measure.addLibrary("http://example.org/Library/123");

        var holder = MeasureOrNpmResourceHolder.measureOnly(measure);

        assertTrue(holder.hasLibrary());
    }

    @Test
    void hasLibraryShouldReturnFalseWhenMeasureHasNoLibrary() {
        var measure = new Measure();

        var holder = MeasureOrNpmResourceHolder.measureOnly(measure);

        assertFalse(holder.hasLibrary());
    }

    @Test
    void getMainLibraryUrlShouldReturnUrlFromMeasure() {
        var measure = new Measure().addLibrary("http://example.org/Library/123");

        var holder = MeasureOrNpmResourceHolder.measureOnly(measure);

        assertEquals(Optional.of("http://example.org/Library/123"), holder.getMainLibraryUrl());
    }

    @Test
    void getMainLibraryUrlShouldReturnEmptyWhenMeasureHasNoLibrary() {
        var measure = new Measure();

        var holder = MeasureOrNpmResourceHolder.measureOnly(measure);

        assertEquals(Optional.empty(), holder.getMainLibraryUrl());
    }

    @Test
    void getMeasureIdElementShouldReturnIdFromMeasure() {
        var measure = (Measure) new Measure().setId("measure-123");

        var holder = MeasureOrNpmResourceHolder.measureOnly(measure);

        assertEquals("measure-123", holder.getMeasureIdElement().getIdPart());
    }

    @Test
    void hasMeasureUrlShouldReturnTrueWhenMeasureHasUrl() {
        var measure = new Measure().setUrl("http://example.org/Measure/123");

        var holder = MeasureOrNpmResourceHolder.measureOnly(measure);

        assertTrue(holder.hasMeasureUrl());
    }

    @Test
    void hasMeasureUrlShouldReturnFalseWhenMeasureHasNoUrl() {
        var measure = new Measure();

        var holder = MeasureOrNpmResourceHolder.measureOnly(measure);

        assertFalse(holder.hasMeasureUrl());
    }

    @Test
    void getMeasureUrlShouldReturnUrlFromMeasure() {
        var measure = new Measure().setUrl("http://example.org/Measure/123");

        var holder = MeasureOrNpmResourceHolder.measureOnly(measure);

        assertEquals("http://example.org/Measure/123", holder.getMeasureUrl());
    }
}
