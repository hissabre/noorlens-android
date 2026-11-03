cmake_minimum_required(VERSION 3.22.1)
project(basirahwhisper LANGUAGES C CXX)

# Toujours compiler en optimisé (sinon la reconnaissance est ~20x plus lente).
set(CMAKE_BUILD_TYPE Release CACHE STRING "" FORCE)
set(CMAKE_C_FLAGS_RELEASE "-O3 -DNDEBUG")
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -DNDEBUG")

if(ANDROID_ABI STREQUAL "arm64-v8a")
    # Quest 2 / Quest 3 : Cortex-A77 et plus récents (fp16 + dotprod)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv8.2-a+fp16+dotprod")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8.2-a+fp16+dotprod")
endif()

set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)

# whisper.cpp est téléchargé par le workflow GitHub dans ce dossier
add_subdirectory(whisper.cpp ${CMAKE_BINARY_DIR}/whisper.cpp)

add_library(basirahwhisper SHARED whisper_jni.cpp)
target_include_directories(basirahwhisper PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/include ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp/ggml/include)
if(ANDROID)
    target_link_libraries(basirahwhisper whisper log android)
else()
    target_link_libraries(basirahwhisper whisper)
endif()
