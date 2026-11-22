# Spell Animations

A small Java tween library. It turns elapsed time into a value from `0.0` to `1.0`, with easing, smooth reversing, delaying and pause/resume.

The animation doesn't draw anything itself. You ask it for `value()` whenever you need it, usually once per frame, and use that number to move, fade or scale whatever you like. There are no threads, listeners or global managers. Each animation is a plain object, so you can have as many as you want.

## Quick example

```java
import dev.harlik.animation.Animation;
import dev.harlik.animation.easing.Easing;

Animation fadeIn = Animation.builder()
        .duration(300)
        .easing(Easing.EASE_OUT)
        .build();

fadeIn.forward();

while (!fadeIn.isFinished()) {
    double opacity = fadeIn.value();   // 0.0 → 1.0 over 300 ms
    // draw something with opacity...
    Thread.sleep(16);
}
```

To animate between any two numbers, pass them to `value()`:

```java
double x = slide.value(100, 350);   // 100 at the start, 350 at the end
```

## Installation

Requires **Java 25** or newer.

The library isn't published to Maven Central yet. Install it to your local Maven repository:

```sh
git clone https://github.com/getharlik/spell-animations.git
cd spell-animations
mvn install
```

Then add it to your project's `pom.xml`:

```xml
<dependency>
    <groupId>dev.harlik</groupId>
    <artifactId>spell-animations</artifactId>
    <version>1.0</version>
</dependency>
```

## Features

### Settings

| Builder method   | Default      | Notes                                                    |
|------------------|--------------|----------------------------------------------------------|
| `duration(ms)`   | `400`        | `0` jumps straight to the end. Negative values throw.    |
| `delay(ms)`      | `0`          | See [Delay](#delay). Negative values throw.              |
| `easing(easing)` | `LINEAR`     | See [Easings](#easings). `null` throws.                  |
| `clock(clock)`   | system clock | See [Testing your animations](#testing-your-animations). |

Invalid values throw `IllegalArgumentException` from the builder. A duration of `0` is handy for turning animations off, for example when the user prefers reduced motion:

```java
Animation.builder().duration(reducedMotion ? 0 : 300).build();
```

### Easings

```java
Animation.builder().easing(Easing.EASE_IN_OUT).build();                       // built-in
Animation.builder().easing(Easing.cubicBezier(0.34, 1.56, 0.64, 1)).build();  // CSS-style curve, overshoots past 1.0
Animation.builder().easing(t -> t * t).build();                               // any function
```

Built-in: `LINEAR` (default), `EASE_IN`, `EASE_OUT`, `EASE_IN_OUT`. These match their CSS equivalents. `cubicBezier` works like CSS `cubic-bezier()`, so you can design curves at [cubic-bezier.com](https://cubic-bezier.com).

### Reverse at any time

```java
menu.forward();   // menu starts opening...
// ...the user clicks again halfway through:
menu.reverse();   // closes from where it is, no jump
```

`forward()` plays towards `1.0` and `reverse()` plays towards `0.0`. Switching direction midway continues from the current value, so the way back takes only as long as the way there did.

### Delay

```java
Animation tooltip = Animation.builder()
        .duration(200)
        .delay(500)
        .build();

tooltip.forward();   // waits 500 ms, then fades in over 200 ms
```

The delay applies when the animation starts from an end: `forward()` from `0.0` or `reverse()` from `1.0`. Turning around midway happens immediately, so a menu closed halfway through opening reacts right away. Calling `forward()` again while it's already playing forwards doesn't restart the wait, so it's safe to call every frame.

During the wait, `value()` stays where it starts and `isRunning()` is `true`. `pause()` freezes the countdown, and `seek()` skips it.

Different delays make a staggered list:

```java
for (int i = 0; i < items.size(); i++) {
    items.get(i).anim = Animation.builder().delay(i * 50L).build();
}
items.forEach(item -> item.anim.forward());   // each starts 50 ms after the previous
```

### Pause and resume

```java
anim.pause();     // value freezes
anim.resume();    // continues; the paused time doesn't count
```

### Jump to a point

```java
anim.seek(0.5);     // jump to halfway in time and keep playing from there
anim.progress();    // 0.5, the time progress before the easing
anim.value();       // the eased value at that point
```

`seek()` and `progress()` use the same scale in both directions: `0.0` is the start and `1.0` is the end. Seeking keeps the current direction, and a paused animation stays paused. Values outside `0..1` are clamped. This is useful for scrubbing with a slider.

### Reset

```java
anim.reset();       // back to "not started": value 0.0, not paused, not reversed
anim.forward();     // plays from the start again, including the delay
```

Once an animation has finished going forwards, calling `forward()` again does nothing. Call `reset()` first to replay it. `reset()` jumps straight to `0.0` without animating; to animate back, use `reverse()`.

### Status

```java
anim.isRunning();   // started, not paused, not at the end yet
anim.isPaused();
anim.isFinished();  // reached 1.0 (forward) or 0.0 (reverse)
anim.isReversed();  // reverse() was called more recently than forward()
```

## Testing your animations

By default, animations use the system clock. In tests you can pass your own clock and control time yourself:

```java
AtomicLong now = new AtomicLong(1_000);

Animation anim = Animation.builder()
        .duration(400)
        .clock(now::get)          // any LongSupplier returning milliseconds
        .build();

anim.forward();
now.addAndGet(100);               // "wait" 100 ms

assertEquals(0.25, anim.value(), 1e-9);
```

## Building

```sh
mvn clean test          # build and run the tests
mvn javadoc:javadoc     # API docs → target/reports/apidocs/index.html
```

## License

[MIT](LICENSE)
