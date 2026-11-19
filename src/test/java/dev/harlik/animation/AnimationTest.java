package dev.harlik.animation;

import dev.harlik.animation.easing.Easing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimationTest {

    private static final double EPSILON = 1e-9;

    private AtomicLong now;

    @BeforeEach
    void setUp() {
        now = new AtomicLong(0);
    }

    private Animation linear(long durationMillis) {
        return Animation.builder()
                .duration(durationMillis)
                .clock(now::get)
                .build();
    }

    @Test
    void isZeroBeforeStart() {
        Animation anim = linear(400);

        assertEquals(0.0, anim.value(), EPSILON);
    }

    @Test
    void isZeroRightAfterStart() {
        Animation anim = linear(400);
        anim.forward();

        assertEquals(0.0, anim.value(), EPSILON);
    }

    @Test
    void isHalfwayAtHalfTime() {
        Animation anim = linear(400);
        anim.forward();

        now.addAndGet(200);

        assertEquals(0.5, anim.value(), EPSILON);
    }

    @Test
    void isOneAtTheEnd() {
        Animation anim = linear(400);
        anim.forward();

        now.addAndGet(400);

        assertEquals(1.0, anim.value(), EPSILON);
    }

    @Test
    void staysAtOneAfterTheEnd() {
        Animation anim = linear(400);
        anim.forward();

        now.addAndGet(10_000);

        assertEquals(1.0, anim.value(), EPSILON);
    }

    @Test
    void zeroDurationFinishesImmediately() {
        Animation anim = linear(0);
        anim.forward();

        assertEquals(1.0, anim.value(), EPSILON);
    }

    @Test
    void zeroDurationReverseFinishesImmediately() {
        Animation anim = linear(0);
        anim.reverse();

        assertEquals(0.0, anim.value(), EPSILON);
    }

    @Test
    void appliesTheEasing() {
        Animation anim = Animation.builder()
                .duration(400)
                .easing(t -> t * t)
                .clock(now::get)
                .build();
        anim.forward();

        now.addAndGet(200);
        assertEquals(0.25, anim.value(), EPSILON);
    }

    @Test
    void movesReverse() {
        Animation anim = linear(400);
        anim.reverse();

        assertEquals(1, anim.value(), EPSILON);
        now.addAndGet(200);
        assertEquals(0.5, anim.value(), EPSILON);
        now.addAndGet(200);
        assertEquals(0, anim.value(), EPSILON);
    }

    @Test
    void reverseContinuesFromCurrentValue() {
        Animation anim = linear(400);

        anim.forward();
        now.addAndGet(120);                       // 0.3

        anim.reverse();
        assertEquals(0.3, anim.value(), EPSILON); // no jump

        now.addAndGet(60);
        assertEquals(0.15, anim.value(), EPSILON); // going down

        now.addAndGet(60);
        assertEquals(0.0, anim.value(), EPSILON);  // took 120ms back, not 400
    }

    @Test
    void canReverseAfterFinishing() {
        Animation anim = linear(400);
        anim.forward();
        now.addAndGet(1_000);                      // fully open, then idle

        anim.reverse();
        assertEquals(1.0, anim.value(), EPSILON);  // starts closing from open

        now.addAndGet(100);
        assertEquals(0.75, anim.value(), EPSILON);
    }
}
