LOCAL_PATH:= $(call my-dir)

# imgutils
#
include $(CLEAR_VARS)

LOCAL_MODULE    := ../imgutils/imgutils
LOCAL_SRC_FILES := ../imgutils/imgutils.c

# this module
#
include $(CLEAR_VARS)

LOCAL_MODULE    := snapface-jni
LOCAL_SRC_FILES := snapface-jni.c

LOCAL_STATIC_LIBRARIES := imgutils

include $(BUILD_SHARED_LIBRARY)
