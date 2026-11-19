package dev.harlik.animation;

import dev.harlik.animation.easing.Easing;

import java.util.function.LongSupplier;

public class Animation {

    private final long durationMillis;
    private final Easing easing;
    private final LongSupplier clock;

    private long startTime;
    private boolean started;

    private Animation(Builder builder) {
        this.durationMillis = builder.durationMillis;
        this.easing = builder.easing;
        this.clock = builder.clock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void start() {
        startTime = clock.getAsLong();
        started = true;
    }

    public double value() {
        if (!started) {
            return 0.0;
        }
        if (durationMillis == 0) {
            return easing.apply(1.0);
        }
        long elapsed = clock.getAsLong() - startTime;
        double progress = (double) elapsed / durationMillis;
        return easing.apply(Math.clamp(progress, 0.0, 1.0));
    }

    public static final class Builder {

        private long durationMillis = 400;
        private Easing easing = Easing.LINEAR;
        private LongSupplier clock = () -> System.nanoTime() / 1_000_000;

        private Builder() {
        }

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
