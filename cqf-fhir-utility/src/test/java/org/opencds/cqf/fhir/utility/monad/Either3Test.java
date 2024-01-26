package org.opencds.cqf.fhir.utility.monad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class Either3Test {

    @Test
    public void equals() {
        var first = Eithers.forLeft3(1);
        var second = Eithers.forLeft3(1);
        assertEquals(first, second);
        assertEquals(second, first);

        var third = Eithers.forRight3(2);
        var fourth = Eithers.forRight3(2);
        assertEquals(third, fourth);
        assertEquals(fourth, third);

        var fifth = Eithers.forLeft3(1);
        var sixth = Eithers.forRight3(1);
        assertNotEquals(fifth, sixth);
        assertNotEquals(sixth, fifth);

        var seventh = Eithers.forMiddle3(2);
        var eighth = Eithers.forMiddle3(2);
        assertEquals(seventh, eighth);
        assertEquals(eighth, seventh);

        assertNotEquals(first, seventh);
        assertNotEquals(third, seventh);
    }

    @Test
    public void properties() {
        var one = Eithers.forLeft3(1);
        assertTrue(one.isLeft());
        assertFalse(one.isMiddle());
        assertFalse(one.isRight());
        assertEquals(1, one.left());
        // Either is right-biased, meaning the "get" is for the right element
        assertThrows(IllegalStateException.class, one::right);
        assertThrows(IllegalStateException.class, one::middle);
        assertThrows(IllegalStateException.class, one::get);

        var two = Eithers.forRight3(1);
        assertFalse(two.isLeft());
        assertFalse(two.isMiddle());
        assertTrue(two.isRight());
        assertEquals(1, two.right());
        assertEquals(1, two.get());
        assertThrows(IllegalStateException.class, two::left);
        assertThrows(IllegalStateException.class, two::middle);

        var three = Eithers.forMiddle3(1);
        assertFalse(three.isLeft());
        assertTrue(three.isMiddle());
        assertFalse(three.isRight());
        assertEquals(1, three.middle());
        assertThrows(IllegalStateException.class, three::left);
        assertThrows(IllegalStateException.class, three::right);
        assertThrows(IllegalStateException.class, three::get);
    }

    @Test
    public void swap() {
        var one = Eithers.forLeft3(1);
        var two = Eithers.forRight3(1);
        var three = Eithers.forMiddle3(1);
        assertEquals(one, two.swap());
        assertEquals(two, one.swap());
        assertEquals(three, three.swap());
    }

    // The identity law states that applying the right identity function
    // (Eithers.forRight3) should be equivalent to the original either.
    // In other words, reconstructing an Either from a value
    // should equal the original eithers
    @Test
    void rightIdentity() {
        Integer b = 42;

        Either3<String, String, Integer> right = Eithers.forRight3(b);

        assertEquals(right.flatMap(Eithers::forRight3), right);
    }

    // The identity law states that applying flatMap to a "left" either should return the
    // original either. In other words, the left value propagates
    @Test
    void leftIdentity() {
        String a = "42";

        Either3<String, String, Integer> left = Eithers.forLeft3(a);

        assertEquals(left.flatMap(x -> Eithers.forRight3(x + 2)), left);
    }

    // The identity law states that applying flatMap to a "middle" either should return the
    // original either. In other words, the middle value propagates
    @Test
    void middleIdentity() {
        String a = "42";

        Either3<String, String, Integer> left = Eithers.forMiddle3(a);

        assertEquals(left.flatMap(x -> Eithers.forRight3(x + 2)), left);
    }

    // This associativity law states that if you chain together two functions using flatMap,
    // the order in which you apply them should not matter.
    @Test
    void associativity() {
        Function<Integer, Either3<String, String, Double>> f = i -> Eithers.forRight3(i / 2.0);
        Function<Double, Either3<String, String, String>> g = d -> Eithers.forRight3("Result: " + d);

        Either3<String, String, Integer> m = Eithers.forRight3(10);

        assertEquals(m.flatMap(f).flatMap(g), m.flatMap(x -> f.apply(x).flatMap(g)));
    }

    // Map is implemented in terms of "unit" (constructors in Java) and "bind" ("flatMap")
    // IOW, bind(m, x -> unit(f(x)))
    @Test
    void map() {
        Function<Integer, Integer> doubleInt = (Integer x) -> x * 2;
        Either3<String, Double, Integer> e = Eithers.forRight3(3);

        var bound = e.flatMap(x -> Eithers.forRight3(doubleInt.apply(x)));
        var map = e.map(doubleInt);

        assertEquals(bound, map);
        assertEquals(6, map.get());

        // But we also need to make sure we preserve left identity;
        Either3<String, Double, Integer> failed = Eithers.forLeft3("Failed either");
        var mapFailed = failed.map(doubleInt);

        assertEquals(failed, mapFailed);

        // And middle identity
        Either3<String, Double, Integer> failed2 = Eithers.forMiddle3(1.5);
        var mapFailed2 = failed2.map(doubleInt);

        assertEquals(failed2, mapFailed2);
    }

    // fold is a reduction operation. Take a left, middle, or right and turn it into
    // a single value.
    @Test
    void fold() {
        var right = Eithers.<String, Double, Integer>forRight3(2);
        var middle = Eithers.<String, Double, Integer>forMiddle3(1.5);
        var left = Eithers.<String, Double, Integer>forLeft3("three");

        Function<Integer, String> foldR = x -> x.toString();
        Function<Double, String> foldM = x -> x.toString();
        Function<String, String> foldL = x -> x;

        var x = right.fold(foldL, foldM, foldR);
        assertEquals("2", x);

        var y = left.fold(foldL, foldM, foldR);
        assertEquals("three", y);

        var z = middle.fold(foldL, foldM, foldR);
        assertEquals("1.5", z);
    }

    @Test
    void forEach() {
        var left = Eithers.<String, Double, Integer>forLeft3("three");
        var middle = Eithers.<String, Double, Integer>forMiddle3(1.5);
        var right = Eithers.<String, Double, Integer>forRight3(2);

        boolean[] flags = {false, false};

        right.forEach(x -> {
            flags[1] = true;
        });
        assertTrue(flags[1]);

        // Left for each shouldn't do anything
        left.forEach(x -> {
            flags[0] = true;
        });
        assertFalse(flags[0]);

        // Middle for each shouldn't do anything
        middle.forEach(x -> {
            flags[0] = true;
        });
        assertFalse(flags[0]);
    }

    // "stream" gives you the ability to use the standard Java Stream apis if you need to
    // left Eithers return an empty stream. Right Eithers return a stream of one.
    @Test
    void stream() {
        var left = Eithers.forLeft3(1);
        var middle = Eithers.forMiddle3(1);
        var right = Eithers.forRight3(1);

        assertEquals(0, left.stream().count());
        assertEquals(0, middle.stream().count());
        assertEquals(1, right.stream().count());
    }

    // "optional" gives you the ability to use the standard Java Optional apis if you need to
    // left Eithers return an empty optional. Right Eithers return a filled optional
    @Test
    void optional() {
        var left = Eithers.forLeft3(1);
        var middle = Eithers.forLeft3(1);
        var right = Eithers.forRight3(1);

        assertFalse(left.optional().isPresent());
        assertFalse(middle.optional().isPresent());
        assertTrue(right.optional().isPresent());
    }
}
