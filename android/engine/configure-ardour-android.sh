#!/usr/bin/env bash
set -euo pipefail

# Cross-configure Ardour's headless engine/backend for Android arm64.
#
# This script intentionally does not download dependencies. It consumes a
# reproducible Android dependency prefix prepared by the dependency-build step.
#
# Required:
#   ANDROID_NDK_HOME
#   ARDOUR_ANDROID_DEPS_PREFIX
#   OBOE_PREFAB_INCLUDE
#   OBOE_PREFAB_LIBDIR
#
# Optional:
#   ARDOUR_ANDROID_API (default: 29)

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
API="${ARDOUR_ANDROID_API:-29}"
DEPS_PREFIX="${ARDOUR_ANDROID_DEPS_PREFIX:?Set ARDOUR_ANDROID_DEPS_PREFIX}"
NDK="${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME}"
# The vcpkg dependency stage includes Oboe. Explicit variables remain useful
# when testing a Gradle/Prefab build of a different Oboe revision.
OBOE_INCLUDE="${OBOE_PREFAB_INCLUDE:-$DEPS_PREFIX/include}"
OBOE_LIBDIR="${OBOE_PREFAB_LIBDIR:-$DEPS_PREFIX/lib}"

case "$(uname -s)" in
    Linux) HOST_TAG="linux-x86_64" ;;
    Darwin)
        case "$(uname -m)" in
            arm64) HOST_TAG="darwin-x86_64" ;; # NDK still ships the x86_64 host toolchain
            *) HOST_TAG="darwin-x86_64" ;;
        esac
        ;;
    *)
        echo "Unsupported NDK host: $(uname -s)" >&2
        exit 2
        ;;
esac

TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$HOST_TAG"
BIN="$TOOLCHAIN/bin"
TARGET="aarch64-linux-android"

for path in     "$BIN/${TARGET}${API}-clang"     "$BIN/${TARGET}${API}-clang++"     "$BIN/llvm-ar"     "$BIN/llvm-ranlib"     "$BIN/llvm-strip"     "$DEPS_PREFIX"     "$OBOE_INCLUDE"     "$OBOE_LIBDIR"
do
    if [[ ! -e "$path" ]]; then
        echo "Missing Android engine build input: $path" >&2
        exit 2
    fi
done

export CC="$BIN/${TARGET}${API}-clang"
export CXX="$BIN/${TARGET}${API}-clang++"
export AR="$BIN/llvm-ar"
export RANLIB="$BIN/llvm-ranlib"
export STRIP="$BIN/llvm-strip"
export NM="$BIN/llvm-nm"

# Never let host pkg-config metadata leak into the target graph.
export PKG_CONFIG_DIR=""
export PKG_CONFIG_PATH=""
export PKG_CONFIG_LIBDIR="$DEPS_PREFIX/lib/pkgconfig:$DEPS_PREFIX/share/pkgconfig"

export CFLAGS="--sysroot=$TOOLCHAIN/sysroot -fPIC ${CFLAGS:-}"
export CXXFLAGS="--sysroot=$TOOLCHAIN/sysroot -fPIC -std=c++17 ${CXXFLAGS:-}"
export LDFLAGS="--sysroot=$TOOLCHAIN/sysroot -L$DEPS_PREFIX/lib ${LDFLAGS:-}"

cd "$ROOT_DIR"

./waf configure     --android-target     --dist-target=aarch64     --with-backends=android     --android-oboe-include="$OBOE_INCLUDE"     --android-oboe-libdir="$OBOE_LIBDIR"     --also-include="$DEPS_PREFIX/include"     --also-libdir="$DEPS_PREFIX/lib"     --cxx17     --no-lxvst     --no-vst3     --no-lrdf     --no-nls     --no-phone-home     --no-threaded-waveviews     --internal-shared-libs

echo
echo "Android Ardour engine configured."
echo "Next: ./waf build --targets=libardour,android_audiobackend"