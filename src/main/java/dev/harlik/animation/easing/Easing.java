package dev.harlik.animation.easing;

/**
 * A curve that shapes how an animation's value changes over time.
 * <p>
 * It takes the time progress {@code t} (how much of the duration has passed)
 * and returns how far the value should be at that moment. Easings should
 * return {@code 0.0} at {@code t = 0} and {@code 1.0} at {@code t = 1}.
 * <p>
 * You can write your own as a lambda: {@code Easing quad = t -> t * t;}
 */
@FunctionalInterface
public interface Easing {

    /**
     * @param t the time progress, from {@code 0.0} (start) to {@code 1.0} (end)
     * @return the eased progress
     */
    double apply(double t);

    /** Constant speed. The value equals the time progress. */
    Easing LINEAR = t -> t;

    /** Starts slow, ends fast. Same as CSS {@code ease-in}. */
    Easing EASE_IN = cubicBezier(0.42, 0.0, 1.0,  1.0);

    /** Starts fast, ends slow. Same as CSS {@code ease-out}. */
    Easing EASE_OUT = cubicBezier(0.0,  0.0, 0.58, 1.0);

    /** Slow at both ends, fast in the middle. Same as CSS {@code ease-in-out}. */
    Easing EASE_IN_OUT = cubicBezier(0.42, 0.0, 0.58, 1.0);

    /**
     * Creates a cubic Bézier easing, the same as CSS
     * {@code cubic-bezier(x1, y1, x2, y2)}. The curve runs from (0, 0) to
     * (1, 1), and the two control points bend it.
     * <p>
     * You can design curves visually at <a href="https://cubic-bezier.com">cubic-bezier.com</a>.
     * {@code y1} and {@code y2} may go outside {@code 0..1} to overshoot.
     * Values of {@code t} outside {@code 0..1} are clamped.
     *
     * @param x1 time of the first control point, between {@code 0} and {@code 1}
     * @param y1 value of the first control point
     * @param x2 time of the second control point, between {@code 0} and {@code 1}
     * @param y2 value of the second control point
     * @return the easing
     */
    static Easing cubicBezier(double x1, double y1, double x2, double y2) {
        return t -> {
            if (t <= 0) return 0;
            if (t >= 1) return 1;
            double lo = 0, hi = 1, u = t;
            for (int i = 0; i < 30; i++) {          // solve bezierX(u) = t
                if (bezier(u, x1, x2) < t) lo = u; else hi = u;
                u = (lo + hi) / 2;
            }
            return bezier(u, y1, y2);
        };
    }

    private static double bezier(double u, double p1, double p2) {
        double m = 1 - u;
        return 3 * m * m * u * p1 + 3 * m * u * u * p2 + u * u * u;
    }
}
