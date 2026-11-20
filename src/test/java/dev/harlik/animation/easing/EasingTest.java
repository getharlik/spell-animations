package dev.harlik.animation.easing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class EasingTest {

    // cubicBezier solves its curve numerically, so its results are close, not exact
    private static final double EPSILON = 1e-6;

    static Stream<Arguments> allEasings() {
        return Stream.of(
                Arguments.of("LINEAR", Easing.LINEAR),
                Arguments.of("EASE_IN", Easing.EASE_IN),
                Arguments.of("EASE_OUT", Easing.EASE_OUT),
                Arguments.of("EASE_IN_OUT", Easing.EASE_IN_OUT)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allEasings")
    void startsAtZero(String name, Easing easing) {
        assertEquals(0.0, easing.apply(0.0), EPSILON);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allEasings")
    void endsAtOne(String name, Easing easing) {
        assertEquals(1.0, easing.apply(1.0), EPSILON);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allEasings")
    void neverGoesBackwards(String name, Easing easing) {
        double previous = easing.apply(0.0);
        for (int i = 1; i <= 100; i++) {
            double current = easing.apply(i / 100.0);
            assertTrue(current >= previous, "dropped at t=" + i / 100.0);
            previous = current;
        }
    }

    @Test
    void linearReturnsInputUnchanged() {
        assertEquals(0.3, Easing.LINEAR.apply(0.3), EPSILON);
        assertEquals(0.75, Easing.LINEAR.apply(0.75), EPSILON);
    }

    @Test
    void easeInStartsSlowerThanLinear() {
        double value = Easing.EASE_IN.apply(0.3);

        assertTrue(value > 0.0 && value < 0.3, "was " + value);
    }

    @Test
    void easeOutStartsFasterThanLinear() {
        double value = Easing.EASE_OUT.apply(0.3);

        assertTrue(value > 0.3 && value < 1.0, "was " + value);
    }

    @Test
    void easeInOutIsHalfwayAtHalfTime() {
        assertEquals(0.5, Easing.EASE_IN_OUT.apply(0.5), EPSILON);
    }

    @Test
    void easeInOutIsSymmetric() {
        for (double t : new double[]{0.1, 0.25, 0.37}) {
            double sum = Easing.EASE_IN_OUT.apply(t) + Easing.EASE_IN_OUT.apply(1 - t);
            assertEquals(1.0, sum, EPSILON, "at t=" + t);
        }
    }

    @Test
    void easeOutIsEaseInMirrored() {
        for (double t : new double[]{0.1, 0.25, 0.37, 0.8}) {
            double mirrored = 1 - Easing.EASE_IN.apply(1 - t);
            assertEquals(mirrored, Easing.EASE_OUT.apply(t), EPSILON, "at t=" + t);
        }
    }

    @Test
    void cubicBezierOnTheDiagonalIsLinear() {
        Easing straight = Easing.cubicBezier(0.25, 0.25, 0.75, 0.75);

        for (double t : new double[]{0.1, 0.3, 0.5, 0.77}) {
            assertEquals(t, straight.apply(t), EPSILON, "at t=" + t);
        }
    }
}
