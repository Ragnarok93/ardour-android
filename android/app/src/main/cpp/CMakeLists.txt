cmake_minimum_required(VERSION 3.22.1)

project(ardour_android_bridge LANGUAGES C CXX)

find_package(oboe REQUIRED CONFIG)

add_library(
    ardour_android
    SHARED
    ardour_android_bridge.cpp
    android_audio_driver.cpp
)

target_compile_features(ardour_android PRIVATE cxx_std_17)

find_library(log_lib log)

target_link_libraries(
    ardour_android
    PRIVATE
    oboe::oboe
    ${log_lib}
)
