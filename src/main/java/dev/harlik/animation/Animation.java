package dev.harlik.animation;

import dev.harlik.animation.easing.Easing;

import java.util.function.LongSupplier;

/**
 * A tween that turns elapsed time into a value between {@code 0.0} and {@code 1.0}.
 * <p>
 * The animation does not draw or update anything by itself. Call {@link #value()}
 * whenever you need the current value, for example once per frame, and use it to
 * move, fade or scale whatever you are animating.
 *
 * <pre>{@code
 * Animation menuOpen = Animation.builder()
 *         .duration(300)
 *         .easing(Easing.EASE_OUT)
 *         .build();
 *
 * menuOpen.forward();                    // start opening
 * // every frame:
 * double x = menuOpen.value();           // 0.0 = closed, 1.0 = open
 * }</pre>
 */
public class Animation {

    private final long durationMillis;
    private final long delayMillis;
    private final Easing easing;
    private Direction direction;
    private final Stopwatch stopwatch;

    private Animation(Builder builder) {
        this.durationMillis = builder.durationMillis;
        this.delayMillis = builder.delayMillis;
        this.easing = builder.easing;
        this.stopwatch = new Stopwatch(builder.clock);
        this.direction = Direction.FORWARD;
    }

    /**
     * Starts configuring a new animation.
     * <p>
     * Defaults: 400 ms, {@link Easing#LINEAR}, the system clock.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Plays the animation towards {@code 1.0}.
     * <p>
     * The first call starts from {@code 0.0}. If the animation is already
     * running backwards, it turns around and continues from its current value
     * without jumping. If it is paused, it stays paused and plays forwards
     * after {@link #resume()}.
     * <p>
     * When starting from {@code 0.0}, it first waits for the
     * {@linkplain Builder#delay(long) delay}. Calling it again while already
     * playing forwards does nothing.
     */
    public void forward() {
        if (stopwatch.isStarted() && direction == Direction.FORWARD) return;

        double p = stopwatch.isStarted() ? progress() : 0;
        long wait = p == 0 ? delayMillis : 0;
        direction = Direction.FORWARD;
        stopwatch.setElapsed(Math.round(p * durationMillis) - wait);
    }

    /**
     * Plays the animation towards {@code 0.0}.
     * <p>
     * The first call starts from {@code 1.0}. If the animation is already
     * running forwards, it turns around and continues from its current value
     * without jumping. If it is paused, it stays paused and plays backwards
     * after {@link #resume()}.
     * <p>
     * When starting from {@code 1.0}, it first waits for the
     * {@linkplain Builder#delay(long) delay}. Calling it again while already
     * playing backwards does nothing.
     */
    public void reverse() {
        if (stopwatch.isStarted() && direction == Direction.REVERSE) return;

        double p = stopwatch.isStarted() ? progress() : 1;
        long wait = p == 1 ? delayMillis : 0;
        direction = Direction.REVERSE;
        stopwatch.setElapsed(Math.round((1 - p) * durationMillis) - wait);
    }

    /**
     * Freezes the animation at its current value.
     * <p>
     * Does nothing if the animation has not been started or is already paused.
     */
    public void pause() {
        stopwatch.pause();
    }

    /**
     * Continues a paused animation from where it was paused. The time spent
     * paused is not counted.
     * <p>
     * Does nothing if the animation is not paused.
     */
    public void resume() {
        stopwatch.resume();
    }

    /**
     * Puts the animation back to how it was before it was first started.
     * <p>
     * The value jumps to {@code 0.0} without animating, and the animation is no
     * longer paused or reversed. The next {@link #forward()} or {@link #reverse()}
     * starts it again as if it were new, including the
     * {@linkplain Builder#delay(long) delay}.
     */
    public void reset() {
        stopwatch.reset();
        direction = Direction.FORWARD;
    }

    /**
     * Jumps to a point in the animation and keeps playing from there.
     * <p>
     * {@code progress} is how much of the duration has passed, before the
     * easing is applied: {@code 0.0} is the start, {@code 0.5} is halfway in
     * time and {@code 1.0} is the end. It means the same point in both
     * directions, so {@code seek(0.25)} lands in the same place whether the
     * animation is playing forwards or backwards.
     * <p>
     * The direction does not change. If the animation is paused, it jumps and
     * stays paused. If it has not been started yet, it starts playing forwards
     * from {@code progress}. Values outside {@code 0..1} are clamped.
     *
     * @param progress where to jump, from {@code 0.0} (start) to {@code 1.0} (end)
     */
    public void seek(double progress) {
        progress = Math.clamp(progress, 0.0, 1.0);
        double p = direction == Direction.FORWARD ? progress : 1 - progress;
        stopwatch.setElapsed(Math.round(p * durationMillis));
    }

    /**
     * Returns how much of the duration has passed, without the easing applied.
     * <p>
     * This is the time progress that {@link #value()} passes to the easing. It
     * changes at a constant speed and never leaves {@code 0.0..1.0}, even if
     * the easing overshoots. It uses the same scale as {@link #seek(double)}.
     * <p>
     * {@code 0.0} before the animation is started. Once it reaches the end, it
     * stays at {@code 1.0} (forward) or {@code 0.0} (reverse).
     *
     * @return the time progress, from {@code 0.0} to {@code 1.0}
     */
    public double progress() {
        if (!stopwatch.isStarted()) return 0.0;

        if (durationMillis == 0) {
            double progress = stopwatch.elapsed() < 0 ? 0.0 : 1.0;   // still waiting : done
            return direction == Direction.FORWARD ? progress : 1 - progress;
        }

        double progress = (double) stopwatch.elapsed() / durationMillis;
        return Math.clamp(direction == Direction.FORWARD ? progress : 1 - progress, 0.0, 1.0);
    }

    /**
     * Returns the current value, with the easing applied.
     * <p>
     * {@code 0.0} before the animation is started. Once it reaches the end, it
     * stays at {@code 1.0} (forward) or {@code 0.0} (reverse).
     *
     * @return the current value, between {@code 0.0} and {@code 1.0} for the
     *         built-in easings
     */
    public double value() {
        return easing.apply(progress());
    }

    /**
     * Returns the current value in the range that you set, with the easing applied.
     * <p>
     * Returns {@code start} value before the animation is started. Once it reaches the end, it
     * stays at {@code end} (forward) or {@code start} (reverse).
     *
     * @param start the starting value of the animation
     * @param end the end value of the animation
     * @return the current value, between {@code start} and {@code end} for the
     *         built-in easings
     */
    public double value(double start, double end) {
        return start + (end - start) * value();
    }

    /**
     * @return {@code true} if the animation has been started, is not paused
     *         and has not reached its end yet
     */
    public boolean isRunning() {
        return stopwatch.isStarted() && !stopwatch.isPaused() && !isFinished();
    }

    /**
     * Tells which way the animation plays: towards {@code 0.0} after
     * {@link #reverse()}, towards {@code 1.0} after {@link #forward()}.
     * <p>
     * This is the direction only, not whether it is moving. A paused or
     * finished animation still reports the direction it was last played in.
     *
     * @return {@code true} if {@link #reverse()} was called more recently than
     *         {@link #forward()}; {@code false} before the animation is started
     */
    public boolean isReversed() {
        return direction == Direction.REVERSE;
    }

    /**
     * @return {@code true} if {@link #pause()} was called and
     *         {@link #resume()} has not been called since
     */
    public boolean isPaused() {
        return stopwatch.isPaused();
    }

    /**
     * Tells whether the animation has reached the end in its current
     * direction: {@code 1.0} for {@link #forward()}, {@code 0.0} for
     * {@link #reverse()}.
     *
     * @return {@code true} if the end has been reached; {@code false} before
     *         the animation is started
     */
    public boolean isFinished() {
        return stopwatch.isStarted() && (stopwatch.elapsed() >= durationMillis);
    }

    /**
     * Configures and creates an {@link Animation}. Get one with
     * {@link Animation#builder()}.
     */
    public static final class Builder {
        private long durationMillis = 400;
        private long delayMillis = 0;
        private Easing easing = Easing.LINEAR;
        private LongSupplier clock = () -> System.nanoTime() / 1_000_000;

        private Builder() {}

        /**
         * Sets how long a full run from {@code 0.0} to {@code 1.0} takes.
         * Shorter if the animation turns around midway.
         * <p>
         * With {@code 0}, the animation jumps to its end immediately.
         * Default: 400.
         *
         * @param millis the duration in milliseconds, {@code 0} or more
         * @return this builder
         */
        public Builder duration(long millis) {
            if (millis < 0) throw new IllegalArgumentException("Duration must be >= 0, got " + millis);
            this.durationMillis = millis;
            return this;
        }

        /**
         * Sets how long to wait before the animation starts moving.
         * <p>
         * The wait happens when {@link Animation#forward()} starts from
         * {@code 0.0} or {@link Animation#reverse()} starts from {@code 1.0},
         * including the first call. Turning around midway happens immediately,
         * and calling {@code forward()} again while already playing forwards
         * does not restart the wait.
         * <p>
         * During the wait, the value stays where it starts,
         * {@link Animation#isRunning()} is {@code true}, and
         * {@link Animation#pause()} freezes the countdown.
         * {@link Animation#seek(double)} skips the rest of the wait.
         * Default: 0.
         *
         * @param millis the delay in milliseconds, {@code 0} or more
         * @return this builder
         */
        public Builder delay(long millis) {
            if (millis < 0) throw new IllegalArgumentException("Delay must be >= 0, got " + millis);
            this.delayMillis = millis;
            return this;
        }

        /**
         * Sets the curve that shapes how the value changes over time.
         * Default: {@link Easing#LINEAR}.
         *
         * @param easing one of the {@link Easing} constants,
         *               {@link Easing#cubicBezier}, or your own function
         * @return this builder
         */
        public Builder easing(Easing easing) {
            if  (easing == null) throw new IllegalArgumentException("Easing cannot be null");
            this.easing = easing;
            return this;
        }

        /**
         * Sets where the animation reads the current time from.
         * <p>
         * You usually don't need this. It's meant for tests, where a fake clock
         * lets you control time, and for frameworks that have their own frame
         * clock. The clock must count in milliseconds and never go backwards.
         * Its starting point does not matter. Default: {@link System#nanoTime()}
         * converted to milliseconds.
         *
         * @param clock returns the current time in milliseconds
         * @return this builder
         */
        public Builder clock(LongSupplier clock) {
            if (clock == null) throw new IllegalArgumentException("Clock cannot be null");
            this.clock = clock;
            return this;
        }

        /**
         * Creates the animation. It doesn't start until {@link Animation#forward()}
         * or {@link Animation#reverse()} is called.
         *
         * @return a new animation with the configured settings
         */
        public Animation build() {
            return new Animation(this);
        }
    }
}
