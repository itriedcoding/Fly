cmake_minimum_required(VERSION 3.22.1)

project(flycore C CXX)

add_library(flycore SHARED
        native-lib.cpp
        flycore.c)

set_target_properties(flycore PROPERTIES
        C_STANDARD 11
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON)

target_compile_options(flycore PRIVATE -Wall -Wextra -O2 -fvisibility=hidden)

find_library(fly_log log)
target_link_libraries(flycore ${fly_log} m)
