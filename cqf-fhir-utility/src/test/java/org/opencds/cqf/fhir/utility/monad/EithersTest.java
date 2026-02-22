package org.opencds.cqf.fhir.utility.monad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EithersTest {

    @Test
    void for2WithLeft() {
        Either<String, Integer> e = Eithers.for2("hello", null);
        assertTrue(e.isLeft());
        assertEquals("hello", e.leftOrThrow());
    }

    @Test
    void for2WithRight() {
        Either<String, Integer> e = Eithers.for2(null, 42);
        assertTrue(e.isRight());
        assertEquals(42, e.rightOrThrow());
    }

    @Test
    void for3WithLeft() {
        Either3<String, Integer, Double> e = Eithers.for3("hello", null, null);
        assertTrue(e.isLeft());
    }

    @Test
    void for3WithMiddle() {
        Either3<String, Integer, Double> e = Eithers.for3(null, 42, null);
        assertTrue(e.isMiddle());
    }

    @Test
    void for3WithRight() {
        Either3<String, Integer, Double> e = Eithers.for3(null, null, 3.14);
        assertTrue(e.isRight());
    }

    @Test
    void forLeft3() {
        Either3<String, Integer, Double> e = Eithers.forLeft3("hello");
        assertTrue(e.isLeft());
        assertFalse(e.isMiddle());
        assertFalse(e.isRight());
    }

    @Test
    void forMiddle3() {
        Either3<String, Integer, Double> e = Eithers.forMiddle3(42);
        assertTrue(e.isMiddle());
    }

    @Test
    void forRight3() {
        Either3<String, Integer, Double> e = Eithers.forRight3(3.14);
        assertTrue(e.isRight());
    }

    @Test
    void eitherOrElse() {
        var right = Eithers.<String, Integer>forRight(42);
        assertEquals(42, right.orElse(0));

        var left = Eithers.<String, Integer>forLeft("error");
        assertEquals(0, left.orElse(0));
    }

    @Test
    void eitherOrElseGet() {
        var right = Eithers.<String, Integer>forRight(42);
        assertEquals(42, right.orElseGet(() -> 0));

        var left = Eithers.<String, Integer>forLeft("error");
        assertEquals(0, left.orElseGet(() -> 0));
    }

    @Test
    void eitherPeek() {
        var called = new boolean[] {false};
        var right = Eithers.<String, Integer>forRight(42);
        var result = right.peek(v -> called[0] = true);
        assertTrue(called[0]);
        assertEquals(right, result);

        var notCalled = new boolean[] {false};
        var left = Eithers.<String, Integer>forLeft("error");
        left.peek(v -> notCalled[0] = true);
        assertFalse(notCalled[0]);
    }

    @Test
    void eitherTransform() {
        var right = Eithers.<String, Integer>forRight(42);
        var result = right.transform(e -> e.isRight() ? "right" : "left");
        assertEquals("right", result);
    }
}
