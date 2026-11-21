package dev.harlik.animation;

import dev.harlik.animation.easing.Easing;
import lombok.Getter;

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
    private final Easing easing;
    @Getter
    private Direction direction;
    private final Stopwatch stopwatch;

    private Animation(Builder builder) {
        this.durationMillis = builder.durationMillis;
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
     */
    public void forward() {
        double p = stopwatch.isStarted() ? progress() : 0;
        direction = Direction.FORWARD;
        stopwatch.setElapsed(Math.round(p * durationMillis));
    }

    /**
     * Plays the animation towards {@code 0.0}.
     * <p>
     * The first call starts from {@code 1.0}. If the animation is already
     * running forwards, it turns around and continues from its current value
     * without jumping. If it is paused, it stays paused and plays backwards
     * after {@link #resume()}.
     */
    public void reverse() {
        double p = stopwatch.isStarted() ? progress() : 1;
        direction = Direction.REVERSE;
        stopwatch.setElapsed(Math.round((1 - p) * durationMillis));
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

    private double progress() {
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
        if (!stopwatch.isStarted()) return 0.0;

        if (durationMillis == 0) {
            return easing.apply(direction == Direction.FORWARD ? 1.0 : 0.0);
        }

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
            if (millis < 0) throw new IllegalArgumentException("Duration must be > 0");
            this.durationMillis = millis;
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
