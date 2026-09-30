# Ardour Android port

This directory contains the additive Android frontend and native bridge for the
Ardour engine. The existing GTK/Waf desktop build remains intact.

## UI source of truth

The Android frontend consumes the current One UI 8 work from:

- repository: `Ragnarok93/oneui-compose`
- branch: `feature/oneui8-compose-components`
- module: `:lib`
- namespace: `org.oneui.compose`

Do not create a second Ardour-specific design system.

Ardour owns DAW layout, editing behavior and state. OneUI Compose owns semantic
colors, typography, shape, motion, interaction behavior, icons and reusable
application controls.

The Gradle build uses a composite source dependency so UI work is compiled
against the actual OneUI Compose library rather than copied source.

## Current bootstrap

- Android Gradle Plugin 9.3.2
- Kotlin / Compose plugin 2.4.20
- Compose BOM 2026.08.00
- Java 21
- compileSdk 37
- arm64-v8a first
- NDK 28.2.13676358
- versioned JNI bridge (`ArdourNative`)
- adaptive phone / tablet / desktop-DeX workspace shell
- OneUI Compose theme/components integrated
- current engine status: JNI bridge only; libardour is not linked yet

## Local setup

Check out the One UI project next to or inside the Android tree, then point the
build at it:

```bash
git clone -b feature/oneui8-compose-components \
  https://github.com/Ragnarok93/oneui-compose.git \
  android/oneui-compose-src

cd android
gradle :app:assembleDebug \
  -PoneuiComposeDir="$PWD/oneui-compose-src"
```

CI performs the same two-repository checkout automatically.

## Architecture rule

The Android frontend must never become part of Ardour's realtime audio path.

```text
OneUI Compose frontend
        |
        v
presentation snapshots / command queue
        |
        v
JNI frontend bridge
        |
        v
libardour
        |
        v
AndroidAudioBackend
        |
        v
Oboe / AAudio
```

UI state, disk access, Kotlin allocation, JNI callbacks and Android lifecycle
work stay off the realtime processing callback.

## Phase 1: engine bring-up

1. Cross-compile backend-only Ardour dependencies for Android arm64.
2. Link the smallest viable libardour subset into the Android native target.
3. Bootstrap Ardour/PBD without GTK.
4. Add a native headless smoke test:
   - initialize engine
   - create/open session
   - import audio
   - process offline
   - save/reopen session
5. Add structured native diagnostics surfaced through `ArdourNative`.

## Phase 2: Android audio backend

Implement `AndroidAudioBackend : ARDOUR::AudioBackend` backed by Oboe/AAudio.

Requirements:

- callback-driven low-latency output
- native sample-rate preference
- explicit buffer-size and XRUN telemetry
- input/recording support
- multichannel USB audio
- hotplug/device-route recovery
- no locks, allocation, JNI, UI or filesystem work in the process callback

## Phase 3: presentation model

Do not expose libardour as thousands of fine-grained JNI getters.

Create batched immutable snapshots for:

- transport
- session
- tracks/routes
- regions/playlists
- mixer
- plugins
- automation

Commands flow toward libardour through a queue; state changes flow back as
coalesced snapshots. Meters use a dedicated sampled telemetry path.

## Phase 4: One UI DAW frontend

Use the OneUI Compose project directly for:

- semantic light/dark colors
- typography
- shapes
- motion
- press/hover/focus behavior
- buttons and icon buttons
- cards/surfaces
- lists
- navigation
- dialogs/sheets
- preference/settings UI
- icons

Ardour-specific components should be limited to domain widgets such as the
timeline, waveform regions, automation curves, piano roll, meters, faders and
plugin-editor hosting.

Layout goals:

- phone: one primary workspace at a time
- tablet: two-pane editing
- DeX / large desktop: inspector + arrange/mixer/editor + browser
- touch, mouse, keyboard and stylus input
- One UI 8 interaction language with professional DAW density
- Logic-style workspace organization without copying Apple assets

## Plugin strategy

1. bundled/native ARM64 plugins
2. Android-native LV2/VST3/AAP
3. optional Windows VST compatibility runtime via isolated Wine/FEX bridge

Plugin execution must not be coupled to the UI process.
