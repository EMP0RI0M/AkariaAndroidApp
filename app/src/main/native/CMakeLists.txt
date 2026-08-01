cmake_minimum_required(VERSION 3.22.1)
project("akaria_engine")

# Engine sources
add_library(
        akaria_engine
        SHARED
        engine/jni_bridge.cpp
        # Will add model_manager.cpp, vision.cpp, etc. here later
)

find_library(
        log-lib
        log
)

target_link_libraries(
        akaria_engine
        ${log-lib}
)
