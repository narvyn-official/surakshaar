cmake_minimum_required(VERSION 3.22.1)
project(suraksha_cardboard LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CARDBOARDSDK_RENDERING_GLESv3 OFF CACHE BOOL "" FORCE)
set(CARDBOARDSDK_RENDERING_VULKAN OFF CACHE BOOL "" FORCE)
set(CARDBOARDSDK_UNITY_PLUGIN OFF CACHE BOOL "" FORCE)
set(CARDBOARD_SOURCE "${CMAKE_CURRENT_SOURCE_DIR}/../../../../cardboard-sdk/vendor/cardboard/sdk")
add_subdirectory("${CARDBOARD_SOURCE}" "${CMAKE_CURRENT_BINARY_DIR}/cardboard")
# Upstream's GLES option controls linking, but its source glob still includes the ES3
# renderer. Exclude that unused implementation rather than requiring GLES3 in ES2 mode.
get_target_property(CARDBOARD_FILES GfxPluginCardboard SOURCES)
list(FILTER CARDBOARD_FILES EXCLUDE REGEX "opengl_es3_distortion_renderer\\.cc$")
set_property(TARGET GfxPluginCardboard PROPERTY SOURCES "${CARDBOARD_FILES}")
# The upstream export map names Unity/Vulkan entrypoints even when disabled. NDK27
# rejects those missing symbols, so use an equivalent map for the enabled C API.
set_property(TARGET GfxPluginCardboard PROPERTY LINK_FLAGS
    " -Wl,--version-script=\"${CMAKE_CURRENT_SOURCE_DIR}/cardboard.exports.map\"")
set_property(TARGET GfxPluginCardboard PROPERTY LINK_DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/cardboard.exports.map")
add_library(suraksha_cardboard SHARED cardboard_bridge.cpp)
target_include_directories(suraksha_cardboard PRIVATE "${CARDBOARD_SOURCE}/include")
target_compile_options(suraksha_cardboard PRIVATE -Wall -Wextra -Werror=return-type)
target_link_libraries(suraksha_cardboard GfxPluginCardboard GLESv2 android log)
# Match Android's current 16 KiB native-page distribution requirement on supported ABIs.
target_link_options(suraksha_cardboard PRIVATE "-Wl,-z,max-page-size=16384")
target_link_options(GfxPluginCardboard PRIVATE "-Wl,-z,max-page-size=16384")
