vcpkg_download_distfile(
    ARCHIVE
    URLS
        "https://download.gnome.org/sources/libsigc++/2.10/libsigc++-2.10.0.tar.xz"
        "https://ftp.gnome.org/pub/GNOME/sources/libsigc++/2.10/libsigc++-2.10.0.tar.xz"
    FILENAME "libsigc++-2.10.0.tar.xz"
    SHA512 5b96df21d6bd6ba41520c7219e77695a86aabc60b7259262c7a9f4b8475ce0e2fd8dc37bcf7c17e24e818ff28c262d682b964c83e215b51bdbe000f3f58794ae
)

vcpkg_extract_source_archive(
    SOURCE_PATH
    ARCHIVE "${ARCHIVE}"
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
