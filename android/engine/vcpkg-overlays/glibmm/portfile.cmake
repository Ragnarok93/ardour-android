vcpkg_download_distfile(
    ARCHIVE
    URLS
        "https://download.gnome.org/sources/glibmm/2.52/glibmm-2.52.1.tar.xz"
        "https://ftp.gnome.org/pub/GNOME/sources/glibmm/2.52/glibmm-2.52.1.tar.xz"
    FILENAME "glibmm-2.52.1.tar.xz"
    SHA512 702158762cb28972b315ab98dc00a62e532bda08b6e76dc2a2556e8cb381c2021290891887a4af2fbff5a62bab4d50581be73037dc8e0dc47d5febd6cbeb7bda
)

vcpkg_extract_source_archive(
    SOURCE_PATH
    ARCHIVE "${ARCHIVE}"
    PATCHES
        fix-thread.h.patch
)

vcpkg_configure_make(
    SOURCE_PATH "${SOURCE_PATH}"
    OPTIONS
        --disable-documentation
)

vcpkg_install_make()
vcpkg_fixup_pkgconfig()

file(REMOVE_RECURSE
    "${CURRENT_PACKAGES_DIR}/debug/include"
    "${CURRENT_PACKAGES_DIR}/debug/share"
)

vcpkg_install_copyright(
    FILE_LIST "${SOURCE_PATH}/COPYING"
)
