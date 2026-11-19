package dev.harlik.animation.easing;

@FunctionalInterface
public interface Easing {
    double apply(double t);

    Easing LINEAR = t -> t;
}
