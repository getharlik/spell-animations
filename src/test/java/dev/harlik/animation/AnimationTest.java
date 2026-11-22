package dev.harlik.animation;

import dev.harlik.animation.easing.Easing;

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
    class Seeking {

        @Test
        void jumpsAndKeepsPlayingForward() {
            Animation anim = linear(400);
            anim.forward();

            anim.seek(0.3);
            assertEquals(0.3, anim.progress(), EPSILON);

            now.addAndGet(100);
            assertEquals(0.55, anim.progress(), EPSILON);  // continues upwards
        }

        @Test
        void jumpsAndKeepsPlayingInReverse() {
            Animation anim = linear(400);
            anim.reverse();

            anim.seek(0.3);
            assertEquals(0.3, anim.progress(), EPSILON);   // same point as forward

            now.addAndGet(60);
            assertEquals(0.15, anim.progress(), EPSILON);  // continues downwards
        }

        @Test
        void valueIsEasedAtTheNewPoint() {
            Animation anim = Animation.builder()
                    .duration(400)
                    .easing(t -> t * t)
                    .clock(now::get)
                    .build();
            anim.forward();

            anim.seek(0.5);

            assertEquals(0.25, anim.value(), EPSILON);
        }

        @Test
        void staysPausedAfterSeeking() {
            Animation anim = linear(400);
            anim.forward();
            anim.pause();

            anim.seek(0.5);
            now.addAndGet(1_000);
            assertEquals(0.5, anim.progress(), EPSILON);   // frozen at the new point
            assertTrue(anim.isPaused());

            anim.resume();
            now.addAndGet(100);
            assertEquals(0.75, anim.progress(), EPSILON);
        }

        @Test
        void startsPlayingForwardIfNotStarted() {
            Animation anim = linear(400);

            anim.seek(0.5);
            assertTrue(anim.isRunning());

            now.addAndGet(100);
            assertEquals(0.75, anim.progress(), EPSILON);
        }

        @Test
        void clampsBelowZero() {
            Animation anim = linear(400);
            anim.forward();

            anim.seek(-0.5);
            assertEquals(0.0, anim.progress(), EPSILON);

            now.addAndGet(100);
            assertEquals(0.25, anim.progress(), EPSILON);  // no hidden delay
        }

        @Test
        void clampsAboveOne() {
            Animation anim = linear(400);
            anim.forward();

            anim.seek(1.5);

            assertEquals(1.0, anim.progress(), EPSILON);
            assertTrue(anim.isFinished());
        }

        @Test
        void clampsAboveOneInReverse() {
            Animation anim = linear(400);
            anim.reverse();

            anim.seek(1.5);

            assertEquals(1.0, anim.progress(), EPSILON);   // back at the top, not finished
            assertFalse(anim.isFinished());
        }
    }

    @Nested
    class Delay {

        private Animation delayed(long durationMillis, long delayMillis) {
            return Animation.builder()
                    .duration(durationMillis)
                    .delay(delayMillis)
                    .clock(now::get)
                    .build();
        }

        @Test
        void staysAtStartDuringDelay() {
            Animation anim = delayed(400, 200);
            anim.forward();

            now.addAndGet(199);

            assertEquals(0.0, anim.value(), EPSILON);
        }

        @Test
        void startsMovingAfterDelay() {
            Animation anim = delayed(400, 200);
            anim.forward();

            now.addAndGet(200);
            assertEquals(0.0, anim.value(), EPSILON);   // the wait just ended

            now.addAndGet(100);
            assertEquals(0.25, anim.value(), EPSILON);
        }

        @Test
        void doesNotShortenTheDuration() {
            Animation anim = delayed(400, 200);
            anim.forward();

            now.addAndGet(599);
            assertFalse(anim.isFinished());

            now.addAndGet(1);
            assertTrue(anim.isFinished());
        }

        @Test
        void isRunningDuringDelay() {
            Animation anim = delayed(400, 200);
            anim.forward();

            now.addAndGet(100);

            assertTrue(anim.isRunning());
            assertFalse(anim.isFinished());
        }

        @Test
        void callingForwardEveryFrameDoesNotRestartDelay() {
            Animation anim = delayed(400, 300);
            anim.forward();
            now.addAndGet(100);
            anim.forward();
            now.addAndGet(100);
            anim.forward();

            now.addAndGet(200);                          // 100ms after the wait ended

            assertEquals(0.25, anim.value(), EPSILON);
        }

        @Test
        void firstReverseWaitsAtOne() {
            Animation anim = delayed(400, 200);
            anim.reverse();

            now.addAndGet(199);
            assertEquals(1.0, anim.value(), EPSILON);

            now.addAndGet(101);
            assertEquals(0.75, anim.value(), EPSILON);
        }

        @Test
        void reverseFromTheEndWaits() {
            Animation anim = delayed(400, 200);
            anim.forward();
            now.addAndGet(1_000);                        // fully open, then idle

            anim.reverse();
            now.addAndGet(199);
            assertEquals(1.0, anim.value(), EPSILON);

            now.addAndGet(101);
            assertEquals(0.75, anim.value(), EPSILON);
        }

        @Test
        void turningAroundMidwaySkipsDelay() {
            Animation anim = delayed(400, 200);
            anim.forward();
            now.addAndGet(320);                          // 0.3

            anim.reverse();
            now.addAndGet(60);

            assertEquals(0.15, anim.value(), EPSILON);   // no wait
        }

        @Test
        void pauseFreezesTheCountdown() {
            Animation anim = delayed(400, 200);
            anim.forward();
            now.addAndGet(100);
            anim.pause();
            now.addAndGet(1_000);
            anim.resume();

            now.addAndGet(100);
            assertEquals(0.0, anim.value(), EPSILON);    // the 1000ms pause didn't count

            now.addAndGet(100);
            assertEquals(0.25, anim.value(), EPSILON);
        }

        @Test
        void seekSkipsDelay() {
            Animation anim = delayed(400, 200);
            anim.forward();

            anim.seek(0.5);
            assertEquals(0.5, anim.value(), EPSILON);

            now.addAndGet(100);
            assertEquals(0.75, anim.value(), EPSILON);
        }

        @Test
        void zeroDurationWaitsForDelay() {
            Animation anim = delayed(0, 200);
            anim.forward();

            now.addAndGet(199);
            assertEquals(0.0, anim.value(), EPSILON);
            assertFalse(anim.isFinished());

            now.addAndGet(1);
            assertEquals(1.0, anim.value(), EPSILON);
            assertTrue(anim.isFinished());
        }

        @Test
        void zeroDurationWaitsForDelayInReverse() {
            Animation anim = delayed(0, 200);
            anim.reverse();

            now.addAndGet(199);
            assertEquals(1.0, anim.value(), EPSILON);

            now.addAndGet(1);
            assertEquals(0.0, anim.value(), EPSILON);
        }

        @Test
        void negativeDelayThrows() {
            assertThrows(IllegalArgumentException.class, () -> Animation.builder().delay(-1));
        }
    }

    @Nested
    class Progress {

        @Test
        void isZeroBeforeStart() {
            Animation anim = linear(400);

            assertEquals(0.0, anim.progress(), EPSILON);
        }

        @Test
        void ignoresTheEasing() {
            Animation anim = Animation.builder()
                    .duration(400)
                    .easing(Easing.cubicBezier(0.34, 1.56, 0.64, 1))  // overshoots past 1.0
                    .clock(now::get)
                    .build();
            anim.forward();

            now.addAndGet(200);

            assertEquals(0.5, anim.progress(), EPSILON);
            assertTrue(anim.value() > 1.0);
        }

        @Test
        void zeroDurationIsOneForward() {
            Animation anim = linear(0);
            anim.forward();

            assertEquals(1.0, anim.progress(), EPSILON);
        }

        @Test
        void zeroDurationIsZeroInReverse() {
            Animation anim = linear(0);
            anim.reverse();

            assertEquals(0.0, anim.progress(), EPSILON);
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
    class InRange {

        @Test
        void isStartBeforeStart() {
            Animation anim = linear(400);

            assertEquals(100.0, anim.value(100, 350), EPSILON);
        }

        @Test
        void isHalfwayAtHalfTime() {
            Animation anim = linear(400);
            anim.forward();

            now.addAndGet(200);

            assertEquals(225.0, anim.value(100, 350), EPSILON);
        }

        @Test
        void isEndAtTheEnd() {
            Animation anim = linear(400);
            anim.forward();

            now.addAndGet(400);

            assertEquals(350.0, anim.value(100, 350), EPSILON);
        }

        @Test
        void reverseMovesFromEndToStart() {
            Animation anim = linear(400);
            anim.reverse();

            assertEquals(350.0, anim.value(100, 350), EPSILON);
            now.addAndGet(200);
            assertEquals(225.0, anim.value(100, 350), EPSILON);
            now.addAndGet(200);
            assertEquals(100.0, anim.value(100, 350), EPSILON);
        }

        @Test
        void appliesTheEasingOnce() {
            Animation anim = Animation.builder()
                    .duration(400)
                    .easing(t -> t * t)
                    .clock(now::get)
                    .build();
            anim.forward();

            now.addAndGet(200);                                    // eased to 0.25
            assertEquals(162.5, anim.value(100, 350), EPSILON);
        }

        @Test
        void canCountDown() {
            Animation anim = linear(400);
            anim.forward();

            now.addAndGet(100);

            assertEquals(287.5, anim.value(350, 100), EPSILON);   // end below start
        }

        @Test
        void zeroDurationFinishesImmediately() {
            Animation anim = linear(0);
            anim.forward();

            assertEquals(350.0, anim.value(100, 350), EPSILON);
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

    @Nested
    class Status {

        @Test
        void neitherRunningNorFinishedBeforeStart() {
            Animation anim = linear(400);

            assertFalse(anim.isRunning());
            assertFalse(anim.isFinished());
        }

        @Test
        void runningWhilePlaying() {
            Animation anim = linear(400);
            anim.forward();
            now.addAndGet(120);

            assertTrue(anim.isRunning());
            assertFalse(anim.isFinished());
        }

        @Test
        void notFinishedOneMillisecondBeforeTheEnd() {
            Animation anim = linear(400);
            anim.forward();
            now.addAndGet(399);

            assertFalse(anim.isFinished());
        }

        @Test
        void finishedAtTheEnd() {
            Animation anim = linear(400);
            anim.forward();
            now.addAndGet(400);

            assertTrue(anim.isFinished());
            assertFalse(anim.isRunning());
        }

        @Test
        void pausedIsNeitherRunningNorFinished() {
            Animation anim = linear(400);
            anim.forward();
            now.addAndGet(120);
            anim.pause();
            now.addAndGet(1_000);                       // way past the duration, but paused

            assertFalse(anim.isRunning());
            assertFalse(anim.isFinished());
        }

        @Test
        void runningAgainAfterResume() {
            Animation anim = linear(400);
            anim.forward();
            now.addAndGet(120);
            anim.pause();
            now.addAndGet(1_000);

            anim.resume();

            assertTrue(anim.isRunning());
            assertFalse(anim.isFinished());
        }

        @Test
        void reverseAfterFinishingRunsUntilZero() {
            Animation anim = linear(400);
            anim.forward();
            now.addAndGet(400);
            assertTrue(anim.isFinished());

            anim.reverse();
            assertTrue(anim.isRunning());
            assertFalse(anim.isFinished());

            now.addAndGet(399);
            assertFalse(anim.isFinished());

            now.addAndGet(1);
            assertTrue(anim.isFinished());
        }

        @Test
        void reverseMidwayFinishesSooner() {
            Animation anim = linear(400);
            anim.forward();
            now.addAndGet(120);                         // 0.3

            anim.reverse();                             // 120ms back to 0
            now.addAndGet(119);
            assertFalse(anim.isFinished());

            now.addAndGet(1);
            assertTrue(anim.isFinished());
        }

        @Test
        void zeroDurationIsFinishedImmediately() {
            Animation anim = linear(0);
            anim.forward();

            assertTrue(anim.isFinished());
            assertFalse(anim.isRunning());
        }
    }
}
