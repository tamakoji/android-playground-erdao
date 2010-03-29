# Copyright (C) 2010 Huan Erdao
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

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
