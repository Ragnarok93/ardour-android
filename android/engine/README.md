# Android engine bring-up

This directory is the bridge between the working Android/Compose/Oboe shell and
Ardour's existing native engine build.

## Current engine target

The first engine target is intentionally narrower than the final DAW:

- Android arm64-v8a
- min API 29
- headless libardour
- Android/Oboe playback backend
- Ardour session graph and internal MIDI buffers
- no GTK/YTK/SUIL
- no desktop VST/LXVST scanner path
- no physical Android MIDI yet
- no audio capture yet

The backend implementation lives in:

    libs/backends/android/

Its realtime topology follows Ardour's PortAudio backend rather than the Dummy
backend:

    Oboe callback
        -> Ardour physical playback port buffers
        -> AudioEngine::process_callback(nframes)
        -> mix connected Ardour output ports
        -> interleave into Oboe output buffer

The first backend exposes only physical playback ports. Capture ports are not
registered until a synchronized Oboe input stream actually exists, so the UI
and routing graph never claim recording support that is not functional.

## Configure contract

Stage dependencies, then configure:

    bash android/engine/build-deps.sh
    export ARDOUR_ANDROID_DEPS_PREFIX="$PWD/.android-engine/vcpkg_installed/arm64-android"
    bash android/engine/configure-ardour-android.sh

Required environment:

- ANDROID_NDK_HOME
- ARDOUR_ANDROID_DEPS_PREFIX

Optional overrides:

- OBOE_PREFAB_INCLUDE
- OBOE_PREFAB_LIBDIR

By default Oboe is taken from the same staged Android dependency prefix.

The dependency prefix must contain Android/arm64 builds and pkg-config metadata.
Host libraries must never be visible through PKG_CONFIG_PATH.

## External dependency prefix

`android/engine/vcpkg.json` and `build-deps.sh` create the initial
`arm64-android` prefix reproducibly. The manifest pins its vcpkg registry and
also pins the older ABI families Ardour currently consumes:

- GLib 2.66.x
- glibmm 2.52.x (`glibmm-2.4` ABI)
- libsigc++ 2.10.x (`sigc++-2.0` ABI)
- TagLib 1.13.x
- Rubber Band 3.3.x

Current glibmm/libsigc++ major releases are deliberately not substituted because
they expose different pkg-config/ABI families from Ardour's existing source.

The current Ardour engine still expects its normal non-GUI dependency graph.
The Android prefix will therefore need target builds for the libraries consumed
by libardour and its core libraries, including at minimum:

- GLib / GThread
- glibmm / giomm
- libsigc++
- libxml2
- libsndfile
- libsamplerate
- libcurl
- libarchive
- liblo
- TagLib
- Boost headers
- FFTW3f
- Aubio
- Vamp SDK / host SDK
- LV2 stack used by libardour (Lilv, Serd, Sord, Sratom) until that source path
  is made optional for the first bootstrap
- UUID support expected by Ardour
- any remaining dependency surfaced by the cross-configure audit

Ardour already carries several libraries in-tree, including pieces such as
libltc, FluidSynth, PT format support, StaffPad support, and the zita DSP
libraries. Those should be cross-built from the Ardour source tree unless an
Android-specific incompatibility forces a different treatment.

## Why Waf remains involved

The Android app uses Gradle/CMake for the APK, JNI shell and Oboe probe, but
libardour has a mature Waf graph with generated configuration and internal
library relationships. Recreating all of that immediately in CMake would add
a second source of truth and make upstream rebases harder.

The intended split is:

1. Cross-build Ardour core/shared libraries and android_audiobackend with Waf.
2. Stage the resulting arm64 shared objects into the Android package.
3. Load/bootstrap libardour from the JNI engine service.
4. Replace the temporary standalone Oboe probe with the Ardour backend stream.

Once this route is proven, individual libraries can be migrated to a tighter
Gradle/CMake build only where doing so materially simplifies Android packaging.

## Next gates

1. Produce the Android dependency prefix.
2. Make Waf configure complete with --android-target.
3. Build libpbd and the smallest transitive core set.
4. Build libardour.
5. Compile android_audiobackend against that libardour.
6. Package the native libraries into the APK.
7. JNI smoke test:
   - ARDOUR::init
   - AudioEngine::create
   - select Android backend
   - start engine
   - create/open a session
   - process silence through a real Ardour master bus
   - stop and deinitialize cleanly
8. Add synchronized Oboe capture and physical capture ports.
9. Add AMidi physical MIDI ports.