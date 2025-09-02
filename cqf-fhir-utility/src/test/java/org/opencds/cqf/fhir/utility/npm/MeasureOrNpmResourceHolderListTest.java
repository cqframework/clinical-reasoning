package org.opencds.cqf.fhir.utility.npm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.Test;

class MeasureOrNpmResourceHolderListTest {

    @Test
    void shouldCreateFromSingleMeasureOrNpmResourceHolder() {
        var measure = new Measure().setUrl("http://example.org/Measure/test");
        var holder = MeasureOrNpmResourceHolder.measureOnly(measure);

        var holderList = MeasureOrNpmResourceHolderList.of(holder);

        assertEquals(1, holderList.size());
        assertEquals(List.of(holder), holderList.getMeasuresOrNpmResourceHolders());
    }

    @Test
    void shouldCreateFromListOfMeasureOrNpmResourceHolders() {
        var measure1 = (Measure) new Measure().setUrl("http://example.org/Measure/test1");
        var measure2 = (Measure) new Measure().setUrl("http://example.org/Measure/test2");

        MeasureOrNpmResourceHolder holder1 = MeasureOrNpmResourceHolder.measureOnly(measure1);
        MeasureOrNpmResourceHolder holder2 = MeasureOrNpmResourceHolder.measureOnly(measure2);

        MeasureOrNpmResourceHolderList list = MeasureOrNpmResourceHolderList.of(List.of(holder1, holder2));

        assertEquals(2, list.size());
        assertEquals(List.of(holder1, holder2), list.getMeasuresOrNpmResourceHolders());
    }

    @Test
    void shouldCreateFromSingleMeasure() {
        var measure = new Measure().setUrl("http://example.org/Measure/test");

        var holderList = MeasureOrNpmResourceHolderList.of(measure);

        // Assert
        assertEquals(1, holderList.size());
        assertEquals(measure, holderList.getMeasures().get(0));
    }

    @Test
    void shouldCreateFromListOfMeasures() {
        var measure1 = new Measure().setUrl("http://example.org/Measure/test1");
        var measure2 = new Measure().setUrl("http://example.org/Measure/test2");

        var holderList = MeasureOrNpmResourceHolderList.ofMeasures(List.of(measure1, measure2));

        assertEquals(2, holderList.size());
        assertEquals(List.of(measure1, measure2), holderList.getMeasures());
    }

    @Test
    void shouldReturnCorrectNpmResourceHolders() {
        var measure1 = new Measure();
        var measure2 = new Measure();

        var holder1 = MeasureOrNpmResourceHolder.measureOnly(measure1);
        var holder2 = MeasureOrNpmResourceHolder.measureOnly(measure2);

        var holderList = MeasureOrNpmResourceHolderList.of(List.of(holder1, holder2));

        var npmResourceHolders = holderList.npmResourceHolders();

        assertEquals(2, npmResourceHolders.size());
        assertEquals(List.of(NpmResourceHolder.EMPTY, NpmResourceHolder.EMPTY), npmResourceHolders);
    }

    @Test
    void shouldReturnCorrectMeasureUrls() {
        var measure1 = new Measure().setUrl("http://example.org/Measure/test1");
        var measure2 = new Measure().setUrl("http://example.org/Measure/test2");

        var holder1 = MeasureOrNpmResourceHolder.measureOnly(measure1);
        var holder2 = MeasureOrNpmResourceHolder.measureOnly(measure2);

        var holderList = MeasureOrNpmResourceHolderList.of(List.of(holder1, holder2));

        var measureUrls = holderList.getMeasureUrls();

        assertEquals(List.of("http://example.org/Measure/test1", "http://example.org/Measure/test2"), measureUrls);
    }

    @Test
    void shouldCheckMeasureLibrariesSuccessfully() {
        var measure = new Measure()
                .setUrl("http://example.org/Measure/test")
                .addLibrary("http://example.org/Library/testLib");

        var holder = MeasureOrNpmResourceHolder.measureOnly(measure);
        var holderList = MeasureOrNpmResourceHolderList.of(holder);

        assertDoesNotThrow(holderList::checkMeasureLibraries);
    }

    @Test
    void shouldThrowExceptionWhenMeasureHasNoLibrary() {
        // Arrange
        var measure = new Measure().setUrl("http://example.org/Measure/test");

        var holder = MeasureOrNpmResourceHolder.measureOnly(measure);
        var holderList = MeasureOrNpmResourceHolderList.of(holder);

        var exception = assertThrows(InvalidRequestException.class, holderList::checkMeasureLibraries);
        assertEquals(
                "Measure http://example.org/Measure/test does not have a primary library specified",
                exception.getMessage());
    }

    @Test
    void shouldImplementEqualsAndHashCodeCorrectly() {
        var measure1 = new Measure().setUrl("http://example.org/Measure/test1");
        var measure2 = new Measure().setUrl("http://example.org/Measure/test2");

        var holder1 = MeasureOrNpmResourceHolder.measureOnly(measure1);
        var holder2 = MeasureOrNpmResourceHolder.measureOnly(measure2);

        // Act
        var holderList1 = MeasureOrNpmResourceHolderList.of(List.of(holder1, holder2));
        var holderList2 = MeasureOrNpmResourceHolderList.of(List.of(holder1, holder2));
        var holderList3 = MeasureOrNpmResourceHolderList.of(List.of(holder1));

        // Assert
        assertEquals(holderList1, holderList2);
        assertEquals(holderList1.hashCode(), holderList2.hashCode());
        assertNotEquals(holderList1, holderList3);
        assertNotEquals(holderList1.hashCode(), holderList3.hashCode());
    }

    @Test
    void shouldHaveCorrectToStringRepresentation() {
        // Arrange
        var measure = new Measure().setUrl("http://example.org/Measure/test");
        var holder = MeasureOrNpmResourceHolder.measureOnly(measure);
        var holderList = MeasureOrNpmResourceHolderList.of(holder);

        // Act
        var toStringResult = holderList.toString();

        // Assert
        assertTrue(toStringResult.contains("MeasurePlusNpmResourceHolderList"));
        assertTrue(toStringResult.contains("measuresPlusNpmResourceHolders"));
    }
}
