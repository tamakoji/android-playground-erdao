LOCAL_PATH:= $(call my-dir)

# imgutils
#
include $(CLEAR_VARS)

LOCAL_MODULE    := imgutils
LOCAL_SRC_FILES := ./imgutils/imgutils.c

include $(BUILD_STATIC_LIBRARY)

# this module
#
include $(CLEAR_VARS)

LOCAL_MODULE    := laughingman-jni
LOCAL_SRC_FILES := laughingman-jni.c

LOCAL_STATIC_LIBRARIES := imgutils

include $(BUILD_SHARED_LIBRARY)
