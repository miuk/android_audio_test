cmake_minimum_required(VERSION 3.4.1)

add_library(udpsocket SHARED
            udpsocket.c)

# Include libraries needed
target_link_libraries(udpsocket
                      android
                      log)
