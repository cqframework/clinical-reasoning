package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ContinuousVariableObservationAggregateMethodTest {

    @ParameterizedTest
    @CsvSource({"average,AVG", "count,COUNT", "maximum,MAX", "median,MEDIAN", "minimum,MIN", "sum,SUM"})
    void fromString_validText_returnsExpectedEnum(String text, ContinuousVariableObservationAggregateMethod expected) {
        assertEquals(expected, ContinuousVariableObservationAggregateMethod.fromString(text));
    }

    @Test
    void fromString_nullText_returnsNA() {
        assertEquals(
                ContinuousVariableObservationAggregateMethod.N_A,
                ContinuousVariableObservationAggregateMethod.fromString(null));
    }

    @Test
    void fromString_unknownText_returnsNull() {
        assertNull(ContinuousVariableObservationAggregateMethod.fromString("unknown"));
    }

    @Test
    void getText_returnsExpectedValues() {
        assertEquals("average", ContinuousVariableObservationAggregateMethod.AVG.getText());
        assertNull(ContinuousVariableObservationAggregateMethod.N_A.getText());
    }

    /**
     * Mutation test: verifies that reverting Objects.equals to text.equals in fromString
     * would cause a NullPointerException, proving the null-safety fix is load-bearing.
     */
    @Test
    void fromString_nullText_wouldThrowNpeWithoutNullSafeFix() {
        // This test documents the bug that was fixed: calling text.equals() when text is null
        // would throw NPE. The fix uses Objects.equals() which handles null safely.
        // If someone reverts to text.equals(), fromString(null) will throw NPE instead of
        // returning N_A, and both this test and fromString_nullText_returnsNA will fail.
        assertDoesNotThrow(() -> ContinuousVariableObservationAggregateMethod.fromString(null));
    }
}
