# UiTemplates: animation template library

This repo is a library of ~200 UI animations written in Compose Multiplatform (Android + iOS), each one a
recreation of a UI-animation reel. It exists so that in **another project** an agent can be asked
"make an animation like X" and copy a working, tuned implementation from here instead of inventing one.

The app in this repo is only a showcase: a list of categories → sets → one button per animation.

## Finding an animation

1. Open [CATALOG.md](CATALOG.md). Every animation has an entry: its name and id, what it looks like, the
   function that draws it, a **Copy** line (the minimal files for that one animation) and a **Strip** line
   (the showcase wrapper to remove).
2. Look both by **component** (the categories) and by **motion** (the "Motion index" at the top: spring,
   squash, stretch, flip/3D, ripple, glow, liquid, particles, shake, morph, stagger, fold, draw, count...).
   Related effects often live in different sets: for example an input that shakes on a wrong value is in
   "Error · Part 1" (`error1-shake-input`, `error1-pin`), while "Input error" holds other error styles. Read
   the candidates and pick by motion, not by set name.
3. Open the demo file in the entry, then the files on its Copy line.
4. If nothing matches what the user describes, say so and offer the closest match (or a combination) before
   building from scratch. Don't claim an animation exists without having found its entry.

To see an animation run: `./gradlew :androidApp:installDebug` on a device or emulator, or open `iosApp/`
in Xcode and run on a simulator. Home → category → set → tap the animation; most play once by themselves, the rest wait for a tap.

## How a set is laid out

Each set is one package under `shared/src/commonMain/kotlin/com/azersun/uitemplates/demos/<set>/`:

| File | What it is | When porting |
|---|---|---|
| `<Name>Demo.kt` | One animation | Copy |
| `*Style.kt` | Shared colours, mock data, icons and the `*Stage` / `*Scene` composable | Take only the declarations the Copy line names |
| `*Demos.kt` | Registers the set in the showcase (`DemoGroup`) | Don't copy |

Some sets reuse helpers from another set or from `common/` (`Confetti.kt`, `CountUp.kt`). Most use
`demos/menus/MenuStyle.kt` for `MenuColors` (the palette) and a `*Stage` (the showcase frame), sometimes one
that lives in another set (`CheckboxStage` in `checkbox1`, `LikeStage` in `like`). The catalog's Copy line
already resolves this: when it says "from `X.kt` only `MenuColors`", copy or inline just that declaration,
not the whole file. `inktheme` is the only set with platform code (an AGSL shader on Android, SkSL on iOS,
via expect/actual), and its Copy line includes the platform files.

## Porting into another project

1. **Copy** exactly what the entry's Copy line lists: the demo file whole, and from every other file only the
   named declarations (with the imports they use). The rest of those files is showcase code or other
   animations, and some of it won't compile without the showcase. Put the code in the target's `commonMain`
   (Compose Multiplatform) or `main` (Android-only Jetpack Compose: all imports are `androidx.compose.*`, so it
   compiles there too). For `inktheme` in an Android-only app, take the Android actual, delete the `expect`
   declaration from `InkShader.kt` and drop the `actual` keyword so it becomes a plain function.
   To port a whole set, combine its entries' Copy lines; a set's `*Demos.kt` is showcase registration only.
   When the entry's call takes an argument (`ThemeToggleDemo(Reveal.Portal)`), one private composable draws
   every variant of the set: keep the branches for your variant (in its `when`s) and delete the others.
2. **Rename** the `package` lines and the `com.azersun.uitemplates.*` imports to the target's package.
   Shared helpers are `internal`: fine in one module, but make them public if the target splits modules.
   Names repeat across sets (`ErrorStyle.kt`/`ErrorColors` in both `error1` and `error2`, a `shake` helper in
   each with different signatures), so rename on copy when porting from more than one set.
3. **Strip the showcase wrapper.** Each demo's body sits inside the stage named on its Strip line
   (`MenuStage(...)`, `CheckboxStage(...)`, `LikeStage(...)`, any `*Stage`). Delete that call and everything
   passed to it: number, title, caption, `autoplay = { … }` and `onReplay = { … }`. Keep only the content
   lambda; that is the component. Usually the autoplay script is just that `autoplay = { … }` argument, but a
   few demos (the kanban board, for one) run their script in their own `LaunchedEffect`: search the demo for
   `autoplay`, `scripted`, `Replay` and any `LaunchedEffect` that begins with a `delay(…)`, and remove those
   too. Sets whose wrapper is a `*Scene` (the stepper, date strip, dialog, product card
   and so on) are different: the scene draws the real component and also takes the frame arguments. Keep the
   scene and remove only its number/title/caption/autoplay/replay parameters and the card it draws around them.
4. **Remove mock content and fake logic.** Demos hardcode sample names, prices and lists, and sometimes fake
   behaviour: a field pre-filled with a sample value, an email regex, a PIN pad that rejects every code, an
   upload that always fails. Replace these with the target's real data and real checks.
5. **Hoist the state.** Demos own their state (`var on by remember { mutableStateOf(false) }`). In the target
   the caller should own it: take `checked: Boolean, onCheckedChange: (Boolean) -> Unit` (or the value plus an
   error flag for inputs) and keep the animation internal. Watch for:
   - `Animatable(0f)` initial values that assume the off state: initialise them from the incoming value so a
     component that starts "on" doesn't animate on first frame.
   - Animations launched by hand in a click handler (`scope.launch { … }` with a manual `Job` cancel): drive
     them from `LaunchedEffect(value)` instead, so external state changes animate too and restarts cancel.
   - One-shot effects such as a shake on a wrong code: trigger them from an event (an incrementing error
     counter in `LaunchedEffect`), not from inside the submit function, so a failure that arrives later from a
     server still plays it.
   - Add `enabled` and the right semantics (`Role.Switch`, `toggleable`) when the demo only has a click.
6. **Restyle**: swap `MenuColors` and the set's palette object for the target's theme colours, sizes and
   typography. Keep the timings, easings, spring parameters and size ratios; those are what make it look the
   same. Demos are drawn for one shape (the hold-to-confirm button is a 96dp circle, switches have fixed track
   sizes); moving to another shape means re-deriving that geometry, not just recolouring.
   Theme reveals (`darkmode-*`, `ink-theme`) compose the content twice: the old theme underneath and the new
   theme on top, clipped by the animated shape. App-wide, wrap the whole screen that way, size the clip to the
   root, take the origin from the real toggle's `positionInRoot()` + half its size, and switch the app's theme
   when the reveal ends.
7. **Run it** and compare side by side with the showcase demo. Compiling is not verifying: these are motion
   effects and only look right or wrong on screen.

Versions used here: Kotlin 2.4, Compose Multiplatform 1.12, Material3. No other libraries are needed by any
animation (the showcase's `navigationevent` dependency is only for its back button).

## Pitfalls already hit in this codebase

- **Negative layout size**: a spring that overshoots below 0 passed to `layout(width, height)` crashes. Clamp
  with `coerceAtLeast(0f)`.
- **`boundsInRoot()` clips** to visible ancestors, so off-screen items report collapsed bounds. Use
  `positionInRoot()` plus `size` when you need where something really is.
- **Lazy list keys must stay unique** even mid-animation (a drag that inserts before it removes will crash).
- **A `Canvas` that returns early** before reading an animated value never subscribes to it and stops redrawing.
  Read the state first, then return.
- **3D rotations** (`rotationX`/`rotationY`) need `cameraDistance` (about `12f * density`) or they distort.
- **Blur** (`Modifier.blur`, `BlurEffect`) only renders on Android 12+ and iOS. Effects must still read on older
  Android through alpha and scale alone.
- **Runtime shaders** need Android 13+ (AGSL); `inktheme` falls back to a clip path below that.
- **Coloured shadows** (`shadow(ambientColor = …, spotColor = …)`) only take their colour on Android 9+; below
  that they draw grey.

## Adding new animations to this repo

The user sends UI-animation reels; each reel usually shows six variations of one component.

1. Study the reel frame by frame and write down, per variation, what moves, how far, in what order and with
   what easing. The reel's own caption is the one-line description.
2. Create a package `demos/<set>/` with a `*Style.kt` (shared scene, palette, autoplay that replays what the
   reel shows ~700 ms after opening, stopped by any touch), one `<Name>Demo.kt` per variation, and a
   `*Demos.kt` exposing `val <set>Group: DemoGroup`.
3. Register the group in the right category in `showcase/Demos.kt`.
4. Run `python3 tools/catalog.py` to regenerate `CATALOG.md`. Never edit the catalog by hand.
5. Verify on a simulator or device by recording the screen and checking frames; then commit
   (Scoped Commits: `<set>: add six <component> animations`).
