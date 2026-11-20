package dev.harlik.animation;

import dev.harlik.animation.easing.Easing;

import java.util.function.LongSupplier;

public class Animation {

    private final long durationMillis;
    private final Easing easing;
    private Direction direction;
    private final Stopwatch stopwatch;

    private Animation(Builder builder) {
        this.durationMillis = builder.durationMillis;
        this.easing = builder.easing;
        this.stopwatch = new Stopwatch(builder.clock);
        this.direction = Direction.FORWARD;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void forward() {
        double p = stopwatch.isStarted() ? progress() : 0;
        direction = Direction.FORWARD;
        stopwatch.setElapsed(Math.round(p * durationMillis));
    }

    public void reverse() {
        double p = stopwatch.isStarted() ? progress() : 1;
        direction = Direction.REVERSE;
        stopwatch.setElapsed(Math.round((1 - p) * durationMillis));
    }

    public void pause() {
        stopwatch.pause();
    }

    public void resume() {
        stopwatch.resume();
    }

    public boolean isPaused() {
        return stopwatch.isPaused();
    }

    private double progress() {
        double progress = (double) stopwatch.elapsed() / durationMillis;
        return Math.clamp(direction == Direction.FORWARD ? progress : 1 - progress, 0.0, 1.0);
    }

    public double value() {
        if (!stopwatch.isStarted()) return 0.0;

        if (durationMillis == 0) {
            return easing.apply(direction == Direction.FORWARD ? 1.0 : 0.0);
        }

        return easing.apply(progress());
    }

    public boolean isRunning() {
        return stopwatch.isStarted() && !stopwatch.isPaused() && !isFinished();
    }

    public boolean isFinished() {
        return stopwatch.isStarted() && (stopwatch.elapsed() >= durationMillis);
    }

    public static final class Builder {
        private long durationMillis = 400;
        private Easing easing = Easing.LINEAR;
        private LongSupplier clock = () -> System.nanoTime() / 1_000_000;

        private Builder() {}

        public Builder duration(long millis) {
            this.durationMillis = millis;
            return this;
        }

        public Builder easing(Easing easing) {
            this.easing = easing;
            return this;
        }

        public Builder clock(LongSupplier clock) {
            this.clock = clock;
            return this;
        }

        public Animation build() {
            return new Animation(this);
        }
    }
}
