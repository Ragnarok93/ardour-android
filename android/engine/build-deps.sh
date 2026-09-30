#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENGINE_DIR="$ROOT_DIR/android/engine"
VCPKG_COMMIT="4cb050be2cfa7a947cdd2dd1a70e24b17774c979"
TRIPLET="${ARDOUR_ANDROID_VCPKG_TRIPLET:-arm64-android}"

: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to the Android NDK root}"

BUILD_ROOT="${ARDOUR_ANDROID_BUILD_ROOT:-$ROOT_DIR/.android-engine}"
VCPKG_ROOT="${ARDOUR_VCPKG_ROOT:-$BUILD_ROOT/vcpkg}"
INSTALL_ROOT="${ARDOUR_ANDROID_VCPKG_INSTALLED:-$BUILD_ROOT/vcpkg_installed}"

mkdir -p "$BUILD_ROOT"

if [[ ! -d "$VCPKG_ROOT/.git" ]]; then
    git clone https://github.com/microsoft/vcpkg.git "$VCPKG_ROOT"
fi

git -C "$VCPKG_ROOT" fetch --depth=1 origin "$VCPKG_COMMIT"
git -C "$VCPKG_ROOT" checkout --detach "$VCPKG_COMMIT"

if [[ ! -x "$VCPKG_ROOT/vcpkg" ]]; then
    "$VCPKG_ROOT/bootstrap-vcpkg.sh" -disableMetrics
fi

# The manifest pins the same registry commit as this checkout. Keeping the
# install root outside vcpkg itself makes CI/local caches replaceable without
# mutating the pinned tool checkout.
"$VCPKG_ROOT/vcpkg" install     --triplet "$TRIPLET"     --x-manifest-root="$ENGINE_DIR"     --x-install-root="$INSTALL_ROOT"

PREFIX="$INSTALL_ROOT/$TRIPLET"

for required in     "$PREFIX/include"     "$PREFIX/lib"     "$PREFIX/lib/pkgconfig"
do
    if [[ ! -e "$required" ]]; then
        echo "Android dependency staging is incomplete: $required is missing" >&2
        exit 3
    fi
done

cat <<EOF

Ardour Android dependency prefix ready:
  $PREFIX

Configure the engine with:
  export ARDOUR_ANDROID_DEPS_PREFIX="$PREFIX"
  "$ENGINE_DIR/configure-ardour-android.sh"
EOF
