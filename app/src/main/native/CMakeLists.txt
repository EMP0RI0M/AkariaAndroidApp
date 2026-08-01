cmake_minimum_required(VERSION 3.22.1)
project("akaria_engine")

# Engine sources
add_library(
        akaria_engine
        SHARED
        engine/jni_bridge.cpp
        # Will add model_manager.cpp, vision.cpp, etc. here later
)

# Add llama.cpp as a static library
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Don't build tests")
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Don't build examples")
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "Don't build server")
add_subdirectory(third_party/llama.cpp)

find_library(
        log-lib
        log
)

target_link_libraries(
        akaria_engine
        ${log-lib}
        llama
)
