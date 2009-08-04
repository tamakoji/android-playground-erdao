LOCAL_PATH:= $(call my-dir)

# imgutils
#
include $(CLEAR_VARS)

LOCAL_MODULE    := imgutils
LOCAL_SRC_FILES := imgutils.c

include $(BUILD_STATIC_LIBRARY)
