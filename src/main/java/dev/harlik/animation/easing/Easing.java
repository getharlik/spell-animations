package dev.harlik.animation.easing;

@FunctionalInterface
public interface Easing {
    double apply(double t);

    Easing LINEAR = t -> t;
    Easing EASE_IN = cubicBezier(0.42, 0.0, 1.0,  1.0);
    Easing EASE_OUT = cubicBezier(0.0,  0.0, 0.58, 1.0);
    Easing EASE_IN_OUT = cubicBezier(0.42, 0.0, 0.58, 1.0);

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
