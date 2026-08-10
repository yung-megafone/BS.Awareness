# LAD — Lazy Ass Developer

LAD is BSA's Windows development helper.

It consolidates the repetitive Android/Android Auto development workflow into one GUI:

- Prefer Android Studio's bundled JBR
- Validate Java 17+
- Detect and authorize ADB devices
- Configure and validate local release signing
- Build the POI/NAV × Debug/Release test matrix
- Install all four variants
- Detect the Android Auto Head Unit Server
- Configure ADB forwarding
- Launch the Desktop Head Unit
- Save logs under `build-logs/`

## Launch

Double-click:

`Launch-LAD.cmd`

## Main button states

- `JUST DO IT`
- `PLEASE DON'T FUCKING BREAK`
- `HOLY SHIT IT WORKED`
- `PLEASE DON'T BREAK (AGAIN)`

## Why PowerShell?

LAD is currently intentionally Windows-first because BSA's development environment is Windows + Android Studio + ADB + DHU. PowerShell gives direct access to those tools and WinForms without requiring an additional runtime.

If LAD later becomes cross-platform, grows beyond a few thousand lines, needs richer state management, or needs a more polished UI, Python (or a small native/.NET app) becomes more attractive.
