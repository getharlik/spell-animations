package dev.harlik.animation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimationTest {

    private static final double EPSILON = 1e-9;

    private AtomicLong now;

    @BeforeEach
    void setUp() {
        now = new AtomicLong(1_000);
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
        anim.start();

        assertEquals(0.0, anim.value(), EPSILON);
    }

    @Test
    void isHalfwayAtHalfTime() {
        Animation anim = linear(400);
        anim.start();

        now.addAndGet(200);

        assertEquals(0.5, anim.value(), EPSILON);
    }

    @Test
    void isOneAtTheEnd() {
        Animation anim = linear(400);
        anim.start();

        now.addAndGet(400);

        assertEquals(1.0, anim.value(), EPSILON);
    }

    @Test
    void staysAtOneAfterTheEnd() {
        Animation anim = linear(400);
        anim.start();

        now.addAndGet(10_000);

        assertEquals(1.0, anim.value(), EPSILON);
    }

    @Test
    void zeroDurationFinishesImmediately() {
        Animation anim = linear(0);
        anim.start();

        assertEquals(1.0, anim.value(), EPSILON);
    }

    @Test
    void appliesTheEasing() {
        Animation anim = Animation.builder()
                .duration(400)
                .easing(t -> t * t)
                .clock(now::get)
                .build();
        anim.start();

        now.addAndGet(200);

        assertEquals(0.25, anim.value(), EPSILON);
    }
}
