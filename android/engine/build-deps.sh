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
DOWNLOADS_ROOT="${ARDOUR_VCPKG_DOWNLOADS:-$BUILD_ROOT/vcpkg-downloads}"
OVERLAY_ROOT="$BUILD_ROOT/vcpkg-overlays"

mkdir -p "$BUILD_ROOT" "$DOWNLOADS_ROOT" "$OVERLAY_ROOT"
export VCPKG_DOWNLOADS="$DOWNLOADS_ROOT"

if [[ ! -d "$VCPKG_ROOT/.git" ]]; then
    rm -rf "$VCPKG_ROOT"
    # vcpkg's version database points at historical git-tree objects. A shallow
    # clone cannot resolve those trees, so keep full commit/tree history while
    # filtering blobs. Missing blobs are fetched lazily by Git as vcpkg needs
    # individual historical port definitions.
    git clone --filter=blob:none --no-checkout         https://github.com/microsoft/vcpkg.git         "$VCPKG_ROOT"
fi

git -C "$VCPKG_ROOT" fetch --filter=blob:none origin "$VCPKG_COMMIT"
git -C "$VCPKG_ROOT" checkout --detach "$VCPKG_COMMIT"

if [[ ! -x "$VCPKG_ROOT/vcpkg" ]]; then
    "$VCPKG_ROOT/bootstrap-vcpkg.sh" -disableMetrics
fi

# Ardour currently requires the glibmm-2.4 ABI. That pins GLib to the historical
# 2.66.4 vcpkg port, whose manifest predates the host-tool rename from
# "tool-meson" to "vcpkg-tool-meson". Materialize that exact historical port as
# an overlay and update only the retired host-tool dependency name.
#
# The tree hash is the registry entry for glib 2.66.4#2.
GLIB_2664_TREE="c8d7eeabc89610c8b583c319b572b16e07f3f035"
GLIB_OVERLAY="$OVERLAY_ROOT/glib"
rm -rf "$GLIB_OVERLAY"
mkdir -p "$GLIB_OVERLAY"
git -C "$VCPKG_ROOT" archive "$GLIB_2664_TREE" | tar -x -C "$GLIB_OVERLAY"

python3 - "$GLIB_OVERLAY/vcpkg.json" <<'PY'
import json
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
data = json.loads(path.read_text())
deps = data.get("dependencies", [])
data["dependencies"] = [
    "vcpkg-tool-meson" if dep == "tool-meson" else dep
    for dep in deps
]
path.write_text(json.dumps(data, indent=2) + "\n")
PY

# The manifest pins the same registry commit as this checkout. Keeping the
# install root outside vcpkg itself makes CI/local caches replaceable without
# mutating the pinned tool checkout.
"$VCPKG_ROOT/vcpkg" install \
    --triplet "$TRIPLET" \
    --overlay-ports="$ENGINE_DIR/vcpkg-overlays" \
    --x-manifest-root="$ENGINE_DIR" \
    --x-install-root="$INSTALL_ROOT"

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