package dev.harlik.animation;

import java.util.function.LongSupplier;

final class Stopwatch {

    private final LongSupplier clock;

    private boolean started;
    private long startTime;
    private boolean paused;
    private long pausedAt;

    Stopwatch(LongSupplier clock) {
        this.clock = clock;
    }

    void setElapsed(long elapsedMillis) {
        startTime = getTime() - elapsedMillis;
        started = true;
    }

    long elapsed() {
        return getTime() - startTime;
    }

    private long getTime() {
        return paused ? pausedAt : clock.getAsLong();
    }

    void pause() {
        if (started && !paused) {
            pausedAt = clock.getAsLong();
            paused = true;
        }
    }

    void resume() {
        if (paused) {
            startTime += clock.getAsLong() - pausedAt;
            paused = false;
        }
    }

    void reset() {
        started = false;
        paused = false;
    }

    boolean isStarted() {
        return started;
    }

    boolean isPaused() {
        return paused;
    }

}
