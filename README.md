# spell-animations

A small Java tween library. It turns elapsed time into a value from `0.0` to `1.0`, with easing, smooth reversing and pause/resume.

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

To animate between any two numbers, use ```value()```'s overload:

```java
double x = slide.value(100, 350);
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

### Pause and resume

```java
anim.pause();     // value freezes
anim.resume();    // continues; the paused time doesn't count
```

### Status

```java
anim.isRunning();   // started, not paused, not at the end yet
anim.isPaused();
anim.isFinished();  // reached 1.0 (forward) or 0.0 (reverse)
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
