LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

MY_CPP_LIST := $(wildcard $(LOCAL_PATH)/librtmp/*.c)
MY_CPP_LIST += libresrtmp.c

LOCAL_SRC_FILES := $(MY_CPP_LIST)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/librtmp
LOCAL_C_INCLUDES += libresrtmp.h
LOCAL_C_INCLUDES += log.h

LOCAL_CFLAGS += -DNO_CRYPTO

LOCAL_MODULE := libresrtmp


LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)