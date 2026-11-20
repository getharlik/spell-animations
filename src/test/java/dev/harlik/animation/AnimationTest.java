package dev.harlik.animation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AnimationTest {

    private static final double EPSILON = 1e-9;

    private AtomicLong now;

    @BeforeEach
    void setUp() {
        now = new AtomicLong(1_000_000);
    }

    private Animation linear(long durationMillis) {
        return Animation.builder()
                .duration(durationMillis)
                .clock(now::get)
                .build();
    }

    @Nested
    class Forward {

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
    }

    @Nested
    class Reverse {

        @Test
        void movesFromOneToZero() {
            Animation anim = linear(400);
            anim.reverse();

            assertEquals(1.0, anim.value(), EPSILON);
            now.addAndGet(200);
            assertEquals(0.5, anim.value(), EPSILON);
            now.addAndGet(200);
            assertEquals(0.0, anim.value(), EPSILON);
        }

        @Test
        void continuesFromCurrentValue() {
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

        @Test
        void zeroDurationFinishesImmediately() {
            Animation anim = linear(0);
            anim.reverse();

            assertEquals(0.0, anim.value(), EPSILON);
        }
    }

    @Nested
    class WithEasing {

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
    }

    @Nested
    class Paused {

        @Test
        void freezesValue() {
            Animation anim = linear(400);
            anim.forward();
            now.addAndGet(120);                         // 0.3

            anim.pause();
            now.addAndGet(1_000);

            assertEquals(0.3, anim.value(), EPSILON);
        }

        @Test
        void resumeContinuesFromPausedValue() {
            Animation anim = linear(400);
            anim.forward();
            now.addAndGet(120);                         // 0.3
            anim.pause();
            now.addAndGet(1_000);

            anim.resume();
            assertEquals(0.3, anim.value(), EPSILON);   // no jump on resume

            now.addAndGet(60);
            assertEquals(0.45, anim.value(), EPSILON);  // the 1000ms pause didn't count
        }

        @Test
        void pausingTwiceDoesNotLoseTime() {
            Animation anim = linear(400);
            anim.forward();
            now.addAndGet(120);                         // 0.3
            anim.pause();
            now.addAndGet(300);
            anim.pause();                               // already paused: must be ignored
            now.addAndGet(300);

            anim.resume();
            now.addAndGet(60);

            assertEquals(0.45, anim.value(), EPSILON);
        }

        @Test
        void resumeWithoutPauseDoesNothing() {
            Animation anim = linear(400);
            anim.forward();
            now.addAndGet(120);

            anim.resume();

            assertEquals(0.3, anim.value(), EPSILON);
            assertFalse(anim.isPaused());
        }

        @Test
        void doesNothingBeforeStart() {
            Animation anim = linear(400);

            anim.pause();
            assertFalse(anim.isPaused());

            anim.forward();
            now.addAndGet(100);
            assertEquals(0.25, anim.value(), EPSILON);  // runs normally
        }

        @Test
        void isPausedReflectsState() {
            Animation anim = linear(400);
            anim.forward();
            assertFalse(anim.isPaused());

            anim.pause();
            assertTrue(anim.isPaused());

            anim.resume();
            assertFalse(anim.isPaused());
        }

        @Test
        void reverseStaysFrozenUntilResume() {
            Animation anim = linear(400);
            anim.forward();
            now.addAndGet(120);                         // 0.3
            anim.pause();

            anim.reverse();
            now.addAndGet(500);
            assertEquals(0.3, anim.value(), EPSILON);   // still paused, still 0.3
            assertTrue(anim.isPaused());

            anim.resume();
            now.addAndGet(60);
            assertEquals(0.15, anim.value(), EPSILON);  // continues downwards
        }

        @Test
        void canPauseAgainAfterResume() {
            Animation anim = linear(400);
            anim.forward();
            now.addAndGet(40);                          // 0.1
            anim.pause();
            now.addAndGet(500);
            anim.resume();
            now.addAndGet(80);                          // 0.3
            anim.pause();
            now.addAndGet(500);
            anim.resume();
            now.addAndGet(60);

            assertEquals(0.45, anim.value(), EPSILON);  // both pauses skipped
        }
    }
}
